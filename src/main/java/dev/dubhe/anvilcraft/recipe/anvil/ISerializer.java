package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 序列化接口，用于定义对象的编解码方式
 * 实现该接口的类可以支持数据的序列化和反序列化
 *
 * @param <T> 序列化对象类型
 */
public interface ISerializer<T> {
    /**
     * 获取MapCodec编解码器
     *
     * @return MapCodec编解码器
     */
    @NotNull MapCodec<T> codec();

    /**
     * 获取StreamCodec编解码器
     *
     * @return StreamCodec编解码器
     */
    @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
}