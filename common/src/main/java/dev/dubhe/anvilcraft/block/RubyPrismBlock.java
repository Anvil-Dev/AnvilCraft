package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeableBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RubyPrismBlock extends BaseEntityBlock implements IHammerRemovable,
    IHammerChangeableBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    /**
     * 方块状态注册
     */
    public RubyPrismBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(
        @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return RubyPrismBlockEntity.createBlockEntity(ModBlockEntities.RUBY_PRISM.get(), pos, state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
        @NotNull CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> 
                Shapes.or(
                    Block.box(0, 0, 0, 16, 4, 16),
                    Block.box(2, 4, 2, 14, 14, 14),
                    Block.box(4, 14, 4, 12, 16, 12));
            case DOWN -> 
                Shapes.or(
                    Block.box(0, 12, 0, 16, 16, 16),
                    Block.box(2, 2, 2, 14, 12, 14),
                    Block.box(4, 0, 4, 12, 2, 12));
            case NORTH -> 
                Shapes.or(
                    Block.box(0, 0, 12, 16, 16, 16),
                    Block.box(2, 2, 2, 14, 14, 12),
                    Block.box(4, 4, 0, 12, 12, 2));
            case SOUTH -> 
                Shapes.or(
                    Block.box(0, 0, 0, 16, 16, 4),
                    Block.box(2, 2, 4, 14, 14, 14),
                    Block.box(4, 4, 14, 12, 12, 16));
            case WEST -> 
                Shapes.or(
                    Block.box(12, 0, 0, 16, 16, 16),
                    Block.box(2, 2, 2, 12, 14, 14),
                    Block.box(0, 4, 4, 2, 12, 12));
            case EAST ->
                Shapes.or(
                    Block.box(0, 0, 0, 4, 16, 16),
                    Block.box(4, 2, 2, 14, 14, 14),
                    Block.box(14, 4, 4, 16, 12, 12));
        };
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        return createTickerHelper(type, ModBlockEntities.RUBY_PRISM.get(),
            (level1, pos, state1, entity) -> entity.tick(level1));
    }
}
