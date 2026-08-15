package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.component.ModNameContents;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record NamespaceCategory(ItemStack icon, Component name, String namespace) implements ICategory {
    public NamespaceCategory(ItemLike icon, String namespace) {
        this(
            new ItemStack(icon.asItem()),
            Component.translatable(
                "category.anvilcraft.namespace",
                MutableComponent.create(new ModNameContents(namespace))
            ),
            namespace
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return stack.typeHolder()
            .unwrapKey()
            .map(key -> key.location().getNamespace().equals(this.namespace))
            .orElse(false);
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.NAMESPACE.get();
    }

    public static class Type implements ICategory.Type<NamespaceCategory> {
        public static final MapCodec<NamespaceCategory> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(NamespaceCategory::icon),
            ComponentSerialization.CODEC
                .fieldOf("name")
                .forGetter(NamespaceCategory::name),
            Codec.STRING
                .fieldOf("namespace")
                .forGetter(NamespaceCategory::namespace)
        ).apply(instance, NamespaceCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, NamespaceCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            NamespaceCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            NamespaceCategory::name,
            ByteBufCodecs.STRING_UTF8,
            NamespaceCategory::namespace,
            NamespaceCategory::new
        );

        @Override
        public MapCodec<NamespaceCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NamespaceCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
