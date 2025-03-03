package dev.dubhe.anvilcraft.inventory.component.jewel;

import dev.dubhe.anvilcraft.inventory.container.JewelSourceContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class JewelResultSlot extends Slot {
    private final JewelSourceContainer sourceContainer;
    private final CraftingContainer craftSlots;

    public JewelResultSlot(JewelSourceContainer sourceContainer, CraftingContainer craftSlots, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.sourceContainer = sourceContainer;
        this.craftSlots = craftSlots;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        if (sourceContainer.getRecipe() != null) {
            for (int i = 0; i < sourceContainer.getRecipe().value().mergedIngredients.size(); i++) {
                var entry = sourceContainer.getRecipe().value().mergedIngredients.get(i);
                craftSlots.removeItem(i, entry.getIntValue());
            }
        }
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
