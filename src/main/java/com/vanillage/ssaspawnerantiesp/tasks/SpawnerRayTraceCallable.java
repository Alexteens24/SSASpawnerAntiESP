package com.vanillage.ssaspawnerantiesp.tasks;

import java.util.List;
import java.util.concurrent.Callable;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.Vector;

import com.vanillage.ssaspawnerantiesp.SSASpawnerAntiESP;
import com.vanillage.ssaspawnerantiesp.data.PlayerSpawnerData;
import com.vanillage.ssaspawnerantiesp.data.VectorialLocation;
import com.vanillage.ssaspawnerantiesp.data.VisibilityResult;
import com.vanillage.ssaspawnerantiesp.index.SpawnerIndex;
import com.vanillage.ssaspawnerantiesp.occlusion.SolidOcclusionMask;
import com.vanillage.ssaspawnerantiesp.occlusion.WorldOcclusionGetter;
import com.vanillage.ssaspawnerantiesp.util.BlockIterator;
import com.vanillage.ssaspawnerantiesp.util.BlockOcclusionCulling;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class SpawnerRayTraceCallable implements Callable<Void> {
    private final SSASpawnerAntiESP plugin;
    private final PlayerSpawnerData playerData;
    private final SpawnerIndex index;
    private final SolidOcclusionMask solidMask;

    public SpawnerRayTraceCallable(SSASpawnerAntiESP plugin, PlayerSpawnerData playerData, SpawnerIndex index, SolidOcclusionMask solidMask) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.index = index;
        this.solidMask = solidMask;
    }

    @Override
    public Void call() {
        VectorialLocation eye = playerData.getEyeLocation();
        World world = eye.getWorld();

        if (world == null || !plugin.isEnabled(world)) {
            return null;
        }

        Level level = ((CraftWorld) world).getHandle();
        WorldOcclusionGetter occlusionGetter = new WorldOcclusionGetter(level, solidMask);
        BlockOcclusionCulling culling = new BlockOcclusionCulling(
            BlockIterator::new,
            occlusionGetter,
            true,
            plugin.isSectionLeapEnabled(world)
        );

        Vector eyeVector = eye.getVector();
        double eyeX = eyeVector.getX();
        double eyeY = eyeVector.getY();
        double eyeZ = eyeVector.getZ();
        double rayTraceDistance = plugin.getRayTraceDistance(world);
        double rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;
        double rehideDistanceSquared = plugin.getRehideDistanceSquared(world);
        boolean rehideBlocks = plugin.isRehideBlocks(world);

        List<BlockPos> targets = index.queryNear(world, eyeX, eyeY, eyeZ, rayTraceDistance);

        for (BlockPos block : targets) {
            occlusionGetter.clearCache();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            double centerX = x + 0.5;
            double centerY = y + 0.5;
            double centerZ = z + 0.5;
            double diffX = eyeX - centerX;
            double diffY = eyeY - centerY;
            double diffZ = eyeZ - centerZ;
            double distanceSquared = diffX * diffX + diffY * diffY + diffZ * diffZ;

            if (distanceSquared > rayTraceDistanceSquared) {
                continue;
            }

            boolean visible = false;

            if (!rehideBlocks || distanceSquared < rehideDistanceSquared) {
                Vector direction = eye.getDirection();
                visible = culling.isVisible(
                    x,
                    y,
                    z,
                    eyeX,
                    eyeY,
                    eyeZ,
                    diffX,
                    diffY,
                    diffZ,
                    distanceSquared,
                    direction.getX(),
                    direction.getY(),
                    direction.getZ()
                );
            }

            long key = PlayerSpawnerData.blockKey(block);
            Boolean hidden = playerData.getHiddenByClient().get(key);

            if (visible) {
                if (hidden != null && hidden) {
                    playerData.getResults().add(new VisibilityResult(block, true));
                    playerData.getHiddenByClient().put(key, false);
                } else if (hidden == null) {
                    playerData.getHiddenByClient().put(key, false);
                }
            } else if (hidden == null || !hidden) {
                playerData.getResults().add(new VisibilityResult(block, false));
                playerData.getHiddenByClient().put(key, true);
            }
        }

        occlusionGetter.clearCache();
        return null;
    }
}
