package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.ItemCompressRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class ItemCompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemCompressRecipe.builder()
                .requires(Items.BONE, 3)
                .result(new ItemStack(Items.BONE_BLOCK))
                .save(provider, AnvilCraft.of("item_compress/bone_block"));
    }
}
