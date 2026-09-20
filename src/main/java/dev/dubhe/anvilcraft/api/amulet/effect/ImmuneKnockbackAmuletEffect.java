package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/// 免疫击退的护符效果
public record ImmuneKnockbackAmuletEffect() implements IAmuletEffect {
    private static final ImmuneKnockbackAmuletEffect INSTANCE = new ImmuneKnockbackAmuletEffect();

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IMMUNE_KNOCKBACK, true);
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_KNOCKBACK.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneKnockbackAmuletEffect> {
        public static final MapCodec<ImmuneKnockbackAmuletEffect> CODEC = MapCodec.unit(ImmuneKnockbackAmuletEffect.INSTANCE);
        public static final StreamCodec<ByteBuf, ImmuneKnockbackAmuletEffect> STREAM_CODEC = StreamCodec.unit(
            ImmuneKnockbackAmuletEffect.INSTANCE
        );

        @Override
        public MapCodec<ImmuneKnockbackAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneKnockbackAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
