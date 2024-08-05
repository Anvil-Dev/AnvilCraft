package dev.dubhe.anvilcraft.integration.kubejs;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.RunCommand;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnExperience;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.block.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.block.HasBlockIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.fluid.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.item.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.item.HasItemIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.block.NotHasBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.kubejs.evnet.AnvilEvents;
import dev.dubhe.anvilcraft.integration.kubejs.listner.RecipeReloadEventListener;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.MobTransformRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.builder.SelectOneBuilder;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import net.minecraft.world.entity.EntityType;

public class AnvilCraftKubeJsPlugin extends KubeJSPlugin {

    @Override
    public void init() {
        AnvilRecipe.init();
        AnvilCraft.EVENT_BUS.register(new RecipeReloadEventListener());
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        super.registerRecipeSchemas(event);
        event.register(AnvilCraft.of("anvil_processing"), AnvilCraftRecipeSchema.SCHEMA);
        event.register(AnvilCraft.of("mob_transform"), MobTransformRecipeSchema.SCHEMA);
    }

    @Override
    public void registerEvents() {
        super.registerEvents();
        AnvilEvents.GROUP.register();
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        super.registerClasses(type, filter);
        filter.allow("dev.dubhe.anvilcraft");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        super.registerBindings(event);

        // minecraft
        event.add("EntityType", EntityType.class);

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

        // recipe
        event.add("AnvilRecipe", AnvilRecipe.class);
        event.add("AnvilRecipeType", AnvilRecipeType.class);
    }
}
