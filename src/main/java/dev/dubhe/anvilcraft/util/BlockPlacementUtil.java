package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.item.IBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;

/**
 * 方块物品和蓝图状态的放置工具。
 */
public final class BlockPlacementUtil {
    private BlockPlacementUtil() {
    }

    public static boolean isMultifaceLike(Block block) {
        return block instanceof MultifaceBlock || block instanceof VineBlock;
    }

    public static ItemStack placeBlock(
        ServerLevel level,
        BlockPos pos,
        ItemStack stack,
        @Nullable BlockState requiredState
    ) {
        IBlockItem blockItem = switch (stack.getItem()) {
            case IBlockItem item -> item;
            case BlockItem item -> IBlockItem.wrap(item);
            default -> null;
        };
        if (blockItem == null) {
            return stack;
        }

        ServerPlayer player = AnvilCraftFakePlayers.getBlockPlacer().offerPlayer(level);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            if (requiredState != null) {
                orientPlayerForState(player, requiredState);
            }
            blockItem.place(level, pos, player, InteractionHand.MAIN_HAND);
            return player.getMainHandItem();
        } finally {
            AnvilCraftFakePlayers.getBlockPlacer().disable(player);
        }
    }

    private static void orientPlayerForState(ServerPlayer player, BlockState state) {
        Direction facing = null;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        }
        if (facing == null) {
            return;
        }
        if (facing.getAxis().isVertical()) {
            player.setXRot(facing == Direction.UP ? -90.0F : 90.0F);
            return;
        }
        player.setXRot(0.0F);
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
    }

    public static boolean isTargetAvailable(Level level, BlockPos targetPos) {
        return level.getBlockState(targetPos).canBeReplaced() && isTargetUnobstructed(level, targetPos);
    }

    public static boolean isTargetUnobstructed(Level level, BlockPos targetPos) {
        return level.isUnobstructed(null, Shapes.create(new AABB(targetPos)));
    }

    public static void applyBlueprintStates(ServerLevel level, List<BlueprintPartSnapshot> snapshots) {
        List<PlacedBlueprintPart> placedParts = new ArrayList<>();
        for (BlueprintPartSnapshot snapshot : snapshots) {
            BlockState placedState = level.getBlockState(snapshot.pos());
            if (!snapshot.previousState().is(snapshot.requiredState().getBlock())
                && placedState.is(snapshot.requiredState().getBlock())) {
                placedParts.add(new PlacedBlueprintPart(snapshot.pos(), snapshot.requiredState(), placedState));
            }
        }
        if (placedParts.isEmpty()) {
            return;
        }

        for (PlacedBlueprintPart part : placedParts) {
            BlockState inheritedState = BlockPlacementRules.applyBlueprintStateRules(
                level.registryAccess(),
                part.placedState(),
                part.requiredState()
            );
            level.setBlock(part.pos(), inheritedState, Block.UPDATE_CLIENTS);
        }

        List<BlockState> contextualStates = new ArrayList<>(placedParts.size());
        boolean valid = true;
        for (PlacedBlueprintPart part : placedParts) {
            BlockState contextualState = Block.updateFromNeighbourShapes(level.getBlockState(part.pos()), level, part.pos());
            BlockState finalState = BlockPlacementRules.applyBlueprintStateRules(
                level.registryAccess(),
                contextualState,
                part.requiredState()
            );
            contextualStates.add(finalState);
            if (!finalState.is(part.requiredState().getBlock()) || !finalState.canSurvive(level, part.pos())) {
                valid = false;
            }
        }

        for (int index = 0; index < placedParts.size(); index++) {
            PlacedBlueprintPart part = placedParts.get(index);
            BlockState finalState = valid ? contextualStates.get(index) : part.placedState();
            level.setBlock(part.pos(), finalState, Block.UPDATE_ALL);
        }
    }

    public static boolean isBlueprintStatePresent(Level level, BlockPos pos, BlockState requiredState) {
        BlockState worldState = level.getBlockState(pos);
        if (!worldState.is(requiredState.getBlock())) {
            return false;
        }
        BlockState contextualState = Block.updateFromNeighbourShapes(worldState, level, pos);
        return contextualState.is(worldState.getBlock()) && contextualState.canSurvive(level, pos);
    }

    public static boolean isMultiblockBlock(BlockState state) {
        return isMultiblockBlock(state.getBlock());
    }

    public static boolean isMultiblockBlock(Block block) {
        boolean multiblock = switch (block) {
            case MultiPartBlockEntity<?, ?> ignored -> true;
            case AbstractMultiPartBlock<?> ignored -> true;
            case IHasMultiBlock ignored -> true;
            default -> false;
        };
        if (multiblock) {
            return true;
        }
        return block instanceof BedBlock || block instanceof DoorBlock || block instanceof DoublePlantBlock;
    }

    public static boolean isSecondaryMultiblockPart(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BedBlock) {
            return state.hasProperty(BED_PART) && state.getValue(BED_PART) == BedPart.HEAD;
        }
        if (block instanceof DoorBlock || block instanceof DoublePlantBlock) {
            return state.hasProperty(DOUBLE_BLOCK_HALF)
                   && state.getValue(DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }
        return block instanceof AbstractMultiPartBlock<?> multiPartBlock && !multiPartBlock.isMainPart(state);
    }

    private static BlockState transformBlueprintState(
        BlockState state,
        LevelAccessor level,
        BlockPos pos,
        Direction targetFacing,
        Direction scannerFacing,
        boolean upsideDown
    ) {
        int quarterTurns = Math.floorMod(
            targetFacing.getOpposite().get2DDataValue() - scannerFacing.get2DDataValue(),
            4
        );
        Rotation rotation = switch (quarterTurns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        BlockState transformedState = state.rotate(level, pos, rotation);
        return upsideDown ? flipVerticalProperties(transformedState) : transformedState;
    }

    private static BlockState flipVerticalProperties(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HALF)) {
            Half half = state.getValue(BlockStateProperties.HALF);
            state = state.setValue(BlockStateProperties.HALF, half == Half.TOP ? Half.BOTTOM : Half.TOP);
        }
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            SlabType slabType = state.getValue(BlockStateProperties.SLAB_TYPE);
            state = state.setValue(
                BlockStateProperties.SLAB_TYPE,
                switch (slabType) {
                    case BOTTOM -> SlabType.TOP;
                    case TOP -> SlabType.BOTTOM;
                    case DOUBLE -> SlabType.DOUBLE;
                }
            );
        }
        if (state.hasProperty(BlockStateProperties.VERTICAL_DIRECTION)) {
            Direction direction = state.getValue(BlockStateProperties.VERTICAL_DIRECTION);
            state = state.setValue(
                BlockStateProperties.VERTICAL_DIRECTION,
                direction == Direction.UP ? Direction.DOWN : Direction.UP
            );
        }
        return state;
    }

    public record BlueprintLayout(
        LevelAccessor level,
        BlockPos placerPos,
        Direction targetFacing,
        Direction scannerFacing,
        boolean upsideDown,
        int gridSize,
        int distance,
        BlockState[] states
    ) {
        public int getStorageIndexForOrder(int orderIndex) {
            int positionsPerLayer = this.gridSize * this.gridSize;
            int layer = orderIndex / positionsPerLayer;
            int inLayer = orderIndex % positionsPerLayer;
            int column = inLayer / this.gridSize;
            int row = inLayer % this.gridSize;
            return layer * positionsPerLayer + row * this.gridSize + column;
        }

        public BlockPos getPosition(int storageIndex) {
            return this.getPosition(storageIndex, this.targetFacing, this.upsideDown);
        }

        private BlockPos getPosition(int storageIndex, Direction targetFacing, boolean upsideDown) {
            int positionsPerLayer = this.gridSize * this.gridSize;
            int layer = storageIndex / positionsPerLayer;
            int position = storageIndex % positionsPerLayer;
            int row = position / this.gridSize;
            int column = position % this.gridSize;
            Direction right = targetFacing.getClockWise();
            int gridRadius = this.gridSize / 2;
            int verticalOffset = upsideDown ? layer - this.gridSize + 1 : layer;
            return this.placerPos.relative(targetFacing, this.distance)
                .above(verticalOffset)
                .relative(right, column - gridRadius)
                .relative(targetFacing, gridRadius - row);
        }

        public BlockState getState(int storageIndex) {
            if (storageIndex < 0 || storageIndex >= this.states.length) {
                return Blocks.AIR.defaultBlockState();
            }
            return transformBlueprintState(
                this.states[storageIndex],
                this.level,
                this.getPosition(storageIndex),
                this.targetFacing,
                this.scannerFacing,
                this.upsideDown
            );
        }

        public BlockState getState(int storageIndex, Direction targetFacing, boolean upsideDown) {
            if (storageIndex < 0 || storageIndex >= this.states.length) {
                return Blocks.AIR.defaultBlockState();
            }
            return transformBlueprintState(
                this.states[storageIndex],
                this.level,
                this.getPosition(storageIndex, targetFacing, upsideDown),
                targetFacing,
                this.scannerFacing,
                upsideDown
            );
        }

        public List<BlueprintPartSnapshot> capturePartSnapshots(Level level, BlockState requiredState) {
            List<BlueprintPartSnapshot> snapshots = new ArrayList<>();
            for (int storageIndex = 0; storageIndex < this.states.length; storageIndex++) {
                BlockState partState = this.getState(storageIndex);
                if (!partState.is(requiredState.getBlock())) {
                    continue;
                }
                BlockPos partPos = this.getPosition(storageIndex);
                snapshots.add(new BlueprintPartSnapshot(partPos, partState, level.getBlockState(partPos)));
            }
            return snapshots;
        }
    }

    public record BlueprintPartSnapshot(BlockPos pos, BlockState requiredState, BlockState previousState) {
    }

    private record PlacedBlueprintPart(BlockPos pos, BlockState requiredState, BlockState placedState) {
    }
}
