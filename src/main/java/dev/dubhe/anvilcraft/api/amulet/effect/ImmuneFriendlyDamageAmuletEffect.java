package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 免疫友军伤害的护符效果
public record ImmuneFriendlyDamageAmuletEffect() implements IImmuneDamageAmuletEffect {
    public static final ImmuneFriendlyDamageAmuletEffect INSTANCE = new ImmuneFriendlyDamageAmuletEffect();

    @Override
    public boolean shouldImmune(LivingEntity entity, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
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
}
