package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class AnvilItemRecipesLoader {
    /**
     * 初始化铁砧配方
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        ItemPredicate.Builder predicate = ItemPredicate.Builder.item().of(Items.ELYTRA)
            .hasDurability(MinMaxBounds.Ints.atLeast(Items.ELYTRA.getMaxDamage()));
        RecipePredicate hasItemIngredient = new HasItemIngredient(Vec3.ZERO, predicate.build());
        ItemStack result = Items.ELYTRA.getDefaultInstance();
        result.setDamageValue(432);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.SMITHING_TABLE)
            .addPredicates(hasItemIngredient)
            .spawnItem(ModItems.ELYTRA_FRAME)
            .spawnItem(Items.PHANTOM_MEMBRANE)
            .spawnItem(result)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ELYTRA), AnvilCraftDatagen.has(Items.ELYTRA))
            .save(provider);
    }
}
