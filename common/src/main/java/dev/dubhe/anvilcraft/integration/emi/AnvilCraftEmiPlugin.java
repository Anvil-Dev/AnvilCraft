package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemLeaves;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.emi.recipe.BaseBlockEmiRecipe;
import dev.dubhe.anvilcraft.integration.emi.recipe.BaseItemEmiRecipe;
import dev.dubhe.anvilcraft.integration.emi.recipe.CompactionEmiRecipe;
import dev.dubhe.anvilcraft.integration.emi.recipe.CookingEmiRecipe;
import dev.dubhe.anvilcraft.integration.emi.recipe.ItemInjectRecipe;
import dev.dubhe.anvilcraft.integration.emi.recipe.SievingEmiRecipe;
import dev.dubhe.anvilcraft.integration.emi.stack.BlockStateEmiStack;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
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
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.BULGING, EmiStack.of(Blocks.WATER_CAULDRON));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.BULGING_LIKE, EmiStack.of(Blocks.CAULDRON));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.FLUID_HANDLING, EmiStack.of(Blocks.WATER_CAULDRON));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.CRYSTALLIZE, EmiStack.of(Blocks.POWDER_SNOW_CAULDRON));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.COMPACTION, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.COMPRESS, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.COOKING, EmiStack.of(Blocks.CAMPFIRE));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.BOIL, EmiStack.of(Blocks.CAMPFIRE));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.ITEM_INJECT, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.BLOCK_SMASH, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.ITEM_SMASH, EmiStack.of(Blocks.IRON_TRAPDOOR));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.SQUEEZE, EmiStack.of(Blocks.ANVIL));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.SUPER_HEATING, EmiStack.of(ModBlocks.HEATER));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.TIMEWARP, EmiStack.of(ModBlocks.CORRUPTED_BEACON));
        registry.addWorkstation(AnvilCraftEmiRecipeTypes.GENERIC, EmiStack.of(Blocks.ANVIL));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.SIEVING)
                .forEach(anvilRecipe -> {
                    if (anvilRecipe.getPredicates().stream().noneMatch(predicate -> predicate instanceof HasItemLeaves))
                        registry.addRecipe(new SievingEmiRecipe(AnvilCraftEmiRecipeTypes.SIEVING, anvilRecipe));
                });
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.STAMPING)
                .forEach(anvilRecipe -> 
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.STAMPING, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.BULGING)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.BULGING, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.BULGING_LIKE)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.BULGING_LIKE, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.FLUID_HANDLING)
                .forEach(anvilRecipe ->
                        registry.addRecipe(
                                new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.FLUID_HANDLING, anvilRecipe)
                        ));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.CRYSTALLIZE)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.CRYSTALLIZE, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.COMPACTION)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new CompactionEmiRecipe(AnvilCraftEmiRecipeTypes.COMPACTION, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.COMPRESS)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.COMPRESS, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.COOKING)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new CookingEmiRecipe(AnvilCraftEmiRecipeTypes.COOKING, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.BOIL)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.BOIL, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.ITEM_INJECT)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new ItemInjectRecipe(AnvilCraftEmiRecipeTypes.ITEM_INJECT, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.BLOCK_SMASH)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseBlockEmiRecipe(AnvilCraftEmiRecipeTypes.BLOCK_SMASH, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.ITEM_SMASH)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.ITEM_SMASH, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.SQUEEZE)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.SQUEEZE, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.SUPER_HEATING)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.SUPER_HEATING, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.TIMEWARP)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.TIMEWARP, anvilRecipe)));
        AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(anvilRecipe1 -> anvilRecipe1.getAnvilRecipeType() == AnvilRecipeType.GENERIC)
                .forEach(anvilRecipe ->
                        registry.addRecipe(new BaseItemEmiRecipe(AnvilCraftEmiRecipeTypes.GENERIC, anvilRecipe)));
    }
}
