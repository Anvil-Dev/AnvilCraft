package dev.dubhe.anvilcraft.block;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InductionLightBlock extends BaseEntityBlock implements IHammerRemovable, SimpleWaterloggedBlock {

    public static final VoxelShape SHAPE_X = Block.box(0, 6, 6, 16, 10, 10);
    public static final VoxelShape SHAPE_Y = Block.box(6, 0, 6, 10, 16, 10);
    public static final VoxelShape SHAPE_Z = Block.box(6, 6, 0, 10, 10, 16);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public InductionLightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(OVERLOAD, true)
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
            @NotNull BlockState state,
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull CollisionContext context
    ) {
        return switch (state.getValue(AXIS)) {
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
            case X -> SHAPE_X;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(@NotNull BlockState blockState) {
        return false;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(POWERED, false)
                .setValue(OVERLOAD, true)
                .setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(
                        WATERLOGGED,
                        context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER
                );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return InductionLightBlockEntity.createBlockEntity(ModBlockEntities.INDUCTION_LIGHT.get(), pos, state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED).add(OVERLOAD).add(AXIS).add(WATERLOGGED);
    }

    List<Direction> orderDirectionByDistance(BlockPos pos, Vec3 hit, Predicate<Direction> includeDirection) {
        Vec3 centerToHit = hit.subtract(Vec3.atLowerCornerOf(pos).add(.5f, .5f, .5f));
        return Arrays.stream(Direction.values())
                .filter(includeDirection)
                .map(dir -> Pair.of(dir, Vec3.atLowerCornerOf(dir.getNormal()).distanceTo(centerToHit)))
                .sorted(Comparator.comparingDouble(Pair::getSecond))
                .map(Pair::getFirst)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() || !player.mayBuild())
            return InteractionResult.PASS;
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.is(ModBlocks.INDUCTION_LIGHT.asItem())) {
            for (Direction direction : orderDirectionByDistance(
                    pos,
                    hit.getLocation(),
                    dir -> dir.getAxis() == state.getValue(AXIS)
            )) {
                int length = 0;
                BlockPos blockPos = pos.relative(direction);
                BlockState blockState = level.getBlockState(blockPos);
                while (!blockState.isAir()
                        && blockState.is(ModBlocks.INDUCTION_LIGHT.get())
                        && blockState.getValue(AXIS) == direction.getAxis()
                        && length <= 6
                ) {
                    ++length;
                    blockPos = blockPos.relative(direction);
                    blockState = level.getBlockState(blockPos);
                }
                if (blockState.canBeReplaced()) {
                    level.setBlockAndUpdate(
                            blockPos,
                            ModBlocks.INDUCTION_LIGHT
                                    .getDefaultState()
                                    .setValue(AXIS, direction.getAxis())
                    );
                    SoundType soundType = ModBlocks.INDUCTION_LIGHT.getDefaultState().getSoundType();
                    level.playSound(
                            null,
                            blockPos,
                            soundType.getPlaceSound(),
                            SoundSource.BLOCKS,
                            (soundType.volume + 1) / 2.0f,
                            soundType.pitch * 0.8f
                    );
                }
                if (!player.isCreative()) {
                    itemInHand.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                ModBlockEntities.INDUCTION_LIGHT.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1)
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Block neighborBlock,
            @NotNull BlockPos neighborPos,
            boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(
            @NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random
    ) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }
}
