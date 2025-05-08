package dev.dubhe.anvilcraft.recipe.multiple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

@MethodsReturnNonnullByDefault
public record MultipleToOneSmithingRecipeInput(ItemStack template, ItemStack material, ItemStack[] inputs,
                                               int inputSize)
    implements RecipeInput {

    public MultipleToOneSmithingRecipeInput(ItemStack template, ItemStack material, int inputSize, ItemStack... inputs) {
        this(template, material, inputs, inputSize);
    }

    @Override
    public ItemStack getItem(int id) {
        return switch (id) {
            case 0 -> this.template();
            case 1 -> this.material();
            default -> {
                if (id - 2 < this.inputs.length) {
                    yield this.inputs[id - 2];
                } else {
                    throw new IllegalArgumentException("Recipe does not contain input " + id);
                }
            }
        };
    }

    public ItemStack getInputItem(int id) {
        if (id < this.inputs.length) {
            return this.inputs[id];
        } else {
            throw new IllegalArgumentException("Recipe inputs does not contain index " + id);
        }
    }

    @Override
    public int size() {
        return this.inputSize() + 2;
    }
}
