package com.vanillage.ssaspawnerantiesp.tasks;

import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class SpawnerRayTraceTimerTask implements Consumer<ScheduledTask> {
    private final SSASpawnerAntiESP plugin;

    public SpawnerRayTraceTimerTask(SSASpawnerAntiESP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void accept(ScheduledTask scheduledTask) {
        if (!plugin.isRunning()) {
            return;
        }

        try {
            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {
            if (plugin.isRunning()) {
                plugin.getLogger().log(Level.WARNING, "Ray trace pool rejected a tick", e);
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Error while scheduling spawner ray trace tasks", t);
        }
    }
}
