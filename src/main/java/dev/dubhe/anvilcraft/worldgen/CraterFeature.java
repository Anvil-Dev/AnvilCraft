package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class CraterFeature extends Feature<CraterConfiguration> {
    public CraterFeature(Codec<CraterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CraterConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, context.origin()).below();
        RandomSource randomSource = context.random();
        CraterConfiguration configuration = context.config();
        int radius = configuration.radius().sample(randomSource);
        int depth = configuration.depth().sample(randomSource);
        if (depth > radius) return false;
        int radiusCrater = (depth * depth + radius * radius) / (2 * depth);
        if (radiusCrater < 15) {
            digCrater(level, pos, radiusCrater, depth);
        } else {
            level.getLevel().getServer().execute(() -> digCrater(level.getLevel(), pos, radiusCrater, depth));
        }

        return true;
    }

    private static void digCrater(LevelAccessor level, BlockPos pos, int radius, int depth) {
        BlockPos canter = pos.above(radius - depth);
        BlockPos.MutableBlockPos digging = pos.mutable();
        for (int i = -depth; i <= radius; i++) {
            boolean bl = false;

            for (int l = -radius; l <= radius; l++) {
                for (int m = -radius; m <= radius; m++) {
                    digging.setWithOffset(pos, l, i, m);
                    if (digging.distSqr(canter) < radius * radius && !level.getBlockState(digging).isAir()) {
                        bl = true;
                        level.setBlock(digging, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }

            if (!bl && i > 0) break;
        }
    }
}
