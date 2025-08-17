package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.SpectralAnvilConversionUtil;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class EndPortalConversionRecipe {
    public final List<ItemIngredientPredicate> ingredients;
    public final List<ChanceItemStack> results;

    public EndPortalConversionRecipe(Block blockInput, float chance) {
        this.ingredients = ImmutableList.of(ItemIngredientPredicate.Builder.item().of(blockInput).build());
        ImmutableList.Builder<ChanceItemStack> builder = ImmutableList.builder();
        if (chance > 0.0) {
            builder.add(ChanceItemStack.of(ModBlocks.SPECTRAL_ANVIL.asStack(), chance));
        }
        if (chance < 1.0) {
            builder.add(ChanceItemStack.of(ModBlocks.END_DUST.asStack(), 1.0f - chance));
        }
        this.results = builder.build();
    }

    public static ImmutableList<EndPortalConversionRecipe> getAllRecipes() {
        ImmutableList.Builder<EndPortalConversionRecipe> builder = ImmutableList.builder();
        SpectralAnvilConversionUtil.SPECTRAL_ANVIL_CONVERSION_CHANCE.object2DoubleEntrySet().forEach(
            it -> builder.add(new EndPortalConversionRecipe(it.getKey(), (float) it.getDoubleValue())));
        return builder.build();
    }
}
