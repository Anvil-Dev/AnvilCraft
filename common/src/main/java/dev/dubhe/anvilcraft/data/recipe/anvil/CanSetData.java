package dev.dubhe.anvilcraft.data.recipe.anvil;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * 可以设置数据
 */
public interface CanSetData {
    void setData(Map<String, CompoundTag> data);
}
