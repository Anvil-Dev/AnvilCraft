package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.FluidPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Optional;

/// 在未满足给定条件时给予佩戴者药水效果。<br>
/// 效果时长会累加，并由 {@link MinMaxBounds.Ints 时长上下界} 限制。
public record GiveEffectAmuletEffect(
    Optional<EntityPredicate> predicate,
    List<Entry> effects
) implements IAmuletEffect {
    public static GiveEffectAmuletEffect inWater(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        return new GiveEffectAmuletEffect(
            Optional.of(GiveEffectAmuletEffect.fluidPredicate(Fluids.WATER)),
            List.of(new Entry(effect, Optional.of(bounds)))
        );
    }

    public static GiveEffectAmuletEffect inLava(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        return new GiveEffectAmuletEffect(
            Optional.of(GiveEffectAmuletEffect.fluidPredicate(Fluids.LAVA)),
            List.of(new Entry(effect, Optional.of(bounds)))
        );
    }

    private static EntityPredicate fluidPredicate(Fluid fluid) {
        return EntityPredicate.Builder.entity()
            .located(LocationPredicate.Builder.location().setFluid(FluidPredicate.Builder.fluid().of(fluid)))
            .build();
    }

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        if (!(player instanceof ServerPlayer serverPlayer) || !ctx.get(ModAmuletEffectContextKeys.ENABLED).orElse(false)) {
            return;
        }
        if (this.predicate.isPresent() && this.predicate.get().matches(serverPlayer, serverPlayer)) {
            return;
        }
        for (Entry entry : this.effects) {
            MobEffectInstance effect = entry.effect();
            Optional<MinMaxBounds.Ints> bounds = entry.bounds();
            Holder<MobEffect> type = effect.getEffect();
            MobEffectInstance exist = serverPlayer.getEffect(type);
            if (exist == null) {
                serverPlayer.addEffect(new MobEffectInstance(
                    type,
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible()
                ));
            } else if (bounds.isEmpty() || bounds.get().matches(exist.getDuration())) {
                serverPlayer.addEffect(new MobEffectInstance(
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
        return ModAmuletEffectTypes.GIVE_EFFECT.get();
    }

    public record Entry(MobEffectInstance effect, Optional<MinMaxBounds.Ints> bounds) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            MobEffectInstance.CODEC
                .fieldOf("effect")
                .forGetter(Entry::effect),
            MinMaxBounds.Ints.CODEC
                .optionalFieldOf("bounds")
                .forGetter(Entry::bounds)
        ).apply(inst, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            MobEffectInstance.STREAM_CODEC,
            Entry::effect,
            ByteBufCodecs.optional(StreamCodecUtil.MIN_MAX_BOUNDS_INTS),
            Entry::bounds,
            Entry::new
        );
    }

    public static class Type implements IAmuletEffect.Type<GiveEffectAmuletEffect> {
        public static final MapCodec<GiveEffectAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            EntityPredicate.CODEC
                .optionalFieldOf("predicate")
                .forGetter(GiveEffectAmuletEffect::predicate),
            Entry.CODEC
                .listOf()
                .fieldOf("effects")
                .forGetter(GiveEffectAmuletEffect::effects)
        ).apply(inst, GiveEffectAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, GiveEffectAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(StreamCodecUtil.ENTITY_PREDICATE),
            GiveEffectAmuletEffect::predicate,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            GiveEffectAmuletEffect::effects,
            GiveEffectAmuletEffect::new
        );

        @Override
        public MapCodec<GiveEffectAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GiveEffectAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
