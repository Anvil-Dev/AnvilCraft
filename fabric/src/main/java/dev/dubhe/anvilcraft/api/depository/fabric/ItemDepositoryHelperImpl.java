package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
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
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class ItemDepositoryHelperImpl {
    @Nullable
    public static IItemDepository getItemDepository(@NotNull Level level, BlockPos pos, Direction direction) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction);
        if (target == null) return null;
        return toItemDepository(target);
    }

    /**
     * 将存有 {@link ItemVariant} 的 {@link Storage} 转换为 {@link IItemDepository}
     *
     * @param storage 要转换的 Storage
     * @return 转换为的 ItemDepository
     */
    public static IItemDepository toItemDepository(Storage<ItemVariant> storage) {
        return new IItemDepositoryImpl(storage);
    }

    /**
     * 将 {@link IItemDepository} 转换为存有 {@link ItemVariant} 的 {@link Storage}
     *
     * @param itemDepository 要转换的 itemDepository
     * @return 转换为的 存有 {@link ItemVariant} 的 {@link Storage}
     */
    public static Storage<ItemVariant> toStorage(IItemDepository itemDepository) {
        return new ItemDepositoryProxyStorage(itemDepository);
    }

    public static void exportToTarget(IItemDepository source, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, Direction direction) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction);
        if (target != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                StorageUtil.move(toStorage(source), target, itemVariant -> predicate.test(itemVariant.toStack()), maxAmount, transaction);
                transaction.commit();
            }
        }
    }

    public static void importToTarget(IItemDepository target, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, Direction direction) {
        Storage<ItemVariant> source = ItemStorage.SIDED.find(level, pos, direction);
        if (source != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                StorageUtil.move(source, toStorage(target), itemVariant -> predicate.test(itemVariant.toStack()), maxAmount, transaction);
                transaction.commit();
            }
        }
    }
}
