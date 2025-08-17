package dev.dubhe.anvilcraft.recipe.multiple.result;

import dev.dubhe.anvilcraft.util.ListUtil;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@Getter
public class ResultContext {
    private final HolderLookup.Provider registries;
    private final ItemStack template;
    private final ItemStack material;
    private final @Unmodifiable List<ItemStack> inputs;
    private final ItemStack result;

    public ResultContext(
        HolderLookup.Provider registries,
        ItemStack template, ItemStack material,
        @Unmodifiable List<ItemStack> inputs,
        ItemStack result
    ) {
        this.registries = registries;
        this.template = template;
        this.material = material;
        this.inputs = inputs;
        this.result = result;
    }

    public ItemStack getInput(int index) {
        return ListUtil.safelyGet(this.inputs, index).orElse(ItemStack.EMPTY);
    }

    public void updateResult(int count) {
        this.result.setCount(count);
    }

    public void updateResult(DataComponentPatch patch) {
        this.result.applyComponents(patch);
    }

    public <T> void updateResult(DataComponentType<T> type, T value) {
        if (value == null) return;
        this.result.set(type, value);
    }
}
