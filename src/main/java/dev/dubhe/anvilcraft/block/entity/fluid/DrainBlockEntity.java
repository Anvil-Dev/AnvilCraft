package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class DrainBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder {
    public static final int CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    private static final int UNIT = FluidType.BUCKET_VOLUME;
    private static final int INTERVAL = 5;
    private static final int FILL_THRESHOLD = FluidType.BUCKET_VOLUME;
    private static final int DRAIN_THRESHOLD = 3 * FluidType.BUCKET_VOLUME;
    private static final int MAX_NODES = 2048;
    private final FluidStackResourceHandler tank = new FluidStackResourceHandler(CAPACITY) {
        @Override
        protected void onContentChanged(FluidStack original) {
            DrainBlockEntity.this.setChanged();
            DrainBlockEntity.this.sendUpdate();
        }
    };

    private int columnBottomY = Integer.MIN_VALUE;

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
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DrainBlockEntity be) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % INTERVAL != 0) return;
        boolean filled = be.tryFillDown(level, pos);
        if (!filled) be.clearColumn();
        be.tryDrainUp(level, pos);
        be.tryGenerateFromInfinite(level, pos);
    }

    private void clearColumn() {
        if (this.columnBottomY != Integer.MIN_VALUE) {
            this.columnBottomY = Integer.MIN_VALUE;
            this.sendUpdate();
        }
    }

    private boolean tryFillDown(Level level, BlockPos pos) {
        FluidStack stored = this.tank.getStack();
        if (stored.getAmount() <= FILL_THRESHOLD) return false;
        Fluid fluid = stored.getFluid();
        if (level.dimensionType().attributes().contains(EnvironmentAttributes.WATER_EVAPORATES) && fluid.isSame(Fluids.WATER)) return false;
        BlockState source = fluid.defaultFluidState().createLegacyBlock();
        if (source.isAir()) return false;

        BlockPos target = this.findFillTarget(level, pos, fluid);
        if (target == null) {
            this.clearColumn();
            return false;
        }
        level.setBlock(target, source, Block.UPDATE_ALL);
        try (Transaction tx = Transaction.openRoot()) {
            this.tank.extract(0, FluidResource.of(stored), UNIT, tx);
            tx.commit();
        }
        if (target.getY() != this.columnBottomY) {
            this.columnBottomY = target.getY();
            this.sendUpdate();
        }
        return true;
    }

    @Nullable
    private BlockPos findFillTarget(Level level, BlockPos drainPos, Fluid fluid) {
        BlockPos start = drainPos.below();
        if (!isPassableForFill(level, start, fluid)) return null;
        int minY = level.getMinY();
        int bottomY = start.getY();
        while (bottomY > minY
               && isPassableForFill(level, new BlockPos(drainPos.getX(), bottomY - 1, drainPos.getZ()), fluid)) {
            bottomY--;
        }
        for (int y = bottomY; y <= start.getY(); y++) {
            BlockPos target = this.findEmptyWithDownward(level, drainPos, new BlockPos(drainPos.getX(), y, drainPos.getZ()), fluid);
            if (target != null) return target;
        }
        return null;
    }

    private static boolean isPassableForFill(Level level, BlockPos pos, Fluid fluid) {
        return isPlaceableEmpty(level, pos, fluid) || isSameFluidSource(level, pos, fluid);
    }

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
            if (!empty && !sameFluid) continue;
            if (empty) empties.add(cur);
            for (Direction d : Direction.Plane.HORIZONTAL) enqueue(queue, visited, cur.relative(d));
            this.checkVertical(level, cur.below(), fluid, queue, visited, empties);
        }
        return pickNearest(empties, drainPos);
    }

    private void checkVertical(
        Level level, BlockPos neighbor, Fluid fluid,
        ArrayDeque<BlockPos> queue, Set<BlockPos> visited, List<BlockPos> empties
    ) {
        if (visited.contains(neighbor.immutable())) return;
        if (isPlaceableEmpty(level, neighbor, fluid)) {
            visited.add(neighbor.immutable());
            empties.add(neighbor);
        } else if (isSameFluidSource(level, neighbor, fluid)) {
            enqueue(queue, visited, neighbor);
        }
    }

    private static void enqueue(ArrayDeque<BlockPos> queue, Set<BlockPos> visited, BlockPos p) {
        if (visited.add(p.immutable())) queue.add(p.immutable());
    }

    private static boolean isPlaceableEmpty(Level level, BlockPos pos, Fluid fluid) {
        BlockState state = level.getBlockState(pos);
        FluidState fs = state.getFluidState();
        if (!fs.isEmpty()) return fs.getType().isSame(fluid) && !fs.isSource();
        return state.isAir() || state.canBeReplaced();
    }

    private static boolean isSameFluidSource(Level level, BlockPos pos, Fluid fluid) {
        FluidState fs = level.getFluidState(pos);
        return !fs.isEmpty() && fs.getType().isSame(fluid) && fs.isSource();
    }

    private static int countSourceNeighbors(Level level, BlockPos pos, Fluid fluid) {
        int count = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (isSameFluidSource(level, pos.relative(d), fluid)) count++;
        }
        return count;
    }

    @Nullable
    private static BlockPos pickNearest(List<BlockPos> empties, BlockPos drainPos) {
        if (empties.isEmpty()) return null;
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

    private void tryDrainUp(Level level, BlockPos pos) {
        FluidStack stored = this.tank.getStack();
        if (!stored.isEmpty() && stored.getAmount() >= DRAIN_THRESHOLD) return;
        Fluid want = stored.isEmpty() ? null : stored.getFluid();
        BlockPos target = this.findHighestNearestFluid(level, pos, want);
        if (target == null) return;
        FluidState fs = level.getFluidState(target);
        Fluid fluid = fs.getType();
        FluidResource resource = FluidResource.of(fluid);
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = this.tank.insert(0, resource, UNIT, tx);
            if (inserted < UNIT) return;
            tx.commit();
        }
        removeSourceWithSuppression(level, target, fluid);
    }

    private void tryGenerateFromInfinite(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        FluidStack stored = this.tank.getStack();
        if (!stored.isEmpty() && stored.getAmount() >= CAPACITY) return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = pos.relative(d);
            FluidState fs = level.getFluidState(n);
            if (fs.isEmpty() || !fs.isSource() || !(fs.getType() instanceof FlowingFluid flowing)) continue;
            if (!canFormInfiniteSource(serverLevel, n, flowing)) continue;
            FluidResource resource = FluidResource.of(flowing.getSource());
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = this.tank.insert(0, resource, UNIT, tx);
                if (inserted < UNIT) continue;
                tx.commit();
                return;
            }
        }
    }

    private static boolean canFormInfiniteSource(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
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

    @Nullable
    private BlockPos findHighestNearestFluid(Level level, BlockPos drainPos, @Nullable Fluid want) {
        BlockPos start = drainPos.above();
        FluidState startFs = level.getFluidState(start);
        if (startFs.isEmpty() || (want != null && !startFs.getType().isSame(want))) return null;
        Fluid fluid = want != null ? want : startFs.getType();
        int maxY = level.getMaxY();
        int topY = start.getY();
        while (topY < maxY) {
            FluidState fs = level.getFluidState(new BlockPos(drainPos.getX(), topY + 1, drainPos.getZ()));
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) break;
            topY++;
        }
        for (int y = topY; y >= start.getY(); y--) {
            BlockPos target = this.findSourceInLayer(level, drainPos, new BlockPos(drainPos.getX(), y, drainPos.getZ()), fluid);
            if (target != null) return target;
        }
        return null;
    }

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
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) continue;
            if (fs.isSource()) sources.add(cur);
            for (Direction d : Direction.Plane.HORIZONTAL) enqueue(queue, visited, cur.relative(d));
            BlockPos above = cur.above();
            if (!visited.contains(above.immutable())) {
                FluidState aboveFs = level.getFluidState(above);
                if (!aboveFs.isEmpty() && aboveFs.getType().isSame(fluid)) enqueue(queue, visited, above);
            }
        }
        if (sources.isEmpty()) return null;
        BlockPos best = null;
        int bestNeighbors = Integer.MAX_VALUE;
        long bestDist = Long.MIN_VALUE;
        for (BlockPos p : sources) {
            int neighbors = countSourceNeighbors(level, p, fluid);
            long dx = p.getX() - drainPos.getX();
            long dz = p.getZ() - drainPos.getZ();
            long dist = dx * dx + dz * dz;
            boolean better = neighbors != bestNeighbors ? neighbors < bestNeighbors : dist > bestDist;
            if (better) {
                bestNeighbors = neighbors;
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private static void removeSourceWithSuppression(Level level, BlockPos target, Fluid fluid) {
        boolean shouldSuppress = false;
        if (level instanceof ServerLevel serverLevel && fluid instanceof FlowingFluid flowing) {
            shouldSuppress = canFormInfiniteSource(serverLevel, target, flowing);
        }
        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (!shouldSuppress) return;
        FlowingFluid flowing = (FlowingFluid) fluid;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = target.relative(d);
            if (isSameFluidSource(level, n, fluid)) {
                BlockState flowingState = flowing.getFlowing(7, false).createLegacyBlock();
                level.setBlock(n, flowingState, Block.UPDATE_ALL);
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
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("ColumnBottomY", this.columnBottomY);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
