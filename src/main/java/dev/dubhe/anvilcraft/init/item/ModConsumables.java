package dev.dubhe.anvilcraft.init.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;

public class ModConsumables {
    public static final Consumable FAST_FOOD = ModConsumables.fastFood().build();
    public static final Consumable INSTANT_FOOD = ModConsumables.instantFood().build();
    public static final Consumable CHOCOLATE = ModConsumables.fastFood()
        .onConsume(new ApplyStatusEffectsConsumeEffect(
            new MobEffectInstance(MobEffects.SPEED, 600, 3)
        ))
        .build();
    public static final Consumable CHOCOLATE_BLACK = ModConsumables.fastFood()
        .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
            new MobEffectInstance(MobEffects.SPEED, 600, 3),
            new MobEffectInstance(MobEffects.HASTE, 600, 2)
        )))
        .build();
    public static final Consumable CHOCOLATE_WHITE = ModConsumables.fastFood()
        .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
            new MobEffectInstance(MobEffects.SPEED, 600, 3),
            new MobEffectInstance(MobEffects.JUMP_BOOST, 600, 3)
        )))
        .build();

    public static Consumable.Builder fastFood() {
        return Consumables.defaultFood().consumeSeconds(0.8F);
    }

    public static Consumable.Builder instantFood() {
        return Consumables.defaultFood().consumeSeconds(0F);
    }
}
