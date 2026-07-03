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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
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
 * 由最低层、就近位置起放置<b>真实源方块</b>（flag=2 不触发更新，避免流动水扩散/无限水），
 * 逐层填满直到排水口正下方一层。
 *
 * <h3>向上抽取</h3>
 * 内部 &lt; 3B 且上方有同种流体（或内部为空、上方任意流体）时，每 {@value #INTERVAL} gt 从上方
 * 流体的最上层起清除 1B 并填充自身（同样 flag=2）。
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
     * 从排水口正下方出发、沿"空格 + 同种流体源"连通区（向下 + 水平，不向上）flood-fill，
     * 在所有<b>空格</b>中取最低层、同层就近的一个作为填充目标。
     *
     * <p>同种流体源作为连通介质被穿行，因此不规则形状（某层边上多出一格）也能顺着流体边界排查到。
     * 若最低层还有任何空格，就一直在该层填（每次调用填一格），直到该层无空格才自然升到上一层。
     */
    @Nullable
    private BlockPos findFillTarget(Level level, BlockPos drainPos, Fluid fluid) {
        BlockPos start = drainPos.below();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.List<BlockPos> empties = new java.util.ArrayList<>();
        queue.add(start);
        visited.add(start);

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
            // 空格与同种流体源都作为连通介质，继续向下 + 水平扩散
            enqueue(queue, visited, cur.below());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                enqueue(queue, visited, cur.relative(d));
            }
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
     * 从排水口正上方出发、向上+水平 flood-fill 流体源，返回最高层、离排水口 XZ 最近的一个。
     *
     * @param want 需匹配的流体；{@code null} 表示接受任意流体
     */
    @Nullable
    private BlockPos findHighestNearestFluid(Level level, BlockPos drainPos, @Nullable Fluid want) {
        BlockPos start = drainPos.above();
        FluidState startFs = level.getFluidState(start);
        if (startFs.isEmpty()) {
            return null;
        }
        Fluid fluid = want != null ? want : startFs.getType();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.List<BlockPos> sources = new java.util.ArrayList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_NODES) {
            BlockPos cur = queue.poll();
            FluidState fs = level.getFluidState(cur);
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) {
                continue; // 非目标流体 → 阻断
            }
            if (fs.isSource()) {
                sources.add(cur);
            }
            // 向上和水平扩散
            enqueue(queue, visited, cur.above());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                enqueue(queue, visited, cur.relative(d));
            }
        }
        if (sources.isEmpty()) {
            return null;
        }
        // 选取顺序：最高层(Y大)优先 → 水平同种源邻居最少(边缘优先) → 最近。
        // "边缘优先"确保移除后不会留下被 ≥2 个源夹住的空格（无限水成因），
        // 使水池从边缘向内剥离，避免 3 格一排删中间被两侧无限回填、永远抽不完。
        BlockPos best = null;
        int bestY = Integer.MIN_VALUE;
        int bestNeighbors = Integer.MAX_VALUE;
        long bestDist = Long.MAX_VALUE;
        for (BlockPos p : sources) {
            int neighbors = countSourceNeighbors(level, p, fluid);
            long dx = p.getX() - drainPos.getX();
            long dz = p.getZ() - drainPos.getZ();
            long dist = dx * dx + dz * dz;
            boolean better;
            if (p.getY() != bestY) {
                better = p.getY() > bestY;
            } else if (neighbors != bestNeighbors) {
                better = neighbors < bestNeighbors;
            } else {
                better = dist < bestDist;
            }
            if (better) {
                bestY = p.getY();
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
