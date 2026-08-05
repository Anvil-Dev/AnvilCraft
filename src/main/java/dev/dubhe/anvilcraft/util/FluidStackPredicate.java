package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public record FluidStackPredicate(
    Optional<HolderSet<Fluid>> fluids,
    Optional<DataComponentPredicate> component,
    Optional<MinMaxBounds.Ints> amount,
    boolean isNegate
) implements Predicate<FluidStack> {
    public static final Codec<FluidStackPredicate> INLINE_CODEC = RegistryCodecs.homogeneousList(Registries.FLUID).xmap(
        fluids -> new FluidStackPredicate(
            fluids.size() == 0 ? Optional.empty() : Optional.of(fluids),
            Optional.empty(),
            Optional.empty(),
            false
        ),
        predicate -> predicate.fluids().orElse(HolderSet.empty())
    );
    public static final Codec<FluidStackPredicate> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryCodecs.homogeneousList(Registries.FLUID).optionalFieldOf("fluids").forGetter(FluidStackPredicate::fluids),
        DataComponentPredicate.CODEC.optionalFieldOf("components").forGetter(FluidStackPredicate::component),
        MinMaxBounds.Ints.CODEC.optionalFieldOf("amount").forGetter(FluidStackPredicate::amount),
        Codec.BOOL.optionalFieldOf("negate", false).forGetter(FluidStackPredicate::isNegate)
    ).apply(instance, FluidStackPredicate::new));
    public static final Codec<FluidStackPredicate> CODEC = Codec.either(INLINE_CODEC, FULL_CODEC).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        predicate -> predicate.component().isEmpty() && predicate.amount().isEmpty() && !predicate.isNegate()
                     ? Either.left(predicate)
                     : Either.right(predicate)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackPredicate> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.FLUID)),
        FluidStackPredicate::fluids,
        ByteBufCodecs.optional(DataComponentPredicate.STREAM_CODEC),
        FluidStackPredicate::component,
        ByteBufCodecs.optional(StreamCodecUtil.MIN_MAX_BOUNDS_INTS),
        FluidStackPredicate::amount,
        ByteBufCodecs.BOOL,
        FluidStackPredicate::isNegate,
        FluidStackPredicate::new
    );
    public static final FluidStackPredicate ANY = FluidStackPredicate.builder().build();

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean test(FluidStack stack) {
        boolean falseValue = this.isNegate;
        if (this.fluids.isPresent() && !stack.is(this.fluids.get())) {
            return falseValue;
        }
        if (this.component.isPresent() && !this.component.get().test(stack)) {
            return falseValue;
        }
        if (this.amount.isPresent() && !this.amount.get().matches(stack.getAmount())) {
            return falseValue;
        }
        return !falseValue; // trueValue
    }

    @Override
    public FluidStackPredicate negate() {
        return new FluidStackPredicate(this.fluids, this.component, this.amount, !this.isNegate);
    }

    public static class Builder {
        private @Nullable HolderSet<Fluid> fluids;
        private @Nullable DataComponentPredicate.Builder component;
        private @Nullable MinMaxBounds.Ints amount;
        private boolean isNegate = false;

        @SuppressWarnings("deprecation")
        public Builder fluid(Fluid fluid) {
            this.fluids = HolderSet.direct(fluid.builtInRegistryHolder());
            return this;
        }

        @SuppressWarnings("deprecation")
        public Builder fluid(Fluid... fluids) {
            this.fluids = HolderSet.direct(Lists.transform(List.of(fluids), Fluid::builtInRegistryHolder));
            return this;
        }

        public Builder fluid(Holder<Fluid> fluid) {
            this.fluids = HolderSet.direct(fluid);
            return this;
        }

        @SafeVarargs
        public final Builder fluid(Holder<Fluid>... fluids) {
            this.fluids = HolderSet.direct(fluids);
            return this;
        }

        public Builder fluid(TagKey<Fluid> fluids) {
            this.fluids = BuiltInRegistries.FLUID.getOrCreateTag(fluids);
            return this;
        }

        public Builder component(Consumer<DataComponentPredicate.Builder> component) {
            this.component = DataComponentPredicate.builder();
            component.accept(this.component);
            return this;
        }

        public Builder min(int min) {
            this.amount = MinMaxBounds.Ints.atLeast(min);
            return this;
        }

        public Builder max(int max) {
            this.amount = MinMaxBounds.Ints.atMost(max);
            return this;
        }

        public Builder amount(int min, int max) {
            this.amount = MinMaxBounds.Ints.between(min, max);
            return this;
        }

        public Builder amount(int exactly) {
            this.amount = MinMaxBounds.Ints.exactly(exactly);
            return this;
        }

        public Builder negate() {
            this.isNegate = true;
            return this;
        }

        public FluidStackPredicate build() {
            return new FluidStackPredicate(
                Optional.ofNullable(this.fluids),
                this.component == null ? Optional.empty() : Optional.of(this.component.build()),
                Optional.ofNullable(this.amount),
                this.isNegate
            );
        }
    }
}
