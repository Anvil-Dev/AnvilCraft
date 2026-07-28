package dev.dubhe.anvilcraft.api.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Defines the items required to place matching block states.
 */
public interface IBlockPlacementRule {
    boolean matches(BlockState state);

    List<PlacementItem> getPlacementItems(BlockState state);

    record PlacementItem(Item item, int count) {
        public boolean isForbidden() {
            return this.count == -1;
        }

        public ItemStack createStack() {
            return this.count > 0 ? new ItemStack(this.item, this.count) : ItemStack.EMPTY;
        }
    }
}
