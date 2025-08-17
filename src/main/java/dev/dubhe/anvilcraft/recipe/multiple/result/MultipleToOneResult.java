package dev.dubhe.anvilcraft.recipe.multiple.result;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.dubhe.anvilcraft.api.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.ApplyData;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.CopyData;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.IResultModifier;
import lombok.Getter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

@Getter
public class MultipleToOneResult {
    public static final Codec<MultipleToOneResult> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        ItemStack.CODEC.fieldOf("item").forGetter(MultipleToOneResult::getResult),
        IResultModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(MultipleToOneResult::getModifiers)
    ).apply(ins, MultipleToOneResult::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MultipleToOneResult> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC, MultipleToOneResult::getResult,
        IResultModifier.STREAM_CODEC.apply(ByteBufCodecs.list()), MultipleToOneResult::getModifiers,
        MultipleToOneResult::new
    );
    private final ItemStack result;
    private final List<IResultModifier> modifiers;

    private MultipleToOneResult(ItemStack result, List<IResultModifier> modifiers) {
        this.result = result;
        this.modifiers = modifiers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ItemStack getResult(ResultContext ctx) {
        for (IResultModifier modifier : this.modifiers) {
            modifier.modify(ctx);
        }
        return ctx.getResult();
    }

    public static class Builder {
        private final ImmutableList.Builder<IResultModifier> modifiers = ImmutableList.builder();
        private ItemStack result;

        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }

        public Builder result(ItemProviderEntry<?, ?> result, int count) {
            return this.result(result.asStack(count));
        }

        public Builder result(ItemProviderEntry<?, ?> result) {
            return this.result(result.asStack());
        }

        @SuppressWarnings("deprecation")
        public Builder result(ItemLike result, int count, DataComponentPatch patch) {
            return this.result(new ItemStack(result.asItem().builtInRegistryHolder(), count, patch));
        }

        public Builder result(ItemLike result, int count) {
            return this.result(new ItemStack(result, count));
        }

        public Builder result(ItemLike result, DataComponentPatch patch) {
            this.result = new ItemStack(result);
            this.result.applyComponents(patch);
            return this;
        }

        public Builder result(ItemLike result) {
            return this.result(new ItemStack(result));
        }

        public Builder copyData(CopyData.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public Builder copyData(ICustomDataComponent<?>... data) {
            return this.copyData(CopyData.copyData(data));
        }

        public Builder withData(ApplyData.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public MultipleToOneResult build() {
            return new MultipleToOneResult(this.result, this.modifiers.build());
        }
    }
}
