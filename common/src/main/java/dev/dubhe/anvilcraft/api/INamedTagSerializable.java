package dev.dubhe.anvilcraft.api;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public interface INamedTagSerializable {
    /**
     * 向NBT中序列化数据
     */
    @NotNull CompoundTag serializeNBT();

    /**
     * 从NBT中反序列化数据
     *
     * @param tag 读取数据的NBT来源
     */
    void deserializeNBT(@NotNull CompoundTag tag);
}
