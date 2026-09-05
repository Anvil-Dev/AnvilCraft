package dev.dubhe.anvilcraft.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 石碑（The Monolith）：玩家第一次登月时在落点附近生成的 32x72x8 磨制深板岩巨碑，
 * 全局唯一，包围盒持久化于 Mun 维度的 SavedData。
 */
public final class TheMonolith {
    /** 碑体宽度（格）。 */
    public static final int WIDTH = 32;
    /** 碑体高度（格）。 */
    public static final int HEIGHT = 72;
    /** 碑体厚度（格）。 */
    public static final int THICKNESS = 8;
    /** 碑体每个面向外扩展的作用范围（格）。 */
    public static final int RANGE = 32;
    /** 碑体底部嵌入地下的深度（格）。 */
    private static final int EMBED_DEPTH = 4;
    /** 碑体中心距玩家落点的水平距离（格）。 */
    private static final int PLACEMENT_DISTANCE = 24;

    private TheMonolith() {
    }

    /** 玩家抵达 Mun 时调用；若石碑尚未生成，则在落点附近生成并记录。 */
    public static void ensureGenerated(ServerLevel mun, BlockPos landingPos) {
        State state = State.get(mun);
        if (state.boundingBox != null) return;
        state.setBoundingBox(place(mun, landingPos));
    }

    /** 在指定位置附近放置碑体，返回碑体包围盒。 */
    private static BoundingBox place(ServerLevel mun, BlockPos near) {
        RandomSource random = mun.getRandom();
        double angle = random.nextDouble() * Mth.TWO_PI;
        int centerX = near.getX() + Mth.floor(Math.cos(angle) * PLACEMENT_DISTANCE);
        int centerZ = near.getZ() + Mth.floor(Math.sin(angle) * PLACEMENT_DISTANCE);
        int baseY = mun.getHeight(Heightmap.Types.WORLD_SURFACE, centerX, centerZ) - EMBED_DEPTH;
        int halfWidth = WIDTH / 2;
        int halfThickness = THICKNESS / 2;
        // 宽边朝向随机
        BoundingBox box = random.nextBoolean()
            ? new BoundingBox(
                centerX - halfWidth, baseY, centerZ - halfThickness,
                centerX + halfWidth - 1, baseY + HEIGHT - 1, centerZ + halfThickness - 1)
            : new BoundingBox(
                centerX - halfThickness, baseY, centerZ - halfWidth,
                centerX + halfThickness - 1, baseY + HEIGHT - 1, centerZ + halfWidth - 1);
        BlockState state = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    mun.setBlock(pos.set(x, y, z), state, 3);
                }
            }
        }
        return box;
    }

    /** 石碑的持久化状态：全局唯一的碑体包围盒。 */
    public static class State extends SavedData {
        private static final String DATA_NAME = "anvilcraft_the_monolith";

        private @Nullable BoundingBox boundingBox;

        public static State get(ServerLevel mun) {
            return mun.getDataStorage()
                .computeIfAbsent(new Factory<>(State::new, State::load, null), DATA_NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider provider) {
            State state = new State();
            if (tag.contains("BoundingBox")) {
                int[] box = tag.getIntArray("BoundingBox");
                state.boundingBox = new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]);
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            if (this.boundingBox != null) {
                BoundingBox box = this.boundingBox;
                tag.putIntArray(
                    "BoundingBox",
                    new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}
                );
            }
            return tag;
        }

        private void setBoundingBox(BoundingBox boundingBox) {
            this.boundingBox = boundingBox;
            this.setDirty();
        }

        /** 判断位置是否处于碑体每个面向外 {@link TheMonolith#RANGE} 格的作用范围内。 */
        public boolean isInRange(BlockPos pos) {
            return this.boundingBox != null && this.boundingBox.inflatedBy(RANGE).isInside(pos);
        }
    }
}
