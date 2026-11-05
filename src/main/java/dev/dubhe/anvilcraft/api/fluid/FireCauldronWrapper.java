package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.block.cauldron.FireCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/**
 * 燃烧的炼药锅的流体能力。<br>
 * NeoForge 的炼药锅内容映射同一种流体只允许登记一个方块，燃烧的炼药锅无法与原油炼药锅同时登记，
 * 因此像牛奶炼药锅一样单独暴露能力，层数读写复用 {@link CauldronUtil}。
 */
public class FireCauldronWrapper extends SnapshotJournal<BlockState> implements ResourceHandler<FluidResource> {
    private static final FluidStack OIL = new FluidStack(ModFluids.OIL.get(), 1);
    private static final int AMOUNT_PER_LEVEL = FluidType.BUCKET_VOLUME / 4;

    private final Level level;
    private final BlockPos pos;

    public FireCauldronWrapper(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, this.size());
        return FluidResource.of(ModFluids.OIL.get());
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, this.size());
        return (long) CauldronUtil.currentLevel(this.level.getBlockState(this.pos)) * AMOUNT_PER_LEVEL;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        return resource.matches(OIL);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (this.level.isClientSide() || !this.isValid(index, resource)) {
            return 0;
        }
        BlockState state = this.level.getBlockState(this.pos);
        if (!(state.getBlock() instanceof FireCauldronBlock)) {
            return 0;
        }
        int currentLevel = CauldronUtil.currentLevel(state);
        int targetLevel = Math.min(
            CauldronUtil.maxLevel(ModBlocks.FIRE_CAULDRON.get()),
            currentLevel + amount / AMOUNT_PER_LEVEL
        );
        if (targetLevel <= currentLevel) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.setLevel(targetLevel);
        return (targetLevel - currentLevel) * AMOUNT_PER_LEVEL;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (this.level.isClientSide() || !this.isValid(index, resource)) {
            return 0;
        }
        BlockState state = this.level.getBlockState(this.pos);
        if (!(state.getBlock() instanceof FireCauldronBlock)) {
            return 0;
        }
        int currentLevel = CauldronUtil.currentLevel(state);
        int drainedLevels = Math.min(currentLevel, amount / AMOUNT_PER_LEVEL);
        if (drainedLevels <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.setLevel(currentLevel - drainedLevels);
        return drainedLevels * AMOUNT_PER_LEVEL;
    }

    private void setLevel(int level) {
        this.level.setBlock(
            this.pos,
            CauldronUtil.getStateFromContentAndLevel(ModBlocks.FIRE_CAULDRON.get(), level),
            0
        );
    }

    @Override
    protected BlockState createSnapshot() {
        return this.level.getBlockState(this.pos);
    }

    @Override
    protected void revertToSnapshot(BlockState snapshot) {
        this.level.setBlock(this.pos, snapshot, 0);
    }

    @Override
    protected void onRootCommit(BlockState originalState) {
        // 事务提交后才通知邻接更新，回滚时不会向周围方块广播中间状态
        this.level.setBlockAndUpdate(this.pos, this.level.getBlockState(this.pos));
    }
}
