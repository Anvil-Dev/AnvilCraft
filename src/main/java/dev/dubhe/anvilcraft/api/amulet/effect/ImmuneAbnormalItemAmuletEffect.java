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

/// 免疫异常物品带来的负面效果的护符效果
public record ImmuneAbnormalItemAmuletEffect() implements IAmuletEffect {
    private static final ImmuneAbnormalItemAmuletEffect INSTANCE = new ImmuneAbnormalItemAmuletEffect();

    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IMMUNE_ABNORMAL_ITEM, true);
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_ABNORMAL_ITEM.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneAbnormalItemAmuletEffect> {
        public static final MapCodec<ImmuneAbnormalItemAmuletEffect> CODEC = MapCodec.unit(ImmuneAbnormalItemAmuletEffect.INSTANCE);
        public static final StreamCodec<ByteBuf, ImmuneAbnormalItemAmuletEffect> STREAM_CODEC = StreamCodec.unit(
            ImmuneAbnormalItemAmuletEffect.INSTANCE
        );

        @Override
        public MapCodec<ImmuneAbnormalItemAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneAbnormalItemAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
