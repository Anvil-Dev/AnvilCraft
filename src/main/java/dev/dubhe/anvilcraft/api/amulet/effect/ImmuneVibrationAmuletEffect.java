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

/// 免疫振动的护符效果
public record ImmuneVibrationAmuletEffect() implements IAmuletEffect {
    private static final ImmuneVibrationAmuletEffect INSTANCE = new ImmuneVibrationAmuletEffect();

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IMMUNE_VIBRATION, true);
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_VIBRATION.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneVibrationAmuletEffect> {
        public static final MapCodec<ImmuneVibrationAmuletEffect> CODEC = MapCodec.unit(ImmuneVibrationAmuletEffect.INSTANCE);
        public static final StreamCodec<ByteBuf, ImmuneVibrationAmuletEffect> STREAM_CODEC = StreamCodec.unit(
            ImmuneVibrationAmuletEffect.INSTANCE
        );

        @Override
        public MapCodec<ImmuneVibrationAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneVibrationAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
