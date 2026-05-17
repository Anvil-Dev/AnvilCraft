package dev.dubhe.anvilcraft.api.itemhandler;

import com.google.common.collect.MapMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Map;
import java.util.function.Predicate;

public class SolidCauldronExtractor extends SnapshotJournal<BlockState> implements ResourceHandler<ItemResource> {
    /**
     * To make sure multiple accesses to the same cauldron return the same wrapper,
     * we maintain a {@code (Level, BlockPos) -> Wrapper} cache.
     */
    private record WrapperLocation(Level level, BlockPos pos) {
        public BlockState getBlockState() {
            return this.level.getBlockState(this.pos);
        }
    }

    /**
     * Wrapper map, similar to {@link VanillaContainerWrapper#wrappers}.
     * We need the cauldron wrapper to hold a strong reference to the wrapper location to avoid the weak keys being cleared too early.
     */
    private static final Map<SolidCauldronExtractor.WrapperLocation, SolidCauldronExtractor> WRAPPERS = new MapMaker()
        .concurrencyLevel(1)
        .weakKeys()
        .weakValues()
        .makeMap();

    public static SolidCauldronExtractor get(Level level, BlockPos pos, Predicate<BlockState> validCauldron) {
        SolidCauldronExtractor.WrapperLocation location = new SolidCauldronExtractor.WrapperLocation(level, pos.immutable());
        return WRAPPERS.computeIfAbsent(location, location1 -> new SolidCauldronExtractor(validCauldron, location1));
    }

    private final Predicate<BlockState> validCauldron;
    private final SolidCauldronExtractor.WrapperLocation location;

    private SolidCauldronExtractor(Predicate<BlockState> validCauldron, SolidCauldronExtractor.WrapperLocation location) {
        this.validCauldron = validCauldron;
        this.location = location;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        if (index != 0) return ItemResource.EMPTY;
        BlockState state = this.location.getBlockState();
        if (!this.validCauldron.test(state)) return ItemResource.EMPTY;
        return ItemResource.of(Items.HONEY_BLOCK);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index != 0) return 0;
        BlockState state = this.location.getBlockState();
        if (!this.validCauldron.test(state)) return 0;
        return 1;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return false;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0) return 0;
        if (index != 0) return 0;
        ItemResource resourceIn = this.getResource(index);
        if (resourceIn.isEmpty()) return 0;
        this.setLevel(transaction);
        return 1;

    }

    private void setLevel(TransactionContext transaction) {
        this.updateSnapshots(transaction);
        this.location.level.setBlock(this.location.pos, Blocks.CAULDRON.defaultBlockState(), 0);
    }

    @Override
    protected BlockState createSnapshot() {
        return this.location.getBlockState();
    }

    @Override
    protected void revertToSnapshot(BlockState snapshot) {
        this.location.level.setBlock(this.location.pos, snapshot, 0);
    }

    @Override
    protected void onRootCommit(BlockState originalState) {
        BlockState state = this.location.getBlockState();

        // Skip updating if nothing changed or if the cauldron was removed
        if (originalState == state || CauldronFluidContent.getForBlock(state.getBlock()) == null) return;

        // Revert back to the blockstate before any changes happened so that the next
        // call will not short-circuit due to the blockstate not really changing.
        this.location.level.setBlock(this.location.pos, originalState, 0);
        // Now perform the change that will trigger notifications to other blocks/neighbors/clients.
        this.location.level.setBlockAndUpdate(this.location.pos, state);

        // Currently we don't send a BLOCK_CHANGE nor FLUID_PLACE/FLUID_PICKUP game event. This can be reconsidered.
    }
}
