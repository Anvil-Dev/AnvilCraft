package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 在满足条件时免疫并移除指定药水效果的护符效果
public record ImmuneMobEffectAmuletEffect(HolderSet<MobEffect> immune, MobEffectCondition condition) implements IAmuletEffect {
    public static ImmuneMobEffectAmuletEffect of(Holder<MobEffect> effect, MobEffectCondition condition) {
        return new ImmuneMobEffectAmuletEffect(HolderSet.direct(effect), condition);
    }

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<MobEffectInstance> instance = ctx.get(ModAmuletEffectContextKeys.MOB_EFFECT);
        if (instance.isPresent()) {
            // 查询上下文里没有 ENABLED，该效果无从得知提供它的护符是否已被包覆者顶替；
            // 目前挂载本效果的护符都是「佩戴即生效」，所以这里直接写回免疫结果。
            if (this.immune.contains(instance.get().getEffect()) && this.condition.test(player)) {
                ctx.set(ModAmuletEffectContextKeys.IMMUNE_MOB_EFFECT, true);
            }
            return;
        }
        if (!ctx.get(ModAmuletEffectContextKeys.ENABLED).orElse(false) || !this.condition.test(player)) {
            return;
        }
        for (Holder<MobEffect> effect : this.immune) {
            player.removeEffect(effect);
        }
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_MOB_EFFECT.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneMobEffectAmuletEffect> {
        public static final MapCodec<ImmuneMobEffectAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT)
                .fieldOf("immune")
                .forGetter(ImmuneMobEffectAmuletEffect::immune),
            MobEffectCondition.CODEC
                .fieldOf("condition")
                .forGetter(ImmuneMobEffectAmuletEffect::condition)
        ).apply(inst, ImmuneMobEffectAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImmuneMobEffectAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.MOB_EFFECT),
            ImmuneMobEffectAmuletEffect::immune,
            StreamCodecUtil.enumStreamCodec(MobEffectCondition.class),
            ImmuneMobEffectAmuletEffect::condition,
            ImmuneMobEffectAmuletEffect::new
        );

        @Override
        public MapCodec<ImmuneMobEffectAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneMobEffectAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
