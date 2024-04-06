package dev.dubhe.anvilcraft.api.depository.util.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.fabric.IItemDepositoryImpl;
import dev.dubhe.anvilcraft.api.depository.fabric.ItemDepositoryProxyStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

@SuppressWarnings("UnstableApiUsage")
public class ItemDepositoryHelper {
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
     * @param itemDepository 要转换的 itemDepository
     * @return 转换为的 存有 {@link ItemVariant} 的 {@link Storage}
     */
    public static Storage<ItemVariant> toStorage(IItemDepository itemDepository) {
        return new ItemDepositoryProxyStorage(itemDepository);
    }
}
