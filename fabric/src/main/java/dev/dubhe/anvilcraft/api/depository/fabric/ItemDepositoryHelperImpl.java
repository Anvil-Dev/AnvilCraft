package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ItemDepositoryHelperImpl {
    /**
     * 获取物品容器
     *
     * @param level     世界
     * @param pos       位置
     * @param direction 方向
     * @return 物品容器
     */
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
        return new ItemStorageProxyItemDepository(storage);
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
}
