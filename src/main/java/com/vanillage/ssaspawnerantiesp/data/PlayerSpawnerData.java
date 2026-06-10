package com.vanillage.ssaspawnerantiesp.data;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.core.BlockPos;

public final class PlayerSpawnerData implements Callable<Void> {
    private volatile VectorialLocation eyeLocation;
    private final ConcurrentMap<Long, Boolean> hiddenByClient = new ConcurrentHashMap<>();
    private final Queue<VisibilityResult> results = new ConcurrentLinkedQueue<>();
    private Callable<Void> callable;
    private volatile ScheduledTask blockUpdateTask;

    public PlayerSpawnerData(VectorialLocation eyeLocation) {
        this.eyeLocation = eyeLocation;
    }

    public VectorialLocation getEyeLocation() {
        return eyeLocation;
    }

    public void setEyeLocation(VectorialLocation eyeLocation) {
        this.eyeLocation = eyeLocation;
    }

    public ConcurrentMap<Long, Boolean> getHiddenByClient() {
        return hiddenByClient;
    }

    public Queue<VisibilityResult> getResults() {
        return results;
    }

    public void setCallable(Callable<Void> callable) {
        this.callable = callable;
    }

    public ScheduledTask getBlockUpdateTask() {
        return blockUpdateTask;
    }

    public void setBlockUpdateTask(ScheduledTask blockUpdateTask) {
        this.blockUpdateTask = blockUpdateTask;
    }

    public static long blockKey(BlockPos pos) {
        return pos.asLong();
    }

    public static void syncEyeFromPlayer(Player player, PlayerSpawnerData playerData) {
        Location eye = player.getEyeLocation();
        VectorialLocation location = playerData.getEyeLocation();

        if (!location.syncFrom(eye)) {
            playerData.setEyeLocation(new VectorialLocation(eye));
        }
    }

    @Override
    public Void call() throws Exception {
        return callable.call();
    }
}
