package com.vanillage.ssaspawnerantiesp.data;

import net.minecraft.core.BlockPos;

public final class VisibilityResult {
    private final BlockPos block;
    private final boolean visible;

    public VisibilityResult(BlockPos block, boolean visible) {
        this.block = block;
        this.visible = visible;
    }

    public BlockPos getBlock() {
        return block;
    }

    public boolean isVisible() {
        return visible;
    }
}
