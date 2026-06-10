package com.vanillage.ssaspawnerantiesp.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.vanillage.ssaspawnerantiesp.nms.NmsCompat;
import com.vanillage.ssaspawnerantiesp.occlusion.WorldOcclusionGetter;

import github.nighter.smartspawner.api.SmartSpawnerAPI;
import github.nighter.smartspawner.api.data.SpawnerDataDTO;
import net.minecraft.core.BlockPos;

public final class SpawnerIndex {
    private final ConcurrentMap<UUID, ConcurrentMap<Long, CopyOnWriteArrayList<BlockPos>>> byWorld = new ConcurrentHashMap<>();

    public void loadAll(SmartSpawnerAPI api) {
        byWorld.clear();

        for (SpawnerDataDTO spawner : api.getAllSpawners()) {
            add(spawner.getLocation());
        }
    }

    public void add(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        UUID worldId = location.getWorld().getUID();
        long chunkKey = WorldOcclusionGetter.chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
        CopyOnWriteArrayList<BlockPos> list = byWorld
            .computeIfAbsent(worldId, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(chunkKey, ignored -> new CopyOnWriteArrayList<>());

        if (list.stream().noneMatch(pos::equals)) {
            list.add(pos);
        }
    }

    public void remove(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        UUID worldId = location.getWorld().getUID();
        ConcurrentMap<Long, CopyOnWriteArrayList<BlockPos>> chunks = byWorld.get(worldId);

        if (chunks == null) {
            return;
        }

        long chunkKey = WorldOcclusionGetter.chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
        CopyOnWriteArrayList<BlockPos> list = chunks.get(chunkKey);

        if (list != null) {
            list.removeIf(block -> block.equals(pos));

            if (list.isEmpty()) {
                chunks.remove(chunkKey, list);
            }
        }

        if (chunks.isEmpty()) {
            byWorld.remove(worldId, chunks);
        }
    }

    public List<BlockPos> queryNear(World world, double x, double y, double z, double maxDistance) {
        if (world == null) {
            return Collections.emptyList();
        }

        ConcurrentMap<Long, CopyOnWriteArrayList<BlockPos>> chunks = byWorld.get(world.getUID());

        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkRadius = (int) Math.ceil(maxDistance / 16.0);
        int centerChunkX = ((int) Math.floor(x)) >> 4;
        int centerChunkZ = ((int) Math.floor(z)) >> 4;
        double maxDistanceSquared = maxDistance * maxDistance;
        List<BlockPos> results = new ArrayList<>();

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                long chunkKey = NmsCompat.chunkKey(centerChunkX + dx, centerChunkZ + dz);
                CopyOnWriteArrayList<BlockPos> list = chunks.get(chunkKey);

                if (list == null) {
                    continue;
                }

                for (BlockPos pos : list) {
                    double centerX = pos.getX() + 0.5;
                    double centerY = pos.getY() + 0.5;
                    double centerZ = pos.getZ() + 0.5;
                    double diffX = x - centerX;
                    double diffY = y - centerY;
                    double diffZ = z - centerZ;
                    double distanceSquared = diffX * diffX + diffY * diffY + diffZ * diffZ;

                    if (distanceSquared <= maxDistanceSquared) {
                        results.add(pos);
                    }
                }
            }
        }

        return results;
    }

    public Collection<World> indexedWorlds() {
        List<World> worlds = new ArrayList<>();

        for (UUID worldId : byWorld.keySet()) {
            World world = Bukkit.getWorld(worldId);

            if (world != null) {
                worlds.add(world);
            }
        }

        return worlds;
    }

    public int totalSpawners() {
        int total = 0;

        for (ConcurrentMap<Long, CopyOnWriteArrayList<BlockPos>> chunks : byWorld.values()) {
            for (CopyOnWriteArrayList<BlockPos> list : chunks.values()) {
                total += list.size();
            }
        }

        return total;
    }
}
