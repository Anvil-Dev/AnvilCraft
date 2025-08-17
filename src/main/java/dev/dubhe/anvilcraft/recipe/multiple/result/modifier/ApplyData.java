package dev.dubhe.anvilcraft.recipe.multiple.result.modifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModResultModifierTypes;
import dev.dubhe.anvilcraft.recipe.multiple.result.ResultContext;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ItemLike;

public record ApplyData(DataComponentPatch patch) implements IResultModifier {
    public static final MapCodec<ApplyData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        DataComponentPatch.CODEC.fieldOf("components").forGetter(ApplyData::patch)
    ).apply(ins, ApplyData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyData> STREAM_CODEC = StreamCodec.composite(
        DataComponentPatch.STREAM_CODEC, ApplyData::patch,
        ApplyData::new
    );

    public static Builder of(ItemLike item) {
        return new Builder(item);
    }

    @Override
    public void modify(ResultContext ctx) {
        ctx.updateResult(this.patch);
    }

    @Override
    public Type type() {
        return ModResultModifierTypes.APPLY_DATA.get();
    }

    public static class Type implements IResultModifier.Type<ApplyData> {
        @Override
        public MapCodec<ApplyData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ApplyData> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends BaseBuilder<Builder> {
        private final PatchedDataComponentMap map;

        public Builder(ItemLike item) {
            this.map = new PatchedDataComponentMap(item.asItem().components());
        }

        public <T> Builder withData(DataComponentType<? super T> type, T value) {
            this.map.set(type, value);
            return this;
        }

        public ApplyData build() {
            return new ApplyData(this.map.asPatch());
        }

        @Override
        Builder getThis() {
            return this;
        }
    }
}
