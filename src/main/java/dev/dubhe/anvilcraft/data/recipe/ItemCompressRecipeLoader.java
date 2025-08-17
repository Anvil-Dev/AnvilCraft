package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemCompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemCompressRecipe.builder()
            .requires(Items.BONE, 3)
            .result(new ItemStack(Items.BONE_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.CREAM, 4)
            .requires(Items.SUGAR)
            .result(new ItemStack(ModBlocks.CREAM_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.CREAM, 4)
            .requires(Items.SUGAR)
            .requires(Items.SWEET_BERRIES)
            .result(new ItemStack(ModBlocks.BERRY_CREAM_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.CREAM, 4)
            .requires(Items.SUGAR)
            .requires(ModItems.CHOCOLATE)
            .result(new ItemStack(ModBlocks.CHOCOLATE_CREAM_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.NEUTRONIUM_INGOT)
            .requires(ModItems.URANIUM_INGOT)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .result(ModItems.LIME_POWDER)
            .result(ModItems.NEUTRONIUM_INGOT)
            .save(provider, AnvilCraft.of("item_compress/plutonium_nugget_from_neutronium_ingot"));

        ItemCompressRecipe.builder()
            .requires(ModItems.STABLE_NEUTRONIUM_INGOT)
            .requires(ModItems.URANIUM_INGOT)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .result(ModItems.LIME_POWDER)
            .result(ModItems.STABLE_NEUTRONIUM_INGOT)
            .save(provider, AnvilCraft.of("item_compress/plutonium_nugget_from_stable_neutronium_ingot"));
    }
}
