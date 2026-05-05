package dev.dubhe.anvilcraft.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;

public class ModFoods {
    public static final FoodProperties CHOCOLATE = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(4.0F)
        .alwaysEdible()
        .fast()
        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 3), 1.0F)
        .build();
    public static final FoodProperties CHOCOLATE_BLACK = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(2.0F)
        .alwaysEdible()
        .fast()
        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1), 1.0F)
        .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 600, 2), 1.0F)
        .build();
    public static final FoodProperties CHOCOLATE_WHITE = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(2.0F)
        .alwaysEdible()
        .fast()
        .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1), 1.0F)
        .effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 3), 1.0F)
        .build();
    public static final FoodProperties CREAMY_BREAD_ROLL =
        new FoodProperties.Builder().nutrition(8).saturationModifier(0.5F).build();
    public static final FoodProperties BEEF_MUSHROOM_STEW =
        new FoodProperties.Builder().nutrition(10).saturationModifier(0.8F).usingConvertsTo(Items.BOWL).build();
}
