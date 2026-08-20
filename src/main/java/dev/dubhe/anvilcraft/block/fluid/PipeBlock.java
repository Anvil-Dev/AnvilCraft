package dev.dubhe.anvilcraft.block.fluid;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.fluid.network.FluidContainerLookup;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeCheckValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.PipeCheckValveBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

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
public abstract class PipeBlock extends Block
    implements SimpleWaterloggedBlock, IHammerRemovable, IHammerChangeable, EntityBlock, IMoveableEntityBlock {

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
     * 本管道是否至少有一个面装了止逆阀。
     *
     * <p>止逆的方向数据不进 blockstate（否则节点 6 面 × 方向组合会爆炸），而是存进
     * {@link PipeCheckValveBlockEntity}；此布尔仅用于决定是否创建该 BE（{@code true} 才创建），
     * 因此每种管道 blockstate 只 ×2。
     */
    public static final BooleanProperty HAS_CHECK_VALVE = BooleanProperty.create("has_check_valve");

    /**
     * 管道中心体碰撞箱（对应 pipe_straight / pipe_side_corner 模型 [4,4,4]→[12,12,12]）
     */
    static final VoxelShape PIPE_CENTER = box(4, 4, 4, 12, 12, 12);
    /**
     * 节点中心体碰撞箱（对应 pipe_node 模型 [3,3,3]→[13,13,13]）
     */
    static final VoxelShape NODE_CENTER = box(3, 3, 3, 13, 13, 13);

    /** 六个方向；{@code values()} 每次调用都会克隆数组，热路径统一复用这份共享副本。 */
    static final Direction[] DIRECTIONS = Direction.values();

    /** 按方向预建的无端头臂，避免每次取形状都重新构造。 */
    private static final VoxelShape[] NO_END_ARMS = new VoxelShape[DIRECTIONS.length];
    /** 按方向预建的有端头臂。 */
    private static final VoxelShape[] END_ARMS = new VoxelShape[DIRECTIONS.length];
    /** 直管 / 弯管形状缓存：两端方向 x 两个端头开关。 */
    private static final AtomicReferenceArray<VoxelShape> TWO_ARM_SHAPES =
        new AtomicReferenceArray<>(DIRECTIONS.length * DIRECTIONS.length * 4);

    private final boolean glassPipe;

    static {
        for (Direction dir : DIRECTIONS) {
            NO_END_ARMS[dir.ordinal()] = buildNoEnd(dir);
            END_ARMS[dir.ordinal()] = buildEnd(dir);
        }
    }

    /**
     * 惰性填充形状缓存。
     *
     * <p>形状只由方块状态决定，但带实体上下文的碰撞查询绕过原版的 per-state 缓存，
     * 每次都会回到 {@code getShape}；在那里现算 {@link Shapes#or} 会让管道附近的每个实体
     * 每 tick 都产生大量形状合并与分配。相同状态算出的形状等价，先写入者胜出即可。</p>
     */
    static VoxelShape cachedShape(AtomicReferenceArray<VoxelShape> cache, int key, Supplier<VoxelShape> builder) {
        VoxelShape cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        VoxelShape shape = builder.get();
        cache.compareAndSet(key, null, shape);
        return shape;
    }

    /**
     * 取指定方向的无端头臂碰撞箱（对应 pipe_no_end 模型）。
     * 从中心体表面延伸到方块边界，4 px 深，8×8 截面。
     */
    static VoxelShape makeNoEnd(Direction dir) {
        return NO_END_ARMS[dir.ordinal()];
    }

    /**
     * 取指定方向的有端头臂碰撞箱（对应 pipe_end 模型）。
     * ring（2 px 深，8×8 截面）+ cap（2 px 深，10×10 截面，与面齐平）。
     */
    static VoxelShape makeEnd(Direction dir) {
        return END_ARMS[dir.ordinal()];
    }

    private static VoxelShape buildNoEnd(Direction dir) {
        return switch (dir) {
            case DOWN -> box(4, 0, 4, 12, 4, 12);
            case UP -> box(4, 12, 4, 12, 16, 12);
            case NORTH -> box(4, 4, 0, 12, 12, 4);
            case SOUTH -> box(4, 4, 12, 12, 12, 16);
            case WEST -> box(0, 4, 4, 4, 12, 12);
            case EAST -> box(12, 4, 4, 16, 12, 12);
        };
    }

    private static VoxelShape buildEnd(Direction dir) {
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
        this(properties, false);
    }

    public PipeBlock(Properties properties, boolean glassPipe) {
        super(properties);
        this.glassPipe = glassPipe;
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(WATERLOGGED, false)
            .setValue(HAS_CHECK_VALVE, false));
    }

    public boolean isGlassPipe() {
        return glassPipe;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        builder.add(HAS_CHECK_VALVE);
    }

    // ---- 止逆阀 BlockEntity（仅 HAS_CHECK_VALVE=true 时创建）----

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (glassPipe) {
            return new GlassPipeBlockEntity(ModBlockEntities.GLASS_PIPE.get(), pos, state);
        }
        return state.getValue(HAS_CHECK_VALVE)
            ? new PipeCheckValveBlockEntity(ModBlockEntities.PIPE_CHECK_VALVE.get(), pos, state)
            : null;
    }

    /** 取该位置的止逆阀 BE（无则 {@code null}）。 */
    @Nullable
    public static AbstractPipeCheckValveBlockEntity getCheckValve(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AbstractPipeCheckValveBlockEntity be ? be : null;
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
     * 判断指定方块状态在给定方向上是否有管道臂/连接（不考虑端头是否可见）。
     * <ul>
     *   <li>直管：方向与轴向相同即为有连接</li>
     *   <li>弯管：方向为弯管两方向之一即为有连接</li>
     *   <li>节点：该方向非 {@link NodePipe#NONE} 即为有连接</li>
     * </ul>
     *
     * @param state  方块状态
     * @param toward 从此方块看向邻居的方向
     * @return 是否有管道臂/连接朝向该方向
     */
    public static boolean hasConnectionToward(BlockState state, Direction toward) {
        Block block = state.getBlock();
        return switch (block) {
            case PipeStraightBlock ignored -> toward.getAxis() == state.getValue(AXIS);
            case PipeCornerBlock ignored -> state.getValue(CORNER_ENDED).containsDirection(toward);
            case PipeNodeBlock ignored -> state.getValue(getPropertyForDirection(toward)) != NodePipe.NONE;
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
     * 检查指定方向的邻居是否为同类管道且其连接朝向本方块。
     * 普通管道与玻璃管道互相连接时仍连通，但模型端头需要保留。
     *
     * @param state 本方块状态
     * @param level 世界
     * @param pos   本方块位置
     * @param dir   从此方块看向邻居的方向
     * @return 邻居同类管道是否朝向本方块
     */
    public static boolean isNeighborSameKindPipeToward(BlockState state, Level level, BlockPos pos, Direction dir) {
        if (!(state.getBlock() instanceof PipeBlock pipe)) {
            return false;
        }
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getBlock() instanceof PipeBlock neighborPipe
            && pipe.isGlassPipe() == neighborPipe.isGlassPipe()
            && hasConnectionToward(neighborState, dir.getOpposite());
    }

    /**
     * 检查指定位置是否为流体处理器（通过 NeoForge Capability 系统）。
     *
     * @param level 世界
     * @param pos   位置
     * @return 该位置是否提供 {@link net.neoforged.neoforge.fluids.capability.IFluidHandler}
     */
    public static boolean isFluidHandler(Level level, BlockPos pos) {
        return FluidContainerLookup.find(level, pos, null) != null;
    }

    /**
     * 检查指定位置是否为流体处理器（通过 NeoForge Capability 系统），
     * 或是连接面正对给定方向的泵。
     *
     * <p>泵仅在其连接面（朝向轴两端，即输入/输出端）正对 {@code towardNeighbor}
     * 方向时才计入；垂直于朝向轴的实体侧面不算连接。
     *
     * @param level          世界
     * @param pos            待检查的位置
     * @param towardNeighbor 从该位置看向待连接邻居的方向
     * @return 该位置是否可在此方向形成流体连接
     */
    public static boolean isFluidHandlerOrConnectablePump(Level level, BlockPos pos, Direction towardNeighbor) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PumpBlock) {
            return PumpBlock.isConnectableFace(state, towardNeighbor);
        }
        if (state.getBlock() instanceof ControlValveBlock) {
            return ControlValveBlock.isConnectableFace(state, towardNeighbor);
        }
        return FluidContainerLookup.find(level, pos, towardNeighbor) != null;
    }

    /**
     * 检查指定方向的邻居是否被"占用"（有管道对准、是流体处理器、或连接面正对本方块的泵）。
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
        return isFluidHandlerOrConnectablePump(level, pos.relative(dir), dir.getOpposite());
    }

    @Override
    public Item asItem() {
        return glassPipe ? ModItems.GLASS_PIPE.get() : ModItems.PIPE.get();
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        if (glassPipe && player.isCreative()) {
            return new ItemStack(ModItems.GLASS_PIPE.get());
        }
        return new ItemStack(ModItems.PIPE.get());
    }

    public static BlockState straightVariant(BlockState state) {
        return (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()
                ? ModBlocks.GLASS_PIPE_STRAIGHT.get()
                : ModBlocks.PIPE_STRAIGHT.get())
            .defaultBlockState();
    }

    public static BlockState cornerVariant(BlockState state) {
        return (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()
                ? ModBlocks.GLASS_PIPE_CORNER.get()
                : ModBlocks.PIPE_CORNER.get())
            .defaultBlockState();
    }

    public static BlockState nodeVariant(BlockState state) {
        return (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()
                ? ModBlocks.GLASS_PIPE_NODE.get()
                : ModBlocks.PIPE_NODE.get())
            .defaultBlockState();
    }

    private static BlockState oppositeVariant(BlockState state) {
        if (state.getBlock() instanceof PipeStraightBlock) {
            return (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()
                    ? ModBlocks.PIPE_STRAIGHT.get()
                    : ModBlocks.GLASS_PIPE_STRAIGHT.get())
                .defaultBlockState()
                .setValue(AXIS, state.getValue(AXIS))
                .setValue(HAS_END_START, state.getValue(HAS_END_START))
                .setValue(HAS_END_END, state.getValue(HAS_END_END))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        if (state.getBlock() instanceof PipeCornerBlock) {
            return (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()
                    ? ModBlocks.PIPE_CORNER.get()
                    : ModBlocks.GLASS_PIPE_CORNER.get())
                .defaultBlockState()
                .setValue(CORNER_ENDED, state.getValue(CORNER_ENDED))
                .setValue(HAS_END_START, state.getValue(HAS_END_START))
                .setValue(HAS_END_END, state.getValue(HAS_END_END))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        if (state.getBlock() instanceof PipeNodeBlock) {
            BlockState newState = (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()
                    ? ModBlocks.PIPE_NODE.get()
                    : ModBlocks.GLASS_PIPE_NODE.get())
                .defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            for (Direction dir : DIRECTIONS) {
                newState = newState.setValue(getPropertyForDirection(dir), state.getValue(getPropertyForDirection(dir)));
            }
            return newState;
        }
        return state;
    }

    // ==================== 止逆阀：面附件交互 ====================

    /**
     * 根据精确点击坐标判断被点击的臂方向（中心体范围 [3,3,3]→[13,13,13]，超出即命中对应臂）。
     * 点击在中心区域返回 {@code null}。所有管型共用同一判定（直管/弯管中心体略大也在此范围内）。
     */
    @Nullable
    public static Direction getArmDirection(BlockPos pos, BlockHitResult hitResult) {
        Vec3 loc = hitResult.getLocation();
        double bx = loc.x - pos.getX();
        double by = loc.y - pos.getY();
        double bz = loc.z - pos.getZ();
        Direction armDir = null;
        double maxDist = 0;
        for (Direction dir : Direction.values()) {
            double dist = switch (dir) {
                case NORTH -> bz < 3.0 / 16 ? 3.0 / 16 - bz : 0;
                case SOUTH -> bz > 13.0 / 16 ? bz - 13.0 / 16 : 0;
                case WEST -> bx < 3.0 / 16 ? 3.0 / 16 - bx : 0;
                case EAST -> bx > 13.0 / 16 ? bx - 13.0 / 16 : 0;
                case DOWN -> by < 3.0 / 16 ? 3.0 / 16 - by : 0;
                case UP -> by > 13.0 / 16 ? by - 13.0 / 16 : 0;
            };
            if (dist > maxDist) {
                maxDist = dist;
                armDir = dir;
            }
        }
        return armDir;
    }

    /** 本管道在该方向上是否有连接（用于校验止逆阀只能装在有臂的面上）。 */
    protected boolean hasArmToward(BlockState state, Direction dir) {
        return hasConnectionToward(state, dir);
    }

    /**
     * 在管道某个面添加止逆阀：把 {@code HAS_CHECK_VALVE} 置真（必要时替换方块以生成 BE），
     * 再写入该面的允许流出方向。仅服务端调用。
     *
     * @param flowOut 无红石信号时允许流出的世界方向
     * @return 是否成功添加
     */
    public boolean addCheckValve(Level level, BlockPos pos, BlockState state, Direction face, Direction flowOut) {
        if (level.isClientSide) {
            return false;
        }
        if (!state.getValue(HAS_CHECK_VALVE)) {
            level.setBlock(pos, state.setValue(HAS_CHECK_VALVE, true), Block.UPDATE_ALL);
        }
        AbstractPipeCheckValveBlockEntity be = getCheckValve(level, pos);
        if (be == null) {
            return false;
        }
        be.setValve(face, flowOut);
        be.setPowered(level.hasNeighborSignal(pos));
        be.sendUpdate();
        FluidNetworkManager.INSTANCE.markDirty(level);
        return true;
    }

    /**
     * 移除管道某面的止逆阀；移除后若无任何面装阀则清除 {@code HAS_CHECK_VALVE}（销毁 BE）。
     * 仅服务端调用。
     *
     * @return 是否确实移除了一个阀
     */
    public boolean removeCheckValve(Level level, BlockPos pos, BlockState state, Direction face) {
        if (level.isClientSide) {
            return false;
        }
        AbstractPipeCheckValveBlockEntity be = getCheckValve(level, pos);
        if (be == null || !be.hasValveOn(face)) {
            return false;
        }
        be.removeValve(face);
        if (be.isEmpty()) {
            setBlockWithoutCheckValve(level, pos, state);
        } else {
            be.sendUpdate();
        }
        FluidNetworkManager.INSTANCE.markDirty(level);
        return true;
    }

    private static void setBlockWithoutCheckValve(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe()) {
            AbstractPipeCheckValveBlockEntity be = getCheckValve(level, pos);
            if (be != null) {
                be.restore(Map.of(), false);
            }
        } else {
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, state.setValue(HAS_CHECK_VALVE, false), Block.UPDATE_ALL);
    }

    /**
     * 管道通用的物品交互：
     * <ul>
     *   <li>手持止逆阀物品点击臂：该面无阀 → 加阀（Shift 反向，消耗物品）；该面已有阀 → 取消止逆阀（退还物品）；</li>
     *   <li>手持扳手点击有阀的臂 → 移除该面阀并掉落物品（无阀时放行给子类扳手逻辑）；</li>
     * </ul>
     */
    protected ItemInteractionResult handleCheckValveInteraction(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        boolean isValveItem = stack.is(ModItems.CHECK_VALVE.get());
        boolean isWrench = stack.is(Tags.Items.TOOLS_WRENCH);
        boolean isHammer = stack.is(ModItemTags.ANVIL_HAMMER);
        if (!isValveItem && !isWrench && !isHammer) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction arm = getArmDirection(pos, hitResult);
        if (arm == null || !hasArmToward(state, arm)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        AbstractPipeCheckValveBlockEntity be = getCheckValve(level, pos);
        boolean hasValveHere = be != null && be.hasValveOn(arm);

        // 扳手 / 铁砧锤：仅当该面已有阀才拦截（取下），否则放行给子类逻辑
        if (isWrench || isHammer) {
            if (!hasValveHere) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            detachCheckValve(level, pos, state, arm, player);
            return ItemInteractionResult.sidedSuccess(false);
        }

        // 止逆阀物品：该面已有阀 → 取消（退还一个物品）；无阀 → 添加（Shift 反向，消耗物品）
        if (hasValveHere) {
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            detachCheckValve(level, pos, state, arm, player);
            return ItemInteractionResult.sidedSuccess(false);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        Direction flowOut = player.isShiftKeyDown() ? arm.getOpposite() : arm;
        if (addCheckValve(level, pos, state, arm, flowOut)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 取下某面止逆阀并把物品退还玩家（创造模式不退）。仅服务端调用。 */
    private void detachCheckValve(Level level, BlockPos pos, BlockState state, Direction arm, @Nullable Player player) {
        if (!removeCheckValve(level, pos, state, arm)) {
            return;
        }
        if (player == null || !player.isCreative()) {
            giveOrDrop(level, pos, player, new ItemStack(ModItems.CHECK_VALVE.get()));
        }
    }

    /** 把物品塞给玩家，塞不下或无玩家则在方块处掉落。 */
    private static void giveOrDrop(Level level, BlockPos pos, @Nullable Player player, ItemStack stack) {
        if (player != null && player.getInventory().add(stack)) {
            return;
        }
        Block.popResource(level, pos, stack);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        ItemInteractionResult result = handleCheckValveInteraction(stack, state, level, pos, player, hitResult);
        if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return result;
        }
        if (stack.is(Tags.Items.GLASS_PANES)
            && state.getBlock() instanceof PipeBlock pipe
            && !pipe.isGlassPipe()) {
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            setBlockPreservingValve(level, pos, state, oppositeVariant(state));
            return ItemInteractionResult.sidedSuccess(false);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /**
     * 空手右键：命中的臂若装有止逆阀则取下（退还物品）。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Direction arm = getArmDirection(pos, hitResult);
        if (arm != null && hasArmToward(state, arm)) {
            AbstractPipeCheckValveBlockEntity be = getCheckValve(level, pos);
            if (be != null && be.hasValveOn(arm)) {
                if (level.isClientSide) {
                    return InteractionResult.sidedSuccess(true);
                }
                detachCheckValve(level, pos, state, arm, player);
                return InteractionResult.sidedSuccess(false);
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    /**
     * 红石信号更新止逆阀 BE 的 powered（所有面流向反转）。子类的 {@code neighborChanged}
     * 应在处理自身逻辑前调用此方法。
     */
    protected void updateCheckValvePower(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide || !state.getValue(HAS_CHECK_VALVE)) {
            return;
        }
        AbstractPipeCheckValveBlockEntity be = getCheckValve(level, pos);
        if (be == null) {
            return;
        }
        if (be.setPowered(level.hasNeighborSignal(pos))) {
            be.sendUpdate();
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    /**
     * 跨管型转换时保留止逆阀数据：读取 {@code oldPos} 处旧 BE 的面映射，给 {@code newState}
     * 打上 {@code HAS_CHECK_VALVE} 并在 {@code setBlock} 后回填到新 BE。
     *
     * <p>调用方应传入<b>尚未 setBlock 的目标 state</b>；本方法负责 setBlock 并返回，
     * 保证形变（node↔straight↔corner）不丢失止逆阀。
     *
     * <p><b>客户端</b>同样保留止逆阀数据，避免依赖服务端同步即可获得即时视觉反馈。
     * <b>服务端</b>额外对因管型变化而失去臂的面掉落止逆阀物品。
     */
    public static void setBlockPreservingValve(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        // 读取旧 BE 的面映射
        Map<Direction, Direction> saved = null;
        boolean powered = false;
        if (oldState.hasProperty(HAS_CHECK_VALVE) && oldState.getValue(HAS_CHECK_VALVE)) {
            AbstractPipeCheckValveBlockEntity oldBe = getCheckValve(level, pos);
            if (oldBe != null && !oldBe.isEmpty()) {
                saved = oldBe.baseFlowCopy();
                powered = oldBe.isPowered();
            }
        }
        // 无旧数据：若 oldState 标记了 HAS_CHECK_VALVE 但 BE 为空，
        // 说明数据尚未恢复（处于外层 setBlockPreservingValve 的 setBlock→onPlace 窗口），
        // 保留标志让外层 restore 后续填充数据；否则清除
        if (saved == null) {
            if (oldState.hasProperty(HAS_CHECK_VALVE) && oldState.getValue(HAS_CHECK_VALVE)) {
                // BE 数据待恢复 → 保留标志，外层 restore 会填充
                if (level.isClientSide) {
                    level.setBlockAndUpdate(pos, newState.setValue(HAS_CHECK_VALVE, true));
                } else {
                    level.setBlock(pos, newState.setValue(HAS_CHECK_VALVE, true), Block.UPDATE_ALL);
                }
            } else {
                setBlockWithoutCheckValve(level, pos, newState);
            }
            return;
        }
        // 只保留新管型仍存在的臂上的阀；其余掉落物品
        Map<Direction, Direction> filtered = new java.util.EnumMap<>(Direction.class);
        for (Map.Entry<Direction, Direction> e : saved.entrySet()) {
            if (hasConnectionToward(newState, e.getKey())) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        // 新旧管型差异导致的失臂面 → 服务端掉落止逆阀物品
        if (!level.isClientSide) {
            for (Direction face : saved.keySet()) {
                if (!filtered.containsKey(face)) {
                    Block.popResource(level, pos, new ItemStack(ModItems.CHECK_VALVE.get()));
                }
            }
        }
        if (filtered.isEmpty()) {
            setBlockWithoutCheckValve(level, pos, newState);
            return;
        }
        // 客户端与服务端分别用合适的 setBlock 方式，写入 BE 数据确保即时渲染
        if (level.isClientSide) {
            level.setBlockAndUpdate(pos, newState.setValue(HAS_CHECK_VALVE, true));
        } else {
            level.setBlock(pos, newState.setValue(HAS_CHECK_VALVE, true), Block.UPDATE_ALL);
        }
        AbstractPipeCheckValveBlockEntity newBe = getCheckValve(level, pos);
        if (newBe != null) {
            newBe.restore(filtered, powered);
            if (!level.isClientSide) {
                newBe.sendUpdate();
            }
        }
    }

    /**
     * 更新直管/弯管的端头状态。
     * 根据邻居是否为同类管道来决定端头开关：
     * <ul>
     *   <li>邻居是同类管道 → {@code HAS_END_*} = false（无端头，开放）</li>
     *   <li>邻居非同类管道 → {@code HAS_END_*} = true（有端头，封闭）</li>
     * </ul>
     *
     * @param level                世界
     * @param pos                  方块位置
     * @param state                当前方块状态
     * @param startDir             第一端方向（用于区分 HAS_END_START / HAS_END_END）
     * @param neighborDir          邻居方向
     * @param neighborIsPipeToward 邻居是否为对准的同类管道
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
            setBlockPreservingValve(level, pos, state, newState);
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

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (glassPipe) {
            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);
                if (drop.is(ModItems.GLASS_PIPE.get())) {
                    drops.set(i, new ItemStack(ModItems.PIPE.get(), drop.getCount()));
                }
            }
        }
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (state.getValue(HAS_CHECK_VALVE)
            && blockEntity instanceof AbstractPipeCheckValveBlockEntity checkValve
            && !checkValve.isEmpty()) {
            drops.add(new ItemStack(ModItems.CHECK_VALVE.get(), checkValve.baseFlowCopy().size()));
        }
        return drops;
    }

    /**
     * 管道部件放置 / 落地时使流体网络缓存失效（拓扑可能变化）。
     * 子类覆写 {@code onPlace} 时须调用 {@code super.onPlace(...)}。
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 同类直管仅改变轴向时不会触发方块实体的常规移除，需要显式清理失效的止逆阀数据。
        if (!glassPipe
            && !state.getValue(HAS_CHECK_VALVE)
            && level.getBlockEntity(pos) instanceof AbstractPipeCheckValveBlockEntity) {
            level.removeBlockEntity(pos);
        }
        if (!level.isClientSide) {
            FluidNetworkManager.INSTANCE.addAdjacentContainers(level, pos);
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    /** 管道部件被移除 / 被推走时使流体网络缓存失效。 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
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
        boolean endStart = state.getValue(HAS_END_START);
        boolean endEnd = state.getValue(HAS_END_END);
        // 两端方向 x 两个端头开关唯一决定形状，直管与弯管共用同一张缓存表。
        int key = ((startDir.ordinal() * DIRECTIONS.length + endDir.ordinal()) * 2 + (endStart ? 1 : 0)) * 2
            + (endEnd ? 1 : 0);
        return cachedShape(TWO_ARM_SHAPES, key, () -> Shapes.or(
            PIPE_CENTER,
            endStart ? makeEnd(startDir) : makeNoEnd(startDir),
            endEnd ? makeEnd(endDir) : makeNoEnd(endDir)
        ));
    }

    @Override
    public boolean checkBlockState(BlockState blockState) {
        return blockState.getBlock() instanceof PipeBlock pipe && pipe.isGlassPipe();
    }

    @Override
    public void notifyMoved(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        // 活塞移动玻璃管道后清除流体显示，避免过期数据随方块实体残留到新位置
        if (be instanceof GlassPipeBlockEntity glassPipe) {
            glassPipe.clearDisplay();
        }
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        if (!(state.getBlock() instanceof PipeBlock pipe) || !pipe.isGlassPipe()) {
            return false;
        }
        setBlockPreservingValve(level, blockPos, state, oppositeVariant(state));
        return true;
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
