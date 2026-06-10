package com.vanillage.ssaspawnerantiesp;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vanillage.ssaspawnerantiesp.commands.SSASpawnerAntiESPTabExecutor;
import com.vanillage.ssaspawnerantiesp.data.PlayerSpawnerData;
import com.vanillage.ssaspawnerantiesp.index.SpawnerIndex;
import com.vanillage.ssaspawnerantiesp.listeners.PlayerListener;
import com.vanillage.ssaspawnerantiesp.listeners.SpawnerListener;
import com.vanillage.ssaspawnerantiesp.occlusion.SolidOcclusionMask;
import com.vanillage.ssaspawnerantiesp.tasks.SpawnerRayTraceTimerTask;

import github.nighter.smartspawner.api.SmartSpawnerAPI;
import github.nighter.smartspawner.api.SmartSpawnerProvider;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.core.BlockPos;

public final class SSASpawnerAntiESP extends JavaPlugin {
    private final ConcurrentMap<UUID, PlayerSpawnerData> playerData = new ConcurrentHashMap<>();
    private final SpawnerIndex spawnerIndex = new SpawnerIndex();
    private SolidOcclusionMask solidMask;
    private SmartSpawnerAPI smartSpawnerApi;
    private ExecutorService executorService;
    private ScheduledTask rayTraceScheduledTask;
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
        solidMask = new SolidOcclusionMask();
        spawnerIndex.loadAll(smartSpawnerApi);
        running = true;
        startRayTraceSchedulerFromConfig(getConfig());

        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, solidMask), this);
        PlayerListener.registerExistingPlayers(this, solidMask);

        SSASpawnerAntiESPTabExecutor commandExecutor = new SSASpawnerAntiESPTabExecutor(this);
        getCommand("ssaspawnerantiesp").setExecutor(commandExecutor);
        getCommand("ssaspawnerantiesp").setTabCompleter(commandExecutor);

        getLogger().info(getPluginMeta().getDisplayName() + " enabled (" + spawnerIndex.totalSpawners() + " spawners indexed)");
    }

    @Override
    public void onDisable() {
        running = false;

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

        PlayerListener.unregisterAll(this);
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
        PlayerListener.unregisterAll(this);
        PlayerListener.registerExistingPlayers(this, solidMask);
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

    public boolean isRunning() {
        return running;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ConcurrentMap<UUID, PlayerSpawnerData> getPlayerData() {
        return playerData;
    }

    public SpawnerIndex getSpawnerIndex() {
        return spawnerIndex;
    }

    public long getUpdateTicks() {
        return updateTicks;
    }

    public boolean isEnabled(World world) {
        if (world == null) {
            return false;
        }

        return getConfig().getBoolean("world-settings." + world.getName() + ".enabled",
            getConfig().getBoolean("world-settings.default.enabled", true));
    }

    public double getRayTraceDistance(World world) {
        return getConfig().getDouble("world-settings." + world.getName() + ".ray-trace-distance",
            getConfig().getDouble("world-settings.default.ray-trace-distance", 64.0));
    }

    public boolean isRehideBlocks(World world) {
        return getConfig().getBoolean("world-settings." + world.getName() + ".rehide-blocks",
            getConfig().getBoolean("world-settings.default.rehide-blocks", true));
    }

    public double getRehideDistanceSquared(World world) {
        double distance = getConfig().getDouble("world-settings." + world.getName() + ".rehide-distance",
            getConfig().getDouble("world-settings.default.rehide-distance", 60.0));
        return distance * distance;
    }

    public boolean isSectionLeapEnabled(World world) {
        return getConfig().getBoolean("world-settings." + world.getName() + ".section-leap",
            getConfig().getBoolean("world-settings.default.section-leap", false));
    }

    public void clearClientStateFor(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        long key = PlayerSpawnerData.blockKey(pos);

        for (PlayerSpawnerData data : playerData.values()) {
            data.getHiddenByClient().remove(key);
        }
    }
}
