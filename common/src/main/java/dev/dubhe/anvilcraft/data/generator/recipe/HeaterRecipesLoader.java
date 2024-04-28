package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;

public class HeaterRecipesLoader {
    /**
     * 初始化加热器配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.EMERALD_BLOCK)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0, -2, 0), Map.entry(OVERLOAD, false))
            .hasBlockIngredient(new Vec3(0, -1, 0), Blocks.CAULDRON)
            .hasItemIngredient(
                new Vec3(0, -1, 0),
                Items.EMERALD_BLOCK,
                ModBlocks.RUBY_BLOCK,
                ModBlocks.TOPAZ_BLOCK,
                ModBlocks.SAPPHIRE_BLOCK
            )
            .setBlock(new Vec3(0, -1, 0), ModBlocks.MELT_GEM_CAULDRON.get())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.EMERALD_BLOCK), AnvilCraftDatagen.has(Items.EMERALD_BLOCK)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK), AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.TOPAZ_BLOCK), AnvilCraftDatagen.has(ModBlocks.TOPAZ_BLOCK)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.SAPPHIRE_BLOCK), AnvilCraftDatagen.has(ModBlocks.SAPPHIRE_BLOCK)
            )
            .save(provider, AnvilCraft.of("heating/melt_gem_cauldron"));
    }
}
