package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ICategory extends Predicate<UnlimitedItemStack> {
    Codec<ICategory> DIRECT_CODEC = Codec.lazyInitialized(
        () -> ModRegistries.CATEGORY_TYPE.byNameCodec().dispatch(ICategory::getType, Type::codec)
    );
    Codec<Holder<ICategory>> HOLDER_CODEC = RegistryFileCodec.create(ModRegistryKeys.CATEGORY, DIRECT_CODEC);
    Codec<ICategory> CODEC = Codec.of(
        new Encoder<>() {
            @Override
            public <T> DataResult<T> encode(ICategory input, DynamicOps<T> ops, T prefix) {
                if (!(ops instanceof RegistryOps<?> registryOps)) {
                    return DataResult.error(() -> "Cannot access registry " + ModRegistryKeys.CATEGORY);
                }

                Optional<HolderGetter<ICategory>> getter = registryOps.getter(ModRegistryKeys.CATEGORY);
                if (getter.isPresent() && getter.get() instanceof HolderLookup.RegistryLookup<ICategory> lookup) {
                    Optional<Holder.Reference<ICategory>> ref = lookup.listElements()
                        .filter(innerRef -> input.equals(innerRef.value()))
                        .findFirst();
                    if (ref.isPresent()) {
                        return HOLDER_CODEC.encode(ref.get(), ops, prefix);
                    }
                }

                return HOLDER_CODEC.encode(Holder.direct(input), ops, prefix);
            }
        },
        HOLDER_CODEC.map(Holder::value)
    );
    StreamCodec<RegistryFriendlyByteBuf, ICategory> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.CATEGORY_TYPE)
        .dispatch(ICategory::getType, Type::streamCodec);
    StreamCodec<RegistryFriendlyByteBuf, List<ICategory>> LIST_STREAM_CODEC = ICategory.STREAM_CODEC.apply(ByteBufCodecs.list());
    Codec<Component> NAME_CODEC = Codec.either(ComponentSerialization.flatCodec(Integer.MAX_VALUE), ComponentSerialization.CODEC)
        .xmap(either -> either.map(Function.identity(), Function.identity()), Either::right);

    ItemStack icon();

    Component name();

    @Override
    boolean test(UnlimitedItemStack stack);

    /**
     * 判定仓储中的流体伪条目是否属于本分类。
     *
     * <p>分类本身是物品谓词，而流体不是物品，因此默认不匹配任何流体；
     * 只有流体分类（以及命名空间这类对流体质地成立的分类）才覆写它。</p>
     *
     * @param fluid 待判定的流体
     * @return 属于本分类时返回 true
     */
    default boolean testFluid(FluidStack fluid) {
        return false;
    }

    Type<? extends ICategory> getType();

    static Component constructName(ResourceLocation suffix) {
        return Component.translatable("category." + suffix.toString().replace(':', '.'));
    }

    interface Type<C extends ICategory> extends ISerializer<C> {
    }
}
