package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

public class PipeBlockItem extends Item {
    public PipeBlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockState blockstate = this.getPlacementState(context);
        if (blockstate == null) {
            return InteractionResult.FAIL;
        }
        if (!this.placeBlock(context, blockstate)) {
            return InteractionResult.FAIL;
        }
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();
        BlockState blockstate1 = level.getBlockState(blockpos);
        if (blockstate1.is(blockstate.getBlock())) {
            blockstate1 = this.updateBlockStateFromTag(blockpos, level, itemstack, blockstate1);
            this.updateCustomBlockEntityTag(blockpos, level, player, itemstack, blockstate1);
            PipeBlockItem.updateBlockEntityComponents(level, blockpos, itemstack);
            blockstate1.getBlock().setPlacedBy(level, blockpos, blockstate1, player, itemstack);
            if (player instanceof ServerPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, blockpos, itemstack);
            }
        }
        SoundType soundtype = blockstate1.getSoundType(level, blockpos, context.getPlayer());
        level.playSound(
            player,
            blockpos,
            this.getPlaceSound(blockstate1, level, blockpos, context.getPlayer()),
            SoundSource.BLOCKS,
            (soundtype.getVolume() + 1.0F) / 2.0F,
            soundtype.getPitch() * 0.8F
        );
        level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate1));
        itemstack.consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        BlockPos targetPos = placePos.relative(clickedFace.getOpposite());
        BlockState targetState = level.getBlockState(targetPos);
        Block targetBlock = targetState.getBlock();

        boolean shiftDown = player != null && player.isShiftKeyDown();
        boolean clickedOnPipe = targetBlock instanceof PipeBlock;
        boolean clickedOnFluidHandler = PipeBlock.isFluidHandler(level, targetPos);

        // Shift+click or not clicking on pipe/fluid handler: place along look direction
        if (shiftDown || (!clickedOnPipe && !clickedOnFluidHandler)) {
            Direction.Axis axis = getLookAxis(player);
            return makeStraightState(level, placePos, axis, true, true);
        }

        // Connect mode: clicking on pipe or fluid handler
        if (targetBlock instanceof PipeCornerBlock) {
            return handleCornerPlacement(level, placePos, clickedFace, targetPos, targetState);
        }

        Direction.Axis axis = clickedFace.getAxis();
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        Direction towardTarget = clickedFace.getOpposite();

        boolean startIsPipe;
        boolean endIsPipe;

        if (towardTarget == startDir) {
            startIsPipe = clickedOnPipe;
            endIsPipe = level.getBlockState(placePos.relative(endDir)).getBlock() instanceof PipeBlock;
        } else {
            endIsPipe = clickedOnPipe;
            startIsPipe = level.getBlockState(placePos.relative(startDir)).getBlock() instanceof PipeBlock;
        }

        return makeStraightState(level, placePos, axis, !startIsPipe, !endIsPipe);
    }

    private BlockState makeStraightState(Level level, BlockPos pos, Direction.Axis axis, boolean hasEndStart, boolean hasEndEnd) {
        return ModBlocks.PIPE_STRAIGHT.get().defaultBlockState()
            .setValue(PipeBlock.AXIS, axis)
            .setValue(PipeBlock.HAS_END_START, hasEndStart)
            .setValue(PipeBlock.HAS_END_END, hasEndEnd)
            .setValue(PipeBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Nullable
    private BlockState handleCornerPlacement(
        Level level, BlockPos placePos, Direction clickedFace,
        BlockPos cornerPos, BlockState cornerState
    ) {
        if (level.isClientSide) return null;

        PipeBlock.CornerEnded corner = cornerState.getValue(PipeBlock.CORNER_ENDED);
        Direction first = corner.getFirstDirection();
        Direction second = corner.getSecondDirection();

        boolean firstOccupied = PipeBlock.isNeighborOccupied(level, cornerPos, first);
        boolean secondOccupied = PipeBlock.isNeighborOccupied(level, cornerPos, second);
        boolean bothFree = !firstOccupied && !secondOccupied;
        boolean bothOccupied = firstOccupied && secondOccupied;
        boolean directionMatches = corner.containsDirection(clickedFace);
        boolean oppositeOccupied = (firstOccupied && clickedFace == first.getOpposite())
                                   || (secondOccupied && clickedFace == second.getOpposite());

        if (bothOccupied) {
            BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
                .setValue(PipeBlock.WATERLOGGED, cornerState.getValue(PipeBlock.WATERLOGGED));
            nodeState = nodeState.setValue(
                PipeBlock.getPropertyForDirection(first),
                PipeNodeBlock.evaluateNeighbor(level, cornerPos, first)
            );
            nodeState = nodeState.setValue(
                PipeBlock.getPropertyForDirection(second),
                PipeNodeBlock.evaluateNeighbor(level, cornerPos, second)
            );
            nodeState = nodeState.setValue(PipeBlock.getPropertyForDirection(clickedFace), PipeBlock.NodePipe.PIPE);
            level.setBlockAndUpdate(cornerPos, nodeState);
        } else if (bothFree || directionMatches || oppositeOccupied) {
            Direction.Axis axis = clickedFace.getAxis();
            Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
            Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);

            boolean startIsPipe = PipeBlock.isNeighborPipeToward(level, cornerPos, startDir);
            boolean endIsPipe = PipeBlock.isNeighborPipeToward(level, cornerPos, endDir);
            if (clickedFace == startDir) {
                startIsPipe = true;
            } else if (clickedFace == endDir) {
                endIsPipe = true;
            }

            BlockState straightState = ModBlocks.PIPE_STRAIGHT.get().defaultBlockState()
                .setValue(PipeBlock.AXIS, axis)
                .setValue(PipeBlock.HAS_END_START, !startIsPipe)
                .setValue(PipeBlock.HAS_END_END, !endIsPipe)
                .setValue(PipeBlock.WATERLOGGED, cornerState.getValue(PipeBlock.WATERLOGGED));
            level.setBlockAndUpdate(cornerPos, straightState);
        } else {
            Direction occupiedEnd = firstOccupied ? first : second;
            PipeBlock.CornerEnded newCorner = PipeBlock.CornerEnded.fromDirections(occupiedEnd, clickedFace);
            boolean occupiedEndIsPipe = PipeBlock.isNeighborPipeToward(level, cornerPos, occupiedEnd);
            boolean firstIsOccupied = newCorner.getFirstDirection() == occupiedEnd;

            BlockState newCornerState = ModBlocks.PIPE_CORNER.get().defaultBlockState()
                .setValue(PipeBlock.WATERLOGGED, cornerState.getValue(PipeBlock.WATERLOGGED))
                .setValue(PipeBlock.CORNER_ENDED, newCorner)
                .setValue(PipeBlock.HAS_END_START, firstIsOccupied && !occupiedEndIsPipe)
                .setValue(PipeBlock.HAS_END_END, !firstIsOccupied && !occupiedEndIsPipe);
            level.setBlockAndUpdate(cornerPos, newCornerState);
        }

        Direction.Axis axis = clickedFace.getAxis();
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        Direction towardCorner = clickedFace.getOpposite();

        boolean startIsPipe = towardCorner == startDir;
        boolean endIsPipe = towardCorner == endDir;
        if (startIsPipe) {
            endIsPipe = level.getBlockState(placePos.relative(endDir)).getBlock() instanceof PipeBlock;
        } else {
            startIsPipe = level.getBlockState(placePos.relative(startDir)).getBlock() instanceof PipeBlock;
        }

        return makeStraightState(level, placePos, axis, !startIsPipe, !endIsPipe);
    }

    private static Direction.Axis getLookAxis(@Nullable Player player) {
        if (player == null) return Direction.Axis.Y;
        Vec3 lookVec = player.getViewVector(1.0f);
        return Direction.getNearest(lookVec.x, lookVec.y, lookVec.z).getAxis();
    }

    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        Player player = context.getPlayer();
        CollisionContext collisioncontext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        return (!this.mustSurvive() || state.canSurvive(context.getLevel(), context.getClickedPos()))
               && context.getLevel().isUnobstructed(state, context.getClickedPos(), collisioncontext);
    }

    protected boolean mustSurvive() {
        return true;
    }

    protected SoundEvent getPlaceSound(BlockState blockState, Level world, BlockPos pos, @Nullable Player entity) {
        return blockState.getSoundType(world, pos, entity).getPlaceSound();
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return context.getLevel().setBlock(context.getClickedPos(), state, 11);
    }

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        return PipeBlockItem.updateCustomBlockEntityTag(level, player, pos, stack);
    }

    public static boolean updateCustomBlockEntityTag(Level level, @Nullable Player player, BlockPos pos, ItemStack stack) {
        MinecraftServer minecraftserver = level.getServer();
        if (minecraftserver == null) {
            return false;
        }
        CustomData customdata = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customdata.isEmpty()) {
            return false;
        }
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity == null) {
            return false;
        }
        if (level.isClientSide || !blockentity.onlyOpCanSetNbt() || player != null && player.canUseGameMasterBlocks()) {
            return customdata.loadInto(blockentity, level.registryAccess());
        }
        return false;
    }

    public BlockState updateBlockStateFromTag(BlockPos pos, Level level, ItemStack stack, BlockState state) {
        BlockItemStateProperties blockitemstateproperties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (blockitemstateproperties.isEmpty()) {
            return state;
        }
        BlockState blockstate = blockitemstateproperties.apply(state);
        if (blockstate != state) {
            level.setBlock(pos, blockstate, 2);
        }
        return blockstate;
    }

    public static void updateBlockEntityComponents(Level level, BlockPos poa, ItemStack stack) {
        BlockEntity blockentity = level.getBlockEntity(poa);
        if (blockentity == null) {
            return;
        }
        blockentity.applyComponentsFromItemStack(stack);
        blockentity.setChanged();
    }
}
