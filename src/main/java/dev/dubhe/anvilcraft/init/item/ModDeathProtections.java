package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.init.ModMobEffects;
import dev.dubhe.anvilcraft.item.property.consume.PreventShrinkingConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.SetFoodLevelConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.TeleportToRespawnPointConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.TryTotemsInBoxConsumeEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;

import java.util.List;

public class ModDeathProtections {
    public static final DeathProtection TOTEM_OF_RECOVERY = new DeathProtection(List.of(
        TeleportToRespawnPointConsumeEffect.INSTANCE,
        ClearAllStatusEffectsConsumeEffect.INSTANCE,
        new ApplyStatusEffectsConsumeEffect(List.of(
            new MobEffectInstance(MobEffects.REGENERATION, 900, 1),
            new MobEffectInstance(MobEffects.ABSORPTION, 100, 1),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)
        ))
    ));

    public static final DeathProtection TOTEM_OF_RAGE = new DeathProtection(List.of(
        new SetFoodLevelConsumeEffect(20),
        ClearAllStatusEffectsConsumeEffect.INSTANCE,
        new ApplyStatusEffectsConsumeEffect(List.of(
            new MobEffectInstance(MobEffects.REGENERATION, 900, 1),
            new MobEffectInstance(MobEffects.ABSORPTION, 100, 1),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0),
            new MobEffectInstance(ModMobEffects.INVULNERABLE, 1200, 0),
            new MobEffectInstance(ModMobEffects.RAGE, 1200, 0)
        ))
    ));

    public static final DeathProtection AMULET_BOX = new DeathProtection(List.of(
        TryTotemsInBoxConsumeEffect.INSTANCE,
        PreventShrinkingConsumeEffect.INSTANCE
    ));
}
