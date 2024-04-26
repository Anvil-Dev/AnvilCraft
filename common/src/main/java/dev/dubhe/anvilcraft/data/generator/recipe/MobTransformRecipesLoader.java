package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.data.recipe.transform.MobTransformRecipe;
import net.minecraft.world.entity.EntityType;

public class MobTransformRecipesLoader {
    /**
     * 生物被腐化信标照射时的转化配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        MobTransformRecipe.builder("bat")
                .input(EntityType.BAT)
                .result(EntityType.PHANTOM, 1)
                .accept(provider);
    }
}
