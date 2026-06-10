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

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;
import com.vanillage.ssaspawnerantiesp.data.PlayerSpawnerData;
import com.vanillage.ssaspawnerantiesp.data.VectorialLocation;
import com.vanillage.ssaspawnerantiesp.occlusion.SolidOcclusionMask;
import com.vanillage.ssaspawnerantiesp.tasks.BlockUpdateTask;
import com.vanillage.ssaspawnerantiesp.tasks.SpawnerRayTraceCallable;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class PlayerListener implements Listener {
    private final SSASpawnerAntiESP plugin;
    private final SolidOcclusionMask solidMask;

    public PlayerListener(SSASpawnerAntiESP plugin, SolidOcclusionMask solidMask) {
        this.plugin = plugin;
        this.solidMask = solidMask;
    }

    public static void registerExistingPlayers(SSASpawnerAntiESP plugin, SolidOcclusionMask solidMask) {
        PlayerListener listener = new PlayerListener(plugin, solidMask);

        for (Player player : Bukkit.getOnlinePlayers()) {
            listener.register(player);
        }
    }

    public static void unregisterAll(SSASpawnerAntiESP plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerSpawnerData data = plugin.getPlayerData().remove(id);

            if (data != null && data.getBlockUpdateTask() != null) {
                data.getBlockUpdateTask().cancel();
            }
        }
    }

    private void register(Player player) {
        if (!player.isOnline()) {
            return;
        }

        if (plugin.getPlayerData().containsKey(player.getUniqueId())) {
            return;
        }

        Location eye = player.getEyeLocation();
        PlayerSpawnerData playerData = new PlayerSpawnerData(new VectorialLocation(eye));
        playerData.setCallable(new SpawnerRayTraceCallable(plugin, playerData, plugin.getSpawnerIndex(), solidMask));
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
                if (!player.isOnline()) {
                    return;
                }

                PlayerSpawnerData data = plugin.getPlayerData().get(player.getUniqueId());

                if (data == null) {
                    return;
                }

                PlayerSpawnerData.syncEyeFromPlayer(player, data);
                BlockUpdateTask.hideNearbySpawners(plugin, player, data);
            },
            null
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        register(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerSpawnerData data = plugin.getPlayerData().remove(id);

        if (data != null && data.getBlockUpdateTask() != null) {
            data.getBlockUpdateTask().cancel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()
            && from.getYaw() == to.getYaw() && from.getPitch() == to.getPitch()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerSpawnerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (playerData == null) {
            return;
        }

        Location eye = to.clone();
        eye.setY(eye.getY() + player.getEyeHeight());
        VectorialLocation location = playerData.getEyeLocation();

        if (!location.syncFrom(eye)) {
            playerData.setEyeLocation(new VectorialLocation(eye));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerSpawnerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (playerData == null) {
            return;
        }

        player.getScheduler().run(
            plugin,
            task -> {
                if (!player.isOnline()) {
                    return;
                }

                PlayerSpawnerData data = plugin.getPlayerData().get(player.getUniqueId());

                if (data == null) {
                    return;
                }

                PlayerSpawnerData.syncEyeFromPlayer(player, data);
                BlockUpdateTask.hideNearbySpawners(plugin, player, data);
            },
            null
        );
    }
}
