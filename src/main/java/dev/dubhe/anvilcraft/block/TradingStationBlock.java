package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.WaterloggedFlexibleMultiPartBlock;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public class TradingStationBlock
    extends WaterloggedFlexibleMultiPartBlock<DirectionVertical2PartHalf, EnumProperty<Direction>, Direction>
    implements MultiPartBlockEntity<DirectionVertical2PartHalf, TradingStationBlock>, IHammerChangeable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DirectionVertical2PartHalf> HALF = EnumProperty.create("half", DirectionVertical2PartHalf.class);

    public TradingStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.defaultBlockState()
                .setValue(TradingStationBlock.HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(TradingStationBlock.FACING, Direction.NORTH)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getHorizontalDirection().getOpposite();
        if (dir.getAxis().isVertical()) dir = Direction.NORTH;
        return this.waterloggedStateForPlacement(context, this.defaultBlockState().setValue(TradingStationBlock.FACING, dir));
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
    public EnumProperty<Direction> getAdditionalProperty() {
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
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(TradingStationBlock.HALF) == DirectionVertical2PartHalf.TOP;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return TradingStationBlock.FACING;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        this.change(blockPos, level, (state) -> state.cycle(TradingStationBlock.FACING));
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(TradingStationBlock.FACING, rotation.rotate(state.getValue(TradingStationBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(TradingStationBlock.FACING, mirror.mirror(state.getValue(TradingStationBlock.FACING)));
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos mainPartPos = this.getMainPartPos(pos, state);
        if (level.getBlockEntity(mainPartPos) instanceof TradingStationBlockEntity be && player instanceof ServerPlayer sp) {
            if (be.tryTradingWithPlayer(sp, hand)) return InteractionResult.CONSUME;
            if (sp.isSpectator() || !be.isOwner(sp)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (sp.getItemInHand(hand).is(ModItems.DISK)) {
                return be.useDisk(level, sp, hand, sp.getItemInHand(hand), hitResult);
            }
            ModMenuTypes.open(sp, be, mainPartPos);
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public boolean onDestroyedByPlayer(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        ItemStack tool,
        boolean willHarvest,
        FluidState fluid
    ) {
        if (level instanceof ServerLevel serverside) {
            // Resolve to main part so onPlayerBreak can find the BlockEntity and check ownership.
            // Otherwise breaking the top half adds topPos to playerBroke while the cascade
            // removes the bottom half via onNonPlayerBreak, which looks for bottomPos — mismatch.
            TradingStationMessageManager.get().onPlayerBreak(serverside, this.getMainPartPos(pos, state), player);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }

    public Collection<BlockState> getBottomStates() {
        return Set.of(
            this.defaultBlockState()
                .setValue(TradingStationBlock.HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(TradingStationBlock.FACING, Direction.NORTH),
            this.defaultBlockState()
                .setValue(TradingStationBlock.HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(TradingStationBlock.FACING, Direction.SOUTH),
            this.defaultBlockState()
                .setValue(TradingStationBlock.HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(TradingStationBlock.FACING, Direction.EAST),
            this.defaultBlockState()
                .setValue(TradingStationBlock.HALF, DirectionVertical2PartHalf.BOTTOM)
                .setValue(TradingStationBlock.FACING, Direction.WEST)
        );
    }

    // Shapes

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(TradingStationBlock.HALF) == DirectionVertical2PartHalf.TOP) return Shapes.empty();
        return switch (state.getValue(TradingStationBlock.FACING)) {
            case NORTH -> TradingStationBlock.NORTH;
            case WEST -> TradingStationBlock.WEST;
            case SOUTH -> TradingStationBlock.SOUTH;
            case EAST -> TradingStationBlock.EAST;
            case UP, DOWN -> Shapes.empty();
        };
    }

    private static final VoxelShape NORTH = ShapeUtil.merge(
        new AABB(0, 0, 0, 16, 16, 16),
        new AABB(0, 30, 0, 16, 32, 16),
        new AABB(0, 16, 11, 2, 30, 14),
        new AABB(14, 16, 11, 16, 30, 14)
    );
    private static final VoxelShape WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, TradingStationBlock.NORTH);
    private static final VoxelShape SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, TradingStationBlock.NORTH);
    private static final VoxelShape EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, TradingStationBlock.NORTH);
}
