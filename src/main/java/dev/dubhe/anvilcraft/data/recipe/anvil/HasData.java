package dev.dubhe.anvilcraft.data.recipe.anvil;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * 包含数据
 */
public interface HasData {
    Map.Entry<String, CompoundTag> getData();
}
