package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings({"UnstableApiUsage", "removal", "ClassCanBeRecord"})
public class ItemDepositoryProxyStorage implements Storage<ItemVariant> {
    public final IItemDepository depository;

    public ItemDepositoryProxyStorage(IItemDepository depository) {
        this.depository = depository;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        long left = maxAmount;
        for (int i = 0; i < depository.getSlots(); i++) {
            final long left2 = left;
            final int j = i;
            left = depository.insert(i, (resource.toStack((int) left)), true).getCount();
            if (left != left2) {
                transaction.addCloseCallback((tx, result) -> {
                    if (result.wasAborted()) return;
                    depository.insert(j, (resource.toStack((int) left2)), false).getCount();
                });
            }
            if (left == 0) break;
        }
        return maxAmount - left;
    }

    @Override
    public long simulateExtract(ItemVariant resource, long maxAmount, @Nullable TransactionContext transaction) {
        int left = maxAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxAmount;
        for (int i = 0; i < depository.getSlots(); i++) {
            ItemStack ext = depository.extract(i, left, true);
            left -= ext.getCount();
        }
        return maxAmount - left;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (simulateExtract(resource, maxAmount, transaction) > 0) {
            long left = maxAmount;
            final long left2 = maxAmount;
            for (int i = 0; i < depository.getSlots(); i++) {
                final int j = i;
                ItemStack extracted = depository.extract(i, (int) left, true);
                if (extracted.getCount() != maxAmount) {
                    transaction.addCloseCallback((tx, result) -> {
                        if (result.wasAborted()) return;
                        depository.extract(j, (int) left2, false);
                    });
                }
                if (resource.matches(extracted)) {
                    left -= extracted.getCount();
                    if (left == 0) break;
                }
            }
            return maxAmount - left;
        }
        return 0;
    }

    @Override
    public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (int i = 0; i < depository.getSlots(); i++) {
            views.add(new ItemStorageView(i));
        }
        return views.iterator();
    }

    private class ItemStorageView extends SnapshotParticipant<ItemStack> implements StorageView<ItemVariant> {
        private final int index;

        public ItemStorageView(int index) {
            this.index = index;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            var extracted = depository.extract(index, (int) maxAmount, true, false);
            if (!resource.matches(extracted) || extracted.getCount() <= 0) return 0;
            transaction.addCloseCallback((tx, result) -> {
                if (result.wasAborted()) return;
                depository.extract(index, extracted.getCount(), false, false);
            });
            return extracted.getCount();
        }

        @Override
        public boolean isResourceBlank() {
            return depository.getStack(index).isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(depository.getStack(index));
        }

        @Override
        public long getAmount() {
            return depository.getStack(index).getCount();
        }

        @Override
        public long getCapacity() {
            return depository.getSlotLimit(index);
        }

        @Override
        protected void onFinalCommit() {
            depository.onContentsChanged();
        }

        @Override
        protected ItemStack createSnapshot() {
            return depository.getStack(index).copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot) {
            depository.setStack(index, snapshot);
        }
    }
}
