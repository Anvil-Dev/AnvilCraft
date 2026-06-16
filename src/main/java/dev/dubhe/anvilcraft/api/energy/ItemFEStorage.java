package dev.dubhe.anvilcraft.api.energy;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * 基于 DataComponent 的 FE 能量存储实现
 * 使用 {@link ModComponents#STORED_ENERGY} 作为后端存储
 *
 * <p>支持两种模式：
 * <ul>
 *   <li><b>ItemAccess 模式</b>（首选）：通过 {@link ItemAccess#exchange} 进行原子物品交换，
 *       与 NeoForge 通用 FE 系统完全兼容。其他模组的充电器/放电器可以正确追踪能量变更。</li>
 *   <li><b>SnapshotJournal 模式</b>（回退）：当 ItemAccess 上下文不可用时，使用事务快照回滚保证一致性。</li>
 * </ul>
 */
public class ItemFEStorage implements EnergyHandler {
    /// ItemAccess 模式字段
    @Nullable
    private final ItemAccess itemAccess;
    private final Item validItem;
    private final int capacity;
    private final int maxInsert;
    private final int maxExtract;

    /// SnapshotJournal 回退模式字段
    @Nullable
    private final ItemStack stack;
    @Nullable
    private final EnergyJournal journal;

    /**
     * 使用 ItemAccess 创建（推荐）——与通用 FE 系统完全兼容
     */
    public ItemFEStorage(ItemAccess itemAccess, int capacity) {
        this(itemAccess, capacity, capacity, capacity);
    }

    /**
     * 使用 ItemAccess 创建并指定传输限制
     */
    public ItemFEStorage(ItemAccess itemAccess, int capacity, int maxInsert, int maxExtract) {
        TransferPreconditions.checkNonNegative(capacity);
        TransferPreconditions.checkNonNegative(maxInsert);
        TransferPreconditions.checkNonNegative(maxExtract);

        this.itemAccess = itemAccess;
        this.validItem = itemAccess.getResource().getItem();
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;

        this.stack = null;
        this.journal = null;
    }

    /**
     * 使用 ItemStack 创建（回退模式）——保持向后兼容
     *
     * @deprecated 建议使用 {@link #ItemFEStorage(ItemAccess, int)}
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public ItemFEStorage(ItemStack stack, int capacity) {
        TransferPreconditions.checkNonNegative(capacity);

        this.stack = stack;
        this.capacity = capacity;
        this.maxInsert = capacity;
        this.maxExtract = capacity;

        this.itemAccess = null;
        this.validItem = stack.getItem();
        this.journal = new EnergyJournal();
    }

    /**
     * 工厂方法：根据上下文自动选择最佳模式
     */
    @SuppressWarnings("Deprecation")
    public static ItemFEStorage create(ItemStack stack, @Nullable ItemAccess ctx, int capacity) {
        if (ctx != null) {
            return new ItemFEStorage(ctx, capacity);
        }
        return new ItemFEStorage(stack, capacity);
    }

    @Override
    public long getAmountAsLong() {
        if (this.itemAccess != null) {
            return (long) this.itemAccess.getAmount() * (long) this.getEnergyFrom(this.itemAccess.getResource());
        }
        if (this.stack == null) return 0;
        return this.stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    @Override
    public long getCapacityAsLong() {
        if (this.itemAccess != null) {
            ItemResource resource = this.itemAccess.getResource();
            if (!resource.is(this.validItem)) return 0;
            return (long) this.itemAccess.getAmount() * (long) this.capacity;
        }
        return this.capacity;
    }

    /**
     * 从 ItemResource 读取存储的能量值
     */
    private int getEnergyFrom(ItemResource resource) {
        if (!resource.is(this.validItem)) return 0;
        return resource.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);

        if (this.itemAccess != null) {
            return this.insertViaExchange(amount, transaction);
        }
        return this.insertViaJournal(amount, transaction);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);

        if (this.itemAccess != null) {
            return this.extractViaExchange(amount, transaction);
        }
        return this.extractViaJournal(amount, transaction);
    }

    /**
     * 通过 ItemAccess.exchange() 插入能量。
     * 这是 NeoForge 通用 FE 系统的标准模式：创建新的 ItemResource（携带更新后的能量），
     * 然后通过原子交换替换旧物品。其他模组的库存系统可以正确追踪此操作。
     */
    private int insertViaExchange(int amount, TransactionContext transaction) {
        if (this.itemAccess == null) return 0;
        int accessAmount = this.itemAccess.getAmount();
        if (accessAmount == 0) return 0;
        int amountPerItem = Math.min(this.maxInsert, amount / accessAmount);
        if (amountPerItem == 0) return 0;

        ItemResource accessResource = this.itemAccess.getResource();
        if (!accessResource.is(this.validItem)) return 0;
        int currentEnergyPerItem = this.getEnergyFrom(accessResource);

        int insertedPerItem = Math.min(amountPerItem, this.capacity - currentEnergyPerItem);
        if (insertedPerItem > 0) {
            ItemResource filledResource = accessResource.with(
                ModComponents.STORED_ENERGY,
                new StoredEnergy(currentEnergyPerItem + insertedPerItem));
            if (!filledResource.isEmpty()) {
                return insertedPerItem * this.itemAccess.exchange(filledResource, accessAmount, transaction);
            }
        }
        return 0;
    }

    /**
     * 通过 ItemAccess.exchange() 抽取能量。
     */
    private int extractViaExchange(int amount, TransactionContext transaction) {
        if (this.itemAccess == null) return 0;
        int accessAmount = this.itemAccess.getAmount();
        if (accessAmount == 0) return 0;
        int amountPerItem = Math.min(this.maxExtract, amount / accessAmount);
        if (amountPerItem == 0) return 0;

        ItemResource accessResource = this.itemAccess.getResource();
        int currentEnergyPerItem = this.getEnergyFrom(accessResource);

        int extractedPerItem = Math.min(amountPerItem, currentEnergyPerItem);
        if (extractedPerItem > 0) {
            ItemResource emptiedResource = accessResource.with(
                ModComponents.STORED_ENERGY,
                new StoredEnergy(currentEnergyPerItem - extractedPerItem));
            if (!emptiedResource.isEmpty()) {
                return extractedPerItem * this.itemAccess.exchange(emptiedResource, accessAmount, transaction);
            }
        }
        return 0;
    }

    /**
     * 通过 SnapshotJournal 插入能量（回退模式）。
     */
    private int insertViaJournal(int amount, TransactionContext transaction) {
        if (this.journal == null) return 0;
        int energy = this.getEnergyFromStack();
        int accepted = Math.min(amount, this.capacity - energy);
        if (accepted > 0) {
            this.journal.updateSnapshots(transaction);
            this.setEnergyToStack(energy + accepted);
        }
        return accepted;
    }

    /**
     * 通过 SnapshotJournal 抽取能量（回退模式）。
     */
    private int extractViaJournal(int amount, TransactionContext transaction) {
        if (this.journal == null) return 0;
        int energy = this.getEnergyFromStack();
        int extracted = Math.min(energy, amount);
        if (extracted > 0) {
            this.journal.updateSnapshots(transaction);
            this.setEnergyToStack(energy - extracted);
        }
        return extracted;
    }

    private int getEnergyFromStack() {
        if (this.stack == null) return 0;
        return this.stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private void setEnergyToStack(int energy) {
        if (this.stack == null) return;
        this.stack.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy));
    }

    /**
     * 能量快照日志，用于在事务回滚时恢复物品的能量值。
     * 仅在回退模式（无 ItemAccess 上下文）下使用。
     */
    private class EnergyJournal extends SnapshotJournal<Integer> {
        @Override
        protected Integer createSnapshot() {
            return ItemFEStorage.this.getEnergyFromStack();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            ItemFEStorage.this.setEnergyToStack(snapshot);
        }
    }
}
