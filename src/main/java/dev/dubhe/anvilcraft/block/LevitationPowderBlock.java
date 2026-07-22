package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LevitationPowderBlock extends FallingBlock {
    private static final double MIN_GRAVITY_SQR = 1.0E-5;

    public LevitationPowderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return null;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Vec3 gravity = GravityManager.getNetGravityVectorForFallingBlock(
            level,
            Vec3.atCenterOf(pos),
            GravityManager.getFallingBlockGravityType(this)
        );
        if (gravity.lengthSqr() < MIN_GRAVITY_SQR) return;

        Direction gravityDirection = Direction.getNearest(gravity.x, gravity.y, gravity.z);
        BlockPos targetPos = pos.relative(gravityDirection);
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof FallingBlock
            && !(targetState.getBlock() instanceof LevitationPowderBlock)) {
            FallingBlockEntity.fall(level, targetPos, targetState);
            LevitatingBlockEntity.levitate(level, pos, state);
            return;
        }
        if (FallingBlock.isFree(targetState)) {
            LevitatingBlockEntity.levitate(level, pos, state);
            return;
        }

        super.tick(state, level, pos, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston
    ) {
        level.scheduleTick(pos, this, 2);
    }
}
