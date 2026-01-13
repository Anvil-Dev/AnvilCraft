package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.recipe.init.reicpe.LibRecipeTriggers;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CoolingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ExtendInWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .hasBlock(Blocks.WATER, Blocks.WATER_CAULDRON)
            .hasItemIngredient(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE)
            .spawnItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE.getDefaultInstance())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                AnvilCraftDatagen.has(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE)
            )
            .group("cooling")
            .save(provider, AnvilCraft.of("cooling_ember_metal_upgrade_smithing_template"));
    }
}
