package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ItemInjectRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .requires(Items.RAW_COPPER_BLOCK, 3)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(Blocks.DEEPSLATE_COPPER_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Items.RAW_IRON_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(Blocks.DEEPSLATE_IRON_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Items.RAW_GOLD_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(Blocks.DEEPSLATE_GOLD_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Items.GOLD_INGOT, 2)
            .inputBlock(Blocks.NETHERRACK)
            .resultBlock(Blocks.NETHER_GOLD_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Items.GOLD_INGOT)
            .inputBlock(Blocks.BLACKSTONE)
            .resultBlock(Blocks.GILDED_BLACKSTONE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_ZINC_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_ZINC_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_TIN_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_TIN_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_TITANIUM_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_TITANIUM_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_TUNGSTEN_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_TUNGSTEN_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_LEAD_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_LEAD_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_SILVER_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_SILVER_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.RAW_URANIUM_BLOCK)
            .inputBlock(Blocks.DEEPSLATE)
            .resultBlock(ModBlocks.DEEPSLATE_URANIUM_ORE)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(Items.SHULKER_BOX)
            .inputBlock(Blocks.SHULKER_BOX)
            .resultBlock(ModBlocks.NESTING_SHULKER_BOX)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.NESTING_SHULKER_BOX)
            .inputBlock(Blocks.SHULKER_BOX)
            .resultBlock(ModBlocks.OVER_NESTING_SHULKER_BOX)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModBlocks.OVER_NESTING_SHULKER_BOX)
            .inputBlock(Blocks.SHULKER_BOX)
            .resultBlock(ModBlocks.SUPERCRITICAL_NESTING_SHULKER_BOX)
            .save(provider);

        ItemInjectRecipe.builder()
            .requires(ModItems.CHARGED_NEUTRONIUM_INGOT)
            .inputBlock(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())
            .result(ModItems.NEUTRONIUM_INGOT.asStack(), 0.5f)
            .resultBlock(ModBlocks.TRANSCENDIUM_BLOCK.get())
            .save(provider);
    }
}
