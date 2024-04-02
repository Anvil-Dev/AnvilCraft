package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ItemDepositoryImpl extends ItemDepository {
    private Storage<ItemVariant> container = null;

    @Override
    public boolean canInject(@NotNull ItemStack thing, long count) {
        return super.canInject(thing, count);
    }

    @Override
    public long inject(@NotNull ItemStack thing, long count) {
        if (super.canInject(thing, count)) return super.inject(thing, count);
        if (null == container) return count;
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        return count - StorageUtil.tryInsertStacking(null, ItemVariant.of(thingType), count, null);
    }

    @Override
    public boolean canTake() {
        return super.canTake();
    }

    @Override
    public @NotNull Thing<ItemStack> take(@NotNull ItemStack thing, long count) {
        if (super.canTake()) return super.take(thing, count);
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        if (null == container) return new Thing<>(thingType, 0);
        return new Thing<>(thingType, container.extract(ItemVariant.of(thingType), count, null));
    }

    public static @Nullable ItemDepository getItemDepository(Level level, BlockPos pos, Direction direction) {
        ItemDepository depository = ItemDepository.getVanillaDepository(level, pos);
        if (depository != null) return depository;
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction);
        if (target == null) return null;
        ItemDepositoryImpl depositoryImpl = new ItemDepositoryImpl();
        depositoryImpl.container = target;
        return depositoryImpl;
    }
}
