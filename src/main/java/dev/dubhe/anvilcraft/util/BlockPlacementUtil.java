package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.item.IBlockItem;
import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.LensBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.item.LargeCakeBlockItem;
import dev.dubhe.anvilcraft.block.item.PipeBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
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

    /**
     * 状态值即放置数量的整数属性（雪层、海泡菜、蛋、蜡烛、花朵数量）。
     */
    public static final List<IntegerProperty> COUNT_PROPERTIES = List.of(
        BlockStateProperties.LAYERS,
        BlockStateProperties.PICKLES,
        BlockStateProperties.EGGS,
        BlockStateProperties.CANDLES,
        BlockStateProperties.FLOWER_AMOUNT
    );

    public static boolean isMultifaceLike(Block block) {
        return block instanceof MultifaceBlock || block instanceof VineBlock;
    }

    public static ItemStack placeBlock(
        ServerLevel level,
        BlockPos pos,
        ItemStack stack,
        @Nullable BlockState requiredState
    ) {
        return placeBlock(level, pos, stack, requiredState, null);
    }

    public static ItemStack placeBlock(
        ServerLevel level,
        BlockPos pos,
        ItemStack stack,
        @Nullable BlockState requiredState,
        @Nullable Direction defaultFacing
    ) {
        if (requiredState != null && requiredState.getBlock() instanceof PipeBlock pipe
            && pipe.isGlassPipe() && stack.is(ModItems.PIPE)) {
            ItemStack remaining = placeBlock(level, pos, stack.transmuteCopy(ModItems.GLASS_PIPE.get()), requiredState, defaultFacing);
            return stack.copyWithCount(remaining.getCount());
        }
        // 桶 → 炼药锅：消耗一桶流体，放置目标锅状态
        // （状态转换如火锅 → 油锅由蓝图状态规则处理）
        if (stack.getItem() instanceof BucketItem && requiredState != null
            && requiredState.getBlock() instanceof AbstractCauldronBlock) {
            if (level.setBlock(pos, requiredState, Block.UPDATE_ALL)) {
                return stack.copyWithCount(stack.getCount() - 1);
            }
            return stack;
        }
        IBlockItem blockItem = switch (stack.getItem()) {
            case PipeBlockItem item -> (world, target, player, hand) -> item.place(world, target, player, hand, requiredState);
            case IBlockItem item -> item;
            case BlockItem item when requiredState != null && requiredState.getBlock() instanceof AbstractSkullBlock ->
                (world, target, player, hand) -> placeSkull(item, world, target, player, hand, requiredState);
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
            } else if (defaultFacing != null) {
                // 无目标状态（定点/自由放置）时按机器朝向决定放置方向，避免始终朝北；
                // 垂直朝向同样生效（抬头/低头放置）。
                orientPlayerForDirection(player, defaultFacing);
            }
            BlockPos placementPos = pos;
            if (requiredState != null && requiredState.getBlock() instanceof AbstractMultiPartBlock<?> multiPartBlock) {
                placementPos = getMultiblockPlacementPos(pos, requiredState, multiPartBlock);
            } else if (requiredState != null && requiredState.getBlock() instanceof LargeCakeBlock) {
                placementPos = LargeCakeBlockItem.origin(pos, requiredState);
            }
            blockItem.place(level, placementPos, player, InteractionHand.MAIN_HAND);
            return player.getMainHandItem();
        } finally {
            AnvilCraftFakePlayers.getBlockPlacer().disable(player);
        }
    }

    private static boolean placeSkull(
        BlockItem item, Level level, BlockPos pos, Player player, InteractionHand hand, BlockState requiredState
    ) {
        Direction attachmentDirection = requiredState.getBlock() instanceof WallSkullBlock
            ? requiredState.getValue(WallSkullBlock.FACING).getOpposite()
            : Direction.DOWN;
        BlockPlaceContext context = new BlockPlaceContext(
            level,
            player,
            hand,
            player.getItemInHand(hand),
            new BlockHitResult(pos.getCenter(), attachmentDirection.getOpposite(), pos, false)
        ) {
            @Override
            public Direction[] getNearestLookingDirections() {
                // 只尝试蓝图指定的附着面，避免回退到另一种头颅方块后被蓝图校验移除。
                return new Direction[]{attachmentDirection};
            }

            @Override
            public float getRotation() {
                return requiredState.getBlock() instanceof SkullBlock
                    ? RotationSegment.convertToDegrees(requiredState.getValue(SkullBlock.ROTATION))
                    : super.getRotation();
            }
        };
        return context.getClickedPos().equals(pos) && item.place(context).consumesAction();
    }

    private static <P extends Enum<P>> BlockPos getMultiblockPlacementPos(
        BlockPos pos, BlockState requiredState, AbstractMultiPartBlock<P> block
    ) {
        // 蓝图定位到核心部件，物品放置则从默认部件开始生成整个结构。
        P placementPart = block.defaultBlockState().getValue(block.getPart());
        return pos.offset(block.offsetFrom(requiredState, placementPart));
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
        orientPlayerForDirection(player, facing);
    }

    /** 按目标方向旋转假玩家，使 {@link BlockItem#place} 放置出对应朝向的方块。 */
    private static void orientPlayerForDirection(ServerPlayer player, Direction facing) {
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

    public static boolean areTargetsAvailable(Level level, List<BlueprintPartSnapshot> snapshots) {
        return !snapshots.isEmpty()
            && snapshots.stream().allMatch(snapshot -> isTargetAvailable(level, snapshot.pos()));
    }

    public static boolean applyBlueprintStates(ServerLevel level, List<BlueprintPartSnapshot> snapshots) {
        List<PlacedBlueprintPart> placedParts = new ArrayList<>();
        for (BlueprintPartSnapshot snapshot : snapshots) {
            BlockState placedState = level.getBlockState(snapshot.pos());
            BlockState requiredState = snapshot.requiredState();
            // 蓝图状态经规则转换后的目标方块（如火锅 → 油锅）
            BlockState transformedRequired = BlockPlacementRules.applyBlueprintStateRules(
                level.registryAccess(),
                requiredState,
                requiredState
            );
            if (!snapshot.previousState().is(requiredState.getBlock())
                && (placedState.is(requiredState.getBlock())
                    || placedState.is(transformedRequired.getBlock()))) {
                placedParts.add(new PlacedBlueprintPart(snapshot.pos(), requiredState, placedState));
            }
        }
        if (placedParts.size() != snapshots.size()) {
            restoreBlueprintSnapshots(level, snapshots);
            return false;
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
            BlockState transformedRequired = BlockPlacementRules.applyBlueprintStateRules(
                level.registryAccess(),
                part.requiredState(),
                part.requiredState()
            );
            contextualStates.add(finalState);
            if ((!finalState.is(part.requiredState().getBlock())
                && !finalState.is(transformedRequired.getBlock()))
                || !finalState.canSurvive(level, part.pos())) {
                valid = false;
            }
        }

        if (!valid) {
            restoreBlueprintSnapshots(level, snapshots);
            return false;
        }
        for (int index = 0; index < placedParts.size(); index++) {
            level.setBlock(placedParts.get(index).pos(), contextualStates.get(index), Block.UPDATE_ALL);
        }
        return true;
    }

    private static void restoreBlueprintSnapshots(ServerLevel level, List<BlueprintPartSnapshot> snapshots) {
        for (BlueprintPartSnapshot snapshot : snapshots) {
            level.setBlock(snapshot.pos(), snapshot.previousState(), Block.UPDATE_ALL);
        }
    }

    public static boolean isBlueprintStatePresent(Level level, BlockPos pos, BlockState requiredState) {
        BlockState worldState = level.getBlockState(pos);
        if (!worldState.is(requiredState.getBlock())
            || worldState.getBlock() instanceof LensBlock
                && worldState.getValue(LensBlock.TYPE) != requiredState.getValue(LensBlock.TYPE)) {
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

    public static boolean isSecondaryBlueprintPart(BlockState state) {
        return isSecondaryMultiblockPart(state) || state.getBlock() instanceof LargeCakeBlock
            && state.getValue(LargeCakeBlock.HALF) != Cube3x3PartHalf.BOTTOM_CENTER;
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

    public static List<MultiblockPart> getExpectedMultiblockParts(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof AbstractMultiPartBlock<?> multiPartBlock) {
            return getExpectedCustomMultiblockParts(pos, state, multiPartBlock);
        }
        if (block instanceof BedBlock) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos footPos = state.getValue(BED_PART) == BedPart.HEAD
                ? pos.relative(facing.getOpposite())
                : pos;
            BlockState footState = state.setValue(BED_PART, BedPart.FOOT);
            return List.of(
                new MultiblockPart(footPos, footState),
                new MultiblockPart(footPos.relative(facing), footState.setValue(BED_PART, BedPart.HEAD))
            );
        }
        if (block instanceof DoorBlock || block instanceof DoublePlantBlock) {
            BlockPos lowerPos = state.getValue(DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockState lowerState = state.setValue(DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            return List.of(
                new MultiblockPart(lowerPos, lowerState),
                new MultiblockPart(lowerPos.above(), lowerState.setValue(DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER))
            );
        }
        return List.of(new MultiblockPart(pos, state));
    }

    private static <P extends Enum<P>> List<MultiblockPart> getExpectedCustomMultiblockParts(
        BlockPos pos,
        BlockState state,
        AbstractMultiPartBlock<P> block
    ) {
        BlockPos mainPos = block.getMainPartPos(pos, state);
        BlockState mainState = state;
        for (P part : block.getParts()) {
            BlockState partState = block.placedState(part, state);
            if (block.isMainPart(partState)) {
                mainState = partState;
                break;
            }
        }
        List<MultiblockPart> parts = new ArrayList<>();
        for (P part : block.getParts()) {
            parts.add(new MultiblockPart(
                mainPos.offset(block.offsetFrom(mainState, part)),
                block.placedState(part, mainState)
            ));
        }
        return List.copyOf(parts);
    }

    public static List<MultiblockPart> getPresentMultiblockParts(Level level, BlockPos pos, BlockState state) {
        List<MultiblockPart> expectedParts = getExpectedMultiblockParts(pos, state);
        List<MultiblockPart> presentParts = new ArrayList<>(expectedParts.size());
        for (MultiblockPart expectedPart : expectedParts) {
            BlockState presentState = level.getBlockState(expectedPart.pos());
            if (!matchesMultiblockPart(presentState, expectedPart.state())) {
                return List.of();
            }
            presentParts.add(new MultiblockPart(expectedPart.pos(), presentState));
        }
        return List.copyOf(presentParts);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean matchesMultiblockPart(BlockState state, BlockState expectedState) {
        if (!state.is(expectedState.getBlock())) {
            return false;
        }
        Block block = state.getBlock();
        switch (block) {
            case AbstractMultiPartBlock<?> multiPartBlock -> {
                Property<?> partProperty = multiPartBlock.getPart();
                return state.getOptionalValue(partProperty).equals(expectedState.getOptionalValue(partProperty));
            }
            case LargeCakeBlock largeCakeBlock -> {
                return state.getValue(LargeCakeBlock.HALF) == expectedState.getValue(LargeCakeBlock.HALF);
            }
            case BedBlock bedBlock -> {
                return state.getValue(BED_PART) == expectedState.getValue(BED_PART);
            }
            default -> {
            }
        }
        if (block instanceof DoorBlock || block instanceof DoublePlantBlock) {
            return state.getValue(DOUBLE_BLOCK_HALF) == expectedState.getValue(DOUBLE_BLOCK_HALF);
        }
        return true;
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

    /**
     * 蓝图在存储网格中占用的范围，用于把放置范围收缩到蓝图实际大小。
     */
    public record BlueprintExtent(int minRow, int maxRow, int minColumn, int maxColumn, int maxLayer) {
        /**
         * 扫描存储网格得到蓝图占用的行列与最高层；全为空气时退化为网格中心的一格。
         */
        public static BlueprintExtent of(BlockState[] states, int gridSize) {
            int positionsPerLayer = gridSize * gridSize;
            int minRow = gridSize;
            int maxRow = -1;
            int minColumn = gridSize;
            int maxColumn = -1;
            int maxLayer = -1;
            for (int index = 0; index < states.length; index++) {
                if (states[index].isAir()) {
                    continue;
                }
                int position = index % positionsPerLayer;
                int row = position / gridSize;
                int column = position % gridSize;
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
                minColumn = Math.min(minColumn, column);
                maxColumn = Math.max(maxColumn, column);
                maxLayer = Math.max(maxLayer, index / positionsPerLayer);
            }
            if (maxLayer < 0) {
                int center = gridSize / 2;
                return new BlueprintExtent(center, center, center, center, 0);
            }
            return new BlueprintExtent(minRow, maxRow, minColumn, maxColumn, maxLayer);
        }

        /**
         * 按蓝图自带的大小信息与居中偏移构造占用范围，无需扫描方块。
         */
        public static BlueprintExtent fromSize(
            int width,
            int depth,
            int height,
            int columnOffset,
            int rowOffset,
            int layerOffset
        ) {
            return new BlueprintExtent(
                rowOffset,
                rowOffset + depth - 1,
                columnOffset,
                columnOffset + width - 1,
                layerOffset + height - 1
            );
        }

        /**
         * 把占用范围裁剪到存储网格内；越界的部分会被丢弃，故裁剪后可能只占部分格。
         */
        public BlueprintExtent clamp(int gridSize) {
            int minRow = Math.clamp(this.minRow, 0, gridSize - 1);
            int maxRow = Math.clamp(this.maxRow, 0, gridSize - 1);
            int minColumn = Math.clamp(this.minColumn, 0, gridSize - 1);
            int maxColumn = Math.clamp(this.maxColumn, 0, gridSize - 1);
            int maxLayer = Math.clamp(this.maxLayer, 0, gridSize - 1);
            return new BlueprintExtent(minRow, maxRow, minColumn, maxColumn, maxLayer);
        }

        public int width() {
            return this.maxColumn - this.minColumn + 1;
        }

        public int depth() {
            return this.maxRow - this.minRow + 1;
        }

        public int height() {
            return this.maxLayer + 1;
        }
    }

    public record BlueprintLayout(
        LevelAccessor level,
        BlockPos placerPos,
        Direction targetFacing,
        Direction scannerFacing,
        boolean autoRotate,
        boolean upsideDown,
        int gridSize,
        int distance,
        BlueprintExtent extent,
        BlockState[] states
    ) {
        /**
         * 顺序索引按 层（下→上）→ 行（远→近）→ 列（左→右）排列，与普通点位模式的放置顺序保持一致。<br>
         * 放置范围收缩到蓝图实际大小，且蓝图未自动旋转时网格轴向与放置方向存在夹角，
         * 因此需按蓝图尺寸把顺序中的物理行列换算回网格行列。
         */
        public int getStorageIndexForOrder(int orderIndex) {
            boolean rotated = this.getLayoutRotation() % 2 == 1;
            int forwardSpan = rotated ? this.extent.width() : this.extent.depth();
            int lateralSpan = rotated ? this.extent.depth() : this.extent.width();
            int perLayer = forwardSpan * lateralSpan;
            int layer = orderIndex / perLayer;
            int local = orderIndex % perLayer;
            int forward = local / lateralSpan;
            int lateral = local % lateralSpan;
            int row;
            int column;
            switch (this.getLayoutRotation()) {
                case 1 -> {
                    row = this.extent.maxRow() - lateral;
                    column = this.extent.minColumn() + forward;
                }
                case 2 -> {
                    row = this.extent.maxRow() - forward;
                    column = this.extent.maxColumn() - lateral;
                }
                case 3 -> {
                    row = this.extent.minRow() + lateral;
                    column = this.extent.maxColumn() - forward;
                }
                default -> {
                    row = this.extent.minRow() + forward;
                    column = this.extent.minColumn() + lateral;
                }
            }
            return layer * this.gridSize * this.gridSize + row * this.gridSize + column;
        }

        /** 放置顺序索引的总数，即蓝图占用的格数。 */
        public int orderCount() {
            return this.extent.width() * this.extent.depth() * this.extent.height();
        }

        /**
         * 放置范围的世界包围盒，即蓝图占用格在放置方向、左右方向与垂直方向上的跨度。
         */
        public AABB getRangeBounds() {
            return this.getRangeBounds(this.targetFacing, this.upsideDown);
        }

        /**
         * 指定放置方向与倒挂状态下的放置范围包围盒。
         */
        public AABB getRangeBounds(Direction targetFacing, boolean upsideDown) {
            int positionsPerLayer = this.gridSize * this.gridSize;
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (int layer = 0; layer <= this.extent.maxLayer(); layer++) {
                for (int row = this.extent.minRow(); row <= this.extent.maxRow(); row++) {
                    for (int column = this.extent.minColumn(); column <= this.extent.maxColumn(); column++) {
                        BlockPos pos = this.getPosition(
                            layer * positionsPerLayer + row * this.gridSize + column,
                            targetFacing,
                            upsideDown
                        );
                        minX = Math.min(minX, pos.getX());
                        minY = Math.min(minY, pos.getY());
                        minZ = Math.min(minZ, pos.getZ());
                        maxX = Math.max(maxX, pos.getX() + 1.0);
                        maxY = Math.max(maxY, pos.getY() + 1.0);
                        maxZ = Math.max(maxZ, pos.getZ() + 1.0);
                    }
                }
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        /**
         * 网格行轴相对放置方向的顺时针旋转，0 表示行轴与放置方向同向。
         */
        private int getLayoutRotation() {
            return Math.floorMod(
                this.getLayoutFacing(this.targetFacing).get2DDataValue() - this.targetFacing.get2DDataValue(),
                4
            );
        }

        public BlockPos getPosition(int storageIndex) {
            return this.getPosition(storageIndex, this.targetFacing, this.upsideDown);
        }

        public BlockPos getPosition(int storageIndex, Direction targetFacing, boolean upsideDown) {
            int positionsPerLayer = this.gridSize * this.gridSize;
            int layer = storageIndex / positionsPerLayer;
            int position = storageIndex % positionsPerLayer;
            int row = position / this.gridSize;
            int column = position % this.gridSize;
            int gridRadius = this.gridSize / 2;
            // 放置范围收缩到蓝图大小：最近的方块与放置器相隔一格，其余方向按蓝图居中
            int forwardIndex = this.getMaxForwardComponent() - this.getForwardComponent(row, column);
            int forwardOffset = this.distance - gridRadius + this.getForwardSpan() - 1 - forwardIndex;
            int lateralOffset = this.getLateralComponent(row, column) - this.getLateralComponentMidpoint();
            int verticalOffset = upsideDown ? layer - this.extent.maxLayer() : layer;
            return this.placerPos.relative(targetFacing, forwardOffset)
                .relative(targetFacing.getClockWise(), lateralOffset)
                .above(verticalOffset);
        }

        /**
         * 以放置方向为基准，蓝图在前后轴上的跨度；旋转 90°/270° 时前后轴对应网格列轴。
         */
        private int getForwardSpan() {
            return this.getLayoutRotation() % 2 == 1 ? this.extent.width() : this.extent.depth();
        }

        /**
         * 存储格在放置方向上的前后分量，数值越大离放置器越远。
         */
        private int getForwardComponent(int row, int column) {
            int gridRadius = this.gridSize / 2;
            return switch (this.getLayoutRotation()) {
                case 1 -> gridRadius - column;
                case 2 -> row - gridRadius;
                case 3 -> column - gridRadius;
                default -> gridRadius - row;
            };
        }

        /**
         * 存储格在放置方向上的左右分量，数值越大越靠右。
         */
        private int getLateralComponent(int row, int column) {
            int gridRadius = this.gridSize / 2;
            return switch (this.getLayoutRotation()) {
                case 1 -> gridRadius - row;
                case 2 -> gridRadius - column;
                case 3 -> row - gridRadius;
                default -> column - gridRadius;
            };
        }

        /**
         * 蓝图占用范围内最远一格的前后分量。
         */
        private int getMaxForwardComponent() {
            int gridRadius = this.gridSize / 2;
            return switch (this.getLayoutRotation()) {
                case 1 -> gridRadius - this.extent.minColumn();
                case 2 -> this.extent.maxRow() - gridRadius;
                case 3 -> this.extent.maxColumn() - gridRadius;
                default -> gridRadius - this.extent.minRow();
            };
        }

        /**
         * 蓝图占用范围内左右分量的中点，用于把蓝图左右居中。
         */
        private int getLateralComponentMidpoint() {
            int gridRadius = this.gridSize / 2;
            return switch (this.getLayoutRotation()) {
                case 1 -> gridRadius - (this.extent.minRow() + this.extent.maxRow()) / 2;
                case 2 -> gridRadius - (this.extent.minColumn() + this.extent.maxColumn()) / 2;
                case 3 -> (this.extent.minRow() + this.extent.maxRow()) / 2 - gridRadius;
                default -> (this.extent.minColumn() + this.extent.maxColumn()) / 2 - gridRadius;
            };
        }

        private Direction getLayoutFacing(Direction targetFacing) {
            if (this.autoRotate) {
                return targetFacing;
            }
            return Direction.from2DDataValue(Math.floorMod(
                this.scannerFacing.getOpposite().get2DDataValue()
                    + targetFacing.get2DDataValue() - this.targetFacing.get2DDataValue(),
                4
            ));
        }

        public BlockState getState(int storageIndex) {
            if (storageIndex < 0 || storageIndex >= this.states.length) {
                return Blocks.AIR.defaultBlockState();
            }
            return transformBlueprintState(
                this.states[storageIndex],
                this.level,
                this.getPosition(storageIndex),
                this.getLayoutFacing(this.targetFacing),
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
                this.getLayoutFacing(targetFacing),
                this.scannerFacing,
                upsideDown
            );
        }

        public List<BlueprintPartSnapshot> capturePartSnapshots(
            Level level,
            BlockPos targetPos,
            BlockState requiredState
        ) {
            List<BlueprintPartSnapshot> snapshots = new ArrayList<>();
            List<MultiblockPart> expected = new ArrayList<>();
            if (requiredState.getBlock() instanceof LargeCakeBlock) {
                LargeCakeBlockItem.forEachPlacedBlock(LargeCakeBlockItem.origin(targetPos, requiredState), requiredState,
                    (pos, state) -> expected.add(new MultiblockPart(pos, state)));
            } else {
                expected.addAll(getExpectedMultiblockParts(targetPos, requiredState));
            }
            for (MultiblockPart expectedPart : expected) {
                int storageIndex = this.getStorageIndex(expectedPart.pos());
                if (storageIndex < 0) {
                    return List.of();
                }
                BlockState partState = this.getState(storageIndex);
                if (!matchesMultiblockPart(partState, expectedPart.state())) {
                    return List.of();
                }
                snapshots.add(new BlueprintPartSnapshot(
                    expectedPart.pos(),
                    partState,
                    level.getBlockState(expectedPart.pos())
                ));
            }
            return List.copyOf(snapshots);
        }

        private int getStorageIndex(BlockPos pos) {
            for (int storageIndex = 0; storageIndex < this.states.length; storageIndex++) {
                if (this.getPosition(storageIndex).equals(pos)) {
                    return storageIndex;
                }
            }
            return -1;
        }
    }

    public record MultiblockPart(BlockPos pos, BlockState state) {
    }

    public record BlueprintPartSnapshot(BlockPos pos, BlockState requiredState, BlockState previousState) {
    }

    private record PlacedBlueprintPart(BlockPos pos, BlockState requiredState, BlockState placedState) {
    }
}
