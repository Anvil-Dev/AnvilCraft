package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.state.DirectionVertical2PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.trading.TradingStationMessageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public class TradingStationBlock extends FlexibleMultiPartBlock<DirectionVertical2PartHalf, DirectionProperty, Direction>
    implements MultiPartBlockEntity<DirectionVertical2PartHalf, TradingStationBlock>, IHammerChangeable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DirectionVertical2PartHalf> HALF = EnumProperty.create("half", DirectionVertical2PartHalf.class);

    public TradingStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.defaultBlockState()
                .setValue(HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getHorizontalDirection().getOpposite();
        if (dir.getAxis().isVertical()) dir = Direction.NORTH;
        return super.getStateForPlacement(context).setValue(FACING, dir);
    }

    @Override
    public Property<DirectionVertical2PartHalf> getPart() {
        return TradingStationBlock.HALF;
    }

    @Override
    public DirectionVertical2PartHalf[] getParts() {
        return DirectionVertical2PartHalf.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return TradingStationBlock.FACING;
    }

    @Override
    public TradingStationBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.TRADING_STATION.create(pos, state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(HALF) == DirectionVertical2PartHalf.TOP;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        this.change(blockPos, level, (state) -> state.cycle(FACING));
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
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
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        BlockPos mainPartPos = this.getMainPartPos(pos, state);
        if (level.getBlockEntity(mainPartPos) instanceof TradingStationBlockEntity be && player instanceof ServerPlayer sp) {
            if (be.tryTradingWithPlayer(sp, hand)) return ItemInteractionResult.CONSUME;
            if (sp.isSpectator() || !be.isOwner(sp)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (sp.getItemInHand(hand).is(ModItems.DISK) && sp.isShiftKeyDown()) {
                return Util.interactionResultConverter().apply(be.useDisk(level, sp, hand, sp.getItemInHand(hand), hitResult));
            }
            ModMenuTypes.open(sp, be, mainPartPos);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level instanceof ServerLevel serverside) {
            TradingStationMessageManager.get().onPlayerBreak(serverside, pos, player);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (oldState.getBlock() != this || oldState.equals(newState)) return;
        if (!(level instanceof ServerLevel serverside)) return;
        TradingStationMessageManager.get().onNonPlayerBreak(serverside, pos);
    }

    public Collection<BlockState> getBottomStates() {
        return Set.of(
            this.defaultBlockState()
                .setValue(HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(FACING, Direction.NORTH),
            this.defaultBlockState()
                .setValue(HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(FACING, Direction.SOUTH),
            this.defaultBlockState()
                .setValue(HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(FACING, Direction.EAST),
            this.defaultBlockState()
                .setValue(HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(FACING, Direction.WEST)
        );
    }

    // Shapes

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM -> Shapes.block();
            case TOP -> switch (state.getValue(FACING)) {
                case NORTH -> TOP_NORTH;
                case WEST -> TOP_WEST;
                case SOUTH -> TOP_SOUTH;
                case EAST -> TOP_EAST;
                case UP, DOWN -> Shapes.empty();
            };
        };
    }

    private static final VoxelShape TOP_NORTH = ShapeUtil.merge(
        new AABB(0, 14, 0, 16, 16, 16),
        new AABB(0, 0, 5, 2, 14, 14),
        new AABB(14, 0, 5, 16, 14, 14)
    );
    private static final VoxelShape TOP_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_NORTH);
    private static final VoxelShape TOP_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_NORTH);
    private static final VoxelShape TOP_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_NORTH);
}
