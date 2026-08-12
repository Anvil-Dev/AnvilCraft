package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record NamespaceCategory(ItemStack icon, Component name, String namespace) implements ICategory {
    public NamespaceCategory(ItemLike icon, String namespace) {
        this(
            new ItemStack(icon.asItem()),
            Component.translatable("category.anvilcraft.namespace", NamespaceCategory.modDisplayName(namespace)),
            namespace
        );
    }

    /// 优先取模组显示名（大小写正确，如 Minecraft/AnvilCraft），找不到时退化用原始命名空间 id
    private static String modDisplayName(String namespace) {
        String key = "component_content.anvilcraft.mod_name." + namespace;
        Component translated = Component.translatable(key);
        if (!translated.getString().equals(key)) {
            return translated.getString();
        }
        return net.neoforged.fml.ModList.get()
            .getModContainerById(namespace)
            .map(container -> container.getModInfo().getDisplayName())
            .orElse(namespace);
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
            ComponentSerialization.flatCodec(Integer.MAX_VALUE)
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