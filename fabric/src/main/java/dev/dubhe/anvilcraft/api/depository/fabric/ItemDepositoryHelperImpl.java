package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        Storage<ItemVariant> target = getItemStorage(level, pos, direction);
        if (target == null) {
            List<InventoryStorage> entities = level
                    .getEntitiesOfClass(Entity.class, new AABB(pos))
                    .stream()
                    .filter(entity -> entity instanceof ContainerEntity)
                    .map(entity -> (ContainerEntity) entity)
                    .map(container -> InventoryStorage.of(container, null))
                    .toList();
            if (!entities.isEmpty()) target = entities.get(level.getRandom().nextInt(0, entities.size()));
        }
        if (target == null) return null;
        return toItemDepository(target);
    }

    @Nullable
    private static Storage<ItemVariant> getItemStorage(Level level, BlockPos pos, Direction direction) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction);
        if (target == null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return null;
            if (be instanceof SidedStorageBlockEntity sidedStorageBlockEntity) {
                return sidedStorageBlockEntity.getItemStorage(direction);
            }
        }
        return target;
    }

    /**
     * 将存有 {@link ItemVariant} 的 {@link Storage} 转换为 {@link IItemDepository}
     *
     * @param storage 要转换的 Storage
     * @return 转换为的 ItemDepository
     */
    public static @NotNull IItemDepository toItemDepository(Storage<ItemVariant> storage) {
        return new ItemStorageProxyItemDepository(storage);
    }

    /**
     * 将 {@link IItemDepository} 转换为存有 {@link ItemVariant} 的 {@link Storage}
     *
     * @param itemDepository 要转换的 itemDepository
     * @return 转换为的 存有 {@link ItemVariant} 的 {@link Storage}
     */
    public static @NotNull Storage<ItemVariant> toStorage(IItemDepository itemDepository) {
        return new ItemDepositoryProxyStorage(itemDepository);
    }
}
