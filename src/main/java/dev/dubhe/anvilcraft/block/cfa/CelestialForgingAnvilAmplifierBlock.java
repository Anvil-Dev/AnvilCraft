package dev.dubhe.anvilcraft.block.cfa;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CelestialForgingAnvilAmplifierBlock
    extends FlexibleMultiPartBlock<DirectionCube232PartHalf, DirectionProperty, Direction>
    implements IHammerChangeable, IHammerRemovable {
    public static final EnumProperty<DirectionCube232PartHalf> HALF = EnumProperty.create("half", DirectionCube232PartHalf.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape NORTH_TIP = ShapeUtil.merge(
        new AABB(0, 0, 0, 16, 4, 16),
        new AABB(5, 5, 5, 10, 10, 10)
    );
    public static final VoxelShape WEST_TIP = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH_TIP);
    public static final VoxelShape SOUTH_TIP = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH_TIP);
    public static final VoxelShape EAST_TIP = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH_TIP);

    public static final VoxelShape NORTH_LEFT_WING = ShapeUtil.merge(
        new AABB(0, 0, 5, 11, 4, 16)
    );
    public static final VoxelShape WEST_LEFT_WING = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH_LEFT_WING);
    public static final VoxelShape SOUTH_LEFT_WING = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH_LEFT_WING);
    public static final VoxelShape EAST_LEFT_WING = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH_LEFT_WING);

    public static final VoxelShape NORTH_RIGHT_WING = ShapeUtil.rotate(
        Direction.Axis.Y,
        90,
        ShapeUtil.mirror(Direction.Axis.X, NORTH_LEFT_WING)
    );
    public static final VoxelShape WEST_RIGHT_WING = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH_RIGHT_WING);
    public static final VoxelShape SOUTH_RIGHT_WING = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH_RIGHT_WING);
    public static final VoxelShape EAST_RIGHT_WING = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH_RIGHT_WING);

    public static final VoxelShape NORTH_BASE = ShapeUtil.merge(
        new AABB(2, 0, 2, 16, 4, 16),
        new AABB(6, 4, 6, 16, 12, 16),
        new AABB(2, 12, 2, 16, 16, 16),

        new AABB(0, 0, 0, 10, 16, 10),

        new AABB(0, 0, 0, 2, 16, 2)
    );
    public static final VoxelShape WEST_BASE = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH_BASE);
    public static final VoxelShape SOUTH_BASE = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH_BASE);
    public static final VoxelShape EAST_BASE = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH_BASE);

    public static final VoxelShape NORTH_ADDITION = ShapeUtil.merge(
        new AABB(0, 0, 0, 4, 16, 4),
        new AABB(0, 0, 0, 13, 13, 13),
        new AABB(5, 13, 5, 13, 16, 13)
    );
    public static final VoxelShape WEST_ADDITION = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH_ADDITION);
    public static final VoxelShape SOUTH_ADDITION = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH_ADDITION);
    public static final VoxelShape EAST_ADDITION = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH_ADDITION);

    public static final VoxelShape NORTH_EXTRA = ShapeUtil.merge(
        new AABB(0, 0, 0, 4, 13, 4),
        new AABB(5, 0, 5, 13, 8, 13)
    );
    public static final VoxelShape WEST_EXTRA = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH_EXTRA);
    public static final VoxelShape SOUTH_EXTRA = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH_EXTRA);
    public static final VoxelShape EAST_EXTRA = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH_EXTRA);

    public CelestialForgingAnvilAmplifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.getStateDefinition().any()
                .setValue(HALF, DirectionCube232PartHalf.BOTTOM_PART)
                .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> switch (state.getValue(HALF)) {
                case BOTTOM_WS -> NORTH_TIP;
                case BOTTOM_S -> NORTH_LEFT_WING;
                case BOTTOM_W -> NORTH_RIGHT_WING;
                case BOTTOM_PART -> NORTH_BASE;
                case MID_PART -> NORTH_ADDITION;
                case TOP_PART -> NORTH_EXTRA;
                case TOP_W, TOP_S, TOP_WS, MID_W, MID_S, MID_WS -> Shapes.empty();
            };
            case SOUTH -> switch (state.getValue(HALF)) {
                case BOTTOM_PART -> SOUTH_TIP;
                case BOTTOM_W -> SOUTH_LEFT_WING;
                case BOTTOM_S -> SOUTH_RIGHT_WING;
                case BOTTOM_WS -> SOUTH_BASE;
                case MID_WS -> SOUTH_ADDITION;
                case TOP_WS -> SOUTH_EXTRA;
                case TOP_W, TOP_S, TOP_PART, MID_W, MID_S, MID_PART -> Shapes.empty();
            };
            case WEST -> switch (state.getValue(HALF)) {
                case BOTTOM_W -> WEST_TIP;
                case BOTTOM_WS -> WEST_LEFT_WING;
                case BOTTOM_PART -> WEST_RIGHT_WING;
                case BOTTOM_S -> WEST_BASE;
                case MID_S -> WEST_ADDITION;
                case TOP_S -> WEST_EXTRA;
                case TOP_W, TOP_WS, TOP_PART, MID_W, MID_WS, MID_PART -> Shapes.empty();
            };
            case EAST -> switch (state.getValue(HALF)) {
                case BOTTOM_S -> EAST_TIP;
                case BOTTOM_PART -> EAST_LEFT_WING;
                case BOTTOM_WS -> EAST_RIGHT_WING;
                case BOTTOM_W -> EAST_BASE;
                case MID_W -> EAST_ADDITION;
                case TOP_W -> EAST_EXTRA;
                case TOP_S, TOP_WS, TOP_PART, MID_S, MID_WS, MID_PART -> Shapes.empty();
            };
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState mapRealModelHolderBlock(Level level, BlockPos blockPos, BlockState original) {
        Direction direction = original.getValue(FACING);
        return switch (direction) {
            case NORTH -> original.setValue(HALF, DirectionCube232PartHalf.MID_PART);
            case EAST -> original.setValue(HALF, DirectionCube232PartHalf.MID_W);
            case SOUTH -> original.setValue(HALF, DirectionCube232PartHalf.MID_WS);
            case WEST -> original.setValue(HALF, DirectionCube232PartHalf.MID_S);
            default -> original;
        };
    }

    @Override
    public BlockState placedState(DirectionCube232PartHalf part, BlockState state) {
        return state.setValue(this.getPart(), part);
    }

    // 增幅器放置时自动检测锻星砧角落并确定朝向
    // 核心方块始终是 ES 角(BOTTOM_PART)，其余3块在西北侧
    // 内层方形(3×3)四角检测角落方块 → 确定 FACING
    // 外层方形(5×5)四角检测 BOTTOM_CENTER  → 确认真在砧角
    private static final int[][] CORNER_CHECKS = {
        { 1,  1, 0}, {-2,  1, 1}, { 1, -2, 2}, {-2, -2, 3},
    };
    private static final int[][] CENTER_CHECKS = {
        { 2,  2}, {-3,  2}, { 2, -3}, {-3, -3},
    };
    private static final Direction[] FACING_MAP = {
        Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH
    };

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // 1. 在内层方形四角检测锻星砧角落方块
        Direction facing = null;
        for (int[] c : CORNER_CHECKS) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockState state = level.getBlockState(pos.offset(c[0], dy, c[1]));
                if (state.getBlock() instanceof CelestialForgingAnvilBlock
                    && state.hasProperty(CelestialForgingAnvilBlock.HALF)
                    && isCornerPart(state.getValue(CelestialForgingAnvilBlock.HALF))) {
                    facing = FACING_MAP[c[2]];
                    break;
                }
            }
            if (facing != null) break;
        }

        // 2. 在外层方形四角验证锻星砧 BOTTOM_CENTER（排除孤立角落方块）
        if (facing != null) {
            boolean valid = false;
            for (int[] c : CENTER_CHECKS) {
                BlockState state = level.getBlockState(pos.offset(c[0], 0, c[1]));
                if (state.getBlock() instanceof CelestialForgingAnvilBlock
                    && state.hasProperty(CelestialForgingAnvilBlock.HALF)
                    && state.getValue(CelestialForgingAnvilBlock.HALF) == Cube323PartHalf.BOTTOM_CENTER) {
                    valid = true;
                    break;
                }
            }
            if (!valid) facing = null;
        }

        if (facing == null) {
            if (context.getPlayer() != null && !level.isClientSide) {
                context.getPlayer().displayClientMessage(
                    Component.translatable("block.anvilcraft.celestial_forging_anvil_amplifier.need_anvil_corner")
                        .withColor(0xFF8888), true);
            }
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private static boolean isCornerPart(Cube323PartHalf half) {
        return switch (half) {
            case BOTTOM_NW, BOTTOM_NE, BOTTOM_SW, BOTTOM_SE,
                 TOP_NW, TOP_NE, TOP_SW, TOP_SE -> true;
            default -> false;
        };
    }

    @Override
    public Property<DirectionCube232PartHalf> getPart() {
        return HALF;
    }

    @Override
    public DirectionCube232PartHalf[] getParts() {
        return DirectionCube232PartHalf.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        this.change(blockPos, level, (state) -> state.cycle(FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

}
