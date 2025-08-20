package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;

@Getter
@AllArgsConstructor
public class InWorldRecipeEvent extends Event {
    private final RecipeType<? extends InWorldRecipe> recipeType;
    private final ResourceLocation id;
    private final InWorldRecipe recipe;
    private final InWorldRecipeContext context;
}
