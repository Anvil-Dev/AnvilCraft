package dev.dubhe.anvilcraft.block.fluid;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.fluid.network.FluidContainerLookup;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
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
     * 是否安装了止回阀
     */
    public static final BooleanProperty HAS_CHECK_VALVE = BooleanProperty.create("has_check_valve");
    /**
     * 是否含水
     */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * 管道中心体碰撞箱（对应 pipe_straight / pipe_side_corner 模型 [4,4,4]→[12,12,12]）
     */
    static final VoxelShape PIPE_CENTER = Block.box(4, 4, 4, 12, 12, 12);
    /**
     * 节点中心体碰撞箱（对应 pipe_node 模型 [3,3,3]→[13,13,13]）
     */
    static final VoxelShape NODE_CENTER = Block.box(3, 3, 3, 13, 13, 13);

    /** 六个方向；{@code values()} 每次调用都会克隆数组，热路径统一复用这份共享副本。 */
    static final Direction[] DIRECTIONS = Direction.values();

    /** 按方向预建的无端头臂，避免每次取形状都重新构造。 */
    private static final VoxelShape[] NO_END_ARMS = new VoxelShape[PipeBlock.DIRECTIONS.length];
    /** 按方向预建的有端头臂。 */
    private static final VoxelShape[] END_ARMS = new VoxelShape[PipeBlock.DIRECTIONS.length];
    /** 直管 / 弯管形状缓存：两端方向 x 两个端头开关。 */
    private static final AtomicReferenceArray<VoxelShape> TWO_ARM_SHAPES =
        new AtomicReferenceArray<>(PipeBlock.DIRECTIONS.length * PipeBlock.DIRECTIONS.length * 4);

    static {
        for (Direction dir : PipeBlock.DIRECTIONS) {
            PipeBlock.NO_END_ARMS[dir.ordinal()] = PipeBlock.buildNoEnd(dir);
            PipeBlock.END_ARMS[dir.ordinal()] = PipeBlock.buildEnd(dir);
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
        return PipeBlock.NO_END_ARMS[dir.ordinal()];
    }

    /**
     * 取指定方向的有端头臂碰撞箱（对应 pipe_end 模型）。
     * ring（2 px 深，8×8 截面）+ cap（2 px 深，10×10 截面，与面齐平）。
     */
    static VoxelShape makeEnd(Direction dir) {
        return PipeBlock.END_ARMS[dir.ordinal()];
    }

    private static VoxelShape buildNoEnd(Direction dir) {
        return switch (dir) {
            case DOWN -> Block.box(4, 0, 4, 12, 4, 12);
            case UP -> Block.box(4, 12, 4, 12, 16, 12);
            case NORTH -> Block.box(4, 4, 0, 12, 12, 4);
            case SOUTH -> Block.box(4, 4, 12, 12, 12, 16);
            case WEST -> Block.box(0, 4, 4, 4, 12, 12);
            case EAST -> Block.box(12, 4, 4, 16, 12, 12);
        };
    }

    private static VoxelShape buildEnd(Direction dir) {
        // ring：内层，紧贴中心体，8×8 截面
        VoxelShape ring = switch (dir) {
            case DOWN -> Block.box(4, 2, 4, 12, 4, 12);
            case UP -> Block.box(4, 12, 4, 12, 14, 12);
            case NORTH -> Block.box(4, 4, 2, 12, 12, 4);
            case SOUTH -> Block.box(4, 4, 12, 12, 12, 14);
            case WEST -> Block.box(2, 4, 4, 4, 12, 12);
            case EAST -> Block.box(12, 4, 4, 14, 12, 12);
        };
        VoxelShape cap = switch (dir) {
            case DOWN -> Block.box(3, 0, 3, 13, 2, 13);
            case UP -> Block.box(3, 14, 3, 13, 16, 13);
            case NORTH -> Block.box(3, 3, 0, 13, 13, 2);
            case SOUTH -> Block.box(3, 3, 14, 13, 13, 16);
            case WEST -> Block.box(0, 3, 3, 2, 13, 13);
            case EAST -> Block.box(14, 3, 3, 16, 13, 13);
        };
        return Shapes.or(ring, cap);
    }

    public PipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(PipeBlock.WATERLOGGED, false).setValue(PipeBlock.HAS_CHECK_VALVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.WATERLOGGED, PipeBlock.HAS_CHECK_VALVE);
    }

    /**
     * 根据轴向和轴方向获取对应的 {@link Direction}。
     */
    public static Direction getDirectionFromAxis(Direction.Axis axis, Direction.AxisDirection axisDirection) {
        return Direction.get(axisDirection, axis);
    }

    /**
     * 获取指定方向对应的节点连接属性。
     */
    public static EnumProperty<NodePipe> getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> PipeBlock.DOWN;
            case UP -> PipeBlock.UP;
            case NORTH -> PipeBlock.NORTH;
            case SOUTH -> PipeBlock.SOUTH;
            case WEST -> PipeBlock.WEST;
            case EAST -> PipeBlock.EAST;
        };
    }

    /**
     * 判断指定方块状态在给定方向上是否有管道连接（不考虑端头状态）。
     */
    public static boolean hasConnectionToward(BlockState state, Direction toward) {
        Block block = state.getBlock();
        return switch (block) {
            case PipeStraightBlock ignored -> toward.getAxis() == state.getValue(PipeBlock.AXIS);
            case PipeCornerBlock ignored -> state.getValue(PipeBlock.CORNER_ENDED).containsDirection(toward);
            case PipeNodeBlock ignored -> state.getValue(PipeBlock.getPropertyForDirection(toward)) == NodePipe.PIPE;
            default -> false;
        };
    }

    /**
     * 检查指定方向的邻居是否为管道且其连接朝向本方块。
     */
    public static boolean isNeighborPipeToward(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getBlock() instanceof PipeBlock && PipeBlock.hasConnectionToward(neighborState, dir.getOpposite());
    }

    /**
     * 检查指定位置是否为流体处理器（通过 NeoForge Capability 系统）。
     * 26.1 使用 Capabilities.Fluid.BLOCK 和 3-参数 getCapability。
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
     */
    public static boolean isNeighborOccupied(Level level, BlockPos pos, Direction dir) {
        if (PipeBlock.isNeighborPipeToward(level, pos, dir)) {
            return true;
        }
        return PipeBlock.isFluidHandlerOrConnectablePump(level, pos.relative(dir), dir.getOpposite());
    }

    @Override
    public Item asItem() {
        return ModItems.PIPE.get();
    }

    /**
     * 更新直管/弯管的端头状态。
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
            newState = newState.setValue(PipeBlock.HAS_END_START, !neighborIsPipeToward);
        } else {
            newState = newState.setValue(PipeBlock.HAS_END_END, !neighborIsPipeToward);
        }

        if (!newState.equals(state)) {
            level.setBlockAndUpdate(pos, newState);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (state.getValue(PipeBlock.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(PipeBlock.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (state.getValue(PipeBlock.HAS_CHECK_VALVE)
            && blockEntity instanceof AbstractPipeBlockEntity checkValve
            && !checkValve.isEmpty()
        ) {
            drops.add(new ItemStack(ModItems.CHECK_VALVE.get(), checkValve.baseFlowCopy().size()));
        }
        return drops;
    }

    /**
     * 构建直管/弯管的碰撞箱：中心体 + 两端按端头状态拼接 arm。
     */
    public VoxelShape getShape(BlockState state, Direction startDir, Direction endDir) {
        boolean endStart = state.getValue(PipeBlock.HAS_END_START);
        boolean endEnd = state.getValue(PipeBlock.HAS_END_END);
        // 两端方向 x 两个端头开关唯一决定形状，直管与弯管共用同一张缓存表。
        int key = ((startDir.ordinal() * PipeBlock.DIRECTIONS.length + endDir.ordinal()) * 2 + (endStart ? 1 : 0)) * 2
            + (endEnd ? 1 : 0);
        return PipeBlock.cachedShape(
            PipeBlock.TWO_ARM_SHAPES, key, () -> Shapes.or(
            PipeBlock.PIPE_CENTER,
            endStart ? PipeBlock.makeEnd(startDir) : PipeBlock.makeNoEnd(startDir),
            endEnd ? PipeBlock.makeEnd(endDir) : PipeBlock.makeNoEnd(endDir)
        ));
    }

    // ======================== Check Valve System ========================
    /**
     * 获取指定位置的止回阀 BE（若存在）。
     */
    @Nullable
    public static AbstractPipeBlockEntity getCheckValve(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof AbstractPipeBlockEntity pipe ? pipe : null;
    }

    /**
     * 根据射线命中检测结果确定玩家点击了哪个臂方向。
     */
    @Nullable
    public static Direction getArmDirection(BlockPos pos, BlockHitResult hitResult) {
        Vec3 loc = hitResult.getLocation();
        double bx = loc.x - pos.getX();
        double by = loc.y - pos.getY();
        double bz = loc.z - pos.getZ();

        double dx = bx - 0.5;
        double dy = by - 0.5;
        double dz = bz - 0.5;

        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        if (ax > ay && ax > az) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (ay > az) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * 判断该管道在给定方向是否有臂（连接）。子类需覆盖以实现具体逻辑。
     */
    protected boolean hasArmToward(BlockState state, Direction dir) {
        return PipeBlock.hasConnectionToward(state, dir);
    }

    /**
     * 在管道上安装止回阀。
     */
    protected boolean addCheckValve(Level level, BlockPos pos, BlockState state, Direction face, Direction flowOut) {
        if (level.isClientSide()) return false;
        BlockState newState = state.setValue(PipeBlock.HAS_CHECK_VALVE, true);
        PipeBlock.setBlockPreservingValve(level, pos, newState);
        AbstractPipeBlockEntity valve = PipeBlock.getCheckValve(level, pos);
        if (valve != null) {
            valve.setValve(face, flowOut);
            valve.sendUpdate();
        }
        FluidNetworkManager.INSTANCE.markDirty(level);
        return true;
    }

    /**
     * 移除管道上指定面的止回阀。
     */
    protected boolean removeCheckValve(Level level, BlockPos pos, BlockState state, Direction face) {
        if (level.isClientSide()) return false;
        AbstractPipeBlockEntity valve = PipeBlock.getCheckValve(level, pos);
        if (valve == null || !valve.hasValveOn(face)) return false;
        valve.removeValve(face);
        valve.sendUpdate();
        if (valve.isEmpty()) {
            BlockState newState = state.setValue(PipeBlock.HAS_CHECK_VALVE, false);
            PipeBlock.setBlockPreservingValve(level, pos, newState);
        }
        FluidNetworkManager.INSTANCE.markDirty(level);
        return true;
    }

    /**
     * 处理止回阀交互逻辑（右键安装/拆卸）。
     */
    protected InteractionResult handleCheckValveInteraction(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (!stack.is(ModItems.CHECK_VALVE.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Direction face = PipeBlock.getArmDirection(pos, hitResult);
        if (face == null || !this.hasArmToward(state, face)) {
            return InteractionResult.PASS;
        }

        // 已有止回阀 → 不重复安装
        AbstractPipeBlockEntity existing = PipeBlock.getCheckValve(level, pos);
        if (existing != null && existing.hasValveOn(face)) {
            if (this.removeCheckValve(level, pos, state, face)) {
                PipeBlock.giveOrDrop(player, level, pos, new ItemStack(ModItems.CHECK_VALVE.get()));
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        // 流出方向为击中面的朝向
        Direction flowOut = player.isShiftKeyDown() ? face.getOpposite() : face;
        if (this.addCheckValve(level, pos, state, face, flowOut)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /**
     * 拆卸止回阀并归还物品。
     */
    protected InteractionResult detachCheckValve(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Direction face = PipeBlock.getArmDirection(pos, hitResult);
        if (face == null) return InteractionResult.PASS;

        AbstractPipeBlockEntity valve = PipeBlock.getCheckValve(level, pos);
        if (valve == null || !valve.hasValveOn(face)) {
            return InteractionResult.PASS;
        }

        if (this.removeCheckValve(level, pos, state, face)) {
            PipeBlock.giveOrDrop(player, level, pos, new ItemStack(ModItems.CHECK_VALVE.get()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /**
     * 将物品交给玩家或掉落在地。
     */
    protected static void giveOrDrop(Player player, Level level, BlockPos pos, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            Block.popResource(level, pos, stack);
        }
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

    // ======================== Item Interaction ========================

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
        // 止回阀安装
        if (stack.is(ModItems.CHECK_VALVE.get())) {
            return this.handleCheckValveInteraction(stack, state, level, pos, player, hitResult);
        }
        // 扳手或锤子拆卸止回阀
        if ((stack.is(Tags.Items.TOOLS_WRENCH) || stack.is(ModItemTags.ANVIL_HAMMER)) && state.getValue(PipeBlock.HAS_CHECK_VALVE)) {
            return this.detachCheckValve(state, level, pos, player, hitResult);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        Direction face = PipeBlock.getArmDirection(pos, hitResult);
        if (face != null && this.hasArmToward(state, face)) {
            AbstractPipeBlockEntity valve = PipeBlock.getCheckValve(level, pos);
            if (valve != null && valve.hasValveOn(face)) {
                return this.detachCheckValve(state, level, pos, player, hitResult);
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    // ======================== Lifecycle & Network ========================

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addAdjacentContainers(level, pos);
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        FluidNetworkManager.INSTANCE.markDirty(level);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (!level.isClientSide() && state.getValue(PipeBlock.HAS_CHECK_VALVE)) {
            this.updateCheckValvePower(level, pos, state);
        }
    }

    /**
     * 根据红石信号更新止回阀的反向状态。
     */
    protected void updateCheckValvePower(Level level, BlockPos pos, BlockState state) {
        AbstractPipeBlockEntity valve = PipeBlock.getCheckValve(level, pos);
        if (valve == null) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (valve.setPowered(powered)) {
            valve.sendUpdate();
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    /**
     * 在管道类型变更时保留止回阀数据。
     * 用于直管 → 节点等变形场景：先保存旧 BE 数据，设置新方块后恢复。
     */
    public static void setBlockPreservingValve(Level level, BlockPos pos, BlockState newState) {
        BlockState oldState = level.getBlockState(pos);
        Map<Direction, Direction> savedFlows = null;
        boolean savedPowered = false;

        if (oldState.hasProperty(PipeBlock.HAS_CHECK_VALVE) && oldState.getValue(PipeBlock.HAS_CHECK_VALVE)) {
            AbstractPipeBlockEntity oldValve = PipeBlock.getCheckValve(level, pos);
            if (oldValve != null && !oldValve.isEmpty()) {
                Map<Direction, Direction> oldFlows = oldValve.baseFlowCopy();
                savedFlows = new EnumMap<>(Direction.class);
                for (Map.Entry<Direction, Direction> entry : oldFlows.entrySet()) {
                    if (PipeBlock.hasConnectionToward(newState, entry.getKey())) {
                        savedFlows.put(entry.getKey(), entry.getValue());
                    } else if (!level.isClientSide()) {
                        Block.popResource(level, pos, new ItemStack(ModItems.CHECK_VALVE.get()));
                    }
                }
                savedPowered = oldValve.isPowered();
                newState = newState.setValue(PipeBlock.HAS_CHECK_VALVE, !savedFlows.isEmpty());
            } else {
                newState = newState.setValue(PipeBlock.HAS_CHECK_VALVE, true);
            }
        } else {
            newState = newState.setValue(PipeBlock.HAS_CHECK_VALVE, false);
        }

        level.setBlockAndUpdate(pos, newState);

        if (savedFlows != null && !savedFlows.isEmpty()) {
            AbstractPipeBlockEntity newValve = PipeBlock.getCheckValve(level, pos);
            if (newValve != null) {
                newValve.restore(savedFlows, savedPowered);
                if (!level.isClientSide()) {
                    newValve.sendUpdate();
                }
            }
        }
    }

    /**
     * 弯管拐角方向枚举。
     */
    public enum CornerEnded implements StringRepresentable {
        DOWN_NORTH(Direction.DOWN, Direction.NORTH),
        DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
        DOWN_WEST(Direction.DOWN, Direction.WEST),
        DOWN_EAST(Direction.DOWN, Direction.EAST),
        UP_NORTH(Direction.UP, Direction.NORTH),
        UP_SOUTH(Direction.UP, Direction.SOUTH),
        UP_WEST(Direction.UP, Direction.WEST),
        UP_EAST(Direction.UP, Direction.EAST),
        NORTH_WEST(Direction.NORTH, Direction.WEST),
        NORTH_EAST(Direction.NORTH, Direction.EAST),
        SOUTH_WEST(Direction.SOUTH, Direction.WEST),
        SOUTH_EAST(Direction.SOUTH, Direction.EAST);

        private final Direction first;
        private final Direction second;

        CornerEnded(Direction first, Direction second) {
            this.first = first;
            this.second = second;
        }

        public Direction getFirstDirection() {
            return this.first;
        }

        public Direction getSecondDirection() {
            return this.second;
        }

        public boolean containsDirection(Direction direction) {
            return this.first == direction || this.second == direction;
        }

        public static CornerEnded fromDirections(Direction a, Direction b) {
            for (CornerEnded corner : CornerEnded.values()) {
                if ((corner.first == a && corner.second == b) || (corner.first == b && corner.second == a)) {
                    return corner;
                }
            }
            return CornerEnded.UP_NORTH;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * 节点的单方向连接状态。
     */
    public enum NodePipe implements StringRepresentable {
        PIPE, // 无端头开放连接（连至另一管道）
        END,  // 有端头封闭连接（连至 IFluidHandler）
        NONE; // 无臂（该方向无连接）

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
