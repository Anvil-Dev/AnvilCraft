package dev.dubhe.anvilcraft.api.depository.util.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.fabric.IItemDepositoryImpl;
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
}
