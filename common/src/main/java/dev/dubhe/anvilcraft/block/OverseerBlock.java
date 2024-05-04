package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Half;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import javax.annotation.Nonnull;
import net.fabricmc.fabric.impl.resource.loader.ServerLanguageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OverseerBlock extends BaseEntityBlock {

    private static final VoxelShape OVERSEER_BASE = Shapes.or(
        Block.box(0, 0, 0, 16, 4, 16),
        Block.box(4, 6, 4, 12, 16, 12)
    );
    private static final VoxelShape OVERSEER_TOP = Block.box(4, 0, 4, 12, 15, 12);
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 4);

    /**
     * @param properties 属性
     */
    public OverseerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, Half.BOTTOM)
                .setValue(LEVEL, 1)
        );
    }


    @Override
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(HALF, Half.BOTTOM)
            .setValue(LEVEL, 1);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF).add(LEVEL);
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context
    ) {
        if (state.getValue(HALF) == Half.BOTTOM) return OVERSEER_BASE;
        if (state.getValue(HALF) == Half.TOP) return OVERSEER_TOP;
        return super.getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
        LivingEntity placer, @NotNull ItemStack stack
    ) {
        BlockPos above = pos.above();
        level.setBlockAndUpdate(above, state.setValue(HALF, Half.TOP).setValue(LEVEL, 1));
    }

    @Override
    public void playerWillDestroy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player
    ) {
        if (state.getValue(HALF) == Half.TOP) {
            level.destroyBlock(pos.below(), false, null);
        } else {
            level.destroyBlock(pos.above(), false, null);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new OverseerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        if (state.getValue(HALF) == Half.TOP) return null;
        return createTickerHelper(type, ModBlockEntities.OVERSEER.get(),
            (level1, pos, state1, entity) -> entity.tick(level1, pos, state1));
    }
}
