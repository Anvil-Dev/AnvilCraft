package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 排水口的 BlockEntity。内部 4B 流体容量，像储罐一样渲染内部流体，并像机械动力软管滑轮一样
 * 向下把内部流体铺放到世界、或从上方抽取流体入内部。
 *
 * <h3>向下填充</h3>
 * 内部流体 &gt; 1B 且下方有空间时，每 {@value #INTERVAL} gt 消耗 1B，在下方 flood-fill 区域内
 * 由最低层、就近位置起放置<b>真实源方块</b>（flag=2 不触发更新，避免流动水扩散/无限水），
 * 逐层填满直到排水口正下方一层。
 *
 * <h3>向上抽取</h3>
 * 内部 &lt; 3B 且上方有同种流体（或内部为空、上方任意流体）时，每 {@value #INTERVAL} gt 从上方
 * 流体的最上层起清除 1B 并填充自身（同样 flag=2）。
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
    /** flood-fill 节点上限，避免大水池卡顿 */
    private static final int MAX_NODES = 2048;

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
        boolean filled = be.tryFillDown(level, pos);
        if (!filled) {
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
     * @return 是否执行了一次有效填充
     */
    private boolean tryFillDown(Level level, BlockPos pos) {
        FluidStack stored = tank.getFluid();
        if (stored.getAmount() <= FILL_THRESHOLD) {
            return false;
        }
        Fluid fluid = stored.getFluid();
        BlockState source = fluid.defaultFluidState().createLegacyBlock();
        if (source.isAir()) {
            return false; // 无对应可放置方块（如蜂蜜）
        }

        BlockPos target = findFillTarget(level, pos, fluid);
        if (target == null) {
            clearColumn();
            return false;
        }
        // 放置真实源方块，UPDATE_ALL 正常触发更新（红石/邻居/渲染）；水自发流动无妨。
        level.setBlock(target, source, Block.UPDATE_ALL);
        tank.drain(UNIT, IFluidHandler.FluidAction.EXECUTE);

        // 更新渲染水柱底部：从排水口下方渲染到本次目标所在层
        int newBottom = target.getY();
        if (newBottom != columnBottomY) {
            columnBottomY = newBottom;
            sendUpdate();
        }
        return true;
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
     * 这样水在盆地底部平铺（受 {@value #MAX_NODES} 预算约束），填满才升层；
     * 遇到鞍部时，上层扫描穿过洞口后自然向下发现隔壁盆地的更深空格，实现正确填充。
     */
    @Nullable
    private BlockPos findFillTarget(Level level, BlockPos drainPos, Fluid fluid) {
        BlockPos start = drainPos.below();
        if (!isPassableForFill(level, start, fluid)) {
            return null; // 正下方被堵，无法向下排水
        }
        // 第一步：沿正下方一路探到最低可放置层
        int minY = level.getMinBuildHeight();
        int bottomY = start.getY();
        while (bottomY > minY
            && isPassableForFill(level, new BlockPos(drainPos.getX(), bottomY - 1, drainPos.getZ()), fluid)) {
            bottomY--;
        }
        // 第二步：自底向上逐层 flood-fill（每层可向下穿透）
        for (int y = bottomY; y <= start.getY(); y++) {
            BlockPos target = findEmptyWithDownward(level, drainPos, new BlockPos(drainPos.getX(), y, drainPos.getZ()), fluid);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    /** 探底可通行判定：待填充空格或同种流体（源/流动皆可穿行）。 */
    private static boolean isPassableForFill(Level level, BlockPos pos, Fluid fluid) {
        return isPlaceableEmpty(level, pos, fluid) || isSameFluidSource(level, pos, fluid);
    }

    /**
     * 以 {@code entry} 为入口做"水平 + 向下/向上" flood-fill，穿行"空格 + 同种流体"连通区，
     * 收集所有<b>空格</b>，返回其中<b>最低层、同层最近</b>的一个。
     *
     * <p>入口层水平穿行到每个可通行格时同时检查正下方和正上方：下方/上方为空格则纳入候选，
     * 下方/上方为同种流体源则继续入队扩散。向下解决鞍部（墙后盆地更深处），
     * 向上解决反向鞍部（天花板凹穴内漏填）。
     * 候选格统一取<b>最低层、同层最近</b>一个。
     */
    @Nullable
    private BlockPos findEmptyWithDownward(Level level, BlockPos drainPos, BlockPos entry, Fluid fluid) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> empties = new ArrayList<>();
        queue.add(entry);
        visited.add(entry);

        while (!queue.isEmpty() && visited.size() <= MAX_NODES) {
            BlockPos cur = queue.poll();
            boolean empty = isPlaceableEmpty(level, cur, fluid);
            boolean sameFluid = !empty && isSameFluidSource(level, cur, fluid);
            if (!empty && !sameFluid) {
                continue; // 墙壁/异种流体 → 阻断
            }
            if (empty) {
                empties.add(cur);
            }
            // 水平扩散
            for (Direction d : Direction.Plane.HORIZONTAL) {
                enqueue(queue, visited, cur.relative(d));
            }
            // 向下穿透：鞍部——穿过洞口后下探隔壁盆地深层空格
            checkVerticalNeighbor(level, cur.below(), fluid, queue, visited, empties);
        }
        if (empties.isEmpty()) {
            return null;
        }
        // 最低层（Y 最小）优先；同层取离排水口 XZ 最近
        BlockPos best = null;
        int bestY = Integer.MAX_VALUE;
        long bestDist = Long.MAX_VALUE;
        for (BlockPos p : empties) {
            long dx = p.getX() - drainPos.getX();
            long dz = p.getZ() - drainPos.getZ();
            long dist = dx * dx + dz * dz;
            if (p.getY() < bestY || (p.getY() == bestY && dist < bestDist)) {
                bestY = p.getY();
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    /**
     * 检查垂直邻居（上或下）：空格直接纳入候选，同种流体源入队继续扩散。
     * 已访问过则跳过。
     */
    private void checkVerticalNeighbor(Level level, BlockPos neighbor, Fluid fluid,
                                        ArrayDeque<BlockPos> queue,
                                        Set<BlockPos> visited,
                                        List<BlockPos> empties) {
        if (visited.contains(neighbor.immutable())) {
            return;
        }
        if (isPlaceableEmpty(level, neighbor, fluid)) {
            visited.add(neighbor.immutable());
            empties.add(neighbor);
        } else if (isSameFluidSource(level, neighbor, fluid)) {
            enqueue(queue, visited, neighbor);
        }
    }

    private static void enqueue(java.util.ArrayDeque<BlockPos> queue, java.util.Set<BlockPos> visited, BlockPos p) {
        if (visited.add(p.immutable())) {
            queue.add(p.immutable());
        }
    }

    /**
     * 该位置是否为"待填充空格"：可放入流体（空气/可替换方块且无流体），
     * 或已有本流体的<b>流动态</b>（非源）——把自发流成的流动水也补成满源，实现逐格填满。
     */
    private static boolean isPlaceableEmpty(Level level, BlockPos pos, Fluid fluid) {
        BlockState state = level.getBlockState(pos);
        FluidState fs = state.getFluidState();
        if (!fs.isEmpty()) {
            // 已有流体：仅当是本流体的流动态（非源）才算待填充（补成源）
            return fs.getType().isSame(fluid) && !fs.isSource();
        }
        return state.isAir() || state.canBeReplaced();
    }

    /** 该位置是否为指定流体的源方块。 */
    private static boolean isSameFluidSource(Level level, BlockPos pos, Fluid fluid) {
        FluidState fs = level.getFluidState(pos);
        return !fs.isEmpty() && fs.getType().isSame(fluid) && fs.isSource();
    }

    /** 统计某位置水平四邻中同种流体<b>源</b>的数量（用于抽取时优先移除边缘源，避免无限水回填）。 */
    private static int countSourceNeighbors(Level level, BlockPos pos, Fluid fluid) {
        int count = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (isSameFluidSource(level, pos.relative(d), fluid)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 向上抽取：内部 &lt;3B 且上方有同种流体（或内部空、上方任意流体）时，从上方流体最上层、
     * 就近处清除 1B 并回填自身。
     */
    private void tryDrainUp(Level level, BlockPos pos) {
        FluidStack stored = tank.getFluid();
        if (!stored.isEmpty() && stored.getAmount() >= DRAIN_THRESHOLD) {
            return;
        }
        // 内部有流体 → 只抽同种；内部为空 → 抽上方任意流体
        Fluid want = stored.isEmpty() ? null : stored.getFluid();
        BlockPos target = findHighestNearestFluid(level, pos, want);
        if (target == null) {
            return;
        }
        FluidState fs = level.getFluidState(target);
        Fluid fluid = fs.getType();
        FluidStack toInsert = new FluidStack(fluid, UNIT);
        // 目标容量/类型校验
        if (tank.fill(toInsert, IFluidHandler.FluidAction.SIMULATE) < UNIT) {
            return;
        }
        // 移除目标源（flag3 正常更新），并抑制同层相邻源回填 → 可抽干 2×2 等无限水
        removeSourceWithSuppression(level, target, fluid);
        tank.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
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

    /**
     * 移除一个流体源并抑制无限水回填。
     *
     * <p>把目标格设为空气（{@link Block#UPDATE_ALL} 正常更新），随后把其<b>同层水平相邻</b>的同种源
     * 降级为<b>流动态</b>（非源）。这样目标格四周不再满足"≥2 个源邻居"的无限水条件，
     * 相邻水不会瞬间把目标重新变回源；被降级的流动水若仍有更外围的源支撑则下 tick 自然复原，
     * 否则逐渐干涸——配合逐格抽取，可把 2×2 乃至任意无限水池真正抽干。
     */
    private static void removeSourceWithSuppression(Level level, BlockPos target, Fluid fluid) {
        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = target.relative(d);
            if (isSameFluidSource(level, n, fluid) && fluid instanceof FlowingFluid flowing) {
                // 降级为高等级流动态（level 7，接近满但非源），打破无限水对
                BlockState flowingState = flowing.getFlowing(7, false).createLegacyBlock();
                level.setBlock(n, flowingState, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * 定位向上抽取目标：先<b>垂直探顶</b>，再<b>水平 flood-fill</b>（不跨层）取"边缘优先、最近"的源。
     *
     * <p><b>第一步（探顶）</b>：从排水口正上方沿同种流体一路向上，找到最高流体层 {@code topY}。
     *
     * <p><b>第二步（单层抽取）</b>：自 {@code topY} 向下逐层，在每一层内<b>仅水平</b> flood-fill
     * （不跨层），返回首个含源的最高层里边缘优先、最近的源。液面很大时先把最顶一层一大片
     * （受 {@value #MAX_NODES} 预算约束）抽完才下探下一层。
     *
     * <p><b>鞍部</b>：不向下跨层保证了排水口这侧抽到洞口高度后不会继续穿过洞口往隔壁盆地深处抽；
     *
     * @param want 需匹配的流体；{@code null} 表示接受正上方任意流体
     */
    @Nullable
    private BlockPos findHighestNearestFluid(Level level, BlockPos drainPos, @Nullable Fluid want) {
        BlockPos start = drainPos.above();
        FluidState startFs = level.getFluidState(start);
        if (startFs.isEmpty() || (want != null && !startFs.getType().isSame(want))) {
            return null;
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
        // 第二步：自顶向下逐层，在单层内找源
        for (int y = topY; y >= start.getY(); y--) {
            BlockPos target = findSourceInLayer(level, drainPos, new BlockPos(drainPos.getX(), y, drainPos.getZ()), fluid);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    /**
     * 以 {@code entry} 为入口做"水平 + 向上" flood-fill 同种流体，
     * 返回所有<b>源</b>中"水平同种源邻居最少（边缘优先）→ 最近"的一个。
     *
     * <p>水平扩散的同时检查正上方一格：上方有同种流体则入队继续探。
     * 从而在反向鞍部（天花板凸起/凹穴）场景中，不会因为仅水平扫描而漏掉上方的水源。
     * <b>不向下</b>扩散，保证不跨鞍部往下抽。
     *
     * <p>"边缘优先"确保移除后不会留下被 &ge;2 个源夹住的空格，使水池从边缘向内剥离。
     */
    @Nullable
    private BlockPos findSourceInLayer(Level level, BlockPos drainPos, BlockPos entry, Fluid fluid) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> sources = new ArrayList<>();
        queue.add(entry);
        visited.add(entry);

        while (!queue.isEmpty() && visited.size() <= MAX_NODES) {
            BlockPos cur = queue.poll();
            FluidState fs = level.getFluidState(cur);
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) {
                continue; // 非目标流体 → 阻断
            }
            if (fs.isSource()) {
                sources.add(cur);
            }
            // 水平扩散
            for (Direction d : Direction.Plane.HORIZONTAL) {
                enqueue(queue, visited, cur.relative(d));
            }
            // 向上穿透：反向鞍部——天花板凹穴内的水源
            BlockPos above = cur.above();
            if (!visited.contains(above.immutable())) {
                FluidState aboveFs = level.getFluidState(above);
                if (!aboveFs.isEmpty() && aboveFs.getType().isSame(fluid)) {
                    enqueue(queue, visited, above);
                }
            }
        }
        if (sources.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        int bestNeighbors = Integer.MAX_VALUE;
        long bestDist = Long.MAX_VALUE;
        for (BlockPos p : sources) {
            int neighbors = countSourceNeighbors(level, p, fluid);
            long dx = p.getX() - drainPos.getX();
            long dz = p.getZ() - drainPos.getZ();
            long dist = dx * dx + dz * dz;
            boolean better;
            if (neighbors != bestNeighbors) {
                better = neighbors < bestNeighbors;
            } else {
                better = dist < bestDist;
            }
            if (better) {
                bestNeighbors = neighbors;
                bestDist = dist;
                best = p;
            }
        }
        return best;
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
