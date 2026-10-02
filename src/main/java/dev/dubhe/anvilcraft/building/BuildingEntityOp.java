package dev.dubhe.anvilcraft.building;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record BuildingEntityOp(CompoundTag entityNbt, ItemStack returnStack) {
}
