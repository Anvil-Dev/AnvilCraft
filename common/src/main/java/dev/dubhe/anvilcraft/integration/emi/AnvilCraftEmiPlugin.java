package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeMap;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.emi.recipe.SievingEmiRecipes;
import dev.dubhe.anvilcraft.integration.emi.recipe.StampingEmiRecipes;
import dev.dubhe.anvilcraft.integration.emi.stack.BlockStateEmiStack;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@EmiEntrypoint
public class AnvilCraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        for (Block block : BuiltInRegistries.BLOCK) {
            EmiStack stack = new BlockStateEmiStack(block.defaultBlockState());
            registry.addEmiStack(stack);
        }
        AnvilCraftEmiRecipeTypes.ALL.forEach((id, category) -> registry.addCategory(category));

        registry.addWorkstation(AnvilCraftEmiRecipeTypes.STAMPING, EmiStack.of(ModBlocks.STAMPING_PLATFORM.get()));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.SIEVING, EmiStack.of(Blocks.SCAFFOLDING));

        RecipeManager manager = registry.getRecipeManager();
        for (AnvilRecipe anvilRecipe : manager.getAllRecipesFor(ModRecipeTypes.ANVIL_RECIPE)) {
            switch (anvilRecipe.getAnvilRecipeType()) {
                case STAMPING -> registry.addRecipe(new StampingEmiRecipes(anvilRecipe));
                case SIEVING -> registry.addRecipe(new SievingEmiRecipes(anvilRecipe));
            }
        }
    }
}
