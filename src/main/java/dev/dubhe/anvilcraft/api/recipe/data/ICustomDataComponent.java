package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义数据组件。通常为必需其它数据组件才能正常构建的数据组件。
 *
 * @param <T> 该数据组件的类型
 */
public interface ICustomDataComponent<T> {
    Codec<ICustomDataComponent<?>> CODEC = Codec.lazyInitialized(() -> ModRegistries.CUSTOM_DATA_TYPE_REGISTRY
        .byNameCodec().dispatch(ICustomDataComponent::getType, ICustomDataComponent.Type::codec));
    StreamCodec<RegistryFriendlyByteBuf, ICustomDataComponent<?>> STREAM_CODEC = StreamCodec.recursive(
        streamCodec -> ByteBufCodecs.registry(ModRegistries.CUSTOM_DATA_TYPE_KEY)
            .dispatch(ICustomDataComponent::getType, ICustomDataComponent.Type::streamCodec));

    /**
     * 获取该数据组件的类型。
     *
     * @return 该数据组件的类型
     */
    DataComponentType<T> getDataComponentType();

    /**
     * 获取该自定义数据组件的类型。
     *
     * @return 该自定义数据组件的类型
     */
    Type<? extends ICustomDataComponent<?>> getType();

    /**
     * 根据上下文构建数据组件。
     *
     * @param ctx 上下文
     * @return 一个全新的数据组件
     */
    @Nullable
    T make(ResultContext ctx);

    default void applyToStack(ItemStack stack, @Nullable T value) {
        if (value == null) stack.remove(this.getDataComponentType());
        stack.set(this.getDataComponentType(), value);
    }

    /**
     * 将两个数据合并为一个新的组件。
     *
     * @param oldData 旧数据
     * @param newData 新数据
     * @return 合并后的数据
     */
    T merge(T oldData, T newData);

    interface Type<T extends ICustomDataComponent<?>> extends ISerializer<T> {
    }
}
