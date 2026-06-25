package com.vanillage.ssaspawnerantiesp.data;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;

import com.vanillage.ssaspawnerantiesp.nms.NmsCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkBlocks {
    private final Reference<LevelChunk> chunk;
    private final LongWrapper key;
    private final Map<BlockPos, Boolean> blocks;

    public ChunkBlocks(LevelChunk chunk, Map<BlockPos, Boolean> blocks) {
        this.chunk = new WeakReference<>(chunk);
        key = new LongWrapper(NmsCompat.chunkPosKey(chunk.getPos()));
        this.blocks = blocks;
    }

    public LevelChunk getChunk() {
        return chunk.get();
    }

    public LongWrapper getKey() {
        return key;
    }

    public Map<BlockPos, Boolean> getBlocks() {
        return blocks;
    }
}
