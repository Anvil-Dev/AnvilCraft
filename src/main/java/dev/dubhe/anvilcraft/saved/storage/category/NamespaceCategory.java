package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.component.ModNameContents;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

public record NamespaceCategory(ItemStackTemplate icon, Component name, String namespace) implements ICategory {
    public NamespaceCategory(ItemLike icon, String namespace) {
        this(
            new ItemStackTemplate(icon.asItem()),
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
            .map(key -> key.identifier().getNamespace().equals(this.namespace))
            .orElse(false);
    }

    @Override
    public boolean testFluid(FluidStack fluid) {
        // 命名空间对流体质地同样成立：minecraft 分类应涵盖水 / 岩浆等原版流体
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getNamespace().equals(this.namespace);
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
            ICategory.NAME_CODEC
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
