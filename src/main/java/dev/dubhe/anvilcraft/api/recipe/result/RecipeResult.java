package dev.dubhe.anvilcraft.api.recipe.result;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.api.recipe.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.ApplyData;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.ChangeDataType;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.CopyData;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.IResultModifier;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.MergeData;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.ModifyCount;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.RemoveAttribute;
import dev.dubhe.anvilcraft.api.recipe.result.modifier.RemoveData;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record RecipeResult(Item result, @Unmodifiable List<IResultModifier> modifiers) {
    public static final MapCodec<RecipeResult> DIRECT_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        CodecUtil.ITEM_CODEC
            .fieldOf("result")
            .forGetter(RecipeResult::result),
        IResultModifier.CODEC
            .listOf()
            .optionalFieldOf("modifiers", List.of())
            .forGetter(RecipeResult::modifiers)
    ).apply(ins, RecipeResult::new));
    public static final Codec<RecipeResult> INLINE_CODEC = CodecUtil.ITEM_CODEC.xmap(RecipeResult::new, RecipeResult::result);
    public static final Codec<RecipeResult> CODEC = Codec.either(RecipeResult.DIRECT_CODEC.codec(), RecipeResult.INLINE_CODEC).xmap(
        Either::unwrap,
        result -> result.modifiers.isEmpty() ? Either.right(result) : Either.left(result)
    );
    public static final MapCodec<List<RecipeResult>> LIST_DIRECT_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        CodecUtil.ITEM_CODEC
            .listOf()
            .fieldOf("items")
            .forGetter(items -> Lists.transform(items, RecipeResult::result)),
        IResultModifier.CODEC
            .listOf()
            .optionalFieldOf("modifiers", List.of())
            .forGetter(items -> items.getFirst().modifiers())
    ).apply(ins, (items, modifiers) -> Lists.transform(items, item -> new RecipeResult(item, modifiers))));
    public static final Codec<List<RecipeResult>> LIST_CODEC = Codec
        .either(RecipeResult.LIST_DIRECT_CODEC.codec(), RecipeResult.CODEC.listOf())
        .xmap(
            Either::unwrap,
            items -> {
                if (items.isEmpty()) return Either.right(items);
                List<IResultModifier> modifiers = items.getFirst().modifiers();
                for (int i = 1, resultsSize = items.size(); i < resultsSize; i++) {
                    RecipeResult result = items.get(i);
                    if (!result.modifiers().equals(modifiers)) return Either.right(items);
                }
                return modifiers.isEmpty() ? Either.right(items) : Either.left(items);
            }
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeResult> STREAM_CODEC = StreamCodec.composite(
        CodecUtil.ITEM_STREAM_CODEC,
        RecipeResult::result,
        IResultModifier.STREAM_CODEC.apply(ByteBufCodecs.list()),
        RecipeResult::modifiers,
        RecipeResult::new
    );

    public RecipeResult(Item result) {
        this(result, List.of());
    }

    public RecipeResult(Item result, List<IResultModifier> modifiers) {
        this.result = result;
        this.modifiers = modifiers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RecipeResult.Builder simple(ItemLike result) {
        return RecipeResult.builder().result(result.asItem());
    }

    public ItemStack getResult(ResultContext ctx) {
        for (IResultModifier modifier : this.modifiers) {
            modifier.modify(ctx);
        }
        return ctx.getResult();
    }

    public static class Builder {
        private final ImmutableList.Builder<IResultModifier> modifiers = ImmutableList.builder();
        private Item result;

        public Builder result(Item result) {
            this.result = result;
            return this;
        }

        public Builder result(ItemLike result) {
            return this.result(result.asItem());
        }

        public Builder result(ItemLike result, int count) {
            return this.result(result).count(count);
        }

        public Builder result(ItemLike result, DataComponentPatch patch) {
            return this.result(result).withData(result, patch);
        }

        public Builder result(ItemLike result, int count, DataComponentPatch patch) {
            return this.result(result).count(count).withData(result, patch);
        }

        public Builder result(ItemStack result) {
            return this.result(result.getItem()).count(result.getCount()).withData(result.getItem(), result.getComponentsPatch());
        }

        public Builder count(ModifyCount.Builder count) {
            this.modifiers.add(count.build());
            return this;
        }

        public Builder count(int count) {
            if (count == 1) return this;
            return this.count(ModifyCount.of().count(count));
        }

        public Builder copyData(CopyData.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public Builder copyData(ICustomDataComponent<?>... data) {
            return this.copyData(CopyData.copyData(data));
        }

        public Builder mergeData(MergeData.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public Builder mergeData(ICustomDataComponent<?>... data) {
            return this.mergeData(MergeData.mergeData(data));
        }

        public Builder withData(ApplyData.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public Builder withData(ItemLike result, DataComponentPatch patch) {
            return this.withData(ApplyData.of(result).withData(patch));
        }

        public Builder removeData(RemoveData.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public Builder removeData(DataComponentType<?>... data) {
            return this.removeData(RemoveData.removeData(data));
        }

        public Builder removeAttribute(RemoveAttribute.Builder builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public Builder removeAttribute(ResourceLocation... attrs) {
            return this.removeAttribute(RemoveAttribute.removeAttr(attrs));
        }

        public <T> Builder changeDataType(ChangeDataType.Builder<T> builder) {
            this.modifiers.add(builder.build());
            return this;
        }

        public <T> Builder changeDataType(RecipeInputSlot slot, DataComponentType<T> orig, ICustomDataComponent<T> dest) {
            return this.changeDataType(ChangeDataType.changeDataType(slot, orig, dest));
        }

        public RecipeResult build() {
            return new RecipeResult(this.result, this.modifiers.build());
        }
    }
}
