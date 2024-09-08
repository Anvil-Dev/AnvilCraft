package dev.dubhe.anvilcraft.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * 可被NBT序列化的
 */
public interface INamedTagSerializable {
    /**
     * 向NBT中序列化数据
     */
    @NotNull
    CompoundTag serializeNbt(HolderLookup.Provider provider);

    /**
     * 从NBT中反序列化数据
     *
     * @param tag 读取数据的NBT来源
     */
    void deserializeNbt(HolderLookup.Provider provider, @NotNull CompoundTag tag);
}
