package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.emi.recipe.AnvilProcessEmiRecipe;

import net.minecraft.world.level.block.Blocks;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.NotNull;

@EmiEntrypoint
public class AnvilCraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(@NotNull EmiRegistry registry) {
        AnvilRecipeCategory.ALL.forEach(registry::addCategory);

        registry.addWorkstation(
                AnvilRecipeCategory.STAMPING, EmiStack.of(ModBlocks.STAMPING_PLATFORM.get()));
        registry.addWorkstation(AnvilRecipeCategory.STAMPING, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.STAMPING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.STAMPING, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.STAMPING, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.SIEVING, EmiStack.of(Blocks.SCAFFOLDING));
        registry.addWorkstation(AnvilRecipeCategory.SIEVING, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SIEVING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SIEVING, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SIEVING, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.BULGING, EmiStack.of(Blocks.WATER_CAULDRON));
        registry.addWorkstation(AnvilRecipeCategory.BULGING, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BULGING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BULGING, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BULGING, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.BULGING_LIKE, EmiStack.of(Blocks.CAULDRON));
        registry.addWorkstation(AnvilRecipeCategory.BULGING_LIKE, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BULGING_LIKE, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BULGING_LIKE, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BULGING_LIKE, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.FLUID_HANDLING, EmiStack.of(Blocks.WATER_CAULDRON));
        registry.addWorkstation(AnvilRecipeCategory.FLUID_HANDLING, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.FLUID_HANDLING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.FLUID_HANDLING, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.FLUID_HANDLING, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(
                AnvilRecipeCategory.CRYSTALLIZE, EmiStack.of(Blocks.POWDER_SNOW_CAULDRON));
        registry.addWorkstation(AnvilRecipeCategory.CRYSTALLIZE, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.CRYSTALLIZE, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.CRYSTALLIZE, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.CRYSTALLIZE, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.COMPACTION, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COMPACTION, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COMPACTION, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COMPACTION, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.COMPRESS, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COMPRESS, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COMPRESS, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COMPRESS, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.COOKING, EmiStack.of(Blocks.CAMPFIRE));
        registry.addWorkstation(AnvilRecipeCategory.COOKING, EmiStack.of(Blocks.SOUL_CAMPFIRE));
        registry.addWorkstation(AnvilRecipeCategory.COOKING, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COOKING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COOKING, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.COOKING, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.BOIL, EmiStack.of(Blocks.CAMPFIRE));
        registry.addWorkstation(AnvilRecipeCategory.BOIL, EmiStack.of(Blocks.SOUL_CAMPFIRE));
        registry.addWorkstation(AnvilRecipeCategory.BOIL, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BOIL, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BOIL, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BOIL, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.ITEM_INJECT, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_INJECT, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_INJECT, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_INJECT, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.BLOCK_SMASH, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BLOCK_SMASH, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BLOCK_SMASH, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.BLOCK_SMASH, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.ITEM_SMASH, EmiStack.of(Blocks.IRON_TRAPDOOR));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_SMASH, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_SMASH, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_SMASH, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.ITEM_SMASH, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.SQUEEZE, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SQUEEZE, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SQUEEZE, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SQUEEZE, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.SUPER_HEATING, EmiStack.of(Blocks.CAULDRON));
        registry.addWorkstation(AnvilRecipeCategory.SUPER_HEATING, EmiStack.of(ModBlocks.HEATER));
        registry.addWorkstation(AnvilRecipeCategory.SUPER_HEATING, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SUPER_HEATING, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SUPER_HEATING, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.SUPER_HEATING, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.TIMEWARP, EmiStack.of(ModBlocks.CORRUPTED_BEACON));
        registry.addWorkstation(AnvilRecipeCategory.TIMEWARP, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.TIMEWARP, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.TIMEWARP, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.TIMEWARP, EmiStack.of(Blocks.DAMAGED_ANVIL));

        registry.addWorkstation(AnvilRecipeCategory.GENERIC, EmiStack.of(ModBlocks.ROYAL_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.GENERIC, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.GENERIC, EmiStack.of(Blocks.CHIPPED_ANVIL));
        registry.addWorkstation(AnvilRecipeCategory.GENERIC, EmiStack.of(Blocks.DAMAGED_ANVIL));

        AnvilRecipeManager.getAnvilRecipeList()
                .forEach(recipe -> registry.addRecipe(new AnvilProcessEmiRecipe(
                        AnvilRecipeCategory.getByType(recipe.getAnvilRecipeType()), recipe)));
    }
}
