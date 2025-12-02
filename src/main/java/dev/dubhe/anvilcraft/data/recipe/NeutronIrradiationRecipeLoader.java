package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.recipe.outcome.ProduceExplosion;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

public class NeutronIrradiationRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        NeutronIrradiationRecipe.builder()
            .requires(ModItemTags.URANIUM_INGOTS)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .save(provider);

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasItemIngredient(builder -> builder.of(ModBlocks.URANIUM_BLOCK.asItem()).offset(0.0, -0.375, 0.0).range(0.75, 0.75, 0.75))
            .hasCauldron(0, -1, 0)
            .hasBlock(builder -> builder.of(ModBlocks.NEUTRON_IRRADIATOR.get()).offset(0, -2, 0))
            .chooseOne(builder -> builder.choice(
                new ProduceExplosion(
                    new Vec3(0.0, -0.75, 0.0),
                    3f,
                    false,
                    Level.ExplosionInteraction.BLOCK,
                    ConstantValue.exactly(1f)
                ), 1f
            ))
            .save(provider, "uranium_block_explosion");
    }
}