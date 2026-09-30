package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

public final class NeutroniumChargingRecipe extends ShapedRecipe {
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER = new RecipeSerializer<>(
        ShapedRecipe.MAP_CODEC.<ShapedRecipe>xmap(NeutroniumChargingRecipe::new, recipe -> recipe),
        ShapedRecipe.STREAM_CODEC.<ShapedRecipe>map(NeutroniumChargingRecipe::new, recipe -> recipe)
    );

    public NeutroniumChargingRecipe(ShapedRecipe recipe) {
        super(new CommonInfo(recipe.showNotification()), new CraftingBookInfo(recipe.category(), recipe.group()),
            recipe.pattern, recipe.result);
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        for (ItemStack source : input.items()) {
            if (!source.is(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)) continue;
            return preserveEnchantments(source, result);
        }
        return result;
    }

    public static ItemStack preserveEnchantments(ItemStack source, ItemStack result) {
        if (source.is(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS) && result.is(ModItems.CHARGED_NEUTRONIUM_INGOT)) {
            var enchantments = source.get(DataComponents.ENCHANTMENTS);
            if (enchantments != null) result.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        return result;
    }

    public static RecipeOutput output(RecipeOutput delegate) {
        return new RecipeOutput() {
            @Override
            public void accept(
                ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions
            ) {
                delegate.accept(id, new NeutroniumChargingRecipe((ShapedRecipe) recipe), advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return delegate.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                delegate.includeRootAdvancement();
            }
        };
    }
}
