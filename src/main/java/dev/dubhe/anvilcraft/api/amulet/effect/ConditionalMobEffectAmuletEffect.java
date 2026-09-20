package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/// 在满足条件时持续刷新药水效果的护符效果
public record ConditionalMobEffectAmuletEffect(
    Holder<MobEffect> effect,
    int amplifier,
    int duration,
    MobEffectCondition condition
) implements IAmuletEffect {
    public static final int REFRESH_DURATION = 2;

    public static ConditionalMobEffectAmuletEffect refresh(Holder<MobEffect> effect, int amplifier, MobEffectCondition condition) {
        return new ConditionalMobEffectAmuletEffect(effect, amplifier, ConditionalMobEffectAmuletEffect.REFRESH_DURATION, condition);
    }

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        if (!ctx.get(ModAmuletEffectContextKeys.ENABLED).orElse(false) || !this.condition.test(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(this.effect, this.duration, this.amplifier, false, false, true));
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.CONDITIONAL_MOB_EFFECT.get();
    }

    public static class Type implements IAmuletEffect.Type<ConditionalMobEffectAmuletEffect> {
        public static final MapCodec<ConditionalMobEffectAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.MOB_EFFECT
                .holderByNameCodec()
                .fieldOf("effect")
                .forGetter(ConditionalMobEffectAmuletEffect::effect),
            Codec.INT
                .fieldOf("amplifier")
                .forGetter(ConditionalMobEffectAmuletEffect::amplifier),
            Codec.INT
                .optionalFieldOf("duration", ConditionalMobEffectAmuletEffect.REFRESH_DURATION)
                .forGetter(ConditionalMobEffectAmuletEffect::duration),
            MobEffectCondition.CODEC
                .fieldOf("condition")
                .forGetter(ConditionalMobEffectAmuletEffect::condition)
        ).apply(inst, ConditionalMobEffectAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConditionalMobEffectAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT),
            ConditionalMobEffectAmuletEffect::effect,
            ByteBufCodecs.VAR_INT,
            ConditionalMobEffectAmuletEffect::amplifier,
            ByteBufCodecs.VAR_INT,
            ConditionalMobEffectAmuletEffect::duration,
            StreamCodecUtil.enumStreamCodec(MobEffectCondition.class),
            ConditionalMobEffectAmuletEffect::condition,
            ConditionalMobEffectAmuletEffect::new
        );

        @Override
        public MapCodec<ConditionalMobEffectAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConditionalMobEffectAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
