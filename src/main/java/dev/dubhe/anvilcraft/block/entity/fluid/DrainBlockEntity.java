package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 排水口的 BlockEntity。内部 4B 流体容量，像储罐一样渲染内部流体，并像机械动力软管滑轮一样
 * 向下把内部流体铺放到世界、或从上方抽取流体入内部。
 *
 * <h3>向下填充</h3>
 * 内部流体 &gt; 1B 且下方有空间时，每 {@value #INTERVAL} gt 消耗 1B，在下方 flood-fill 区域内
 * 由最低层、就近位置起放置<b>真实源方块</b>，逐层填满直到排水口正下方一层。
 *
 * <h3>向上抽取</h3>
 * 内部 &lt; 3B 且上方有同种流体（或内部为空、上方任意流体）时，每 {@value #INTERVAL} gt 从上方
 * 流体的最上层起清除 1B 并填充自身。
 *
 * <h3>同层无限生成</h3>
 * 排水口<b>同层</b>（同 Y）水平四邻若存在能形成无限源的流体源（无限水、开启对应游戏规则的岩浆、
 * 或模组注册的可无限流体），则每 {@value #INTERVAL} gt 在自身内部凭空 +1B 该流体。
 * 判定复刻原版 {@code getNewLiquid} 的无限源成因：相邻源格自身两侧≥2 个可转化源邻居且下方为
 * 实体/同种源。只认同层紧邻，不做 flood-fill。
 */
@Getter
public class DrainBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = 4 * FluidType.BUCKET_VOLUME; // 4B
    private static final int UNIT = FluidType.BUCKET_VOLUME;        // 每次操作 1B
    private static final int INTERVAL = 5;                          // 每 5gt 一次
    private static final int FILL_THRESHOLD = FluidType.BUCKET_VOLUME;      // >1B 才向下填充
    private static final int DRAIN_THRESHOLD = 3 * FluidType.BUCKET_VOLUME; // <3B 才向上抽取
    /** 单次 tick 的 flood-fill 节点预算；未完成的搜索会在后续 tick 续扫。 */
    private static final int MAX_NODES = 2048;
    private static final int FILL_BLOCKED = 0;
    private static final int FILL_TARGET = 1;
    private static final int FILL_SOURCE = 2;
    private static final int FILL_SEARCH_REBUILD_INTERVAL = 256;
    private static final long EXHAUSTED_SEARCH_TTL = 100;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            DrainBlockEntity.this.setChanged();
            DrainBlockEntity.this.sendUpdate();
        }

        @Override
        public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
            FluidTank t = super.readFromNBT(lookupProvider, nbt);
            this.onContentsChanged();
            return t;
        }
    };

    /**
     * 客户端渲染用：当前向下排水柱的底部 Y（从排水口下方一直渲染流动水到此 Y）。
     * {@link Integer#MIN_VALUE} 表示当前无向下排水（不渲染水柱）。
     */
    private int columnBottomY = Integer.MIN_VALUE;
    @Getter(AccessLevel.NONE)
    @Nullable
    private FillSearch fillSearch;
    @Getter(AccessLevel.NONE)
    @Nullable
    private DrainSearch drainSearch;
    @Getter(AccessLevel.NONE)
    private final LongArrayFIFOQueue flowCleanupQueue = new LongArrayFIFOQueue();
    @Getter(AccessLevel.NONE)
    private final LongOpenHashSet flowCleanupVisited = new LongOpenHashSet();
    @Getter(AccessLevel.NONE)
    @Nullable
    private Fluid flowCleanupFluid;

    public DrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    // ---- 网络注册 ----

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.getBlockPos());
        }
        super.setRemoved();
    }

    public void sendUpdate() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- tick ----

    public static void tick(Level level, BlockPos pos, BlockState state, DrainBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        if (level.getGameTime() % INTERVAL != 0) {
            return;
        }
        // 连抽带排：向下填充与向上抽取同一 tick 各执行一次，互不阻断
        FillResult fillResult = be.tryFillDown(level, pos);
        if (fillResult == FillResult.NONE) {
            be.clearColumn();
        }
        be.tryDrainUp(level, pos);
        // 同层紧邻无限源 → 内部凭空 +1B
        be.tryGenerateFromInfinite(level, pos);
    }

    private void clearColumn() {
        if (columnBottomY != Integer.MIN_VALUE) {
            columnBottomY = Integer.MIN_VALUE;
            sendUpdate();
        }
    }

    /**
     * 向下填充：内部 &gt;1B 且流体有对应 LiquidBlock 时，逐层在下方连通区找空格放源方块。
     *
     * <p>逐层：先填满当前最低有空位的一层（沿本层流体连通边界排查，含不规则外延格），
     * 该层填满才升到上一层。放置前若该格已因水自发流动变成本流体源，则跳过、找下一空格。
     *
     * @return 本 tick 已填充、仍在搜索，或确实没有目标
     */
    private FillResult tryFillDown(Level level, BlockPos pos) {
        FluidStack stored = tank.getFluid();
        if (stored.getAmount() <= FILL_THRESHOLD) {
            fillSearch = null;
            return FillResult.NONE;
        }
        Fluid fluid = stored.getFluid();
        // 下界等超温维度不能放水，和水桶行为一致
        if (level.dimensionType().ultraWarm() && fluid.isSame(Fluids.WATER)) {
            fillSearch = null;
            return FillResult.NONE;
        }
        BlockState source = fluid.defaultFluidState().createLegacyBlock();
        if (source.isAir()) {
            fillSearch = null;
            return FillResult.NONE; // 无对应可放置方块（如蜂蜜）
        }

        SearchResult searchResult = findFillTarget(level, pos, fluid);
        BlockPos target = searchResult.target();
        if (target == null) {
            return searchResult.pending() ? FillResult.SEARCHING : FillResult.NONE;
        }
        if (fillSearch == null || !fillSearch.isTargetStillReachable(level, target.asLong())) {
            fillSearch = null;
            return FillResult.SEARCHING;
        }
        // 放置真实源方块，UPDATE_ALL 正常触发更新（红石/邻居/渲染）；水自发流动无妨。
        level.setBlock(target, source, Block.UPDATE_ALL);
        tank.drain(UNIT, IFluidHandler.FluidAction.EXECUTE);
        if (fillSearch != null && fillSearch.acceptFilled(target.asLong())) {
            fillSearch = null;
        }

        // 更新渲染水柱底部：从排水口下方渲染到本次目标所在层
        int newBottom = target.getY();
        if (newBottom != columnBottomY) {
            columnBottomY = newBottom;
            sendUpdate();
        }
        return FillResult.FILLED;
    }

    /**
     * 定位向下填充目标：先<b>垂直探底</b>，再自底向上逐层 flood-fill，每层可向下穿透。
     *
     * <p><b>第一步（探底）</b>：从排水口正下方沿"可通行格（空格 / 同种流体）"一路向下，
     * 找到最低可放置层 {@code bottomY}（其正下方为实体方块或世界底）。
     *
     * <p><b>第二步（逐层铺开）</b>：自 {@code bottomY} 向上逐层，每层水平 flood-fill 同时
     * <b>允许向下穿透</b>——可通行格的下方若是空格会被纳入候选，下方若可通行则继续入队往下探。
     * 候选格统一取<b>最低层、同层最近</b>一个。
     * 这样水在盆地底部平铺，填满才升层；每 tick 最多扫描 {@value #MAX_NODES} 个源节点，
     * 未扫完的连通区域会在后续 tick 继续，而不会被误判为本层已满。
     * 遇到鞍部时，上层扫描穿过洞口后自然向下发现隔壁盆地的更深空格，实现正确填充。
     */
    private SearchResult findFillTarget(Level level, BlockPos drainPos, Fluid fluid) {
        BlockPos start = drainPos.below();
        if (!isPassableForFill(level, start, fluid)) {
            fillSearch = null;
            return SearchResult.EXHAUSTED; // 正下方被堵，无法向下排水
        }
        // 第一步：沿正下方一路探到最低可放置层
        int minY = level.getMinBuildHeight();
        int bottomY = start.getY();
        while (bottomY > minY
            && isPassableForFill(level, new BlockPos(drainPos.getX(), bottomY - 1, drainPos.getZ()), fluid)) {
            bottomY--;
        }
        if (fillSearch == null
            || !fillSearch.matches(drainPos, fluid, bottomY, start.getY(), level.getGameTime())) {
            fillSearch = new FillSearch(drainPos, fluid, bottomY, start.getY());
        }
        return fillSearch.advance(level);
    }

    /** 探底可通行判定：待填充空格或同种流体（源/流动皆可穿行）。 */
    private static boolean isPassableForFill(Level level, BlockPos pos, Fluid fluid) {
        return classifyForFill(level, pos, fluid) != FILL_BLOCKED;
    }

    private static long offset(BlockPos pos, Direction direction) {
        return BlockPos.asLong(
            pos.getX() + direction.getStepX(),
            pos.getY() + direction.getStepY(),
            pos.getZ() + direction.getStepZ()
        );
    }

    /** 将位置分类为阻挡、待填充格或可继续遍历的同种流体源。 */
    private static int classifyForFill(Level level, BlockPos pos, Fluid fluid) {
        // flood-fill 只检查已加载区块，避免读取方块状态时同步加载新区块。
        if (!level.hasChunkAt(pos)) {
            return FILL_BLOCKED;
        }
        BlockState state = level.getBlockState(pos);
        FluidState fs = state.getFluidState();
        if (!fs.isEmpty()) {
            if (!fs.getType().isSame(fluid)) {
                return FILL_BLOCKED;
            }
            // 本流体的流动态是待填充格；源方块仅用于继续遍历。
            return fs.isSource() ? FILL_SOURCE : FILL_TARGET;
        }
        return state.isAir() || state.canBeReplaced() ? FILL_TARGET : FILL_BLOCKED;
    }

    /** 该位置是否为指定流体的源方块。 */
    private static boolean isSameFluidSource(Level level, BlockPos pos, Fluid fluid) {
        FluidState fs = level.getFluidState(pos);
        return !fs.isEmpty() && fs.getType().isSame(fluid) && fs.isSource();
    }

    /** 水平相邻源越少越接近当前缺口；抽取时沿缺口连续剥离可避免无限源重新闭合。 */
    private static int countSourceNeighbors(
        Level level, BlockPos pos, Fluid fluid, BlockPos.MutableBlockPos neighborCursor
    ) {
        int count = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            neighborCursor.set(
                pos.getX() + d.getStepX(),
                pos.getY(),
                pos.getZ() + d.getStepZ()
            );
            if (isSameFluidSource(level, neighborCursor, fluid)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 向上抽取：内部 &lt;3B 且上方有同种流体（或内部空、上方任意流体）时，从上方流体最上层、
     * 最远处开始清除 1B 并回填自身。
     */
    private void tryDrainUp(Level level, BlockPos pos) {
        if (!processFlowCleanup(level)) {
            return;
        }
        FluidStack stored = tank.getFluid();
        if (!stored.isEmpty() && stored.getAmount() >= DRAIN_THRESHOLD) {
            drainSearch = null;
            return;
        }
        // 内部有流体 → 只抽同种；内部为空 → 抽上方任意流体
        Fluid want = stored.isEmpty() ? null : stored.getFluid();
        SearchResult searchResult = findHighestDrainTarget(level, pos, want);
        BlockPos target = searchResult.target();
        if (target == null) {
            return;
        }
        FluidState fs = level.getFluidState(target);
        if (fs.isEmpty() || (want != null && !fs.getType().isSame(want))) {
            drainSearch = null;
            return;
        }
        Fluid fluid = fs.getType();
        if (!fs.isSource()) {
            startFlowCleanup(level, target, fluid);
            processFlowCleanup(level);
            drainSearch = null;
            return;
        }
        FluidStack toInsert = new FluidStack(fluid, UNIT);
        // 目标容量/类型校验
        if (tank.fill(toInsert, IFluidHandler.FluidAction.SIMULATE) < UNIT) {
            return;
        }
        // 源只计量并静默移除一格；紧邻的流动态另行连通清理，不计入储罐。
        removeSourceAndQueueFlowCleanup(level, target, fluid);
        processFlowCleanup(level);
        tank.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
        // 抽取会缩小甚至切断连通区域；下一次必须从入口重建，不能复用旧边界。
        drainSearch = null;
    }

    /**
     * 同层紧邻无限源生成：排水口<b>同层</b>（同 Y）水平四邻若有能构成无限源的流体源，
     * 则每 tick 在内部凭空 +1B 该流体。
     *
     * <p>"能构成无限源"复刻原版 {@code FlowingFluid#getNewLiquid}：该相邻源格自身水平四邻中，
     * 通过 {@link EventHooks#canCreateFluidSource} 的同种源 &ge;2 个，且其正下方为实体方块或同种源。
     * 该判定天然覆盖无限水、开启 {@code lavaSourceConversion} 的岩浆、以及模组注册的可无限流体
     * （均由 {@code canConvertToSource} 统一裁决）。只认同层紧邻，不做 flood-fill。
     */
    private void tryGenerateFromInfinite(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tank.getFluidAmount() >= CAPACITY) {
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = pos.relative(d);
            FluidState fs = level.getFluidState(n);
            if (fs.isEmpty() || !fs.isSource() || !(fs.getType() instanceof FlowingFluid flowing)) {
                continue;
            }
            if (!canFormInfiniteSource(serverLevel, n, flowing)) {
                continue;
            }
            FluidStack toInsert = new FluidStack(flowing.getSource(), UNIT);
            // 校验类型/容量：内部已有异种流体则跳过，另寻可接受的邻居
            if (tank.fill(toInsert, IFluidHandler.FluidAction.SIMULATE) < UNIT) {
                continue;
            }
            tank.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
    }

    /**
     * 复刻原版无限源成因：位置 {@code pos}（本身为 {@code fluid} 源）的水平四邻中，可转化的同种源
     * &ge;2 个，且正下方为实体方块或同种源。满足则该源可被无限再生。
     */
    private static boolean canFormInfiniteSource(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        return canRegenerateSourceAt(level, pos, fluid);
    }

    /** 判断空气/流动态位置是否会按原版规则被相邻源重新生成。 */
    private static boolean canRegenerateSourceAt(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        int neighbourSources = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos rel = pos.relative(d);
            BlockState relState = level.getBlockState(rel);
            FluidState relFs = relState.getFluidState();
            if (relFs.isSource()
                && relFs.getType().isSame(fluid)
                && EventHooks.canCreateFluidSource(level, rel, relState)) {
                neighbourSources++;
            }
        }
        if (neighbourSources < 2) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        FluidState belowFs = below.getFluidState();
        return below.isSolid() || (belowFs.isSource() && belowFs.getType().isSame(fluid));
    }

    /** 静默移除一个源，并把与其相邻的同种流动态加入清理队列；相邻源不会被修改。 */
    private void removeSourceAndQueueFlowCleanup(Level level, BlockPos target, Fluid fluid) {
        removeFluidSilently(level, target);
        prepareFlowCleanup(fluid);
        for (Direction direction : Direction.values()) {
            enqueueFlowing(level, target.relative(direction), fluid);
        }
    }

    /** 从一个流动态开始清理它所在的六向连通片；遍历不会穿过源方块。 */
    private void startFlowCleanup(Level level, BlockPos target, Fluid fluid) {
        prepareFlowCleanup(fluid);
        enqueueFlowing(level, target, fluid);
    }

    private static void removeFluidSilently(Level level, BlockPos target) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockState state = level.getBlockState(target);
        BlockState dryState = state.hasProperty(BlockStateProperties.WATERLOGGED)
                              && state.getValue(BlockStateProperties.WATERLOGGED)
            ? state.setValue(BlockStateProperties.WATERLOGGED, false)
            : Blocks.AIR.defaultBlockState();
        level.setBlock(target, dryState, flags);
    }

    private void prepareFlowCleanup(Fluid fluid) {
        if (flowCleanupFluid != null && !flowCleanupFluid.isSame(fluid)) {
            clearFlowCleanup();
        }
        flowCleanupFluid = fluid;
    }

    private void enqueueFlowing(Level level, BlockPos pos, Fluid fluid) {
        long packedPos = pos.asLong();
        if (!flowCleanupVisited.add(packedPos)) {
            return;
        }
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty() && !fluidState.isSource() && fluidState.getType().isSame(fluid)) {
            flowCleanupQueue.enqueue(packedPos);
        }
    }

    /** 返回当前流动态连通片是否已经清理完成；每 tick 最多处理 {@value #MAX_NODES} 格。*/
    private boolean processFlowCleanup(Level level) {
        if (flowCleanupQueue.isEmpty() || flowCleanupFluid == null) {
            clearFlowCleanup();
            return true;
        }
        Fluid fluid = flowCleanupFluid;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int processed = 0;
        while (!flowCleanupQueue.isEmpty() && processed < MAX_NODES) {
            long current = flowCleanupQueue.dequeueLong();
            processed++;
            cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
            FluidState fluidState = level.getFluidState(cursor);
            if (fluidState.isEmpty() || fluidState.isSource() || !fluidState.getType().isSame(fluid)) {
                continue;
            }
            removeFluidSilently(level, cursor);
            for (Direction direction : Direction.values()) {
                cursor.move(direction);
                enqueueFlowing(level, cursor, fluid);
                cursor.move(direction.getOpposite());
            }
        }
        if (flowCleanupQueue.isEmpty()) {
            clearFlowCleanup();
            return true;
        }
        return false;
    }

    private void clearFlowCleanup() {
        flowCleanupQueue.clear();
        flowCleanupVisited.clear();
        flowCleanupFluid = null;
    }

    /**
     * 定位向上抽取目标：先<b>垂直探顶</b>，再<b>水平 flood-fill</b>（不跨层）沿缺口抽取源。
     *
     * <p><b>第一步（探顶）</b>：从排水口正上方沿同种流体一路向上，找到最高流体层 {@code topY}。
     *
     * <p><b>第二步（单层抽取）</b>：自 {@code topY} 向下逐层，在每一层内<b>仅水平</b> flood-fill
     * （不跨层），返回首个含源的最高层里源邻居最少、同条件下最远的源。液面很大时，搜索按
     * 每 tick {@value #MAX_NODES} 个节点的预算续扫，确认完整边界后才抽取或下探下一层。
     *
     * <p><b>鞍部</b>：不向下跨层保证了排水口这侧抽到洞口高度后不会继续穿过洞口往隔壁盆地深处抽；
     *
     * @param want 需匹配的流体；{@code null} 表示接受正上方任意流体
     */
    private SearchResult findHighestDrainTarget(Level level, BlockPos drainPos, @Nullable Fluid want) {
        BlockPos start = drainPos.above();
        FluidState startFs = level.getFluidState(start);
        if (startFs.isEmpty() || (want != null && !startFs.getType().isSame(want))) {
            drainSearch = null;
            return SearchResult.EXHAUSTED;
        }
        Fluid fluid = want != null ? want : startFs.getType();
        // 第一步：沿正上方一路探到最高同种流体层
        int maxY = level.getMaxBuildHeight();
        int topY = start.getY();
        while (topY < maxY) {
            FluidState fs = level.getFluidState(new BlockPos(drainPos.getX(), topY + 1, drainPos.getZ()));
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) {
                break;
            }
            topY++;
        }
        if (drainSearch == null
            || !drainSearch.matches(drainPos, fluid, start.getY(), topY, level.getGameTime())) {
            drainSearch = new DrainSearch(
                drainPos,
                fluid,
                start.getY(),
                topY,
                level.getGameTime() / INTERVAL
            );
        }
        return drainSearch.advance(level);
    }

    /** 等距候选按四个水平方向轮转，避免无遮挡平面长期偏向同一方向。 */
    private static boolean isPreferredHorizontalTie(
        long candidateDx, long candidateDz, long bestDx, long bestDz, long selectionPhase
    ) {
        long candidatePrimary;
        long candidateSecondary;
        long bestPrimary;
        long bestSecondary;
        switch ((int) Math.floorMod(selectionPhase, 4)) {
            case 0 -> {
                candidatePrimary = candidateDx;
                candidateSecondary = candidateDz;
                bestPrimary = bestDx;
                bestSecondary = bestDz;
            }
            case 1 -> {
                candidatePrimary = candidateDz;
                candidateSecondary = -candidateDx;
                bestPrimary = bestDz;
                bestSecondary = -bestDx;
            }
            case 2 -> {
                candidatePrimary = -candidateDx;
                candidateSecondary = -candidateDz;
                bestPrimary = -bestDx;
                bestSecondary = -bestDz;
            }
            default -> {
                candidatePrimary = -candidateDz;
                candidateSecondary = candidateDx;
                bestPrimary = -bestDz;
                bestSecondary = bestDx;
            }
        }
        return candidatePrimary > bestPrimary
            || (candidatePrimary == bestPrimary && candidateSecondary > bestSecondary);
    }

    private enum FillResult {
        FILLED,
        SEARCHING,
        NONE
    }

    private record SearchResult(@Nullable BlockPos target, boolean pending) {
        private static final SearchResult PENDING = new SearchResult(null, true);
        private static final SearchResult EXHAUSTED = new SearchResult(null, false);

        private static SearchResult found(long target) {
            return new SearchResult(BlockPos.of(target), false);
        }
    }

    /**
     * 向下填充的跨 tick 搜索。每层只遍历已有源；空气和流动态仅进入边界候选，实际填成源后
     * 才继续从该位置扩展。因此开放空间不会消耗完整 flood-fill 预算，也不会因预算耗尽提前升层。
     */
    private static final class FillSearch {
        private final long drainPos;
        private final Fluid fluid;
        private final int bottomY;
        private final int topY;
        private int currentY;
        private int filledTargets;
        private long exhaustedAt = Long.MIN_VALUE;
        @Nullable
        private FillLayerSearch layerSearch;

        private FillSearch(BlockPos drainPos, Fluid fluid, int bottomY, int topY) {
            this.drainPos = drainPos.asLong();
            this.fluid = fluid;
            this.bottomY = bottomY;
            this.topY = topY;
            this.currentY = bottomY;
        }

        private boolean matches(BlockPos drainPos, Fluid fluid, int bottomY, int topY, long gameTime) {
            return this.drainPos == drainPos.asLong()
                && this.fluid.isSame(fluid)
                && this.bottomY == bottomY
                && this.topY == topY
                && (exhaustedAt == Long.MIN_VALUE || gameTime - exhaustedAt < EXHAUSTED_SEARCH_TTL);
        }

        private SearchResult advance(Level level) {
            while (currentY <= topY) {
                if (layerSearch == null) {
                    long entry = BlockPos.asLong(BlockPos.getX(drainPos), currentY, BlockPos.getZ(drainPos));
                    layerSearch = new FillLayerSearch(drainPos, entry, fluid);
                }
                SearchResult result = layerSearch.advance(level, level.getGameTime() / INTERVAL);
                if (result.pending() || result.target() != null) {
                    exhaustedAt = Long.MIN_VALUE;
                    return result;
                }
                currentY++;
                layerSearch = null;
            }
            if (exhaustedAt == Long.MIN_VALUE) {
                exhaustedAt = level.getGameTime();
            }
            return SearchResult.EXHAUSTED;
        }

        private boolean isTargetStillReachable(Level level, long target) {
            return layerSearch != null && layerSearch.isTargetStillReachable(level, target);
        }

        /** 返回是否应周期性重建边界，以吸收外部方块/流体变化。 */
        private boolean acceptFilled(long target) {
            if (layerSearch == null) {
                return true;
            }
            layerSearch.acceptFilled(target);
            exhaustedAt = Long.MIN_VALUE;
            return ++filledTargets >= FILL_SEARCH_REBUILD_INTERVAL;
        }
    }

    private static final class FillLayerSearch {
        private final long drainPos;
        private final long entry;
        private final Fluid fluid;
        private final LongOpenHashSet discovered = new LongOpenHashSet(MAX_NODES);
        private final Long2LongOpenHashMap predecessors = new Long2LongOpenHashMap(MAX_NODES);
        private final LongOpenHashSet candidates = new LongOpenHashSet();
        private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue(MAX_NODES);

        private FillLayerSearch(long drainPos, long entry, Fluid fluid) {
            this.drainPos = drainPos;
            this.entry = entry;
            this.fluid = fluid;
            discovered.add(entry);
            queue.enqueue(entry);
        }

        private SearchResult advance(Level level, long selectionPhase) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int processed = 0;
            while (true) {
                while (!queue.isEmpty() && processed < MAX_NODES) {
                    long current = queue.dequeueLong();
                    processed++;
                    cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                    int fillType = classifyForFill(level, cursor, fluid);
                    if (fillType == FILL_TARGET) {
                        candidates.add(current);
                    } else if (fillType == FILL_SOURCE) {
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                            discover(level, offset(cursor, direction), current, cursor);
                            cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                        }
                        discover(
                            level,
                            BlockPos.asLong(cursor.getX(), cursor.getY() - 1, cursor.getZ()),
                            current,
                            cursor
                        );
                    }
                }
                if (!queue.isEmpty()) {
                    return SearchResult.PENDING;
                }

                long best = 0;
                int bestY = Integer.MAX_VALUE;
                long bestDist = Long.MAX_VALUE;
                long bestDx = 0;
                long bestDz = 0;
                boolean found = false;
                boolean discoveredSource = false;
                LongIterator iterator = candidates.iterator();
                while (iterator.hasNext()) {
                    long candidate = iterator.nextLong();
                    cursor.set(BlockPos.getX(candidate), BlockPos.getY(candidate), BlockPos.getZ(candidate));
                    int fillType = classifyForFill(level, cursor, fluid);
                    if (fillType == FILL_BLOCKED) {
                        iterator.remove();
                        continue;
                    }
                    if (fillType == FILL_SOURCE) {
                        iterator.remove();
                        queue.enqueue(candidate);
                        discoveredSource = true;
                        continue;
                    }
                    int y = cursor.getY();
                    long dx = (long) cursor.getX() - BlockPos.getX(drainPos);
                    long dz = (long) cursor.getZ() - BlockPos.getZ(drainPos);
                    long dist = dx * dx + dz * dz;
                    if (!found
                        || y < bestY
                        || (y == bestY
                            && (dist < bestDist
                                || (dist == bestDist
                                    && isPreferredHorizontalTie(dx, dz, bestDx, bestDz, selectionPhase))))) {
                        best = candidate;
                        bestY = y;
                        bestDist = dist;
                        bestDx = dx;
                        bestDz = dz;
                        found = true;
                    }
                }
                if (discoveredSource) {
                    if (processed >= MAX_NODES) {
                        return SearchResult.PENDING;
                    }
                    continue;
                }
                return found ? SearchResult.found(best) : SearchResult.EXHAUSTED;
            }
        }

        private void discover(Level level, long pos, long predecessor, BlockPos.MutableBlockPos cursor) {
            if (!discovered.add(pos)) {
                return;
            }
            predecessors.put(pos, predecessor);
            cursor.set(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
            int fillType = classifyForFill(level, cursor, fluid);
            if (fillType == FILL_SOURCE) {
                queue.enqueue(pos);
            } else if (fillType == FILL_TARGET) {
                candidates.add(pos);
            }
        }

        /**
         * 缓存候选可能因洞口被封而与入口断开。沿首次发现时的前驱链复核当前源方块，
         * 任一路径节点失效就让上层重建搜索；重建时仍存在的其他通路会被重新发现。
         */
        private boolean isTargetStillReachable(Level level, long target) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            cursor.set(BlockPos.getX(target), BlockPos.getY(target), BlockPos.getZ(target));
            if (!discovered.contains(target) || classifyForFill(level, cursor, fluid) != FILL_TARGET) {
                return false;
            }

            long current = target;
            int remaining = discovered.size();
            while (current != entry && remaining-- > 0) {
                if (!predecessors.containsKey(current)) {
                    return false;
                }
                current = predecessors.get(current);
                cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                if (classifyForFill(level, cursor, fluid) != FILL_SOURCE) {
                    return false;
                }
            }
            return current == entry;
        }

        private void acceptFilled(long target) {
            candidates.remove(target);
            discovered.add(target);
            queue.enqueue(target);
        }
    }

    /** 抽取会缩小连通区域，因此每次动作后重建；单次重建可跨 tick 续扫，绝不把预算耗尽当作层完成。 */
    private static final class DrainSearch {
        private final long drainPos;
        private final Fluid fluid;
        private final int bottomY;
        private final int topY;
        private final long selectionPhase;
        private int currentY;
        private long exhaustedAt = Long.MIN_VALUE;
        @Nullable
        private DrainLayerSearch layerSearch;

        private DrainSearch(
            BlockPos drainPos,
            Fluid fluid,
            int bottomY,
            int topY,
            long selectionPhase
        ) {
            this.drainPos = drainPos.asLong();
            this.fluid = fluid;
            this.bottomY = bottomY;
            this.topY = topY;
            this.currentY = topY;
            this.selectionPhase = selectionPhase;
        }

        private boolean matches(BlockPos drainPos, Fluid fluid, int bottomY, int topY, long gameTime) {
            return this.drainPos == drainPos.asLong()
                && this.fluid.isSame(fluid)
                && this.bottomY == bottomY
                && this.topY == topY
                && (exhaustedAt == Long.MIN_VALUE || gameTime - exhaustedAt < EXHAUSTED_SEARCH_TTL);
        }

        private SearchResult advance(Level level) {
            while (currentY >= bottomY) {
                if (layerSearch == null) {
                    long entry = BlockPos.asLong(BlockPos.getX(drainPos), currentY, BlockPos.getZ(drainPos));
                    layerSearch = new DrainLayerSearch(drainPos, entry, fluid, selectionPhase);
                }
                SearchResult result = layerSearch.advance(level);
                if (result.pending() || result.target() != null) {
                    exhaustedAt = Long.MIN_VALUE;
                    return result;
                }
                currentY--;
                layerSearch = null;
            }
            if (exhaustedAt == Long.MIN_VALUE) {
                exhaustedAt = level.getGameTime();
            }
            return SearchResult.EXHAUSTED;
        }
    }

    private static final class DrainLayerSearch {
        private final long drainPos;
        private final Fluid fluid;
        private final long selectionPhase;
        private final LongOpenHashSet discovered = new LongOpenHashSet(MAX_NODES);
        private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue(MAX_NODES);
        private long best;
        private int bestY = Integer.MIN_VALUE;
        private long bestDist = Long.MIN_VALUE;
        private long bestDx;
        private long bestDz;
        private int bestNeighbors = Integer.MAX_VALUE;
        private boolean found;
        private long bestFlowing;
        private int bestFlowingY = Integer.MIN_VALUE;
        private long bestFlowingDist = Long.MIN_VALUE;
        private long bestFlowingDx;
        private long bestFlowingDz;
        private boolean foundFlowing;

        private DrainLayerSearch(long drainPos, long entry, Fluid fluid, long selectionPhase) {
            this.drainPos = drainPos;
            this.fluid = fluid;
            this.selectionPhase = selectionPhase;
            discovered.add(entry);
            queue.enqueue(entry);
        }

        private SearchResult advance(Level level) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos neighborCursor = new BlockPos.MutableBlockPos();
            int processed = 0;
            while (!queue.isEmpty() && processed < MAX_NODES) {
                long current = queue.dequeueLong();
                processed++;
                cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                FluidState fluidState = level.getFluidState(cursor);
                if (fluidState.isEmpty() || !fluidState.getType().isSame(fluid)) {
                    continue;
                }
                if (fluidState.isSource()) {
                    considerSource(level, current, cursor, neighborCursor);
                } else {
                    considerFlowing(current, cursor);
                }
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    discoverFluid(level, offset(cursor, direction), neighborCursor);
                }
                discoverFluid(
                    level,
                    BlockPos.asLong(cursor.getX(), cursor.getY() + 1, cursor.getZ()),
                    neighborCursor
                );
            }
            if (!queue.isEmpty()) {
                return SearchResult.PENDING;
            }
            if (found) {
                return SearchResult.found(best);
            }
            return foundFlowing ? SearchResult.found(bestFlowing) : SearchResult.EXHAUSTED;
        }

        private void discoverFluid(Level level, long pos, BlockPos.MutableBlockPos cursor) {
            if (!discovered.add(pos)) {
                return;
            }
            cursor.set(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
            FluidState fluidState = level.getFluidState(cursor);
            if (!fluidState.isEmpty() && fluidState.getType().isSame(fluid)) {
                queue.enqueue(pos);
            }
        }

        private void considerSource(
            Level level,
            long source,
            BlockPos sourcePos,
            BlockPos.MutableBlockPos neighborCursor
        ) {
            int neighbors = countSourceNeighbors(level, sourcePos, fluid, neighborCursor);
            long dx = (long) sourcePos.getX() - BlockPos.getX(drainPos);
            long dz = (long) sourcePos.getZ() - BlockPos.getZ(drainPos);
            long dist = dx * dx + dz * dz;
            int y = sourcePos.getY();
            if (!found
                || neighbors < bestNeighbors
                || (neighbors == bestNeighbors
                    && (dist > bestDist
                        || (dist == bestDist
                            && (isPreferredHorizontalTie(dx, dz, bestDx, bestDz, selectionPhase)
                                || (dx == bestDx && dz == bestDz && y > bestY)))))) {
                best = source;
                bestY = y;
                bestDist = dist;
                bestDx = dx;
                bestDz = dz;
                bestNeighbors = neighbors;
                found = true;
            }
        }

        private void considerFlowing(long flowing, BlockPos flowingPos) {
            long dx = (long) flowingPos.getX() - BlockPos.getX(drainPos);
            long dz = (long) flowingPos.getZ() - BlockPos.getZ(drainPos);
            long dist = dx * dx + dz * dz;
            int y = flowingPos.getY();
            if (!foundFlowing
                || dist > bestFlowingDist
                || (dist == bestFlowingDist
                    && (isPreferredHorizontalTie(
                            dx,
                            dz,
                            bestFlowingDx,
                            bestFlowingDz,
                            selectionPhase
                        )
                        || (dx == bestFlowingDx && dz == bestFlowingDz && y > bestFlowingY)))) {
                bestFlowing = flowing;
                bestFlowingY = y;
                bestFlowingDist = dist;
                bestFlowingDx = dx;
                bestFlowingDz = dz;
                foundFlowing = true;
            }
        }
    }

    // ---- NBT / 同步 ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankNbt = tank.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        tag.putInt("ColumnBottomY", columnBottomY);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Fluid"));
        // 客户端经 ClientboundBlockEntityDataPacket → onDataPacket → loadAdditional 应用，
        // 故 columnBottomY 必须在此读取（而非 handleUpdateTag），否则水柱数据到不了客户端。
        this.columnBottomY = tag.contains("ColumnBottomY") ? tag.getInt("ColumnBottomY") : Integer.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
