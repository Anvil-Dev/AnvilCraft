package dev.dubhe.anvilcraft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class TerminalUnbindRecipe extends ShapelessRecipe {
    public static final RecipeSerializer<ShapelessRecipe> SERIALIZER = new RecipeSerializer<>(
        ShapelessRecipe.MAP_CODEC.<ShapelessRecipe>xmap(TerminalUnbindRecipe::new, recipe -> recipe),
        ShapelessRecipe.STREAM_CODEC.<ShapelessRecipe>map(TerminalUnbindRecipe::new, recipe -> recipe)
    );

    public TerminalUnbindRecipe(ShapelessRecipe recipe) {
        super(new CommonInfo(recipe.showNotification()), new CraftingBookInfo(recipe.category(), recipe.group()),
            recipe.result, recipe.placementInfo().ingredients());
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return TerminalUpgradeRecipe.preserveContents(super.assemble(input), input);
    }
}
