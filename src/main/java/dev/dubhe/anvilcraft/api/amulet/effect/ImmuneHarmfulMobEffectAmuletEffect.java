package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/// 进食时免疫负面药水效果的护符效果
public record ImmuneHarmfulMobEffectAmuletEffect() implements IAmuletEffect {
    private static final ImmuneHarmfulMobEffectAmuletEffect INSTANCE = new ImmuneHarmfulMobEffectAmuletEffect();

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        if (!ctx.get(ModAmuletEffectContextKeys.CONSUMING_FOOD).orElse(false)) {
            return;
        }
        ctx.get(ModAmuletEffectContextKeys.MOB_EFFECT)
            .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
            .ifPresent(effect -> ctx.set(ModAmuletEffectContextKeys.IMMUNE_MOB_EFFECT, true));
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_HARMFUL_MOB_EFFECT.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneHarmfulMobEffectAmuletEffect> {
        public static final MapCodec<ImmuneHarmfulMobEffectAmuletEffect> CODEC = MapCodec.unit(ImmuneHarmfulMobEffectAmuletEffect.INSTANCE);
        public static final StreamCodec<ByteBuf, ImmuneHarmfulMobEffectAmuletEffect> STREAM_CODEC = StreamCodec.unit(
            ImmuneHarmfulMobEffectAmuletEffect.INSTANCE
        );

        @Override
        public MapCodec<ImmuneHarmfulMobEffectAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneHarmfulMobEffectAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
