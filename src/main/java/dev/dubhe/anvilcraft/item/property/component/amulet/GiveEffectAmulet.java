package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import net.minecraft.advancements.criterion.EntityFlagsPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.FluidPredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record GiveEffectAmulet(
    Optional<EntityPredicate> predicate,
    List<MobEffectInstance> effects,
    Optional<MinMaxBounds.Ints> bounds
) implements IAmulet {
    public GiveEffectAmulet(EntityPredicate predicate, List<MobEffectInstance> effects, MinMaxBounds.Ints bounds) {
        this(Optional.of(predicate), effects, Optional.of(bounds));
    }

    public GiveEffectAmulet(List<MobEffectInstance> effects, MinMaxBounds.Ints bounds) {
        this(Optional.empty(), effects, Optional.of(bounds));
    }

    public GiveEffectAmulet(EntityPredicate predicate, List<MobEffectInstance> effects) {
        this(Optional.of(predicate), effects, Optional.empty());
    }

    public GiveEffectAmulet(List<MobEffectInstance> effects) {
        this(Optional.empty(), effects, Optional.empty());
    }

    public GiveEffectAmulet(EntityPredicate predicate, MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        this(Optional.of(predicate), Collections.singletonList(effect), Optional.of(bounds));
    }

    public GiveEffectAmulet(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        this(Optional.empty(), Collections.singletonList(effect), Optional.of(bounds));
    }

    public GiveEffectAmulet(EntityPredicate predicate, MobEffectInstance effect) {
        this(Optional.of(predicate), Collections.singletonList(effect), Optional.empty());
    }

    public GiveEffectAmulet(MobEffectInstance effect) {
        this(Optional.empty(), Collections.singletonList(effect), Optional.empty());
    }

    public static GiveEffectAmulet inWater(List<MobEffectInstance> effects) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsInWater(true)).build(),
            effects
        );
    }

    public static GiveEffectAmulet inWater(List<MobEffectInstance> effects, MinMaxBounds.Ints bounds) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsInWater(true)).build(),
            effects,
            bounds
        );
    }

    public static GiveEffectAmulet inWater(MobEffectInstance effect) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsInWater(true)).build(),
            effect
        );
    }

    public static GiveEffectAmulet inWater(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsInWater(true)).build(),
            effect,
            bounds
        );
    }

    public static GiveEffectAmulet inLava(List<MobEffectInstance> effects) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity()
                .located(LocationPredicate.Builder.location().setFluid(FluidPredicate.Builder.fluid().of(Fluids.LAVA)))
                .build(),
            effects
        );
    }

    public static GiveEffectAmulet inLava(List<MobEffectInstance> effects, MinMaxBounds.Ints bounds) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity()
                .located(LocationPredicate.Builder.location().setFluid(FluidPredicate.Builder.fluid().of(Fluids.LAVA)))
                .build(),
            effects,
            bounds
        );
    }

    public static GiveEffectAmulet inLava(MobEffectInstance effect) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity()
                .located(LocationPredicate.Builder.location().setFluid(FluidPredicate.Builder.fluid().of(Fluids.LAVA)))
                .build(),
            effect
        );
    }

    public static GiveEffectAmulet inLava(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        return new GiveEffectAmulet(
            EntityPredicate.Builder.entity()
                .located(LocationPredicate.Builder.location().setFluid(FluidPredicate.Builder.fluid().of(Fluids.LAVA)))
                .build(),
            effect,
            bounds
        );
    }

    @Override
    public void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
        if (this.predicate.isPresent() && !this.predicate.get().matches(player, player)) {
            return;
        }
        for (MobEffectInstance effect : this.effects) {
            Holder<MobEffect> type = effect.getEffect();
            MobEffectInstance exist = player.getEffect(type);
            if (exist == null) {
                player.addEffect(new MobEffectInstance(
                    type,
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible()
                ));
            } else if (this.bounds.isEmpty() || this.bounds.get().matches(exist.getDuration())) {
                player.addEffect(new MobEffectInstance(
                    type,
                    exist.getDuration() + effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible()
                ));
            }
        }
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.GIVE_EFFECT.get();
    }

    public static class Type implements IAmulet.Type<GiveEffectAmulet> {
        public static final MapCodec<GiveEffectAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            EntityPredicate.CODEC
                .optionalFieldOf("predicate")
                .forGetter(GiveEffectAmulet::predicate),
            MobEffectInstance.CODEC
                .listOf()
                .fieldOf("effects")
                .forGetter(GiveEffectAmulet::effects),
            MinMaxBounds.Ints.CODEC
                .optionalFieldOf("bounds")
                .forGetter(GiveEffectAmulet::bounds)
        ).apply(inst, GiveEffectAmulet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, GiveEffectAmulet> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(StreamCodecUtil.ENTITY_PREDICATE),
            GiveEffectAmulet::predicate,
            MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
            GiveEffectAmulet::effects,
            ByteBufCodecs.optional(MinMaxBounds.Ints.STREAM_CODEC),
            GiveEffectAmulet::bounds,
            GiveEffectAmulet::new
        );

        @Override
        public MapCodec<GiveEffectAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GiveEffectAmulet> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
