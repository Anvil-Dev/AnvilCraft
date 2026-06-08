package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.component.TranslatableContents;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;

public record NamespaceCategory(ItemStackTemplate icon, Component name, String namespace) implements ICategory {
    public NamespaceCategory(ItemLike icon, String namespace) {
        this(
            new ItemStackTemplate(icon.asItem()),
            Component.translatable(
                "category.anvilcraft.namespace",
                MutableComponent.create(new TranslatableContents(
                    "category.anvilcraft.namespace." + namespace,
                    ModList.get()
                        .getModContainerById(namespace)
                        .map(container -> container.getModInfo().getDisplayName())
                        .map(Component::literal)
                        .orElse(Component.translatable("category.anvilcraft.unknown_namespace", namespace)),
                    TranslatableContents.NO_ARGS
                ))
            ),
            namespace
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return stack.typeHolder()
            .unwrapKey()
            .map(key -> key.identifier().getNamespace().equals(this.namespace))
            .orElse(false);
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.NAMESPACE.get();
    }

    public static class Type implements ICategory.Type<NamespaceCategory> {
        public static final MapCodec<NamespaceCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(NamespaceCategory::icon),
            ComponentSerialization.flatRestrictedCodec(Integer.MAX_VALUE)
                .fieldOf("name")
                .forGetter(NamespaceCategory::name),
            Codec.STRING
                .fieldOf("namespace")
                .forGetter(NamespaceCategory::namespace),
            NamespaceCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, NamespaceCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
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
