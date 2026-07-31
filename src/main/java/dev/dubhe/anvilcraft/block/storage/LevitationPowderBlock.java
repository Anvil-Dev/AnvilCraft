package dev.dubhe.anvilcraft.block.storage;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LevitationPowderBlock extends FallingBlock {
    private static final double MIN_GRAVITY_SQR = 1.0E-5;

    public LevitationPowderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return BlockBehaviour.simpleCodec(LevitationPowderBlock::new);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 悬浮粉沿净重力方向与相邻重力方块交换位置，而不再只看正上方
        Vec3 gravity = GravityManager.getNetGravityVectorForFallingBlock(
            level,
            Vec3.atCenterOf(pos),
            GravityManager.getFallingBlockGravityType(this)
        );
        if (gravity.lengthSqr() < LevitationPowderBlock.MIN_GRAVITY_SQR) return;

        Direction gravityDirection = Direction.getApproximateNearest(gravity.x, gravity.y, gravity.z);
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
    public int getDustColor(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        // color of sand
        return 14406560;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        level.scheduleTick(pos, this, 2);
    }
}
