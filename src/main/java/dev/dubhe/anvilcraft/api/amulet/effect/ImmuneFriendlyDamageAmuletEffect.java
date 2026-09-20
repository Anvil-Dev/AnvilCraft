package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 免疫友军伤害的护符效果
public record ImmuneFriendlyDamageAmuletEffect() implements IImmuneDamageAmuletEffect {
    private static final ImmuneFriendlyDamageAmuletEffect INSTANCE = new ImmuneFriendlyDamageAmuletEffect();

    @Override
    public boolean shouldImmune(Player player, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        Comrades comrades = amulet.getOrDefault(ModComponents.COMRADES, Comrades.EMPTY);
        return ImmuneFriendlyDamageAmuletEffect.isMurdererComrade(amulet, source, comrades);
    }

    private static boolean isMurdererComrade(ItemStack amulet, DamageSource source, Comrades comrades) {
        return Optional.ofNullable(source.getEntity())
            .flatMap(entity -> Util.castSafely(entity, Player.class))
            .map(murderer -> murderer.getGameProfile().getId())
            .filter(id -> !amulet.isEmpty() && comrades.contains(id))
            .isPresent();
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_FRIENDLY_DAMAGE.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneFriendlyDamageAmuletEffect> {
        public static final MapCodec<ImmuneFriendlyDamageAmuletEffect> CODEC = MapCodec.unit(ImmuneFriendlyDamageAmuletEffect.INSTANCE);
        public static final StreamCodec<ByteBuf, ImmuneFriendlyDamageAmuletEffect> STREAM_CODEC = StreamCodec.unit(
            ImmuneFriendlyDamageAmuletEffect.INSTANCE
        );

        @Override
        public MapCodec<ImmuneFriendlyDamageAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneFriendlyDamageAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
