package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.MeshRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.neoforged.neoforge.common.Tags;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class MeshRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MeshRecipe.builder()
                .input(Ingredient.of(Tags.Items.SANDS))
                .result(new ItemStack(Items.CLAY_BALL))
                .resultAmount(BinomialDistributionGenerator.binomial(1, 0.25f))
                .save(provider, AnvilCraft.of("mesh/sand/clay_ball"));
        MeshRecipe.builder()
                .input(Ingredient.of(Tags.Items.SANDS))
                .result(new ItemStack(Items.GOLD_NUGGET))
                .resultAmount(BinomialDistributionGenerator.binomial(1, 0.05f))
                .save(provider, AnvilCraft.of("mesh/sand/gold_nugget"));
    }
}
