package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class GammaLaserBehavior implements ILaserComponent {
    private static final int[] EXPOSURE_TICKS = {Integer.MAX_VALUE, 60, 20, 5, 1};
    @Nullable
    private BlockPos irradiatingPos;
    private int exposureTicks;

    @Override
    public void onEmitPre(ILaserComponentOwner owner) {
        if (!LaserTypeComponent.isGamma(owner)) onEmissionStopped(owner);
    }

    @Override
    public void onEmissionStopped(ILaserComponentOwner owner) {
        irradiatingPos = null;
        exposureTicks = 0;
    }

    @Override
    public boolean onHitBlock(ILaserComponentOwner owner, Level level, BlockPos blockPos) {
        if (!LaserTypeComponent.isGamma(owner)) return true;
        if (!(level instanceof ServerLevel)) return false;
        int strength = LaserStrengthComponent.getStrength(owner);
        GammaLaserEffects.destroyPrisms(level, owner.getLaserSourcePos(), owner.getLaserDirection(), blockPos);
        if (!blockPos.equals(irradiatingPos)) {
            irradiatingPos = blockPos.immutable();
            exposureTicks = 0;
        }
        BlockState state = level.getBlockState(blockPos);
        boolean canBreak = !state.is(BlockTags.WITHER_IMMUNE) && !state.isAir() && state.getDestroySpeed(level, blockPos) >= 0;
        if (canBreak) {
            exposureTicks++;
            if (exposureTicks >= EXPOSURE_TICKS[Math.clamp(strength / 4, 0, 4)]) {
                exposureTicks = 0;
                BlockPos breakPos = blockPos;
                if (state.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> multiPartBlock) {
                    breakPos = multiPartBlock.getMainPartPos(blockPos, state);
                }
                level.destroyBlock(strength >= 16 ? breakPos : blockPos, strength < 16);
            }
        } else {
            exposureTicks = 0;
        }
        GammaLaserEffects.heatEmberMetal(level, blockPos, owner.getLaserDirection(), strength, Block.UPDATE_CLIENTS);
        return false;
    }

    // 合并只比较配置，连续照射进度保留在当前光束的实例中。
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof GammaLaserBehavior;
    }

    @Override
    public int hashCode() {
        return GammaLaserBehavior.class.hashCode();
    }
}
