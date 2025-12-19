package dev.dubhe.anvilcraft.api.container.category;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.util.ISerializer;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public interface ICategory extends Predicate<UnlimitedItemStack> {
    Codec<ICategory> DIRECT_CODEC = Codec.lazyInitialized(
        () -> ModRegistries.CATEGORY_TYPE_REGISTRY.byNameCodec().dispatch(ICategory::getType, Type::codec)
    );
    Codec<Holder<ICategory>> HOLDER_CODEC = RegistryFileCodec.create(ModRegistries.CATEGORY_KEY, DIRECT_CODEC);
    Codec<ICategory> CODEC = HOLDER_CODEC.xmap(
        HolderHolder::new,
        category -> category instanceof HolderHolder(Holder<ICategory> category1) ? category1 : new Holder.Direct<>(category)
    );
    StreamCodec<RegistryFriendlyByteBuf, ICategory> STREAM_CODEC = ByteBufCodecs.registry(ModRegistries.CATEGORY_TYPE_KEY)
        .dispatch(ICategory::getType, Type::streamCodec);

    ItemStack icon();

    Component name();

    Type<? extends ICategory> getType();

    @Override
    boolean test(UnlimitedItemStack stack);

    static Component constructName(String suffix) {
        return Component.translatable("category.anvilcraft." + suffix);
    }

    interface Type<T extends ICategory> extends ISerializer<T> {
    }

    record HolderHolder(Holder<ICategory> category) implements ICategory {
        @Override
        public ItemStack icon() {
            return this.category.value().icon();
        }

        @Override
        public Component name() {
            return this.category.value().name();
        }

        @Override
        public Type<? extends ICategory> getType() {
            return this.category.value().getType();
        }

        @Override
        public boolean test(UnlimitedItemStack stack) {
            return this.category.value().test(stack);
        }
    }
}
