package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.recipe.outcome.ProduceExplosion;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;

import java.util.Optional;

public class NeutronIrradiationRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> items = provider.getItems();
        NeutronIrradiationRecipe.builder()
            .requires(items, ModItemTags.URANIUM_INGOTS)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .save(provider);

        var radioactiveBlocks = new OrHolderSet<>(
            items.getOrThrow(ModItemTags.STORAGE_BLOCKS_URANIUM), items.getOrThrow(ModItemTags.STORAGE_BLOCKS_PLUTONIUM));
        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .with(HasItemIngredient.fromPredicate(
                new ItemIngredientPredicate(Optional.of(radioactiveBlocks), 1, DataComponentMatchers.ANY),
                new Vec3(0, -0.375, 0), new Vec3(0.75, 0.75, 0.75)))
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
            .group("neutron_irradiation")
            .save(provider, AnvilCraft.of("radiation_block_explosion"));
    }
}
