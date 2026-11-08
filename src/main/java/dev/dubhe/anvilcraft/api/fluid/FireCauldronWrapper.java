package dev.dubhe.anvilcraft.api.fluid;

import com.google.common.collect.MapMaker;
import dev.dubhe.anvilcraft.block.cauldron.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Map;
import java.util.Objects;

/** 燃烧油锅与未点燃油锅共用流体，因此单独提供能力以保留燃烧状态。 */
public final class FireCauldronWrapper extends SnapshotJournal<BlockState> implements ResourceHandler<FluidResource> {
    private static final int AMOUNT_PER_LEVEL = FluidType.BUCKET_VOLUME / 4;
    private static final Map<Location, FireCauldronWrapper> WRAPPERS = new MapMaker().concurrencyLevel(1).weakValues().makeMap();
    private final Location location;

    private record Location(Level level, BlockPos pos) {
        BlockState state() {
            return this.level.getBlockState(this.pos);
        }
    }

    private FireCauldronWrapper(Location location) {
        this.location = location;
    }

    public static FireCauldronWrapper get(Level level, BlockPos pos) {
        return WRAPPERS.computeIfAbsent(new Location(level, pos.immutable()), FireCauldronWrapper::new);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return this.getAmountAsLong(index) > 0 ? FluidResource.of(ModFluids.OIL.get()) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, this.size());
        BlockState state = this.location.state();
        return state.is(ModBlocks.FIRE_CAULDRON) || state.is(ModBlocks.OIL_CAULDRON)
            ? state.getValue(Layered4LevelCauldronBlock.LEVEL) * AMOUNT_PER_LEVEL : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        return resource.is(ModFluids.OIL.get()) && resource.isComponentsPatchEmpty();
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        BlockState state = this.location.state();
        if (!this.isValid(index, resource) || !isOilCauldron(state)) {
            return 0;
        }
        int current = this.getAmountAsInt(index);
        int inserted = Math.min(amount / AMOUNT_PER_LEVEL, (FluidType.BUCKET_VOLUME - current) / AMOUNT_PER_LEVEL) * AMOUNT_PER_LEVEL;
        if (inserted > 0) {
            this.setAmount(state, current + inserted, transaction);
        }
        return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!this.isValid(index, resource)) {
            return 0;
        }
        int extracted = Math.min(amount / AMOUNT_PER_LEVEL, this.getAmountAsInt(index) / AMOUNT_PER_LEVEL) * AMOUNT_PER_LEVEL;
        if (extracted > 0) {
            this.setAmount(this.location.state(), this.getAmountAsInt(index) - extracted, transaction);
        }
        return extracted;
    }

    private static boolean isOilCauldron(BlockState state) {
        return state.is(ModBlocks.FIRE_CAULDRON) || state.is(ModBlocks.OIL_CAULDRON) || state.is(Blocks.CAULDRON);
    }

    private void setAmount(BlockState state, int amount, TransactionContext transaction) {
        this.updateSnapshots(transaction);
        BlockState result = amount == 0 ? Blocks.CAULDRON.defaultBlockState()
            : (state.is(Blocks.CAULDRON) ? ModBlocks.OIL_CAULDRON.getDefaultState() : state)
                .setValue(Layered4LevelCauldronBlock.LEVEL, amount / AMOUNT_PER_LEVEL);
        this.location.level.setBlock(this.location.pos, result, 0);
    }

    @Override
    protected BlockState createSnapshot() {
        return this.location.state();
    }

    @Override
    protected void revertToSnapshot(BlockState snapshot) {
        this.location.level.setBlock(this.location.pos, snapshot, 0);
    }

    @Override
    protected void onRootCommit(BlockState originalState) {
        BlockState state = this.location.state();
        if (state == originalState || !isOilCauldron(state)) {
            return;
        }
        // 先恢复原状态，避免同状态写入跳过管道邻居与客户端更新。
        this.location.level.setBlock(this.location.pos, originalState, 0);
        this.location.level.setBlockAndUpdate(this.location.pos, state);
    }
}
