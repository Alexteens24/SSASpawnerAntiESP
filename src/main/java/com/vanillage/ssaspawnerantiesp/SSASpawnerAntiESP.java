package com.vanillage.ssaspawnerantiesp;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vanillage.ssaspawnerantiesp.antixray.SpawnerChunkPacketBlockController;
import com.vanillage.ssaspawnerantiesp.commands.SSASpawnerAntiESPTabExecutor;
import com.vanillage.ssaspawnerantiesp.compat.LeafAsyncChunkSendCompat;
import com.vanillage.ssaspawnerantiesp.data.ChunkBlocks;
import com.vanillage.ssaspawnerantiesp.data.PlayerData;
import com.vanillage.ssaspawnerantiesp.data.VectorialLocation;
import com.vanillage.ssaspawnerantiesp.index.SpawnerIndex;
import com.vanillage.ssaspawnerantiesp.listeners.PacketListener;
import com.vanillage.ssaspawnerantiesp.listeners.PlayerListener;
import com.vanillage.ssaspawnerantiesp.listeners.SpawnerListener;
import com.vanillage.ssaspawnerantiesp.listeners.WorldListener;
import com.vanillage.ssaspawnerantiesp.nms.NmsCompat;
import com.vanillage.ssaspawnerantiesp.tasks.BlockUpdateTask;
import com.vanillage.ssaspawnerantiesp.tasks.SpawnerRayTraceTimerTask;

import github.nighter.smartspawner.api.SmartSpawnerAPI;
import github.nighter.smartspawner.api.SmartSpawnerProvider;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SSASpawnerAntiESP extends JavaPlugin {
    private final ConcurrentMap<UUID, ConcurrentMap<Long, ConcurrentLinkedQueue<ChunkBlocks>>> pendingChunkBlocksByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private final SpawnerIndex spawnerIndex = new SpawnerIndex();
    private SmartSpawnerAPI smartSpawnerApi;
    private ExecutorService executorService;
    private ScheduledTask rayTraceScheduledTask;
    private PacketListenerCommon packetEventsChunkListener;
    private volatile boolean running;
    private long updateTicks = 1L;

    @Override
    public void onEnable() {
        smartSpawnerApi = SmartSpawnerProvider.getAPI();

        if (smartSpawnerApi == null) {
            getLogger().severe("SmartSpawner API not available — disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        spawnerIndex.loadAll(smartSpawnerApi);
        running = true;
        startRayTraceSchedulerFromConfig(getConfig());

        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        PlayerListener.registerExistingPlayers(this);

        for (World world : Bukkit.getWorlds()) {
            WorldListener.handleLoad(this, world);
        }

        packetEventsChunkListener = new PacketListener(this);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(packetEventsChunkListener);

        SSASpawnerAntiESPTabExecutor commandExecutor = new SSASpawnerAntiESPTabExecutor(this);
        getCommand("ssaspawnerantiesp").setExecutor(commandExecutor);
        getCommand("ssaspawnerantiesp").setTabCompleter(commandExecutor);

        getLogger().info(getPluginMeta().getDisplayName() + " enabled (" + spawnerIndex.totalSpawners() + " spawners indexed)");
        LeafAsyncChunkSendCompat.logStatus(getLogger());
    }

    @Override
    public void onDisable() {
        running = false;

        if (packetEventsChunkListener != null) {
            try {
                com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().unregisterListener(packetEventsChunkListener);
                packetEventsChunkListener = null;
            } catch (Throwable ignored) {
                // PacketEvents may already be torn down during shutdown.
            }
        }

        if (rayTraceScheduledTask != null) {
            rayTraceScheduledTask.cancel();
            rayTraceScheduledTask = null;
        }

        if (executorService != null) {
            executorService.shutdownNow();

            try {
                executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        pendingChunkBlocksByPlayer.clear();

        for (PlayerData data : playerData.values()) {
            if (data.getBlockUpdateTask() != null) {
                data.getBlockUpdateTask().cancel();
            }
        }

        playerData.clear();
        getLogger().info(getPluginMeta().getDisplayName() + " disabled");
    }

    public void reloadPluginConfiguration() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("reloadPluginConfiguration must be called from the server thread");
        }

        reloadConfig();
        stopRayTraceScheduler();
        startRayTraceSchedulerFromConfig(getConfig());
        spawnerIndex.loadAll(smartSpawnerApi);

        for (World world : Bukkit.getWorlds()) {
            WorldListener.handleLoad(this, world);
        }

        PlayerListener.unregisterAndReregisterAll(this);
    }

    private void startRayTraceSchedulerFromConfig(FileConfiguration config) {
        config.options().copyDefaults(true);
        int threads = config.getInt("settings.ray-trace-threads", 1);
        long msPerTick = config.getLong("settings.ms-per-ray-trace-tick", 50L);
        updateTicks = config.getLong("settings.update-ticks", 1L);

        executorService = Executors.newFixedThreadPool(
            threads,
            new ThreadFactoryBuilder().setNameFormat("SSASpawnerAntiESP - %d").build()
        );

        rayTraceScheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
            this,
            new SpawnerRayTraceTimerTask(this),
            msPerTick,
            msPerTick,
            TimeUnit.MILLISECONDS
        );
    }

    private void stopRayTraceScheduler() {
        if (rayTraceScheduledTask != null) {
            rayTraceScheduledTask.cancel();
            rayTraceScheduledTask = null;
        }

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    public void enqueuePendingChunkBlocks(UUID playerId, int chunkX, int chunkZ, ChunkBlocks chunkBlocks) {
        long chunkKey = NmsCompat.chunkKey(chunkX, chunkZ);
        pendingChunkBlocksByPlayer
            .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(chunkKey, ignored -> new ConcurrentLinkedQueue<>())
            .add(chunkBlocks);
    }

    public ChunkBlocks pollPendingChunkBlocks(UUID playerId, int chunkX, int chunkZ) {
        long chunkKey = NmsCompat.chunkKey(chunkX, chunkZ);
        ConcurrentMap<Long, ConcurrentLinkedQueue<ChunkBlocks>> byChunk = pendingChunkBlocksByPlayer.get(playerId);

        if (byChunk == null) {
            return null;
        }

        ConcurrentLinkedQueue<ChunkBlocks> queue = byChunk.get(chunkKey);
        return queue != null ? queue.poll() : null;
    }

    public void clearPendingChunkBlocksFor(UUID playerId) {
        pendingChunkBlocksByPlayer.remove(playerId);
    }

    public void replacePlayerData(UUID uniqueId, PlayerData newData) {
        PlayerData old = playerData.put(uniqueId, newData);

        if (old != null && old.getBlockUpdateTask() != null) {
            newData.setBlockUpdateTask(old.getBlockUpdateTask());
        }
    }

    public void onSpawnerBlockChanged(Location location, boolean stillSpawner) {
        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (!stillSpawner) {
            spawnerIndex.remove(location);
        }

        for (PlayerData data : playerData.values()) {
            BlockUpdateTask.removeBlockFromPlayerState(data, pos);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (location.getWorld() == null || !player.getWorld().equals(location.getWorld())) {
                continue;
            }

            player.getScheduler().run(
                this,
                task -> BlockUpdateTask.syncServerBlock(this, player, location),
                null
            );
        }
    }

    public boolean isRunning() {
        return running;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ConcurrentMap<UUID, PlayerData> getPlayerData() {
        return playerData;
    }

    public SpawnerIndex getSpawnerIndex() {
        return spawnerIndex;
    }

    public long getUpdateTicks() {
        return updateTicks;
    }

    public double getRayTraceDistance(World world) {
        if (world == null) {
            return 0.;
        }

        String worldName = world.getName();
        return Math.max(getConfig().getDouble("world-settings." + worldName + ".ray-trace-distance",
            getConfig().getDouble("world-settings.default.ray-trace-distance", 64.0)), 0.);
    }

    public boolean isEnabled(World world) {
        if (world == null) {
            return false;
        }

        return getConfig().getBoolean("world-settings." + world.getName() + ".enabled",
            getConfig().getBoolean("world-settings.default.enabled", true));
    }

    public boolean validatePlayer(Player player) {
        return !player.hasMetadata("NPC");
    }

    public boolean validatePlayerData(Player player, PlayerData playerData, String methodName) {
        if (playerData == null) {
            if (validatePlayer(player)) {
                Logger logger = getLogger();
                logger.warning("Missing player data for " + player.getName() + " in " + methodName);
            }

            return false;
        }

        return true;
    }

    public static VectorialLocation[] getLocations(Entity entity, VectorialLocation location) {
        World world = location.getWorld();
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) world).getHandle().chunkPacketBlockController;

        if (chunkPacketBlockController instanceof SpawnerChunkPacketBlockController controller && controller.rayTraceThirdPerson) {
            VectorialLocation thirdPersonFrontLocation = new VectorialLocation(location);
            thirdPersonFrontLocation.getDirection().multiply(-1.);
            return new VectorialLocation[] {
                location,
                move(entity, new VectorialLocation(world, location.getVector().clone(), location.getDirection())),
                move(entity, thirdPersonFrontLocation)
            };
        }

        return new VectorialLocation[] { location };
    }

    private static VectorialLocation move(Entity entity, VectorialLocation location) {
        location.getVector().subtract(location.getDirection().clone().multiply(getMaxZoom(entity, location, 4.)));
        return location;
    }

    private static double getMaxZoom(Entity entity, VectorialLocation location, double maxZoom) {
        Vector vector = location.getVector();
        Vec3 position = new Vec3(vector.getX(), vector.getY(), vector.getZ());
        double positionX = position.x;
        double positionY = position.y;
        double positionZ = position.z;
        Vector direction = location.getDirection();
        double directionX = direction.getX();
        double directionY = direction.getY();
        double directionZ = direction.getZ();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();

        for (int i = 0; i < 8; i++) {
            float cornerX = (float) ((i & 1) * 2 - 1);
            float cornerY = (float) ((i >> 1 & 1) * 2 - 1);
            float cornerZ = (float) ((i >> 2 & 1) * 2 - 1);
            cornerX *= 0.1f;
            cornerY *= 0.1f;
            cornerZ *= 0.1f;
            Vec3 corner = position.add(cornerX, cornerY, cornerZ);
            Vec3 cornerMoved = new Vec3(
                positionX - directionX * maxZoom + cornerX,
                positionY - directionY * maxZoom + cornerY,
                positionZ - directionZ * maxZoom + cornerZ
            );
            BlockHitResult result = serverLevel.clip(new ClipContext(corner, cornerMoved, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, handle));

            if (result.getType() != HitResult.Type.MISS) {
                double zoom = result.getLocation().distanceTo(position);

                if (zoom < maxZoom) {
                    maxZoom = zoom;
                }
            }
        }

        return maxZoom;
    }
}
