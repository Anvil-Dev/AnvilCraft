package dev.dubhe.anvilcraft.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;

public class ModFoods {
    public static final FoodProperties CHOCOLATE = new FoodProperties.Builder().nutrition(20).saturationMod(0.6f).alwaysEat().fast().build();
    public static final FoodProperties CHOCOLATE_BLACK = new FoodProperties.Builder().nutrition(20).saturationMod(0.6f).alwaysEat().fast().effect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200), 1.0f).build();
    public static final FoodProperties CHOCOLATE_WHITE = new FoodProperties.Builder().nutrition(20).saturationMod(0.6f).alwaysEat().fast().effect(new MobEffectInstance(MobEffects.JUMP, 1200), 1.0f).build();
    public static final FoodProperties CREAMY_BREAD_ROLL = new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).build();
    public static final FoodProperties MEATBALLS = new FoodProperties.Builder().nutrition(8).saturationMod(0.8f).meat().build();
    public static final FoodProperties DUMPLING = new FoodProperties.Builder().nutrition(6).saturationMod(0.8f).build();
    public static final FoodProperties SHENGJIAN = new FoodProperties.Builder().nutrition(8).saturationMod(0.8f).meat().build();
    public static final FoodProperties SWEET_DUMPLING = new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).meat().build();
    public static final FoodProperties BEEF_MUSHROOM_STEW = new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).effect(new MobEffectInstance(MobEffects.SATURATION, 1200), 1.0f).build();
}
