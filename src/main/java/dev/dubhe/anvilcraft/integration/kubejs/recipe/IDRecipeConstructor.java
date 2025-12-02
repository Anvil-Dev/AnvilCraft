package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.RecipeTypeFunction;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.ErrorStack;
import dev.latvian.mods.kubejs.util.KubeResourceLocation;
import dev.latvian.mods.rhino.Context;
import net.minecraft.resources.ResourceLocation;

public class IDRecipeConstructor extends RecipeConstructor {
    private static final RecipeKey<ResourceLocation> ID = AnvilCraftRecipeComponents.RESOURCE_LOCATION.otherKey("id");

    public IDRecipeConstructor() {
        super(ID);
    }

    @Override
    public KubeRecipe create(Context cx, SourceLine sourceLine, RecipeTypeFunction type, RecipeSchemaType schemaType, ComponentValueMap from) {
        var r = super.create(cx, sourceLine, type, schemaType, from);
        RecipeScriptContext ctx = new RecipeScriptContext.Impl(cx, r, new ErrorStack());
        r.id(KubeResourceLocation.wrap(from.getValue(ctx, ID)));
        return r;
    }
}
