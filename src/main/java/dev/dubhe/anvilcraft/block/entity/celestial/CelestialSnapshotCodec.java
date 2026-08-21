package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

/** Reads and writes celestial snapshots carried by disks and singularity crystals. */
public final class CelestialSnapshotCodec {
    private static final String BODY_KEY = "celestialBody";
    private static final String SNAPSHOT_KEY = "celestialSnapshot";

    private CelestialSnapshotCodec() {
    }

    public static @Nullable CompoundTag extract(ItemStack stack) {
        return load(stack);
    }

    public static @Nullable CompoundTag load(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            CompoundTag data = DiskItem.getData(stack);
            return data.contains(BODY_KEY) ? data.copy() : null;
        }
        if (!stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) return null;
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag snapshot = customData.copyTag().getCompound(SNAPSHOT_KEY);
        return snapshot.contains(BODY_KEY) ? snapshot.copy() : null;
    }

    public static void save(ItemStack stack, CompoundTag snapshot) {
        if (stack.getItem() instanceof DiskItem) {
            if (containsExtremeBody(snapshot)) return;
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
            updated.put(SNAPSHOT_KEY, snapshot.copy());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(updated));
        }
    }

    private static boolean containsExtremeBody(CompoundTag snapshot) {
        if (!snapshot.contains(BODY_KEY)) return false;
        String bodyClass = snapshot.getCompound(BODY_KEY).getString("bodyClass");
        return CelestialBodyClass.BLACK_HOLE.name().equals(bodyClass)
            || CelestialBodyClass.NEUTRON_STAR.name().equals(bodyClass);
    }
}
