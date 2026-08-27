package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
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

import java.util.Objects;

public record NamespaceCategory(ItemStack icon, Component name, String namespace) implements ICategory {
    public NamespaceCategory(ItemStack icon, String namespace) {
        this(
            icon,
            Component.translatable(
                "category.anvilcraft.namespace",
                MutableComponent.create(new ModNameContents(namespace))
            ),
            namespace
        );
    }

    public NamespaceCategory(ItemLike icon, String namespace) {
        this(new ItemStack(icon.asItem()), namespace);
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NamespaceCategory(ItemStack icon1, Component name1, String namespace1))) return false;
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && Objects.equals(this.namespace(), namespace1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31 + Objects.hash(this.name(), this.namespace());
    }

    public static class Type implements ICategory.Type<NamespaceCategory> {
        public static final MapCodec<NamespaceCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(NamespaceCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(NamespaceCategory::name),
            Codec.STRING
                .fieldOf("namespace")
                .forGetter(NamespaceCategory::namespace),
            NamespaceCategory::new
        );
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
