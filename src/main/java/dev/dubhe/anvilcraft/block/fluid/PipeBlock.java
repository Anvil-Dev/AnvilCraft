package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 管道系统抽象基类，定义了所有管道类型共用的方块状态属性、碰撞箱形状、
 * 邻居检查工具方法和两个核心枚举。
 *
 * <p>子类：
 * <ul>
 *   <li>{@link PipeStraightBlock} — 直管，沿单一轴向延伸</li>
 *   <li>{@link PipeCornerBlock} — 弯管，连接两个垂直方向</li>
 *   <li>{@link PipeNodeBlock} — 节点，最多六个方向连接，可自动退化</li>
 * </ul>
 *
 * <p>管道连接规则：
 * <ul>
 *   <li>管道 ↔ 管道：无端头（开放连接）</li>
 *   <li>管道 ↔ IFluidHandler：有端头（封闭连接）</li>
 *   <li>管道 ↔ 空气/其他：有端头</li>
 * </ul>
 */
public abstract class PipeBlock extends Block implements SimpleWaterloggedBlock, IHammerRemovable, EntityBlock, IHammerChangeable {

    /**
     * 直管的轴向（X / Y / Z）
     */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /**
     * 弯管的拐角方向组合
     */
    public static final EnumProperty<CornerEnded> CORNER_ENDED = EnumProperty.create("corner_ended", CornerEnded.class);
    /**
     * 第一端（直管负轴端/弯管 first 方向）是否有端头
     */
    public static final BooleanProperty HAS_END_START = BooleanProperty.create("has_end_start");
    /**
     * 第二端（直管正轴端/弯管 second 方向）是否有端头
     */
    public static final BooleanProperty HAS_END_END = BooleanProperty.create("has_end_end");
    /**
     * 节点下方连接状态
     */
    public static final EnumProperty<NodePipe> DOWN = EnumProperty.create("down", NodePipe.class);
    /**
     * 节点上方连接状态
     */
    public static final EnumProperty<NodePipe> UP = EnumProperty.create("up", NodePipe.class);
    /**
     * 节点北向连接状态
     */
    public static final EnumProperty<NodePipe> NORTH = EnumProperty.create("north", NodePipe.class);
    /**
     * 节点南向连接状态
     */
    public static final EnumProperty<NodePipe> SOUTH = EnumProperty.create("south", NodePipe.class);
    /**
     * 节点西向连接状态
     */
    public static final EnumProperty<NodePipe> WEST = EnumProperty.create("west", NodePipe.class);
    /**
     * 节点东向连接状态
     */
    public static final EnumProperty<NodePipe> EAST = EnumProperty.create("east", NodePipe.class);
    /**
     * 是否含水
     */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * 管道中心体碰撞箱（对应 pipe_straight / pipe_side_corner 模型 [4,4,4]→[12,12,12]）
     */
    static final VoxelShape PIPE_CENTER = box(4, 4, 4, 12, 12, 12);
    /**
     * 节点中心体碰撞箱（对应 pipe_node 模型 [3,3,3]→[13,13,13]）
     */
    static final VoxelShape NODE_CENTER = box(3, 3, 3, 13, 13, 13);

    /**
     * 创建指定方向的无端头臂碰撞箱（对应 pipe_no_end 模型）。
     * 从中心体表面延伸到方块边界，4 px 深，8×8 截面。
     */
    static VoxelShape makeNoEnd(Direction dir) {
        return switch (dir) {
            case DOWN -> box(4, 0, 4, 12, 4, 12);
            case UP -> box(4, 12, 4, 12, 16, 12);
            case NORTH -> box(4, 4, 0, 12, 12, 4);
            case SOUTH -> box(4, 4, 12, 12, 12, 16);
            case WEST -> box(0, 4, 4, 4, 12, 12);
            case EAST -> box(12, 4, 4, 16, 12, 12);
        };
    }

    /**
     * 创建指定方向的有端头臂碰撞箱（对应 pipe_end 模型）。
     * ring（2 px 深，8×8 截面）+ cap（2 px 深，10×10 截面，与面齐平）。
     */
    static VoxelShape makeEnd(Direction dir) {
        // ring：内层，紧贴中心体，8×8 截面
        VoxelShape ring = switch (dir) {
            case DOWN -> box(4, 2, 4, 12, 4, 12);
            case UP -> box(4, 12, 4, 12, 14, 12);
            case NORTH -> box(4, 4, 2, 12, 12, 4);
            case SOUTH -> box(4, 4, 12, 12, 12, 14);
            case WEST -> box(2, 4, 4, 4, 12, 12);
            case EAST -> box(12, 4, 4, 14, 12, 12);
        };
        // cap：外层，与方块面齐平，10×10 截面
        VoxelShape cap = switch (dir) {
            case DOWN -> box(3, 0, 3, 13, 2, 13);
            case UP -> box(3, 14, 3, 13, 16, 13);
            case NORTH -> box(3, 3, 0, 13, 13, 2);
            case SOUTH -> box(3, 3, 14, 13, 13, 16);
            case WEST -> box(0, 3, 3, 2, 13, 13);
            case EAST -> box(14, 3, 3, 16, 13, 13);
        };
        return Shapes.or(ring, cap);
    }

    public PipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    /**
     * 根据轴向和轴方向获取对应的 {@link Direction}。
     *
     * @param axis          轴向
     * @param axisDirection 轴方向（NEGATIVE = 负方向，POSITIVE = 正方向）
     * @return 对应的方向（如 X+NEGATIVE → WEST）
     */
    public static Direction getDirectionFromAxis(Direction.Axis axis, Direction.AxisDirection axisDirection) {
        return Direction.get(axisDirection, axis);
    }

    /**
     * 获取指定方向对应的节点连接属性。
     *
     * @param direction 方向
     * @return 对应方向的 {@link EnumProperty}&lt;{@link NodePipe}&gt;
     */
    public static EnumProperty<NodePipe> getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    /**
     * 判断指定方块状态在给定方向上是否有管道连接（不考虑端头状态）。
     * <ul>
     *   <li>直管：方向与轴向相同即为有连接</li>
     *   <li>弯管：方向为弯管两方向之一即为有连接</li>
     *   <li>节点：该方向为 {@link NodePipe#PIPE} 即为有连接</li>
     * </ul>
     *
     * @param state  方块状态
     * @param toward 从此方块看向邻居的方向
     * @return 是否有管道连接朝向该方向
     */
    public static boolean hasConnectionToward(BlockState state, Direction toward) {
        Block block = state.getBlock();
        return switch (block) {
            case PipeStraightBlock ignored -> toward.getAxis() == state.getValue(AXIS);
            case PipeCornerBlock ignored -> state.getValue(CORNER_ENDED).containsDirection(toward);
            case PipeNodeBlock ignored -> state.getValue(getPropertyForDirection(toward)) == NodePipe.PIPE;
            default -> false;
        };
    }

    /**
     * 检查指定方向的邻居是否为管道且其连接朝向本方块。
     * 用于判断本方块是否应与此邻居建立管道连接。
     *
     * @param level 世界
     * @param pos   本方块位置
     * @param dir   从此方块看向邻居的方向
     * @return 邻居管道是否朝向本方块
     */
    public static boolean isNeighborPipeToward(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getBlock() instanceof PipeBlock && hasConnectionToward(neighborState, dir.getOpposite());
    }

    /**
     * 检查指定位置是否为流体处理器（通过 NeoForge Capability 系统）。
     *
     * @param level 世界
     * @param pos   位置
     * @return 该位置是否提供 {@link net.neoforged.neoforge.fluids.capability.IFluidHandler}
     */
    public static boolean isFluidHandler(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, be, null) != null;
    }

    /**
     * 检查指定位置是否为流体处理器（通过 NeoForge Capability 系统）或泵。
     *
     * @param level 世界
     * @param pos   位置
     * @return 该位置是否提供 {@link net.neoforged.neoforge.fluids.capability.IFluidHandler} 或是泵
     */
    public static boolean isFluidHandlerOrPump(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PumpBlock) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, be, null) != null;
    }

    /**
     * 检查指定方向的邻居是否被"占用"（有管道对准 或 是流体处理器）。
     * 用于判断管道端头是否应该打开（无端头连接）。
     *
     * @param level 世界
     * @param pos   本方块位置
     * @param dir   从此方块看向邻居的方向
     * @return 邻居是否占用该端
     */
    public static boolean isNeighborOccupied(Level level, BlockPos pos, Direction dir) {
        if (isNeighborPipeToward(level, pos, dir)) {
            return true;
        }
        return isFluidHandler(level, pos.relative(dir));
    }

    @Override
    public Item asItem() {
        return ModItems.PIPE.get();
    }

    /**
     * 更新直管/弯管的端头状态。
     * 根据邻居是否为管道来决定端头开关：
     * <ul>
     *   <li>邻居是管道 → {@code HAS_END_*} = false（无端头，开放）</li>
     *   <li>邻居非管道 → {@code HAS_END_*} = true（有端头，封闭）</li>
     * </ul>
     *
     * @param level                世界
     * @param pos                  方块位置
     * @param state                当前方块状态
     * @param startDir             第一端方向（用于区分 HAS_END_START / HAS_END_END）
     * @param neighborDir          邻居方向
     * @param neighborIsPipeToward 邻居是否为对准的管道
     */
    protected void changePipeState(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction startDir,
        Direction neighborDir,
        boolean neighborIsPipeToward
    ) {
        BlockState newState = state;
        if (neighborDir == startDir) {
            newState = newState.setValue(HAS_END_START, !neighborIsPipeToward);
        } else {
            newState = newState.setValue(HAS_END_END, !neighborIsPipeToward);
        }

        if (newState != state) {
            level.setBlockAndUpdate(pos, newState);
        }
    }

    @Override
    protected BlockState updateShape(
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

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * 构建直管/弯管的碰撞箱：中心体 + 两端按端头状态拼接 arm。
     *
     * @param state    方块状态
     * @param startDir 第一端方向
     * @param endDir   第二端方向
     * @return 完整碰撞箱
     */
    public VoxelShape getShape(BlockState state, Direction startDir, Direction endDir) {
        VoxelShape shape = PIPE_CENTER;
        if (state.getValue(HAS_END_START)) {
            shape = Shapes.or(shape, makeEnd(startDir));
        } else {
            shape = Shapes.or(shape, makeNoEnd(startDir));
        }
        if (state.getValue(HAS_END_END)) {
            shape = Shapes.or(shape, makeEnd(endDir));
        } else {
            shape = Shapes.or(shape, makeNoEnd(endDir));
        }
        return shape;
    }

    @Override
    public boolean checkBlockState(BlockState blockState) {
        return false;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        return false;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return null;
    }

    /**
     * 弯管拐角方向枚举，表示管道在两个垂直方向之间的转弯。
     *
     * <p>命名规则：{@code 第一方向_第二方向}（如 UP_NORTH = 上→北）。
     * {@code HAS_END_START} 控制第一方向的端头，{@code HAS_END_END} 控制第二方向。
     */
    public enum CornerEnded implements StringRepresentable {
        /**
         * 下 → 北
         */
        DOWN_NORTH(Direction.DOWN, Direction.NORTH),
        /**
         * 下 → 南
         */
        DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
        /**
         * 下 → 西
         */
        DOWN_WEST(Direction.DOWN, Direction.WEST),
        /**
         * 下 → 东
         */
        DOWN_EAST(Direction.DOWN, Direction.EAST),
        /**
         * 上 → 北
         */
        UP_NORTH(Direction.UP, Direction.NORTH),
        /**
         * 上 → 南
         */
        UP_SOUTH(Direction.UP, Direction.SOUTH),
        /**
         * 上 → 西
         */
        UP_WEST(Direction.UP, Direction.WEST),
        /**
         * 上 → 东
         */
        UP_EAST(Direction.UP, Direction.EAST),
        /**
         * 北 → 西
         */
        NORTH_WEST(Direction.NORTH, Direction.WEST),
        /**
         * 北 → 东
         */
        NORTH_EAST(Direction.NORTH, Direction.EAST),
        /**
         * 南 → 西
         */
        SOUTH_WEST(Direction.SOUTH, Direction.WEST),
        /**
         * 南 → 东
         */
        SOUTH_EAST(Direction.SOUTH, Direction.EAST);

        private final Direction first;
        private final Direction second;

        CornerEnded(Direction first, Direction second) {
            this.first = first;
            this.second = second;
        }

        /**
         * 获取第一方向
         *
         * @return 第一方向（受 {@link PipeBlock#HAS_END_START} 控制）
         */
        public Direction getFirstDirection() {
            return first;
        }

        /**
         * 获取第二方向
         *
         * @return 第二方向（受 {@link PipeBlock#HAS_END_END} 控制）
         */
        public Direction getSecondDirection() {
            return second;
        }

        /**
         * 该弯管是否包含指定方向
         *
         * @return 该弯管是否包含指定方向
         */
        public boolean containsDirection(Direction direction) {
            return first == direction || second == direction;
        }

        /**
         * 根据两个方向查找匹配的弯管配置。顺序无关（a→b 和 b→a 均可匹配）。
         * 无匹配时回退为 {@link #UP_NORTH}。
         */
        public static CornerEnded fromDirections(Direction a, Direction b) {
            for (CornerEnded corner : values()) {
                if ((corner.first == a && corner.second == b) || (corner.first == b && corner.second == a)) {
                    return corner;
                }
            }
            return UP_NORTH;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * 节点的单方向连接状态。
     * <ul>
     *   <li>{@link #PIPE} — 无端头，连接至另一管道</li>
     *   <li>{@link #END}  — 有端头，连接至流体处理器</li>
     *   <li>{@link #NONE} — 无臂，该方向无连接</li>
     * </ul>
     */
    public enum NodePipe implements StringRepresentable {
        /**
         * 无端头开放连接（连至另一管道）
         */
        PIPE,
        /**
         * 有端头封闭连接（连至 IFluidHandler）
         */
        END,
        /**
         * 无臂（该方向无连接）
         */
        NONE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
