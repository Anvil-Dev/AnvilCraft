package dev.dubhe.anvilcraft.api.recipe.result;

import com.google.common.collect.ImmutableMap;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

@Getter
public class ResultContext {
    private final HolderLookup.Provider registries;
    private final RandomSource random;
    private final @Unmodifiable Map<RecipeInputSlot, ItemStack> inputs;
    private final ItemStack result;

    private ResultContext(
        HolderLookup.Provider registries,
        RandomSource random,
        @Unmodifiable Map<RecipeInputSlot, ItemStack> inputs,
        ItemStack result
    ) {
        this.registries = registries;
        this.random = random;
        this.inputs = inputs;
        this.result = result;
    }

    public static Builder builder(HolderLookup.Provider registries, RandomSource random, ItemStack result) {
        return new Builder(registries, random, result);
    }

    public ItemStack getInput(RecipeInputSlot slot) {
        return this.inputs.getOrDefault(slot, ItemStack.EMPTY);
    }

    public void updateResult(int count) {
        this.result.setCount(count);
    }

    public void updateResult(DataComponentPatch patch) {
        this.result.applyComponents(patch);
    }

    public <T> void updateResult(DataComponentType<T> type, @Nullable T value) {
        if (value == null) return;
        this.result.set(type, value);
    }

    public static class Builder {
        private final HolderLookup.Provider registries;
        private final RandomSource random;
        private final ImmutableMap.Builder<RecipeInputSlot, ItemStack> inputs;
        private final ItemStack result;

        public Builder(HolderLookup.Provider registries, RandomSource random, ItemStack result) {
            this.registries = registries;
            this.random = random;
            this.inputs = ImmutableMap.builder();
            this.result = result;
        }

        public Builder slot(RecipeInputSlot slot, ItemStack input) {
            this.inputs.put(slot, input);
            return this;
        }

        public Builder input(int index, ItemStack input) {
            return this.slot(RecipeInputSlot.input(index), input);
        }

        public ResultContext build() {
            return new ResultContext(this.registries, this.random, this.inputs.build(), this.result);
        }
    }
}
