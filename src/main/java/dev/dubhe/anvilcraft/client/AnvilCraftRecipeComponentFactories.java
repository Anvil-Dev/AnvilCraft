package dev.dubhe.anvilcraft.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDMeshRecipeComponent;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class AnvilCraftRecipeComponentFactories {
    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORIES = DeferredRegister.create(AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY_KEY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        MESH = RECIPE_COMPONENT_FACTORIES.register(
        "mesh", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.MESH_TYPE.get(),
            MDMeshRecipeComponent::new
        )
    );

    private AnvilCraftRecipeComponentFactories() {}
}
