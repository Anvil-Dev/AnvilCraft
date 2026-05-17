package dev.dubhe.anvilcraft.init.item;

import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties CHOCOLATE = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(4.0F)
        .alwaysEdible()
        .build();
    public static final FoodProperties CHOCOLATE_BLACK = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(2.0F)
        .alwaysEdible()
        .build();
    public static final FoodProperties CHOCOLATE_WHITE = new FoodProperties.Builder()
        .nutrition(2)
        .saturationModifier(2.0F)
        .alwaysEdible()
        .build();
    public static final FoodProperties CREAMY_BREAD_ROLL =
        new FoodProperties.Builder().nutrition(8).saturationModifier(0.5F).build();
    public static final FoodProperties BEEF_MUSHROOM_STEW =
        new FoodProperties.Builder().nutrition(10).saturationModifier(0.8F).build();
}
