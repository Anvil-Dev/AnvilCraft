package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.function.Predicate;

public interface ICategory extends Predicate<UnlimitedItemStack> {
    Codec<ICategory> DIRECT_CODEC = Codec.lazyInitialized(
        () -> ModRegistries.CATEGORY_TYPE.byNameCodec().dispatch(ICategory::getType, Type::codec)
    );
    Codec<Holder<ICategory>> HOLDER_CODEC = RegistryFileCodec.create(ModRegistryKeys.CATEGORY, DIRECT_CODEC);
    Codec<ICategory> CODEC = HOLDER_CODEC.xmap(
        HolderHolder::new,
        category -> category instanceof HolderHolder(Holder<ICategory> category1) ? category1 : Holder.direct(category)
    );
    StreamCodec<RegistryFriendlyByteBuf, ICategory> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.CATEGORY_TYPE)
        .dispatch(ICategory::getType, Type::streamCodec);

    ItemStackTemplate icon();

    Component name();

    @Override
    boolean test(UnlimitedItemStack stack);

    Type<? extends ICategory> getType();

    static Component constructName(Identifier suffix) {
        return Component.translatable("category." + suffix.toString().replace(':', '.'));
    }

    interface Type<C extends ICategory> extends ISerializer<C> {
    }

    record HolderHolder(Holder<ICategory> category) implements ICategory {
        @Override
        public ItemStackTemplate icon() {
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
