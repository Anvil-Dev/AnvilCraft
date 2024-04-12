package dev.dubhe.anvilcraft.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties CHOCOLATE = new FoodProperties.Builder()
        .nutrition(2).saturationMod(10.0f).alwaysEat().fast().build();
    public static final FoodProperties CHOCOLATE_BLACK = new FoodProperties.Builder()
        .nutrition(2).saturationMod(2.0f).alwaysEat().fast()
        .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200), 1.0f).build();
    public static final FoodProperties CHOCOLATE_WHITE = new FoodProperties.Builder()
        .nutrition(2).saturationMod(2.0f).alwaysEat().fast()
        .effect(new MobEffectInstance(MobEffects.JUMP, 1200), 1.0f).build();
    public static final FoodProperties CREAMY_BREAD_ROLL = new FoodProperties.Builder()
        .nutrition(8).saturationMod(0.5f).build();
    public static final FoodProperties BEEF_MUSHROOM_STEW = new FoodProperties.Builder()
        .nutrition(10).saturationMod(0.8f)
        .effect(new MobEffectInstance(MobEffects.SATURATION, 100), 1.0f).build();
}
