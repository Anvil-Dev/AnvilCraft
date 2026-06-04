package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.display.recipe.CanningFoodRecipeDisplay;
import dev.dubhe.anvilcraft.recipe.display.recipe.PillRecipeDisplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModRecipeDisplays {
    @SubscribeEvent
    public static void on(RegisterEvent event) {
        ModRecipeDisplays.register(event, "canning_food", CanningFoodRecipeDisplay.TYPE);
        ModRecipeDisplays.register(event, "pill", PillRecipeDisplay.TYPE);
    }

    private static void register(RegisterEvent event, String name, RecipeDisplay.Type<?> type) {
        event.register(Registries.RECIPE_DISPLAY, AnvilCraft.of(name), () -> type);
    }
}
