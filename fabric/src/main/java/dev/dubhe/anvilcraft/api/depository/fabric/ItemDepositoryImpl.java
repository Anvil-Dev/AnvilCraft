package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ItemDepositoryImpl extends ItemDepository {
    private Storage<ItemVariant> container = null;

    // TODO
    @Override
    public boolean canInject(@NotNull ItemStack thing, long count) {
        if (null == container) return false;
        return this.inject(thing, count, true) == 0;
    }

    @Override
    public long inject(@NotNull ItemStack thing, long count, boolean simulate) {
        if (count <= 0) return 0;
        if (null == container) return count;
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        long insertCount;
        if (simulate) insertCount = StorageUtil.simulateInsert(container, ItemVariant.of(thingType), count, null);
        else {
            try (Transaction transaction = Transaction.openNested(null)) {
                insertCount = container.insert(ItemVariant.of(thingType), count, transaction);
                transaction.commit();
            }
        }
        return count - insertCount;
    }

    // TODO
    @Override
    public boolean canTake() {
        return true;
    }

    @Override
    public @NotNull Thing<ItemStack> take(@NotNull ItemStack thing, long count) {
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        if (null == container) return new Thing<>(thingType, 0);
        return new Thing<>(thingType, container.extract(ItemVariant.of(thingType), count, null));
    }

    public static @Nullable ItemDepository getItemDepository(Level level, BlockPos pos, Direction direction) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction);
        if (target == null) return null;
        ItemDepositoryImpl depositoryImpl = new ItemDepositoryImpl();
        depositoryImpl.container = target;
        return depositoryImpl;
    }
}
