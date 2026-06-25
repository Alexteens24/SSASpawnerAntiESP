package com.vanillage.ssaspawnerantiesp.nms;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;

public final class NmsCompat {
    private NmsCompat() {
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return ChunkPos.pack(chunkX, chunkZ);
    }

    public static long chunkPosKey(ChunkPos chunkPos) {
        return chunkPos.pack();
    }

    public static int chunkX(ChunkPos chunkPos) {
        return chunkPos.x();
    }

    public static int chunkZ(ChunkPos chunkPos) {
        return chunkPos.z();
    }

    public static boolean isConnectionDisconnected(ServerGamePacketListenerImpl connection) {
        return connection.isDisconnected();
    }
}
