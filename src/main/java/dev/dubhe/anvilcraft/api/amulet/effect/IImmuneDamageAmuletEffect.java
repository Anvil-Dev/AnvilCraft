package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 免疫伤害的护符效果。<br>
/// 同一护符上的多个免疫效果之间为“或”关系。
public interface IImmuneDamageAmuletEffect extends IAmuletEffect {
    @Override
    default void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<DamageSource> sourceOp = ctx.get(ModAmuletEffectContextKeys.DAMAGE_SOURCE);
        if (sourceOp.isEmpty()) {
            return;
        }
        boolean immune = ctx.get(ModAmuletEffectContextKeys.IMMUNE_DAMAGE).orElse(false)
                         || this.shouldImmune(player, amulet, sourceOp.get(), ctx);
        ctx.set(ModAmuletEffectContextKeys.IMMUNE_DAMAGE, immune);
    }

    boolean shouldImmune(Player player, ItemStack amulet, DamageSource source, AmuletEffectContext ctx);
}
