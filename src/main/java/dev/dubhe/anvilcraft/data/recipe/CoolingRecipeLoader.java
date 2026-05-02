package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTriggers;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CoolingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderLookup.RegistryLookup<Item> lookup = provider.getRegistries().lookupOrThrow(Registries.ITEM);
        ExtendInWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .hasBlock(Blocks.WATER, Blocks.WATER_CAULDRON)
            .hasItemIngredient(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE)
            .spawnItem(ItemStackTemplate.fromNonEmptyStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE.getDefaultInstance()))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                AnvilCraftDatagen.has(lookup, ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE)
            )
            .group("cooling")
            .save(provider, AnvilCraft.of("cooling_ember_metal_upgrade_smithing_template"));
    }
}
