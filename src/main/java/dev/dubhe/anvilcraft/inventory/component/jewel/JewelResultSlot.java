package dev.dubhe.anvilcraft.inventory.component.jewel;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class JewelResultSlot extends Slot {
    private final ResultContainer resultContainer;
    private final CraftingContainer craftSlots;

    public JewelResultSlot(
        ResultContainer resultContainer,
        CraftingContainer craftSlots,
        Container container,
        int slot,
        int x,
        int y
    ) {
        super(container, slot, x, y);
        this.resultContainer = resultContainer;
        this.craftSlots = craftSlots;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        RecipeHolder<JewelCraftingRecipe> recipe = this.resultContainer.getRecipeUsed() == null
                                                   ? null
                                                   : Util.cast(this.resultContainer.getRecipeUsed());
        if (recipe != null) {
            for (int i = 0; i < recipe.value().ingredients().size(); i++) {
                var entry = recipe.value().ingredients().get(i);
                this.craftSlots.removeItem(i, entry.count());
            }
        }
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
