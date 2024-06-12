package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class OverseerBlock extends AbstractMultiplePartBlock<Vertical3PartHalf>
        implements IHammerRemovable, IHasMultiBlock, EntityBlock {
    private static final VoxelShape OVERSEER_BASE =
            Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 8, 2, 14, 16, 14));
    private static final VoxelShape OVERSEER_MID = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape OVERSEER_TOP = Block.box(2, 0, 2, 14, 16, 14);
    public static final EnumProperty<Vertical3PartHalf> HALF =
            EnumProperty.create("half", Vertical3PartHalf.class);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);

    /**
     * @param properties 属性
     */
    public OverseerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(HALF, Vertical3PartHalf.BOTTOM).setValue(LEVEL, 0));
    }

    @Override
    protected @NotNull Property<Vertical3PartHalf> getPart() {
        return OverseerBlock.HALF;
    }

    @Override
    protected Vertical3PartHalf[] getParts() {
        return Vertical3PartHalf.values();
    }

    @Override
    @Nullable public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HALF, Vertical3PartHalf.BOTTOM).setValue(LEVEL, 0);
    }

    @Override
    protected void createBlockStateDefinition(
            @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF).add(LEVEL);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(
            @NotNull BlockState state,
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case TOP -> OVERSEER_TOP;
            case MID -> OVERSEER_MID;
            case BOTTOM -> OVERSEER_BASE;
        };
    }

    @Override
    public void setPlacedBy(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            LivingEntity placer,
            @NotNull ItemStack stack) {
        BlockPos above = pos.above();
        level.setBlockAndUpdate(above, state.setValue(HALF, Vertical3PartHalf.MID).setValue(LEVEL, 1));
        above = above.above();
        level.setBlockAndUpdate(above, state.setValue(HALF, Vertical3PartHalf.TOP).setValue(LEVEL, 1));
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new OverseerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (state.getValue(HALF) != Vertical3PartHalf.BOTTOM) return null;
        return (level1, pos, state1, entity) -> {
            if (entity instanceof OverseerBlockEntity be) be.tick(level1, pos, state1);
        };
    }

    @Override
    public void onRemove(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {}

    @Override
    public void onPlace(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {}
}
