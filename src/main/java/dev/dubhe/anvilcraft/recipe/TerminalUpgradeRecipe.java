package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.TerminalItem;
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
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

public final class TerminalUpgradeRecipe extends ShapedRecipe {
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER = new RecipeSerializer<>(
        ShapedRecipe.MAP_CODEC.<ShapedRecipe>xmap(TerminalUpgradeRecipe::new, recipe -> recipe),
        ShapedRecipe.STREAM_CODEC.<ShapedRecipe>map(TerminalUpgradeRecipe::new, recipe -> recipe)
    );

    public TerminalUpgradeRecipe(ShapedRecipe recipe) {
        super(new CommonInfo(recipe.showNotification()), new CraftingBookInfo(recipe.category(), recipe.group()),
            recipe.pattern, recipe.result);
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return preserveContents(super.assemble(input), input);
    }

    static ItemStack preserveContents(ItemStack output, CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!(stack.getItem() instanceof TerminalItem)) continue;
            var crafting = stack.get(ModComponents.CRAFTING);
            if (crafting != null) {
                output.set(ModComponents.CRAFTING, crafting.withStonecutterInput(crafting.stonecutterInput().copy())
                    .withCraftingInput(crafting.craftingInput().stream().map(ItemStack::copy).toList()));
            }
            if (stack.has(ModComponents.TERMINAL_BALANCE_MODE)) {
                output.set(ModComponents.TERMINAL_BALANCE_MODE, stack.get(ModComponents.TERMINAL_BALANCE_MODE));
            }
            if (stack.has(DataComponents.CUSTOM_NAME)) output.set(DataComponents.CUSTOM_NAME, stack.get(DataComponents.CUSTOM_NAME));
            break;
        }
        return output;
    }

    public static RecipeOutput output(RecipeOutput delegate) {
        return new RecipeOutput() {
            @Override
            public void accept(
                ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions
            ) {
                Recipe<?> preserved = recipe instanceof ShapedRecipe shaped
                    ? new TerminalUpgradeRecipe(shaped) : new TerminalUnbindRecipe((ShapelessRecipe) recipe);
                delegate.accept(id, preserved, advancement, conditions);
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
