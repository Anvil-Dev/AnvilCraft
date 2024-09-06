package dev.dubhe.anvilcraft.inventory;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimpleCraftingContainer extends SimpleContainer implements CraftingContainer {
    public SimpleCraftingContainer() {
        super(9);
    }

    /**
     * @param stacks 物品
     */
    public SimpleCraftingContainer(ItemStack @NotNull ... stacks) {
        this();
        for (int i = 0; i < stacks.length && i < this.getContainerSize(); i++) {
            this.setItem(i, stacks[i]);
        }
    }

    /**
     * @param stack 物品
     */
    public static @NotNull SimpleCraftingContainer create4x(@NotNull ItemStack stack) {
        SimpleCraftingContainer container = new SimpleCraftingContainer();
        container.setItem(0, stack.copy());
        container.setItem(1, stack.copy());
        container.setItem(3, stack.copy());
        container.setItem(4, stack.copy());
        return container;
    }

    /**
     * @param stack 物品
     */
    public static @NotNull SimpleCraftingContainer create9x(@NotNull ItemStack stack) {
        SimpleCraftingContainer container = new SimpleCraftingContainer();
        container.setItem(0, stack.copy());
        container.setItem(1, stack.copy());
        container.setItem(2, stack.copy());
        container.setItem(3, stack.copy());
        container.setItem(4, stack.copy());
        container.setItem(5, stack.copy());
        container.setItem(6, stack.copy());
        container.setItem(7, stack.copy());
        container.setItem(8, stack.copy());
        return container;
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public @NotNull List<ItemStack> getItems() {
        return this.items;
    }
}
