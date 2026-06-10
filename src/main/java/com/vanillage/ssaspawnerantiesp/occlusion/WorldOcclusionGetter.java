package com.vanillage.ssaspawnerantiesp.occlusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import com.vanillage.ssaspawnerantiesp.nms.NmsCompat;
import com.vanillage.ssaspawnerantiesp.util.BlockOcclusionCulling;

public final class WorldOcclusionGetter implements BlockOcclusionCulling.BlockOcclusionGetter {
    private static final BlockState AIR = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    private static final boolean UNLOADED_OCCLUDING = true;

    private final Level level;
    private final SolidOcclusionMask solidMask;
    private LevelChunk chunk;
    private LevelChunkSection section;
    private int chunkX;
    private int sectionY;
    private int chunkZ;

    public WorldOcclusionGetter(Level level, SolidOcclusionMask solidMask) {
        this.level = level;
        this.solidMask = solidMask;
    }

    @Override
    public boolean isOccluding(int x, int y, int z) {
        return isSolidAt(x, y, z);
    }

    @Override
    public boolean isOccludingRay(int x, int y, int z) {
        int cx = x >> 4;
        int sy = y >> 4;
        int cz = z >> 4;

        if (chunkX != cx || chunkZ != cz) {
            chunkX = cx;
            sectionY = sy;
            chunkZ = cz;
            chunk = level.getChunkIfLoaded(cx, cz);

            if (chunk == null) {
                section = null;
                return UNLOADED_OCCLUDING;
            }

            section = getSection(chunk, sy);
            if (section == null) {
                return false;
            }

            if (section.hasOnlyAir()) {
                return false;
            }

            return solidMask.isSolid(getBlockState(section, x, y, z));
        }

        if (sectionY != sy) {
            sectionY = sy;

            if (chunk == null) {
                return UNLOADED_OCCLUDING;
            }

            section = getSection(chunk, sy);
            if (section == null) {
                return false;
            }

            if (section.hasOnlyAir()) {
                return false;
            }
        }

        if (section == null) {
            return false;
        }

        return solidMask.isSolid(getBlockState(section, x, y, z));
    }

    @Override
    public boolean sectionHasOnlyAir(int x, int y, int z) {
        int cx = x >> 4;
        int sy = y >> 4;
        int cz = z >> 4;
        LevelChunk loaded = level.getChunkIfLoaded(cx, cz);

        if (loaded == null) {
            return false;
        }

        LevelChunkSection s = getSection(loaded, sy);
        return s != null && s.hasOnlyAir();
    }

    public void clearCache() {
        chunk = null;
        section = null;
    }

    private boolean isSolidAt(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;

        if (chunkX != cx || chunkZ != cz) {
            chunkX = cx;
            chunkZ = cz;
            chunk = level.getChunkIfLoaded(cx, cz);

            if (chunk == null) {
                return UNLOADED_OCCLUDING;
            }
        }

        int sy = y >> 4;

        if (sectionY != sy) {
            sectionY = sy;
            section = chunk == null ? null : getSection(chunk, sy);
        }

        if (section == null) {
            return false;
        }

        if (section.hasOnlyAir()) {
            return false;
        }

        return solidMask.isSolid(getBlockState(section, x, y, z));
    }

    private static LevelChunkSection getSection(LevelChunk chunk, int sectionY) {
        int minSectionY = chunk.getMinSectionY();

        if (sectionY < minSectionY || sectionY >= chunk.getMaxSectionY()) {
            return null;
        }

        return chunk.getSections()[sectionY - minSectionY];
    }

    private static BlockState getBlockState(LevelChunkSection section, int x, int y, int z) {
        try {
            return section.getBlockState(x & 15, y & 15, z & 15);
        } catch (MissingPaletteEntryException e) {
            return AIR;
        }
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return NmsCompat.chunkKey(chunkX, chunkZ);
    }
}
