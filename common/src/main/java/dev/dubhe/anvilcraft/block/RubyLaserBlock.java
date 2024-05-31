package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeableBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent.Switch;
import dev.dubhe.anvilcraft.block.entity.RubyLaserBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RubyLaserBlock extends BaseEntityBlock implements IHammerRemovable,
    IHammerChangeableBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    /**
     * 方块状态注册
     */
    public RubyLaserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(OVERLOAD, false)
                .setValue(SWITCH, Switch.OFF));
    }

    @Override
    protected void createBlockStateDefinition(
        @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(OVERLOAD).add(SWITCH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return RubyLaserBlockEntity.createBlockEntity(ModBlockEntities.RUBY_LASER.get(), pos, state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
        @NotNull CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> 
                Shapes.or(
                    Block.box(4, 3, 4, 12, 13, 12), 
                    Block.box(5, 13, 5, 11, 16, 11), 
                    Block.box(3, 0, 3, 13, 3, 13));
            case DOWN -> 
                Shapes.or(
                    Block.box(4, 3, 4, 12, 13, 12), 
                    Block.box(5, 0, 5, 11, 3, 11), 
                    Block.box(3, 13, 3, 13, 16, 13));
            case NORTH -> 
                Shapes.or(
                    Block.box(4, 4, 3, 12, 12, 13), 
                    Block.box(5, 5, 0, 11, 11, 3), 
                    Block.box(3, 3, 13, 13, 13, 16));
            case SOUTH -> 
                Shapes.or(
                    Block.box(4, 4, 3, 12, 12, 13), 
                    Block.box(5, 5, 13, 11, 11, 16), 
                    Block.box(3, 3, 0, 13, 13, 3));
            case WEST -> 
                Shapes.or(
                    Block.box(3, 4, 4, 13, 12, 12), 
                    Block.box(0, 5, 5, 3, 11, 11), 
                    Block.box(13, 3, 3, 16, 13, 13));
            case EAST -> 
                Shapes.or(
                    Block.box(3, 4, 4, 13, 12, 12), 
                    Block.box(13, 5, 5, 16, 11, 11), 
                    Block.box(0, 3, 3, 3, 13, 13));
        };
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        return createTickerHelper(type, ModBlockEntities.RUBY_LASER.get(),
            (level1, pos, state1, entity) -> entity.tick(level1));
    }
}
