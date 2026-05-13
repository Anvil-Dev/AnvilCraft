package dev.dubhe.anvilcraft.block.storage;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class LevitationPowderBlock extends FallingBlock {
    public LevitationPowderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return null;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        // 和上方重力方块交换位置
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() instanceof FallingBlock && !(above.getBlock() instanceof LevitationPowderBlock)) {
            if (above.getBlock() instanceof Fallable) {
                FallingBlockEntity.fall(level, pos.above(), above);
            }
            LevitatingBlockEntity.levitate(level, pos, state);
        }
    }

    @Override
    public int getDustColor(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        //color of sand
        return 14406560;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }
}
