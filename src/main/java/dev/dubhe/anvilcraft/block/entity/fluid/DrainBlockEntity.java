package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.DrainChunkAccess;
import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
    private static final int UNIT = FluidType.BUCKET_VOLUME;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int FILL_THRESHOLD = FluidType.BUCKET_VOLUME;
    private static final int DRAIN_THRESHOLD = 3 * FluidType.BUCKET_VOLUME;
    private static final int MAX_NODES = 2048;
    private static final int FILL_BLOCKED = 0;
    private static final int FILL_TARGET = 1;
    private static final int FILL_SOURCE = 2;
    private static final int FILL_SEARCH_REBUILD_INTERVAL = 256;
    private static final long EXHAUSTED_SEARCH_TTL = 100;
    private final FluidStackResourceHandler tank = new FluidStackResourceHandler(AnvilCraft.CONFIG.machines.drainCapacity) {
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
    @Getter(AccessLevel.NONE)
    private boolean cleaningFlows;
    @Getter(AccessLevel.NONE)
    private DrainChunkAccess flowCleanupChunks = new DrainChunkAccess();

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
        this.drainSearch = null;
        this.clearFlowCleanup();
        super.setRemoved();
    }

    public void sendUpdate() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DrainBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel) || be.isRemoved()) return;
        boolean extract = level.getGameTime() % AnvilCraft.CONFIG.machines.drainInterval == 0;
        if (extract) {
            FillResult fillResult = be.tryFillDown(level, pos);
            if (fillResult == FillResult.NONE) be.clearColumn();
        }
        be.tryDrainUp(serverLevel, pos, extract);
        if (extract) be.tryGenerateFromInfinite(level, pos);
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

    private void tryDrainUp(ServerLevel level, BlockPos pos, boolean extract) {
        int interval = AnvilCraft.CONFIG.machines.drainInterval;
        int budget = (DrainBlockEntity.MAX_NODES + interval - 1) / interval;
        if (this.cleaningFlows) budget = this.processFlowCleanup(level, budget);
        if (budget == 0 || this.cleaningFlows) return;
        FluidStack stored = this.tank.getStack();
        if (!stored.isEmpty() && stored.getAmount() >= DrainBlockEntity.DRAIN_THRESHOLD) {
            this.drainSearch = null;
            return;
        }
        FluidState entry = level.getFluidState(pos.above());
        if (entry.isEmpty() || !stored.isEmpty() && !entry.getType().isSame(stored.getFluid())) {
            this.clearFlowCleanup();
            this.drainSearch = null;
            return;
        }
        Fluid fluid = entry.getType();
        if (this.drainSearch == null || !this.drainSearch.fluid.isSame(fluid) || !this.drainSearch.chunks.isValid(level)) {
            this.drainSearch = new DrainSearch(pos, fluid, level.getGameTime() / AnvilCraft.CONFIG.machines.drainInterval);
        }
        this.drainSearch.chunks.tick(level);
        SearchResult result = this.drainSearch.advance(level, budget);
        BlockPos target = result.target();
        if (!extract || target == null) return;
        BlockState targetState = this.drainSearch.chunks.read(level, target);
        if (targetState == null) return;
        FluidState targetFluid = targetState.getFluidState();
        if (targetFluid.isEmpty() || !targetFluid.getType().isSame(fluid)) {
            this.drainSearch = null;
            return;
        }
        if (!targetFluid.isSource()) {
            this.prepareFlowCleanup(fluid);
            this.enqueueFlowing(target);
            this.cleaningFlows = true;
            this.drainSearch = null;
        } else {
            if (!this.canInsert(fluid, DrainBlockEntity.UNIT)) return;
            if (!DrainBlockEntity.removeFluidSilently(level, target, targetState)) {
                this.drainSearch = null;
                return;
            }
            this.prepareFlowCleanup(fluid);
            for (Direction direction : DrainBlockEntity.DIRECTIONS) this.enqueueFlowing(target.relative(direction));
            this.insert(fluid, DrainBlockEntity.UNIT);
            if (!this.drainSearch.acceptDrained(level, target)) this.drainSearch = null;
            if (target.equals(pos.above())) {
                this.cleaningFlows = true;
                this.drainSearch = null;
            }
        }
    }

    private void tryGenerateFromInfinite(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        FluidStack stored = this.tank.getStack();
        if (!stored.isEmpty() && stored.getAmount() >= AnvilCraft.CONFIG.machines.drainCapacity) return;
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

    private static boolean removeFluidSilently(ServerLevel level, BlockPos target, BlockState state) {
        BlockState replacement = state.hasProperty(BlockStateProperties.WATERLOGGED)
            && state.getValue(BlockStateProperties.WATERLOGGED)
            ? state.setValue(BlockStateProperties.WATERLOGGED, false) : Blocks.AIR.defaultBlockState();
        return level.setBlock(target, replacement, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private void prepareFlowCleanup(Fluid fluid) {
        if (this.flowCleanupFluid != null && !this.flowCleanupFluid.isSame(fluid)) this.clearFlowCleanup();
        this.flowCleanupFluid = fluid;
    }

    private void enqueueFlowing(BlockPos pos) {
        long packedPos = pos.asLong();
        if (this.flowCleanupVisited.add(packedPos)) this.flowCleanupQueue.enqueue(packedPos);
    }

    private int processFlowCleanup(ServerLevel level, int budget) {
        if (this.flowCleanupQueue.isEmpty() || this.flowCleanupFluid == null) {
            this.clearFlowCleanup();
            return budget;
        }
        if (!this.flowCleanupChunks.isValid(level)) this.flowCleanupChunks = new DrainChunkAccess();
        this.flowCleanupChunks.tick(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!this.flowCleanupQueue.isEmpty() && budget > 0) {
            cursor.set(this.flowCleanupQueue.firstLong());
            BlockState state = this.flowCleanupChunks.read(level, cursor);
            if (state == null) return 0;
            this.flowCleanupQueue.dequeueLong();
            budget--;
            FluidState fluidState = state.getFluidState();
            if (fluidState.isEmpty() || fluidState.isSource() || !fluidState.getType().isSame(this.flowCleanupFluid)) continue;
            DrainBlockEntity.removeFluidSilently(level, cursor, state);
            for (Direction direction : DrainBlockEntity.DIRECTIONS) {
                cursor.move(direction);
                this.enqueueFlowing(cursor);
                cursor.move(direction.getOpposite());
            }
        }
        if (this.flowCleanupQueue.isEmpty()) this.clearFlowCleanup();
        return budget;
    }

    private void clearFlowCleanup() {
        if (this.flowCleanupFluid == null && this.flowCleanupQueue.isEmpty() && this.flowCleanupVisited.isEmpty()) return;
        this.flowCleanupQueue.clear();
        this.flowCleanupVisited.clear();
        this.flowCleanupFluid = null;
        this.cleaningFlows = false;
        this.flowCleanupChunks = new DrainChunkAccess();
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
                SearchResult result = this.layerSearch.advance(level, level.getGameTime() / AnvilCraft.CONFIG.machines.drainInterval);
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
        private final long selectionPhase;
        private final DrainChunkAccess chunks = new DrainChunkAccess();
        private final LongOpenHashSet discovered = new LongOpenHashSet(DrainBlockEntity.MAX_NODES);
        private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue(DrainBlockEntity.MAX_NODES);
        private final Long2LongOpenHashMap sourceAncestors = new Long2LongOpenHashMap();
        private final Long2IntOpenHashMap sourceCandidates = new Long2IntOpenHashMap();
        private final Long2IntOpenHashMap sourceChildren = new Long2IntOpenHashMap();
        private @Nullable LongIterator candidateIterator;
        private long best;
        private int bestY = Integer.MIN_VALUE;
        private int bestNeighbors = Integer.MAX_VALUE;
        private long bestDist = Long.MIN_VALUE;
        private long bestDx;
        private long bestDz;
        private boolean found;
        private long flowing;
        private boolean foundFlowing;

        private DrainSearch(BlockPos drainPos, Fluid fluid, long selectionPhase) {
            this.drainPos = drainPos.asLong();
            this.fluid = fluid;
            this.selectionPhase = selectionPhase;
            this.sourceAncestors.defaultReturnValue(Long.MIN_VALUE);
            long entry = drainPos.above().asLong();
            this.discovered.add(entry);
            this.queue.enqueue(entry);
        }

        private SearchResult advance(ServerLevel level, int budget) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
            FluidState[] neighbors = new FluidState[DrainBlockEntity.DIRECTIONS.length];
            while (!this.queue.isEmpty() && budget > 0) {
                budget--;
                long current = this.queue.firstLong();
                cursor.set(current);
                BlockState state = this.chunks.read(level, cursor);
                if (state == null) return SearchResult.PENDING;
                FluidState fluidState = state.getFluidState();
                if (fluidState.isEmpty() || !fluidState.getType().isSame(this.fluid)) {
                    this.queue.dequeueLong();
                    continue;
                }
                boolean pending = false;
                int sourceNeighbors = 0;
                for (Direction direction : DrainBlockEntity.DIRECTIONS) {
                    neighbor.setWithOffset(cursor, direction);
                    BlockState next = neighbor.getY() <= BlockPos.getY(this.drainPos)
                        ? Blocks.AIR.defaultBlockState() : this.chunks.read(level, neighbor);
                    if (next == null) {
                        pending = true;
                        continue;
                    }
                    FluidState nextFluid = next.getFluidState();
                    neighbors[direction.ordinal()] = nextFluid;
                    if (direction.getAxis().isHorizontal() && nextFluid.isSource() && nextFluid.getType().isSame(this.fluid)) {
                        sourceNeighbors++;
                    }
                }
                if (pending) return SearchResult.PENDING;
                this.queue.dequeueLong();
                long ancestor = this.sourceAncestors.get(current);
                if (fluidState.isSource()) {
                    if (ancestor != Long.MIN_VALUE) {
                        this.sourceCandidates.remove(ancestor);
                        this.sourceChildren.addTo(ancestor, 1);
                    }
                    this.sourceCandidates.put(current, sourceNeighbors);
                    ancestor = current;
                } else {
                    this.flowing = current;
                    this.foundFlowing = true;
                }
                for (Direction direction : DrainBlockEntity.DIRECTIONS) {
                    FluidState next = neighbors[direction.ordinal()];
                    if (next.isEmpty() || !next.getType().isSame(this.fluid)) continue;
                    long adjacent = DrainBlockEntity.offset(cursor, direction);
                    if (this.discovered.add(adjacent)) {
                        this.sourceAncestors.put(adjacent, ancestor);
                        this.queue.enqueue(adjacent);
                    }
                }
            }
            if (!this.queue.isEmpty()) return SearchResult.PENDING;
            // Keep source ancestors until their descendants are drained; flowing tails must not hide the last source.
            if (this.candidateIterator == null) this.candidateIterator = this.sourceCandidates.keySet().iterator();
            while (this.candidateIterator.hasNext() && budget-- > 0) {
                long candidate = this.candidateIterator.nextLong();
                cursor.set(candidate);
                this.considerSource(candidate, cursor, this.sourceCandidates.get(candidate));
            }
            if (this.candidateIterator.hasNext()) return SearchResult.PENDING;
            if (this.found) return SearchResult.found(this.best);
            return this.foundFlowing ? SearchResult.found(this.flowing) : SearchResult.EXHAUSTED;
        }

        private boolean acceptDrained(ServerLevel level, BlockPos target) {
            if (!this.chunks.acceptRemoval(target)) return false;
            long removed = target.asLong();
            this.sourceCandidates.remove(removed);
            long ancestor = this.sourceAncestors.get(removed);
            if (ancestor != Long.MIN_VALUE) {
                int remaining = this.sourceChildren.addTo(ancestor, -1) - 1;
                if (remaining == 0 && !this.refreshCandidate(level, BlockPos.of(ancestor))) return false;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = target.relative(direction);
                if (this.sourceCandidates.containsKey(neighbor.asLong()) && !this.refreshCandidate(level, neighbor)) return false;
            }
            this.candidateIterator = null;
            this.found = false;
            return true;
        }

        private boolean refreshCandidate(ServerLevel level, BlockPos pos) {
            BlockState state = this.chunks.read(level, pos);
            if (state == null || !state.getFluidState().isSource() || !state.getFluidState().getType().isSame(this.fluid)) return false;
            int neighbors = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState neighbor = this.chunks.read(level, pos.relative(direction));
                if (neighbor == null) return false;
                FluidState fluidState = neighbor.getFluidState();
                if (fluidState.isSource() && fluidState.getType().isSame(this.fluid)) neighbors++;
            }
            this.sourceCandidates.put(pos.asLong(), neighbors);
            return true;
        }

        private void considerSource(long source, BlockPos pos, int neighbors) {
            long dx = (long) pos.getX() - BlockPos.getX(this.drainPos);
            long dz = (long) pos.getZ() - BlockPos.getZ(this.drainPos);
            long dist = dx * dx + dz * dz;
            int y = pos.getY();
            if (!this.found || y > this.bestY
                || y == this.bestY && (neighbors < this.bestNeighbors
                    || neighbors == this.bestNeighbors && (dist > this.bestDist
                        || dist == this.bestDist && DrainBlockEntity.isPreferredHorizontalTie(
                            dx, dz, this.bestDx, this.bestDz, this.selectionPhase)))) {
                this.best = source;
                this.bestY = y;
                this.bestNeighbors = neighbors;
                this.bestDist = dist;
                this.bestDx = dx;
                this.bestDz = dz;
                this.found = true;
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
