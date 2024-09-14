package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;

import net.minecraft.world.entity.EntityType;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class MobTransformRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MobTransformRecipe.from(EntityType.COW).to(EntityType.SHEEP).save(provider);
    }
}
