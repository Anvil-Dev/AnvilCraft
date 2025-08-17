package dev.dubhe.anvilcraft.integration.kubejs;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.JewelCraftingRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.BlockCompressRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.BlockCrushRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.BlockSmearRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.BulgingRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.InWorldRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.ItemInjectRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.ItemProcessRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.SqueezingRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.SuperHeatingRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil.TimeWarpRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceItemStackComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ItemIngredientPredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.mineral.MineralFountainChanceRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.mineral.MineralFountainRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.multiblock.MultiblockRecipeSchema;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.transform.MobTransformRecipeSchema;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.recipe.transform.TagModification;
import dev.dubhe.anvilcraft.recipe.transform.TransformOptions;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeComponentFactoryRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class AnvilCraftKubeJsPlugin implements KubeJSPlugin {
    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("dev.dubhe.anvilcraft");
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("ChanceItemStack", ChanceItemStack.class);
        bindings.add("BlockPredicateWithState", BlockPredicateWithState.class);

        bindings.add("ValueFunction", NumericTagValuePredicate.ValueFunction.class);
        bindings.add("ModifyOperation", TagModification.ModifyOperation.class);
        bindings.add("TransformOptions", TransformOptions.class);

        // 绑定配方接口类型
        bindings.add("IRecipeTrigger", IRecipeTrigger.class);
        bindings.add("IRecipePredicate", IRecipePredicate.class);
        bindings.add("IRecipeOutcome", IRecipeOutcome.class);
    }

    @Override
    public void registerRecipeComponents(RecipeComponentFactoryRegistry registry) {
        registry.register(AnvilCraftRecipeComponents.RESOURCE_LOCATION);
        registry.register(AnvilCraftRecipeComponents.TRIGGER);
        registry.register(AnvilCraftRecipeComponents.RECIPE_PREDICATE);
        registry.register(AnvilCraftRecipeComponents.RECIPE_OUTCOME);
        registry.register(AnvilCraftRecipeComponents.BLOCK_PATTERN);
        registry.register(AnvilCraftRecipeComponents.ENTITY_TYPE);
        registry.register(AnvilCraftRecipeComponents.TRANSFORM_RESULT);
        registry.register(AnvilCraftRecipeComponents.NUMERIC_TAG_VALUE_PREDICATE);
        registry.register(AnvilCraftRecipeComponents.TAG_MODIFICATION);
        registry.register(AnvilCraftRecipeComponents.TRANSFORM_OPTIONS);

        registry.register(BlockStatePredicateComponent.INSTANCE);
        registry.register(ChanceBlockStateComponent.INSTANCE);
        registry.register(ChanceItemStackComponent.INSTANCE);
        registry.register(ItemIngredientPredicateComponent.INSTANCE);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.register(AnvilCraft.of("block_compress"), BlockCompressRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("block_crush"), BlockCrushRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("block_smear"), BlockSmearRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("item_crush"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("boiling"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("cooking"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("item_compress"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("unpack"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("stamping"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("mesh"), ItemProcessRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("super_heating"), SuperHeatingRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("item_inject"), ItemInjectRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("squeezing"), SqueezingRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("bulging"), BulgingRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("time_warp"), TimeWarpRecipeSchema.SCHEMA);

        // 注册InWorldRecipe
        registry.register(AnvilCraft.of("in_world"), InWorldRecipeSchema.SCHEMA);

        registry.register(AnvilCraft.of("multiblock"), MultiblockRecipeSchema.SCHEMA);

        registry.register(AnvilCraft.of("mob_transform"), MobTransformRecipeSchema.SCHEMA);

        registry.register(AnvilCraft.of("mineral_fountain"), MineralFountainRecipeSchema.SCHEMA);
        registry.register(AnvilCraft.of("mineral_fountain_chance"), MineralFountainChanceRecipeSchema.SCHEMA);

        registry.register(AnvilCraft.of("jewel_crafting"), JewelCraftingRecipeSchema.SCHEMA);
    }

}