package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.SiftingTableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class SiftingTableBlock extends Block implements
    SimpleWaterloggedBlock, IHammerRemovable, IMoveableEntityBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape REDUCE_AABB = Shapes.or(
        Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
        Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
        Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
        Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape REDUCE_AABB_INTERACTION = Shapes.or(
        Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
        Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
        Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape AABB = Shapes.join(Shapes.block(), REDUCE_AABB, BooleanOp.ONLY_FIRST);
    private static final VoxelShape INTERACTION_BOX =
        Shapes.join(Shapes.block(), REDUCE_AABB_INTERACTION, BooleanOp.ONLY_FIRST);

    public SiftingTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AABB;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INTERACTION_BOX;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = context.getLevel().getFluidState(pos);
        BlockState state = super.getStateForPlacement(context);
        state = null != state ? state : this.defaultBlockState();
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (hitResult.getDirection() != Direction.UP) {
            return ProcessingTableConversion.tryConvert(level, player, hand, state, pos, hitResult);
        }
        if (level.getBlockEntity(pos) instanceof SiftingTableBlockEntity table
            && table.tryInteractItems(player, hand)
        ) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SIFTING_TABLE.create(pos, state);
    }


    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide()) return;
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (!itemEntity.anvilcraft$isAdsorbable()) return;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        ItemStack stack = itemEntity.getItem();
        ItemStack remaining = ItemHandlerUtil.insertItem(handler, stack.copy(), false);
        if (remaining.getCount() == stack.getCount()) return;
        if (remaining.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(remaining);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ItemEntity itemEntity)) return;
        if (!itemEntity.anvilcraft$isAdsorbable()) return;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        ItemStack stack = itemEntity.getItem();
        ItemStack remaining = ItemHandlerUtil.insertItem(handler, stack.copy(), false);
        if (remaining.getCount() == stack.getCount()) return;
        if (remaining.isEmpty()) itemEntity.discard();
        else itemEntity.setItem(remaining);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && !movedByPiston
            && level.getBlockEntity(pos) instanceof SiftingTableBlockEntity table) {
            table.dropAllContent(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        Direction direction,
        BlockState neighborState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
