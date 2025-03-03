package dev.dubhe.anvilcraft.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;

public class ModFoods {
    public static final FoodProperties CHOCOLATE = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(4.0f)
        .alwaysEdible()
        .fast()
        .build();
    public static final FoodProperties CHOCOLATE_BLACK = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(2.0f)
        .alwaysEdible()
        .fast()
        .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1), 1.0f)
        .build();
    public static final FoodProperties CHOCOLATE_WHITE = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(2.0f)
        .alwaysEdible()
        .fast()
        .effect(new MobEffectInstance(MobEffects.JUMP, 600, 1), 1.0f)
        .build();
    public static final FoodProperties CREAMY_BREAD_ROLL =
        new FoodProperties.Builder().nutrition(8).saturationModifier(0.5f).build();
    public static final FoodProperties BEEF_MUSHROOM_STEW =
        new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f).usingConvertsTo(Items.BOWL).build();
}
