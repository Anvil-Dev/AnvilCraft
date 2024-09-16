package dev.dubhe.anvilcraft.integration.jei;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockCompressCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockCrushCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BoilingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.CookingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemCompressCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemCrushCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.MeshRecipeCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.StampingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.SuperHeatingCategory;
import dev.dubhe.anvilcraft.integration.jei.recipe.MeshRecipeGroup;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.SuperHeatingRecipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AnvilCraftJeiPlugin implements IModPlugin {

    public static final RecipeType<MeshRecipeGroup> MESH = createRecipeType("mesh", MeshRecipeGroup.class);
    public static final RecipeType<BlockCompressRecipe> BLOCK_COMPRESS =
            createRecipeType("block_compress", BlockCompressRecipe.class);
    public static final RecipeType<BlockCrushRecipe> BLOCK_CRUSH =
            createRecipeType("block_crush", BlockCrushRecipe.class);
    public static final RecipeType<ItemCompressRecipe> ITEM_COMPRESS =
            createRecipeType("item_compress", ItemCompressRecipe.class);
    public static final RecipeType<ItemCrushRecipe> ITEM_CRUSH = createRecipeType("item_crush", ItemCrushRecipe.class);
    public static final RecipeType<CookingRecipe> COOKING = createRecipeType("cooking", CookingRecipe.class);
    public static final RecipeType<BoilingRecipe> BOILING = createRecipeType("boiling", BoilingRecipe.class);
    public static final RecipeType<StampingRecipe> STAMPING = createRecipeType("stamping", StampingRecipe.class);
    public static final RecipeType<SuperHeatingRecipe> SUPER_HEATING =
            createRecipeType("super_heating", SuperHeatingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return AnvilCraft.of("jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MeshRecipeCategory.registerRecipes(registration);
        BlockCompressCategory.registerRecipes(registration);
        BlockCrushCategory.registerRecipes(registration);
        ItemCompressCategory.registerRecipes(registration);
        ItemCrushCategory.registerRecipes(registration);
        CookingCategory.registerRecipes(registration);
        BoilingCategory.registerRecipes(registration);
        StampingCategory.registerRecipes(registration);
        SuperHeatingCategory.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        MeshRecipeCategory.registerRecipeCatalysts(registration);
        BlockCompressCategory.registerRecipeCatalysts(registration);
        BlockCrushCategory.registerRecipeCatalysts(registration);
        ItemCompressCategory.registerRecipeCatalysts(registration);
        ItemCrushCategory.registerRecipeCatalysts(registration);
        CookingCategory.registerRecipeCatalysts(registration);
        BoilingCategory.registerRecipeCatalysts(registration);
        StampingCategory.registerRecipeCatalysts(registration);
        SuperHeatingCategory.registerRecipeCatalysts(registration);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

        registration.addRecipeCategories(new MeshRecipeCategory(guiHelper));
        registration.addRecipeCategories(new BlockCompressCategory(guiHelper));
        registration.addRecipeCategories(new BlockCrushCategory(guiHelper));
        registration.addRecipeCategories(new ItemCompressCategory(guiHelper));
        registration.addRecipeCategories(new ItemCrushCategory(guiHelper));
        registration.addRecipeCategories(new CookingCategory(guiHelper));
        registration.addRecipeCategories(new BoilingCategory(guiHelper));
        registration.addRecipeCategories(new StampingCategory(guiHelper));
        registration.addRecipeCategories(new SuperHeatingCategory(guiHelper));
    }

    public static <T> RecipeType<T> createRecipeType(String name, Class<T> clazz) {
        return new RecipeType<>(AnvilCraft.of(name), clazz);
    }
}
