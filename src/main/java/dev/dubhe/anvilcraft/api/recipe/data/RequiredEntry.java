package dev.dubhe.anvilcraft.api.recipe.data;

import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import net.minecraft.core.component.DataComponentType;

/**
 * 必需的数据组件类型条目
 */
public record RequiredEntry(RecipeInputSlot slot, DataComponentType<?> component, boolean isNullable) {
    /**
     * 构建一个必需条目
     *
     * @param slot 输入材料的槽位
     * @param component 数据组件类型
     * @param isNullable 是否可为 {@code null}
     */
    public RequiredEntry {
    }

    /**
     * 构建一个必需条目
     *
     * @param input 输入的槽位
     * @param component 数据组件类型
     * @param isNullable 是否可为 {@code null}
     */
    public RequiredEntry(int input, DataComponentType<?> component, boolean isNullable) {
        this(RecipeInputSlot.input(input), component, isNullable);
    }
}
