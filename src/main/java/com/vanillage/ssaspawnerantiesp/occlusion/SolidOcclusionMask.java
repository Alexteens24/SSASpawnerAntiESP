package com.vanillage.ssaspawnerantiesp.occlusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;

public final class SolidOcclusionMask {
    private static final Palette<BlockState> GLOBAL_BLOCKSTATE_PALETTE = new GlobalPalette<>(Block.BLOCK_STATE_REGISTRY);
    private final boolean[] solid;

    public SolidOcclusionMask() {
        solid = new boolean[Block.BLOCK_STATE_REGISTRY.size()];
        EmptyLevelChunk emptyChunk = new EmptyLevelChunk(
            MinecraftServer.getServer().overworld(),
            new ChunkPos(0, 0),
            MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)
        );
        BlockPos zeroPos = new BlockPos(0, 0, 0);

        for (int i = 0; i < solid.length; i++) {
            BlockState blockState = GLOBAL_BLOCKSTATE_PALETTE.valueFor(i);

            if (blockState != null) {
                Block block = blockState.getBlock();
                solid[i] = blockState.isRedstoneConductor(emptyChunk, zeroPos)
                    && block != Blocks.SPAWNER
                    && block != Blocks.BARRIER
                    && block != Blocks.SHULKER_BOX
                    && block != Blocks.SLIME_BLOCK
                    && block != Blocks.MANGROVE_ROOTS;
            }
        }
    }

    public boolean isSolid(BlockState blockState) {
        if (blockState == null || blockState.isAir()) {
            return false;
        }

        return solid[GLOBAL_BLOCKSTATE_PALETTE.idFor(blockState, PaletteResize.noResizeExpected())];
    }
}
