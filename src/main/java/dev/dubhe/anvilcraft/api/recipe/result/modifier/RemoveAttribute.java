package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * 删除指定输入物品的数据。
 *
 * @param attrs 包含指定的输入物品和将要删除的数据组件类型。
 */
public record RemoveAttribute(List<ResourceLocation> attrs) implements IResultModifier {
    public static final MapCodec<RemoveAttribute> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ResourceLocation.CODEC
            .listOf()
            .fieldOf("attrs")
            .forGetter(RemoveAttribute::attrs)
    ).apply(ins, RemoveAttribute::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveAttribute> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
        RemoveAttribute::attrs,
        RemoveAttribute::new
    );

    public static Builder builder() {
        return new Builder();
    }

    public static Builder removeAttr(ResourceLocation... attrs) {
        return new Builder().withAttrs(attrs);
    }

    @Override
    public void modify(ResultContext ctx) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (var entry : ctx.getResult().getAttributeModifiers().modifiers()) {
            if (!this.attrs.contains(entry.modifier().id())) builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        ctx.getResult().set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.REMOVE_ATTRIBUTE.get();
    }

    public static class Type implements IResultModifier.Type<RemoveAttribute> {
        @Override
        public MapCodec<RemoveAttribute> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RemoveAttribute> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<ResourceLocation> attrs = ImmutableList.builder();

        public Builder withAttr(ResourceLocation attr) {
            this.attrs.add(attr);
            return this;
        }

        public Builder withAttrs(ResourceLocation... attrs) {
            for (ResourceLocation attr : attrs) {
                this.withAttr(attr);
            }
            return this;
        }

        public Builder withAttrs(List<ResourceLocation> attrs) {
            this.attrs.addAll(attrs);
            return this;
        }

        public RemoveAttribute build() {
            return new RemoveAttribute(this.attrs.build());
        }
    }
}
