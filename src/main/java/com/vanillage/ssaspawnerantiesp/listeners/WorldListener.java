package com.vanillage.ssaspawnerantiesp.listeners;

import java.lang.reflect.Field;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;
import com.vanillage.ssaspawnerantiesp.antixray.SpawnerChunkPacketBlockController;

import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.configuration.WorldConfiguration.Anticheat.AntiXray;
import io.papermc.paper.configuration.type.EngineMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class WorldListener implements Listener {
    private final SSASpawnerAntiESP plugin;

    public WorldListener(SSASpawnerAntiESP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        handleLoad(plugin, event.getWorld());
    }

    public static void handleLoad(SSASpawnerAntiESP plugin, World world) {
        FileConfiguration config = plugin.getConfig();
        String worldName = world.getName();
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        ChunkPacketBlockController controller;

        if (plugin.isEnabled(world)) {
            boolean rayTraceThirdPerson = config.getBoolean("world-settings." + worldName + ".ray-trace-third-person",
                config.getBoolean("world-settings.default.ray-trace-third-person", false));
            double rayTraceDistance = Math.max(config.getDouble("world-settings." + worldName + ".ray-trace-distance",
                config.getDouble("world-settings.default.ray-trace-distance", 64.0)), 0.);
            boolean rehideBlocks = config.getBoolean("world-settings." + worldName + ".rehide-blocks",
                config.getBoolean("world-settings.default.rehide-blocks", true));
            double rehideDistance = Math.max(config.getDouble("world-settings." + worldName + ".rehide-distance",
                config.getDouble("world-settings.default.rehide-distance", 60.0)), 0.);
            boolean sectionLeap = config.getBoolean("world-settings." + worldName + ".section-leap",
                config.getBoolean("world-settings.default.section-leap", false));
            int maxRayTraceBlockCountPerChunk = Math.max(config.getInt("world-settings." + worldName + ".max-ray-trace-block-count-per-chunk",
                config.getInt("world-settings.default.max-ray-trace-block-count-per-chunk", 64)), 0);
            controller = new SpawnerChunkPacketBlockController(
                plugin,
                rayTraceThirdPerson,
                rayTraceDistance,
                rehideBlocks,
                rehideDistance,
                sectionLeap,
                maxRayTraceBlockCountPerChunk,
                serverLevel,
                MinecraftServer.getServer().executor
            );
        } else if (paperUsesEngineModeHide(serverLevel)) {
            controller = new io.papermc.paper.antixray.ChunkPacketBlockControllerAntiXray(serverLevel, MinecraftServer.getServer().executor);
        } else {
            controller = ChunkPacketBlockController.NO_OPERATION_INSTANCE;
        }

        setChunkPacketBlockController(serverLevel, controller);
    }

    private static boolean paperUsesEngineModeHide(ServerLevel serverLevel) {
        AntiXray antiXray = serverLevel.paperConfig().anticheat.antiXray;
        return antiXray.enabled && antiXray.engineMode == EngineMode.HIDE;
    }

    private static void setChunkPacketBlockController(ServerLevel serverLevel, ChunkPacketBlockController controller) {
        try {
            Field field = Level.class.getDeclaredField("chunkPacketBlockController");
            field.setAccessible(true);
            field.set(serverLevel, controller);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
