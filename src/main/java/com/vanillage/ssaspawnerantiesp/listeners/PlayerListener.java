package com.vanillage.ssaspawnerantiesp.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;
import com.vanillage.ssaspawnerantiesp.data.PlayerData;
import com.vanillage.ssaspawnerantiesp.data.VectorialLocation;
import com.vanillage.ssaspawnerantiesp.tasks.BlockUpdateTask;
import com.vanillage.ssaspawnerantiesp.tasks.SpawnerRayTraceCallable;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class PlayerListener implements Listener {
    private final SSASpawnerAntiESP plugin;

    public PlayerListener(SSASpawnerAntiESP plugin) {
        this.plugin = plugin;
    }

    public static void registerExistingPlayers(SSASpawnerAntiESP plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            register(plugin, player);
        }
    }

    public static void unregisterAndReregisterAll(SSASpawnerAntiESP plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerData data = plugin.getPlayerData().remove(id);

            if (data != null && data.getBlockUpdateTask() != null) {
                data.getBlockUpdateTask().cancel();
            }

            plugin.clearPendingChunkBlocksFor(id);
        }

        registerExistingPlayers(plugin);
    }

    public static void register(SSASpawnerAntiESP plugin, Player player) {
        if (!plugin.validatePlayer(player)) {
            return;
        }

        if (plugin.getPlayerData().containsKey(player.getUniqueId())) {
            return;
        }

        PlayerData playerData = new PlayerData(SSASpawnerAntiESP.getLocations(player, new VectorialLocation(player.getEyeLocation())));
        playerData.setCallable(new SpawnerRayTraceCallable(plugin, playerData));
        plugin.getPlayerData().put(player.getUniqueId(), playerData);

        ScheduledTask updateTask = player.getScheduler().runAtFixedRate(
            plugin,
            new BlockUpdateTask(plugin, player),
            null,
            1L,
            plugin.getUpdateTicks()
        );
        playerData.setBlockUpdateTask(updateTask);

        player.getScheduler().run(
            plugin,
            task -> {
                if (player.isOnline()) {
                    BlockUpdateTask.hideNearbySpawners(plugin, player);
                }
            },
            null
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        register(plugin, event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerData data = plugin.getPlayerData().remove(id);

        if (data != null && data.getBlockUpdateTask() != null) {
            data.getBlockUpdateTask().cancel();
        }

        plugin.clearPendingChunkBlocksFor(id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        Location from = event.getFrom();

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()
            && from.getYaw() == to.getYaw() && from.getPitch() == to.getPitch()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (playerData == null) {
            return;
        }

        if (to.getWorld().equals(playerData.getLocations()[0].getWorld())) {
            VectorialLocation location = new VectorialLocation(to);
            location.getVector().setY(location.getVector().getY() + player.getEyeHeight());
            playerData.setLocations(SSASpawnerAntiESP.getLocations(player, location));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (playerData == null) {
            return;
        }

        player.getScheduler().run(
            plugin,
            task -> {
                if (!player.isOnline()) {
                    return;
                }

                PlayerData data = plugin.getPlayerData().get(player.getUniqueId());

                if (data == null) {
                    return;
                }

                data.setLocations(SSASpawnerAntiESP.getLocations(player, new VectorialLocation(player.getEyeLocation())));
                data.getChunks().clear();
                BlockUpdateTask.hideNearbySpawners(plugin, player);
            },
            null
        );
    }
}
