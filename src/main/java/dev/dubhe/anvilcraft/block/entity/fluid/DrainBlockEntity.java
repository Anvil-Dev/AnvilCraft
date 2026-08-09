package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
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
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

@Getter
public class DrainBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder {
    public static final int CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    private static final int UNIT = FluidType.BUCKET_VOLUME;
    private static final int INTERVAL = 5;
    private static final int FILL_THRESHOLD = FluidType.BUCKET_VOLUME;
    private static final int DRAIN_THRESHOLD = 3 * FluidType.BUCKET_VOLUME;
    private static final int MAX_NODES = 2048;
    private static final int FILL_BLOCKED = 0;
    private static final int FILL_TARGET = 1;
    private static final int FILL_SOURCE = 2;
    private static final int FILL_SEARCH_REBUILD_INTERVAL = 256;
    private static final long EXHAUSTED_SEARCH_TTL = 100;
    private final FluidStackResourceHandler tank = new FluidStackResourceHandler(DrainBlockEntity.CAPACITY) {
        @Override
        protected void onContentChanged(FluidStack original) {
            DrainBlockEntity.this.setChanged();
            DrainBlockEntity.this.sendUpdate();
        }
    };

    private int columnBottomY = Integer.MIN_VALUE;
    @Getter(AccessLevel.NONE)
    private @Nullable FillSearch fillSearch;
    @Getter(AccessLevel.NONE)
    private @Nullable DrainSearch drainSearch;
    @Getter(AccessLevel.NONE)
    private final LongArrayFIFOQueue flowCleanupQueue = new LongArrayFIFOQueue();
    @Getter(AccessLevel.NONE)
    private final LongOpenHashSet flowCleanupVisited = new LongOpenHashSet();
    @Getter(AccessLevel.NONE)
    private @Nullable Fluid flowCleanupFluid;

    public DrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

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
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DrainBlockEntity be) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % DrainBlockEntity.INTERVAL != 0) return;
        FillResult fillResult = be.tryFillDown(level, pos);
        if (fillResult == FillResult.NONE) be.clearColumn();
        be.tryDrainUp(level, pos);
        be.tryGenerateFromInfinite(level, pos);
    }

    private void clearColumn() {
        if (this.columnBottomY != Integer.MIN_VALUE) {
            this.columnBottomY = Integer.MIN_VALUE;
            this.sendUpdate();
        }
    }

    private FillResult tryFillDown(Level level, BlockPos pos) {
        FluidStack stored = this.tank.getStack();
        if (stored.getAmount() <= DrainBlockEntity.FILL_THRESHOLD) {
            this.fillSearch = null;
            return FillResult.NONE;
        }
        Fluid fluid = stored.getFluid();
        if (level.dimensionType().attributes().contains(EnvironmentAttributes.WATER_EVAPORATES)
            && fluid.isSame(Fluids.WATER)) {
            this.fillSearch = null;
            return FillResult.NONE;
        }
        BlockState source = fluid.defaultFluidState().createLegacyBlock();
        if (source.isAir()) {
            this.fillSearch = null;
            return FillResult.NONE;
        }

        SearchResult searchResult = this.findFillTarget(level, pos, fluid);
        BlockPos target = searchResult.target();
        if (target == null) {
            return searchResult.pending() ? FillResult.SEARCHING : FillResult.NONE;
        }
        if (this.fillSearch == null || !this.fillSearch.isTargetStillReachable(level, target.asLong())) {
            this.fillSearch = null;
            return FillResult.SEARCHING;
        }
        level.setBlock(target, source, Block.UPDATE_ALL);
        try (Transaction tx = Transaction.openRoot()) {
            this.tank.extract(0, FluidResource.of(stored), DrainBlockEntity.UNIT, tx);
            tx.commit();
        }
        if (this.fillSearch != null && this.fillSearch.acceptFilled(target.asLong())) {
            this.fillSearch = null;
        }
        if (target.getY() != this.columnBottomY) {
            this.columnBottomY = target.getY();
            this.sendUpdate();
        }
        return FillResult.FILLED;
    }

    private SearchResult findFillTarget(Level level, BlockPos drainPos, Fluid fluid) {
        BlockPos start = drainPos.below();
        if (!DrainBlockEntity.isPassableForFill(level, start, fluid)) {
            this.fillSearch = null;
            return SearchResult.EXHAUSTED;
        }
        int minY = level.getMinY();
        int bottomY = start.getY();
        while (bottomY > minY
               && DrainBlockEntity.isPassableForFill(level, new BlockPos(drainPos.getX(), bottomY - 1, drainPos.getZ()), fluid)) {
            bottomY--;
        }
        if (this.fillSearch == null
            || !this.fillSearch.matches(drainPos, fluid, bottomY, start.getY(), level.getGameTime())) {
            this.fillSearch = new FillSearch(drainPos, fluid, bottomY, start.getY());
        }
        return this.fillSearch.advance(level);
    }

    private static boolean isPassableForFill(Level level, BlockPos pos, Fluid fluid) {
        return DrainBlockEntity.classifyForFill(level, pos, fluid) != DrainBlockEntity.FILL_BLOCKED;
    }

    private static long offset(BlockPos pos, Direction direction) {
        return BlockPos.asLong(
            pos.getX() + direction.getStepX(),
            pos.getY() + direction.getStepY(),
            pos.getZ() + direction.getStepZ()
        );
    }

    private static int classifyForFill(Level level, BlockPos pos, Fluid fluid) {
        BlockState state = level.getBlockState(pos);
        FluidState fs = state.getFluidState();
        if (!fs.isEmpty()) {
            if (!fs.getType().isSame(fluid)) return DrainBlockEntity.FILL_BLOCKED;
            return fs.isSource() ? DrainBlockEntity.FILL_SOURCE : DrainBlockEntity.FILL_TARGET;
        }
        return state.isAir() || state.canBeReplaced() ? DrainBlockEntity.FILL_TARGET : DrainBlockEntity.FILL_BLOCKED;
    }

    private static boolean isSameFluidSource(Level level, BlockPos pos, Fluid fluid) {
        FluidState fs = level.getFluidState(pos);
        return !fs.isEmpty() && fs.getType().isSame(fluid) && fs.isSource();
    }

    private static int countSourceNeighbors(
        Level level, BlockPos pos, Fluid fluid, BlockPos.MutableBlockPos neighborCursor
    ) {
        int count = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            neighborCursor.set(pos.getX() + d.getStepX(), pos.getY(), pos.getZ() + d.getStepZ());
            if (DrainBlockEntity.isSameFluidSource(level, neighborCursor, fluid)) count++;
        }
        return count;
    }

    private void tryDrainUp(Level level, BlockPos pos) {
        if (!this.processFlowCleanup(level)) return;
        FluidStack stored = this.tank.getStack();
        if (!stored.isEmpty() && stored.getAmount() >= DrainBlockEntity.DRAIN_THRESHOLD) {
            this.drainSearch = null;
            return;
        }
        Fluid want = stored.isEmpty() ? null : stored.getFluid();
        SearchResult searchResult = this.findHighestDrainTarget(level, pos, want);
        BlockPos target = searchResult.target();
        if (target == null) return;
        FluidState fs = level.getFluidState(target);
        if (fs.isEmpty() || (want != null && !fs.getType().isSame(want))) {
            this.drainSearch = null;
            return;
        }
        Fluid fluid = fs.getType();
        if (!fs.isSource()) {
            this.startFlowCleanup(level, target, fluid);
            this.processFlowCleanup(level);
            this.drainSearch = null;
            return;
        }
        if (!this.canInsert(fluid, DrainBlockEntity.UNIT)) return;
        this.removeSourceAndQueueFlowCleanup(level, target, fluid);
        this.processFlowCleanup(level);
        this.insert(fluid, DrainBlockEntity.UNIT);
        this.drainSearch = null;
    }

    private void tryGenerateFromInfinite(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        FluidStack stored = this.tank.getStack();
        if (!stored.isEmpty() && stored.getAmount() >= DrainBlockEntity.CAPACITY) return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = pos.relative(d);
            FluidState fs = level.getFluidState(n);
            if (fs.isEmpty() || !fs.isSource() || !(fs.getType() instanceof FlowingFluid flowing)) continue;
            if (!DrainBlockEntity.canFormInfiniteSource(serverLevel, n, flowing)) continue;
            Fluid source = flowing.getSource();
            if (!this.canInsert(source, DrainBlockEntity.UNIT)) continue;
            this.insert(source, DrainBlockEntity.UNIT);
            return;
        }
    }

    private boolean canInsert(Fluid fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            return this.tank.insert(0, FluidResource.of(fluid), amount, tx) == amount;
        }
    }

    private void insert(Fluid fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            this.tank.insert(0, FluidResource.of(fluid), amount, tx);
            tx.commit();
        }
    }

    private static boolean canFormInfiniteSource(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        return DrainBlockEntity.canRegenerateSourceAt(level, pos, fluid);
    }

    @SuppressWarnings("deprecation")
    private static boolean canRegenerateSourceAt(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        int neighbourSources = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos rel = pos.relative(d);
            BlockState relState = level.getBlockState(rel);
            FluidState relFs = relState.getFluidState();
            if (relFs.isSource() && relFs.getType().isSame(fluid)
                && EventHooks.canCreateFluidSource(level, rel, relState)) {
                neighbourSources++;
            }
        }
        if (neighbourSources < 2) return false;
        BlockState below = level.getBlockState(pos.below());
        FluidState belowFs = below.getFluidState();
        return below.isSolid() || (belowFs.isSource() && belowFs.getType().isSame(fluid));
    }

    private SearchResult findHighestDrainTarget(Level level, BlockPos drainPos, @Nullable Fluid want) {
        BlockPos start = drainPos.above();
        FluidState startFs = level.getFluidState(start);
        if (startFs.isEmpty() || (want != null && !startFs.getType().isSame(want))) {
            this.drainSearch = null;
            return SearchResult.EXHAUSTED;
        }
        Fluid fluid = want != null ? want : startFs.getType();
        int maxY = level.getMaxY();
        int topY = start.getY();
        while (topY < maxY) {
            FluidState fs = level.getFluidState(new BlockPos(drainPos.getX(), topY + 1, drainPos.getZ()));
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) break;
            topY++;
        }
        if (this.drainSearch == null
            || !this.drainSearch.matches(drainPos, fluid, start.getY(), topY, level.getGameTime())) {
            this.drainSearch = new DrainSearch(
                drainPos,
                fluid,
                start.getY(),
                topY,
                level.getGameTime() / DrainBlockEntity.INTERVAL
            );
        }
        return this.drainSearch.advance(level);
    }

    private void removeSourceAndQueueFlowCleanup(Level level, BlockPos target, Fluid fluid) {
        DrainBlockEntity.removeFluidSilently(level, target);
        this.prepareFlowCleanup(fluid);
        for (Direction direction : Direction.values()) {
            this.enqueueFlowing(level, target.relative(direction), fluid);
        }
    }

    private void startFlowCleanup(Level level, BlockPos target, Fluid fluid) {
        this.prepareFlowCleanup(fluid);
        this.enqueueFlowing(level, target, fluid);
    }

    private static void removeFluidSilently(Level level, BlockPos target) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(target, Blocks.AIR.defaultBlockState(), flags);
    }

    private void prepareFlowCleanup(Fluid fluid) {
        if (this.flowCleanupFluid != null && !this.flowCleanupFluid.isSame(fluid)) {
            this.clearFlowCleanup();
        }
        this.flowCleanupFluid = fluid;
    }

    private void enqueueFlowing(Level level, BlockPos pos, Fluid fluid) {
        long packedPos = pos.asLong();
        if (!this.flowCleanupVisited.add(packedPos)) return;
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty() && !fluidState.isSource() && fluidState.getType().isSame(fluid)) {
            this.flowCleanupQueue.enqueue(packedPos);
        }
    }

    private boolean processFlowCleanup(Level level) {
        if (this.flowCleanupQueue.isEmpty() || this.flowCleanupFluid == null) {
            this.clearFlowCleanup();
            return true;
        }
        Fluid fluid = this.flowCleanupFluid;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int processed = 0;
        while (!this.flowCleanupQueue.isEmpty() && processed < DrainBlockEntity.MAX_NODES) {
            long current = this.flowCleanupQueue.dequeueLong();
            processed++;
            cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
            FluidState fluidState = level.getFluidState(cursor);
            if (fluidState.isEmpty() || fluidState.isSource() || !fluidState.getType().isSame(fluid)) continue;
            DrainBlockEntity.removeFluidSilently(level, cursor);
            for (Direction direction : Direction.values()) {
                cursor.move(direction);
                this.enqueueFlowing(level, cursor, fluid);
                cursor.move(direction.getOpposite());
            }
        }
        if (this.flowCleanupQueue.isEmpty()) {
            this.clearFlowCleanup();
            return true;
        }
        return false;
    }

    private void clearFlowCleanup() {
        this.flowCleanupQueue.clear();
        this.flowCleanupVisited.clear();
        this.flowCleanupFluid = null;
    }

    private static boolean isPreferredHorizontalTie(
        long candidateDx, long candidateDz, long bestDx, long bestDz, long selectionPhase
    ) {
        long candidatePrimary;
        long candidateSecondary;
        long bestPrimary;
        long bestSecondary;
        switch (Math.floorMod(selectionPhase, 4)) {
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
            || candidatePrimary == bestPrimary && candidateSecondary > bestSecondary;
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

    private static final class FillSearch {
        private final long drainPos;
        private final Fluid fluid;
        private final int bottomY;
        private final int topY;
        private int currentY;
        private int filledTargets;
        private long exhaustedAt = Long.MIN_VALUE;
        private @Nullable FillLayerSearch layerSearch;

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
                && (this.exhaustedAt == Long.MIN_VALUE || gameTime - this.exhaustedAt < DrainBlockEntity.EXHAUSTED_SEARCH_TTL);
        }

        private SearchResult advance(Level level) {
            while (this.currentY <= this.topY) {
                if (this.layerSearch == null) {
                    long entry = BlockPos.asLong(
                        BlockPos.getX(this.drainPos),
                        this.currentY,
                        BlockPos.getZ(this.drainPos)
                    );
                    this.layerSearch = new FillLayerSearch(this.drainPos, entry, this.fluid);
                }
                SearchResult result = this.layerSearch.advance(level, level.getGameTime() / DrainBlockEntity.INTERVAL);
                if (result.pending() || result.target() != null) {
                    this.exhaustedAt = Long.MIN_VALUE;
                    return result;
                }
                this.currentY++;
                this.layerSearch = null;
            }
            if (this.exhaustedAt == Long.MIN_VALUE) {
                this.exhaustedAt = level.getGameTime();
            }
            return SearchResult.EXHAUSTED;
        }

        private boolean isTargetStillReachable(Level level, long target) {
            return this.layerSearch != null && this.layerSearch.isTargetStillReachable(level, target);
        }

        private boolean acceptFilled(long target) {
            if (this.layerSearch == null) return true;
            this.layerSearch.acceptFilled(target);
            this.exhaustedAt = Long.MIN_VALUE;
            return ++this.filledTargets >= DrainBlockEntity.FILL_SEARCH_REBUILD_INTERVAL;
        }
    }

    private static final class FillLayerSearch {
        private final long drainPos;
        private final long entry;
        private final Fluid fluid;
        private final LongOpenHashSet discovered = new LongOpenHashSet(DrainBlockEntity.MAX_NODES);
        private final Long2LongOpenHashMap predecessors = new Long2LongOpenHashMap(DrainBlockEntity.MAX_NODES);
        private final LongOpenHashSet candidates = new LongOpenHashSet();
        private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue(DrainBlockEntity.MAX_NODES);

        private FillLayerSearch(long drainPos, long entry, Fluid fluid) {
            this.drainPos = drainPos;
            this.entry = entry;
            this.fluid = fluid;
            this.discovered.add(entry);
            this.queue.enqueue(entry);
        }

        private SearchResult advance(Level level, long selectionPhase) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int processed = 0;
            while (true) {
                while (!this.queue.isEmpty() && processed < DrainBlockEntity.MAX_NODES) {
                    long current = this.queue.dequeueLong();
                    processed++;
                    cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                    int fillType = DrainBlockEntity.classifyForFill(level, cursor, this.fluid);
                    if (fillType == DrainBlockEntity.FILL_TARGET) {
                        this.candidates.add(current);
                    } else if (fillType == DrainBlockEntity.FILL_SOURCE) {
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                            this.discover(level, DrainBlockEntity.offset(cursor, direction), current, cursor);
                            cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                        }
                        this.discover(
                            level,
                            BlockPos.asLong(cursor.getX(), cursor.getY() - 1, cursor.getZ()),
                            current,
                            cursor
                        );
                    }
                }
                if (!this.queue.isEmpty()) return SearchResult.PENDING;

                long best = 0;
                int bestY = Integer.MAX_VALUE;
                long bestDist = Long.MAX_VALUE;
                long bestDx = 0;
                long bestDz = 0;
                boolean found = false;
                boolean discoveredSource = false;
                LongIterator iterator = this.candidates.iterator();
                while (iterator.hasNext()) {
                    long candidate = iterator.nextLong();
                    cursor.set(BlockPos.getX(candidate), BlockPos.getY(candidate), BlockPos.getZ(candidate));
                    int fillType = DrainBlockEntity.classifyForFill(level, cursor, this.fluid);
                    if (fillType == DrainBlockEntity.FILL_BLOCKED) {
                        iterator.remove();
                        continue;
                    }
                    if (fillType == DrainBlockEntity.FILL_SOURCE) {
                        iterator.remove();
                        this.queue.enqueue(candidate);
                        discoveredSource = true;
                        continue;
                    }
                    int y = cursor.getY();
                    long dx = (long) cursor.getX() - BlockPos.getX(this.drainPos);
                    long dz = (long) cursor.getZ() - BlockPos.getZ(this.drainPos);
                    long dist = dx * dx + dz * dz;
                    if (!found
                        || y < bestY
                        || y == bestY
                        && (dist < bestDist
                            || dist == bestDist
                               && DrainBlockEntity.isPreferredHorizontalTie(dx, dz, bestDx, bestDz, selectionPhase))) {
                        best = candidate;
                        bestY = y;
                        bestDist = dist;
                        bestDx = dx;
                        bestDz = dz;
                        found = true;
                    }
                }
                if (discoveredSource) {
                    if (processed >= DrainBlockEntity.MAX_NODES) return SearchResult.PENDING;
                    continue;
                }
                return found ? SearchResult.found(best) : SearchResult.EXHAUSTED;
            }
        }

        private void discover(Level level, long pos, long predecessor, BlockPos.MutableBlockPos cursor) {
            if (!this.discovered.add(pos)) return;
            this.predecessors.put(pos, predecessor);
            cursor.set(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
            int fillType = DrainBlockEntity.classifyForFill(level, cursor, this.fluid);
            if (fillType == DrainBlockEntity.FILL_SOURCE) {
                this.queue.enqueue(pos);
            } else if (fillType == DrainBlockEntity.FILL_TARGET) {
                this.candidates.add(pos);
            }
        }

        /**
         * 缓存候选可能因洞口被封而与入口断开。沿首次发现时的前驱链复核当前源方块，
         * 任一路径节点失效就让上层重建搜索；重建时仍存在的其他通路会被重新发现。
         */
        private boolean isTargetStillReachable(Level level, long target) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            cursor.set(BlockPos.getX(target), BlockPos.getY(target), BlockPos.getZ(target));
            if (!this.discovered.contains(target)
                || DrainBlockEntity.classifyForFill(level, cursor, this.fluid) != DrainBlockEntity.FILL_TARGET) {
                return false;
            }

            long current = target;
            int remaining = this.discovered.size();
            while (current != this.entry && remaining-- > 0) {
                if (!this.predecessors.containsKey(current)) return false;
                current = this.predecessors.get(current);
                cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                if (DrainBlockEntity.classifyForFill(level, cursor, this.fluid) != DrainBlockEntity.FILL_SOURCE) return false;
            }
            return current == this.entry;
        }

        private void acceptFilled(long target) {
            this.candidates.remove(target);
            this.discovered.add(target);
            this.queue.enqueue(target);
        }
    }

    private static final class DrainSearch {
        private final long drainPos;
        private final Fluid fluid;
        private final int bottomY;
        private final int topY;
        private final long selectionPhase;
        private int currentY;
        private long exhaustedAt = Long.MIN_VALUE;
        private @Nullable DrainLayerSearch layerSearch;

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
                && (this.exhaustedAt == Long.MIN_VALUE || gameTime - this.exhaustedAt < DrainBlockEntity.EXHAUSTED_SEARCH_TTL);
        }

        private SearchResult advance(Level level) {
            while (this.currentY >= this.bottomY) {
                if (this.layerSearch == null) {
                    long entry = BlockPos.asLong(
                        BlockPos.getX(this.drainPos),
                        this.currentY,
                        BlockPos.getZ(this.drainPos)
                    );
                    this.layerSearch = new DrainLayerSearch(
                        this.drainPos,
                        entry,
                        this.fluid,
                        this.selectionPhase
                    );
                }
                SearchResult result = this.layerSearch.advance(level);
                if (result.pending() || result.target() != null) {
                    this.exhaustedAt = Long.MIN_VALUE;
                    return result;
                }
                this.currentY--;
                this.layerSearch = null;
            }
            if (this.exhaustedAt == Long.MIN_VALUE) {
                this.exhaustedAt = level.getGameTime();
            }
            return SearchResult.EXHAUSTED;
        }
    }

    private static final class DrainLayerSearch {
        private final long drainPos;
        private final Fluid fluid;
        private final long selectionPhase;
        private final LongOpenHashSet discovered = new LongOpenHashSet(DrainBlockEntity.MAX_NODES);
        private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue(DrainBlockEntity.MAX_NODES);
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
            this.discovered.add(entry);
            this.queue.enqueue(entry);
        }

        private SearchResult advance(Level level) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos neighborCursor = new BlockPos.MutableBlockPos();
            int processed = 0;
            while (!this.queue.isEmpty() && processed < DrainBlockEntity.MAX_NODES) {
                long current = this.queue.dequeueLong();
                processed++;
                cursor.set(BlockPos.getX(current), BlockPos.getY(current), BlockPos.getZ(current));
                FluidState fluidState = level.getFluidState(cursor);
                if (fluidState.isEmpty() || !fluidState.getType().isSame(this.fluid)) continue;
                if (fluidState.isSource()) {
                    this.considerSource(level, current, cursor, neighborCursor);
                } else {
                    this.considerFlowing(current, cursor);
                }
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.discoverFluid(level, DrainBlockEntity.offset(cursor, direction), neighborCursor);
                }
                this.discoverFluid(
                    level,
                    BlockPos.asLong(cursor.getX(), cursor.getY() + 1, cursor.getZ()),
                    neighborCursor
                );
            }
            if (!this.queue.isEmpty()) return SearchResult.PENDING;
            if (this.found) return SearchResult.found(this.best);
            return this.foundFlowing ? SearchResult.found(this.bestFlowing) : SearchResult.EXHAUSTED;
        }

        private void discoverFluid(Level level, long pos, BlockPos.MutableBlockPos cursor) {
            if (!this.discovered.add(pos)) return;
            cursor.set(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
            FluidState fluidState = level.getFluidState(cursor);
            if (!fluidState.isEmpty() && fluidState.getType().isSame(this.fluid)) {
                this.queue.enqueue(pos);
            }
        }

        private void considerSource(
            Level level,
            long source,
            BlockPos sourcePos,
            BlockPos.MutableBlockPos neighborCursor
        ) {
            int neighbors = DrainBlockEntity.countSourceNeighbors(level, sourcePos, this.fluid, neighborCursor);
            long dx = (long) sourcePos.getX() - BlockPos.getX(this.drainPos);
            long dz = (long) sourcePos.getZ() - BlockPos.getZ(this.drainPos);
            long dist = dx * dx + dz * dz;
            int y = sourcePos.getY();
            if (!this.found
                || neighbors < this.bestNeighbors
                || neighbors == this.bestNeighbors
                && (dist > this.bestDist
                    || dist == this.bestDist
                    && (DrainBlockEntity.isPreferredHorizontalTie(
                            dx,
                            dz,
                            this.bestDx,
                            this.bestDz,
                            this.selectionPhase
                        )
                        || dx == this.bestDx && dz == this.bestDz && y > this.bestY))) {
                this.best = source;
                this.bestY = y;
                this.bestDist = dist;
                this.bestDx = dx;
                this.bestDz = dz;
                this.bestNeighbors = neighbors;
                this.found = true;
            }
        }

        private void considerFlowing(long flowing, BlockPos flowingPos) {
            long dx = (long) flowingPos.getX() - BlockPos.getX(this.drainPos);
            long dz = (long) flowingPos.getZ() - BlockPos.getZ(this.drainPos);
            long dist = dx * dx + dz * dz;
            int y = flowingPos.getY();
            if (!this.foundFlowing
                || dist > this.bestFlowingDist
                || dist == this.bestFlowingDist
                && (DrainBlockEntity.isPreferredHorizontalTie(
                        dx,
                        dz,
                        this.bestFlowingDx,
                        this.bestFlowingDz,
                        this.selectionPhase
                    )
                    || dx == this.bestFlowingDx && dz == this.bestFlowingDz && y > this.bestFlowingY)) {
                this.bestFlowing = flowing;
                this.bestFlowingY = y;
                this.bestFlowingDist = dist;
                this.bestFlowingDx = dx;
                this.bestFlowingDz = dz;
                this.foundFlowing = true;
            }
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output);
        output.putInt("ColumnBottomY", this.columnBottomY);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input);
        this.columnBottomY = input.getIntOr("ColumnBottomY", Integer.MIN_VALUE);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
