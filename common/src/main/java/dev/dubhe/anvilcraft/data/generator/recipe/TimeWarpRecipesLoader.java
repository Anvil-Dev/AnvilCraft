package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class TimeWarpRecipesLoader {
    /**
     * 时移配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(ModItems.SEA_HEART_SHELL)
            .hasBlock(ModBlocks.CORRUPTED_BEACON.get(), new Vec3(0.0, -2.0, 0.0))
            .hasBlock(Blocks.WATER_CAULDRON, new Vec3(0.0, -1.0, 0.0), Map.entry(LayeredCauldronBlock.LEVEL, 3))
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), ModItems.SEA_HEART_SHELL_SHARD)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.SEA_HEART_SHELL)
            .setBlock(Blocks.CAULDRON.defaultBlockState())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SEA_HEART_SHELL_SHARD.get()),
                AnvilCraftDatagen.has(ModItems.SEA_HEART_SHELL_SHARD))
            .save(provider, AnvilCraft.of("timewarp/sea_heart_shell"));
    }
}
