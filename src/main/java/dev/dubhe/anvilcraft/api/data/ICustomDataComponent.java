package dev.dubhe.anvilcraft.api.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.recipe.anvil.ISerializer;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 自定义数据组件。通常为必需其它数据组件才能正常构建的数据组件。
 *
 * @param <T> 该数据组件的类型
 */
public interface ICustomDataComponent<T> {
    Codec<ICustomDataComponent<?>> CODEC = Codec.lazyInitialized(() -> ModRegistries.CUSTOM_DATA_TYPE_REGISTRY
        .byNameCodec().dispatch(ICustomDataComponent::getType, ICustomDataComponent.Type::codec));
    StreamCodec<RegistryFriendlyByteBuf, ICustomDataComponent<?>> STREAM_CODEC = StreamCodec.recursive(
        streamCodec -> ByteBufCodecs.registry(ModRegistries.CUSTOM_DATA_KEY)
            .dispatch(ICustomDataComponent::getType, ICustomDataComponent.Type::streamCodec));

    /**
     * 获取该数据组件的类型。
     *
     * @return 该数据组件的类型
     */
    DataComponentType<? super T> getDataComponentType();

    /**
     * 获取该自定义数据组件的类型。
     *
     * @return 该自定义数据组件的类型
     */
    Type<? extends ICustomDataComponent<?>> getType();

    /**
     * 获取该数据组件所必需的数据组件类型。<br>
     * {@code Integer} 值决定了这个数据组件来自于哪个输入材料。<br>
     * {@code boolean} 值决定了这个数据组件是否可为 {@code null}。<br><br>
     * 由于 {@link ICustomDataComponent#make(List)} 方法强依赖于该方法返回的 {@link Object2BooleanMap} 的顺序，
     * 该 {@link Object2BooleanMap} 必须为有顺序的，且建议其顺序为已知可控的。<br>
     * 否则可能导致代码复杂度提升。
     *
     * @return 一个包含所有必需的数据组件类型和其值是否可为 {@code null} 的 {@link Object2BooleanMap}
     */
    Object2BooleanMap<Pair<Integer, DataComponentType<?>>> getRequiredOthers();

    /**
     * 使用所有必需的数据组件构建一个该数据组件。
     *
     * @param data 所有必需的数据组件。顺序由 {@link ICustomDataComponent#getRequiredOthers()} 方法中返回的 {@link Object2BooleanMap} 控制。
     *
     * @return 一个全新的该数据组件
     */
    T make(List<Object> data);

    default void applyToStack(ItemStack stack, T value) {
        stack.set(this.getDataComponentType(), value);
    }

    interface Type<T extends ICustomDataComponent<?>> extends ISerializer<T> {
    }
}
