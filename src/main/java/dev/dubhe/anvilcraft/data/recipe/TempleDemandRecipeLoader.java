package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.TempleDemandRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * Data generation loader for temple demand recipes.
 */
public class TempleDemandRecipeLoader {

    public static void init(RegistrumRecipeProvider provider) {
        TempleDemandRecipeLoader.createBlessingRecipe(provider);
        TempleDemandRecipeLoader.createPunishmentRecipe(provider);
    }

    private static void saveRecipe(RecipeOutput output, String name, TempleDemandRecipe recipe) {
        Identifier id = AnvilCraft.of("temple_demand/" + name);
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(key))
            .rewards(AdvancementRewards.Builder.recipe(key))
            .requirements(AdvancementRequirements.Strategy.OR);
        output.accept(key, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    private static void createBlessingRecipe(RegistrumRecipeProvider provider) {
        var recipe = new TempleDemandRecipe(
            TempleDemandRecipe.Category.BLESSING,
            List.of(
                new TempleDemandRecipe.Entry("minecraft:bread", 1024),
                new TempleDemandRecipe.Entry("minecraft:cooked_beef", 256),
                new TempleDemandRecipe.Entry("minecraft:cooked_cod", 256),
                new TempleDemandRecipe.Entry("anvilcraft:butter_bread_roll", 256),
                new TempleDemandRecipe.Entry("minecraft:cookie", 64)
            )
        );
        TempleDemandRecipeLoader.saveRecipe(provider, "blessing", recipe);
    }

    private static void createPunishmentRecipe(RegistrumRecipeProvider provider) {
        var recipe = new TempleDemandRecipe(
            TempleDemandRecipe.Category.PUNISHMENT,
            List.of(
                new TempleDemandRecipe.Entry("anvilcraft:topaz", 1),
                new TempleDemandRecipe.Entry("minecraft:lava_bucket", 1),
                new TempleDemandRecipe.Entry("minecraft:tnt", 1024)
            )
        );
        TempleDemandRecipeLoader.saveRecipe(provider, "punishment", recipe);
    }
}
