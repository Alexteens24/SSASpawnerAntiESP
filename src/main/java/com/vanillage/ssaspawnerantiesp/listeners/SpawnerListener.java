package com.vanillage.ssaspawnerantiesp.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;

import github.nighter.smartspawner.api.events.SpawnerBreakEvent;
import github.nighter.smartspawner.api.events.SpawnerPlaceEvent;

public final class SpawnerListener implements Listener {
    private final SSASpawnerAntiESP plugin;

    public SpawnerListener(SSASpawnerAntiESP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerPlace(SpawnerPlaceEvent event) {
        plugin.getSpawnerIndex().add(event.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerBreak(SpawnerBreakEvent event) {
        Location location = event.getLocation();
        boolean stillSpawner = location.getBlock().getType() == Material.SPAWNER;
        plugin.onSpawnerBlockChanged(location, stillSpawner);
    }
}
