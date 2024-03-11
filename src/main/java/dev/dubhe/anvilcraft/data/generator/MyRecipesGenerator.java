package dev.dubhe.anvilcraft.data.generator;

import dev.dubhe.anvilcraft.data.generator.recipe.CompressRecipesGenerator;
import dev.dubhe.anvilcraft.data.generator.recipe.SmashBlockRecipesGenerator;
import dev.dubhe.anvilcraft.data.generator.recipe.SqueezeRecipesGenerator;
import dev.dubhe.anvilcraft.data.generator.recipe.VanillaRecipesGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MyRecipesGenerator extends FabricRecipeProvider {
    public MyRecipesGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        VanillaRecipesGenerator.buildRecipes(exporter);
        // 铁砧物品处理
        CompressRecipesGenerator.buildRecipes(exporter);
        // 铁砧方块处理
        SqueezeRecipesGenerator.buildRecipes(exporter);
        SmashBlockRecipesGenerator.buildRecipes(exporter);
    }

    public static @NotNull String hasItem(@NotNull Item item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
