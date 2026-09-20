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

/// 无视天体引力的护符效果
public record IgnoreGravityAmuletEffect() implements IAmuletEffect {
    private static final IgnoreGravityAmuletEffect INSTANCE = new IgnoreGravityAmuletEffect();

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IGNORE_GRAVITY, true);
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IGNORE_GRAVITY.get();
    }

    public static class Type implements IAmuletEffect.Type<IgnoreGravityAmuletEffect> {
        public static final MapCodec<IgnoreGravityAmuletEffect> CODEC = MapCodec.unit(IgnoreGravityAmuletEffect.INSTANCE);
        public static final StreamCodec<ByteBuf, IgnoreGravityAmuletEffect> STREAM_CODEC = StreamCodec.unit(
            IgnoreGravityAmuletEffect.INSTANCE
        );

        @Override
        public MapCodec<IgnoreGravityAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, IgnoreGravityAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
