package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.recipe.outcome.ProduceExplosion;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

public class NeutronIrradiationRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        NeutronIrradiationRecipe.builder()
            .requires(items, ModItemTags.URANIUM_INGOTS)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .save(provider);

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasItemIngredient(builder -> builder.of(ModBlocks.URANIUM_BLOCK.asItem()).offset(0.0, -0.375, 0.0).range(0.75, 0.75, 0.75))
            .hasCauldron(0, -1, 0)
            .hasBlock(builder -> builder.of(ModBlocks.NEUTRON_IRRADIATOR.get()).offset(0, -2, 0))
            .chooseOne(builder -> builder.choice(
                new ProduceExplosion(
                    new Vec3(0.0, -0.75, 0.0),
                    3F,
                    false,
                    Level.ExplosionInteraction.BLOCK,
                    ConstantValue.exactly(1F)
                ), 1F
            ))
            .save(provider, "uranium_block_explosion");
    }
}
