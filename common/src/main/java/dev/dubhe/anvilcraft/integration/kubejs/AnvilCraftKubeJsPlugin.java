package dev.dubhe.anvilcraft.integration.kubejs;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.RunCommand;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnExperience;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlockIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.NotHasBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.builder.SelectOneBuilder;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

public class AnvilCraftKubeJsPlugin extends KubeJSPlugin {

    @Override
    public void init() {
        AnvilRecipe.init();
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        super.registerRecipeSchemas(event);
        event.register(AnvilCraft.of("anvil_processing"), AnvilCraftRecipeSchema.SCHEMA);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        super.registerClasses(type, filter);
        filter.allow("dev.dubhe.anvilcraft");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        super.registerBindings(event);

        event.add("AnvilCraftItems", ModItems.class);
        event.add("AnvilCraftBlocks", ModBlocks.class);
        event.add("AnvilCraftBlockPredicate", HasBlock.ModBlockPredicate.class);

        // outcomes
        event.add("DamageAnvil", DamageAnvil.class);
        event.add("RunCommand", RunCommand.class);
        event.add("SelectOne", SelectOne.class);
        event.add("SetBlock", SetBlock.class);
        event.add("SpawnExperience", SpawnExperience.class);
        event.add("SpawnItem", SpawnItem.class);

        // predicates
        event.add("HasBlock", HasBlock.class);
        event.add("HasBlockIngredient", HasBlockIngredient.class);
        event.add("HasFluidCauldron", HasFluidCauldron.class);
        event.add("HasItem", HasItem.class);
        event.add("HasItemIngredient", HasItemIngredient.class);
        event.add("NotHasBlock", NotHasBlock.class);

        // builder
        event.add("SelectOneBuilder", SelectOneBuilder.class);
    }
}
