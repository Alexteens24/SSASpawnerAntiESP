package com.vanillage.ssaspawnerantiesp.antixray;

import io.papermc.paper.antixray.ChunkPacketInfo;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class SpawnerChunkPacketInfo extends ChunkPacketInfo<BlockState> implements Runnable {

    private final SpawnerChunkPacketBlockController controller;
    private final ServerPlayer targetPlayer;
    private LevelChunk[] nearbyChunks;

    public SpawnerChunkPacketInfo(
        ClientboundLevelChunkWithLightPacket chunkPacket,
        LevelChunk chunk,
        SpawnerChunkPacketBlockController controller,
        ServerPlayer targetPlayer
    ) {
        super(chunkPacket, chunk);
        this.controller = controller;
        this.targetPlayer = targetPlayer;
    }

    public ServerPlayer getTargetPlayer() {
        return targetPlayer;
    }

    public LevelChunk[] getNearbyChunks() {
        return nearbyChunks;
    }

    public void setNearbyChunks(LevelChunk... nearbyChunks) {
        this.nearbyChunks = nearbyChunks;
    }

    @Override
    public void run() {
        controller.obfuscate(this);
    }
}
