package dev.dubhe.anvilcraft.block.batch;

import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class BaseBatchCraftingBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    protected BaseBatchCraftingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(POWERED, false)
                .setValue(OVERLOAD, true)
                .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof BatchCrafterBlockEntity crafterBlockEntity) {
            return crafterBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = player.getItemInHand(hand);
        if (be instanceof IDiskCloneable cloneable && stack.is(ModItems.DISK.get())) {
            return cloneable.useDisk(level, player, hand, stack, hit);
        }

        return this.playerUse(level, pos, state, be, player, hand, hit);
    }

    /**
     * 当玩家交互此方块时调用
     *
     * @param level 方块所在的世界
     * @param pos 方块所在的位置
     * @param state 方块的状态
     * @param be 方块的方块实体
     * @param player 交互该方块的玩家
     * @param hand 玩家交互该方块的手
     * @param hit 玩家视线与方块的碰撞计算结果
     * @return 此次交互的结果
     */
    protected abstract InteractionResult playerUse(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable BlockEntity be,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    );

    public abstract Item getToastSymbol();

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof IItemHandlerHolder holder) {
            Vec3 vec3 = pos.getCenter();
            IItemHandler handler = holder.getItemHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, handler.getStackInSlot(slot));
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getVisualShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) dir = dir.getOpposite();
        return this.defaultBlockState()
            .setValue(FACING, dir)
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
            .setValue(OVERLOAD, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED).add(OVERLOAD).add(FACING);
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) return;
        level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), 2);
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    public static BlockState copy(BlockState from, BlockState to) {
        if (!(from.getBlock() instanceof BaseBatchCraftingBlock) || !(to.getBlock() instanceof BaseBatchCraftingBlock)) return to;
        return to
            .setValue(POWERED, from.getValue(POWERED))
            .setValue(OVERLOAD, from.getValue(OVERLOAD))
            .setValue(FACING, from.getValue(FACING));
    }

    private static final List<Supplier<BaseBatchCraftingBlock>> BATCH_CRAFTING_BLOCK_GETTERS = new ArrayList<>();

    public static void registerBatchCrafting(Supplier<BaseBatchCraftingBlock> getter) {
        BaseBatchCraftingBlock.BATCH_CRAFTING_BLOCK_GETTERS.add(getter);
    }

    public static @Unmodifiable List<Supplier<BaseBatchCraftingBlock>> getBatchCraftingBlockGetters() {
        return BaseBatchCraftingBlock.BATCH_CRAFTING_BLOCK_GETTERS;
    }
}
