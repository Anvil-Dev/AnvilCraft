package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import dev.dubhe.anvilcraft.item.utility.DiskItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * 读写磁盘和奇点晶体携带的天体快照。
 */
public final class CelestialSnapshotCodec {
    private static final String BODY_KEY = "celestialBody";
    private static final String SNAPSHOT_KEY = "celestialSnapshot";

    private CelestialSnapshotCodec() {
    }

    public static @Nullable CompoundTag extract(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            return DiskItem.getData(stack).copy();
        }
        return CelestialSnapshotCodec.load(stack);
    }

    public static @Nullable CompoundTag load(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            CompoundTag data = DiskItem.getData(stack);
            if (data.contains(CelestialSnapshotCodec.BODY_KEY)) return data.copy();
        }
        if (!stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) return null;
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag snapshot = customData.copyTag().getCompoundOrEmpty(CelestialSnapshotCodec.SNAPSHOT_KEY);
        return snapshot.contains(CelestialSnapshotCodec.BODY_KEY) ? snapshot.copy() : null;
    }

    public static void save(ItemStack stack, CompoundTag snapshot) {
        if (stack.getItem() instanceof DiskItem) {
            if (CelestialSnapshotCodec.containsExtremeBody(snapshot)) return;
            CompoundTag diskTag = DiskItem.hasDataStored(stack)
                ? DiskItem.getData(stack).copy()
                : new CompoundTag();
            diskTag.merge(snapshot);
            stack.set(ModComponents.DISK_DATA, new DiskData(diskTag));
            return;
        }
        if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            CustomData oldCustom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag updated = oldCustom.copyTag();
            updated.put(CelestialSnapshotCodec.SNAPSHOT_KEY, snapshot.copy());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(updated));
        }
    }

    private static boolean containsExtremeBody(CompoundTag snapshot) {
        if (!snapshot.contains(CelestialSnapshotCodec.BODY_KEY)) return false;
        String bodyClass = snapshot.getCompoundOrEmpty(CelestialSnapshotCodec.BODY_KEY).getStringOr("bodyClass", "");
        return CelestialBodyClass.BLACK_HOLE.name().equals(bodyClass)
            || CelestialBodyClass.NEUTRON_STAR.name().equals(bodyClass);
    }
}
