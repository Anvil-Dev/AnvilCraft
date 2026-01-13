package dev.dubhe.anvilcraft.recipe.multiple;

import dev.dubhe.anvilcraft.util.ListUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record MultipleToOneSmithingRecipeInput(ItemStack template, ItemStack material, List<ItemStack> inputs) implements RecipeInput {
    public MultipleToOneSmithingRecipeInput(ItemStack template, ItemStack material, ItemStack... inputs) {
        this(template, material, List.of(inputs));
    }

    @Override
    public ItemStack getItem(int id) {
        return switch (id) {
            case 0 -> this.template();
            case 1 -> this.material();
            default -> {
                if (id - 2 < this.inputs.size()) {
                    yield this.inputs.get(id - 2);
                } else {
                    throw new IllegalArgumentException("Recipe does not contain input " + id);
                }
            }
        };
    }

    public ItemStack getInputItem(int id) {
        return ListUtil.safelyGet(this.inputs, id)
            .orElseThrow(() -> new IllegalArgumentException("Recipe inputs does not contain index " + id));
    }

    @Override
    public int size() {
        return this.inputs.size() + 2;
    }
}
