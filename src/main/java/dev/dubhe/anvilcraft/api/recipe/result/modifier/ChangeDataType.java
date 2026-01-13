package dev.dubhe.anvilcraft.api.recipe.result.modifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * 复制指定输入物品的数据，并将其粘贴到另一个数据组件类型下。
 */
public record ChangeDataType<T>(RecipeInputSlot input, DataComponentType<T> orig, ICustomDataComponent<T> dest) implements IResultModifier {
    public static final MapCodec<ChangeDataType<?>> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ResourceLocation.CODEC
            .fieldOf("orig")
            .forGetter(ChangeDataType::origId),
        ICustomDataComponent.CODEC
            .fieldOf("dest")
            .forGetter(ChangeDataType::dest),
        RecipeInputSlot.CODEC
            .forGetter(ChangeDataType::input)
    ).apply(ins, ChangeDataType::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeDataType<?>> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        ChangeDataType::origId,
        ICustomDataComponent.STREAM_CODEC,
        ChangeDataType::dest,
        RecipeInputSlot.STREAM_CODEC,
        ChangeDataType::input,
        ChangeDataType::new
    );

    public ChangeDataType(ResourceLocation origId, ICustomDataComponent<T> dest, RecipeInputSlot slot) {
        this(
            slot,
            Util.cast(BuiltInRegistries.DATA_COMPONENT_TYPE.get(origId)),
            dest
        );
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> Builder<T> changeDataType(RecipeInputSlot input, DataComponentType<T> orig, ICustomDataComponent<T> dest) {
        return new Builder<T>().input(input).orig(orig).dest(dest);
    }

    @Override
    public void modify(ResultContext ctx) {
        T value = ctx.getInput(this.input).get(this.orig);
        if (value == null) return;
        ctx.getResult().set(this.orig, null);
        ctx.getResult().set(
            this.dest.getDataComponentType(),
            this.dest.merge(ctx.getResult().get(this.dest.getDataComponentType()), value)
        );
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.CHANGE_DATA_TYPE.get();
    }

    private ResourceLocation origId() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(this.orig);
    }

    public static class Type implements IResultModifier.Type<ChangeDataType<?>> {
        @Override
        public MapCodec<ChangeDataType<?>> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChangeDataType<?>> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Accessors(fluent = true, chain = true)
    @Setter
    public static class Builder<T> {
        private RecipeInputSlot input;
        private DataComponentType<T> orig;
        private ICustomDataComponent<T> dest;

        public ChangeDataType<T> build() {
            return new ChangeDataType<>(this.input, this.orig, this.dest);
        }
    }
}
