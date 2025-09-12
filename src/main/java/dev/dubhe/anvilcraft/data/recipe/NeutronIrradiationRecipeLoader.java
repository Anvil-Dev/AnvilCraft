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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

public class NeutronIrradiationRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        NeutronIrradiationRecipe.builder()
            .requires(ModItemTags.URANIUM_INGOTS)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .result(ModItems.LIME_POWDER)
            .save(provider);

        NeutronIrradiationRecipe.builder()
            .consume(1000)
            .fluid(Blocks.WATER_CAULDRON)
            .produce(1000)
            .fluid(ModBlocks.HEAVY_WATER_CAULDRON.get())
            .save(provider);

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasItemIngredient(builder -> builder
                .of(ModBlocks.URANIUM_BLOCK)
                .count(1)
                .offset(0.0, -0.375, 0.0)
                .range(0.75, 0.75, 0.75)
            )
            .chooseOne(builder -> builder
                .choice(
                    new ProduceExplosion(
                        new Vec3(0.0, -0.75, 0.0),
                        3f,
                        true,
                        Level.ExplosionInteraction.BLOCK,
                        //同权重二选一已经包含50%概率了，这里的概率要填1.0
                        ConstantValue.exactly(1f)
                    ),
                    1
                )
                .build()
            )
            .group("neutron_irradiation")
            .save(provider);
    }
}
