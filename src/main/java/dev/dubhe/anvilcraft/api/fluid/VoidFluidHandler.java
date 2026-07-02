package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 只进不出的虚空流体容器。
 *
 * <p>接受任意流体的填充并直接丢弃（相当于创造流体储罐的只输入版本），
 * 永不存储、永不输出。用于门格海绵：通过管道输入的流体会被无限吸收并消失。
 */
public final class VoidFluidHandler implements ResourceHandler<FluidResource> {
    public static final VoidFluidHandler INSTANCE = new VoidFluidHandler();

    private VoidFluidHandler() {
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        // 无限吸收：接受全部输入并丢弃，不占用事务快照（状态不变）。
        return amount;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }
}
