package com.vanillage.ssaspawnerantiesp.data;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class VectorialLocation {
    private final Reference<World> world;
    private final Vector vector;
    private final Vector direction;

    public VectorialLocation(Location location) {
        this(location.getWorld(), location.toVector(), location.getDirection());
    }

    public VectorialLocation(World world, Vector vector, Vector direction) {
        this.world = new WeakReference<>(world);
        this.vector = vector;
        this.direction = direction;
    }

    public World getWorld() {
        return world.get();
    }

    public Vector getVector() {
        return vector;
    }

    public Vector getDirection() {
        return direction;
    }

    /**
     * @return {@code false} when the eye location is in a different world and this instance must be replaced
     */
    public boolean syncFrom(Location eyeLocation) {
        World world = eyeLocation.getWorld();

        if (world == null) {
            return false;
        }

        World currentWorld = getWorld();

        if (currentWorld != null && !currentWorld.equals(world)) {
            return false;
        }

        vector.setX(eyeLocation.getX());
        vector.setY(eyeLocation.getY());
        vector.setZ(eyeLocation.getZ());
        Vector lookDirection = eyeLocation.getDirection();
        direction.setX(lookDirection.getX());
        direction.setY(lookDirection.getY());
        direction.setZ(lookDirection.getZ());
        return true;
    }
}
