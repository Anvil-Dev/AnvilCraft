package dev.dubhe.anvilcraft.integration.kubejs;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeSchema;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

public class AnvilCraftKubeJSPlugin extends KubeJSPlugin {

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
    }
}
