package dev.dubhe.anvilcraft.building;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 建造侧实体适配:匹配类型、材料、剥离内容、提交时直接生成。
 * 注册项放在 init,这里不引用客户端或 FTB。
 */
public interface EntityBuildAdapter {
    record SlotStack(int slot, ItemStack stack) {
    }

    record Planned(
        ItemStack material,
        ItemStack returned,
        CompoundTag entityNbt,
        List<SlotStack> contents,
        List<FluidBuildAdapter.TankFluid> fluids,
        boolean unsupported
    ) {
        public static Planned skip() {
            return new Planned(ItemStack.EMPTY, ItemStack.EMPTY, new CompoundTag(), List.of(), List.of(), true);
        }
    }

    boolean matches(EntityType<?> type, CompoundTag nbt);

    Planned plan(ServerLevel level, StructureSnapshot.EntityEntry entry, CompoundTag transformedNbt);

    default ItemStack returnAfterDeliver(ServerLevel level, BuildingEntityOp op) {
        return op.returnStack().copy();
    }

    default @Nullable Entity spawn(ServerLevel level, BuildingEntityOp op) {
        CompoundTag nbt = op.entityNbt();
        if (nbt == null || nbt.getString("id").isEmpty()) {
            return null;
        }
        CompoundTag copy = nbt.copy();
        copy.remove("UUID");
        return EntityType.create(copy, level).map(entity -> {
            if (!level.addFreshEntity(entity)) {
                return null;
            }
            return entity;
        }).orElse(null);
    }

    default void insertContents(Entity entity, List<SlotStack> contents, HolderLookup.Provider registries) {
        if (contents.isEmpty()) {
            return;
        }
        if (entity instanceof Container container) {
            for (SlotStack content : contents) {
                if (content.slot() >= 0 && content.slot() < container.getContainerSize()) {
                    container.setItem(content.slot(), content.stack().copy());
                }
            }
        }
    }
}
