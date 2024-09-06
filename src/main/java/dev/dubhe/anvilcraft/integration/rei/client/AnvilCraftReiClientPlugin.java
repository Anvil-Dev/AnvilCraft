package dev.dubhe.anvilcraft.integration.rei.client;

import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.rei.AnvilCraftCategoryIdentifiers;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class AnvilCraftReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(@NotNull CategoryRegistry registry) {
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.STAMPING));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.SIEVING));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.BULGING));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.BULGING_LIKE));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.FLUID_HANDLING));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.CRYSTALLIZE));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.COMPACTION));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.COMPRESS));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.COOKING));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.ITEM_INJECT));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.BLOCK_SMASH));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.ITEM_SMASH));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.SQUEEZE));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.SUPER_HEATING));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.TIMEWARP));
        registry.add(new AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifiers.GENERIC));
        
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.STAMPING.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.STAMPING_PLATFORM.get()),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.SIEVING.getCategoryIdentifier(),
                EntryStacks.of(Blocks.SCAFFOLDING),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.BULGING.getCategoryIdentifier(),
                EntryStacks.of(Blocks.WATER_CAULDRON),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.BULGING_LIKE.getCategoryIdentifier(),
                EntryStacks.of(Blocks.CAULDRON),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.FLUID_HANDLING.getCategoryIdentifier(),
                EntryStacks.of(Blocks.WATER_CAULDRON),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.CRYSTALLIZE.getCategoryIdentifier(),
                EntryStacks.of(Blocks.POWDER_SNOW_CAULDRON),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.COMPACTION.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.COMPRESS.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.COOKING.getCategoryIdentifier(),
                EntryStacks.of(Blocks.CAMPFIRE),
                EntryStacks.of(Blocks.SOUL_CAMPFIRE),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.ITEM_INJECT.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.BLOCK_SMASH.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.ITEM_SMASH.getCategoryIdentifier(),
                EntryStacks.of(Blocks.IRON_TRAPDOOR),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.SQUEEZE.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.SUPER_HEATING.getCategoryIdentifier(),
                EntryStacks.of(Blocks.CAULDRON),
                EntryStacks.of(ModBlocks.HEATER),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.TIMEWARP.getCategoryIdentifier(),
                EntryStacks.of(ModBlocks.CORRUPTED_BEACON),
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
        registry.addWorkstations(AnvilCraftCategoryIdentifiers.GENERIC.getCategoryIdentifier(), 
                EntryStacks.of(ModBlocks.ROYAL_ANVIL),
                EntryStacks.of(Blocks.ANVIL),
                EntryStacks.of(Blocks.CHIPPED_ANVIL),
                EntryStacks.of(Blocks.DAMAGED_ANVIL));
    }

    @Override
    public void registerScreens(@NotNull ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new GhostIngredientHandler<>());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        AnvilRecipeManager.getAnvilRecipeList()
                .stream()
                .filter(recipe -> recipe.getAnvilRecipeType() != AnvilRecipeType.MULTIBLOCK_CRAFTING)
                .forEach(recipe ->
                        registry.add(AnvilRecipeDisplay.of(
                                AnvilCraftCategoryIdentifiers.get(recipe.getAnvilRecipeType()),
                                recipe)));
    }
}
