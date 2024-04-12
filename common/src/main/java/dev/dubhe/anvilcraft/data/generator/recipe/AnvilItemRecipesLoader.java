package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
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
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.CAULDRON)
            .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.IRON_INGOT)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.EMERALD)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.DIAMOND)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.AMETHYST_SHARD)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.EMERALD), AnvilCraftDatagen.has(Items.EMERALD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DIAMOND), AnvilCraftDatagen.has(Items.DIAMOND))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_SHARD), AnvilCraftDatagen.has(Items.AMETHYST_SHARD))
            .save(provider, AnvilCraft.of("craft_a_royal_steel_ingot"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.SMITHING_TABLE)
            .hasItem(Items.ANCIENT_DEBRIS)
            .spawnItem(ModItems.DEBRIS_SCRAP)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ANCIENT_DEBRIS), AnvilCraftDatagen.has(Items.ANCIENT_DEBRIS))
            .save(provider);
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
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.SOUL_SOIL)
            .hasItemIngredient(ModItems.NETHERITE_COIL)
            .setBlock(Blocks.ANCIENT_DEBRIS)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.SOUL_SOIL), AnvilCraftDatagen.has(Items.SOUL_SOIL))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.NETHERITE_COIL.get()),
                AnvilCraftDatagen.has(ModItems.NETHERITE_COIL)
            )
            .save(provider);
    }
}
