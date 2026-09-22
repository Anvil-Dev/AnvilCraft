package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.mixin.SlotPositionAccessor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PocketSlot extends Slot {
    private final Player owner;
    private final int pocketIndex;

    public PocketSlot(Player owner, int index) {
        super(new Access(owner), index, 0, 84 + index % 3 * 18);
        this.owner = owner;
        this.pocketIndex = index;
        this.updatePosition();
    }

    public void updatePosition() {
        int capacity = PocketInventory.capacity(this.owner);
        int perSide = capacity == 12 ? 6 : 3;
        int width = capacity == 12 ? 44 : 26;
        int side = this.pocketIndex / perSide;
        ((SlotPositionAccessor) this).anvilcraft$setX((side == 0 ? -width - 2 : 178) + 5 + (this.pocketIndex % perSide / 3) * 18);
    }

    @Override
    public boolean isActive() {
        this.updatePosition();
        return this.pocketIndex < PocketInventory.capacity(this.owner);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isActive();
    }

    @Override
    public boolean mayPickup(Player player) {
        return player == this.owner && this.isActive();
    }

    private record Access(Player owner) implements Container {
        @Override
        public int getContainerSize() {
            return PocketInventory.MAX_SIZE;
        }

        @Override
        public boolean isEmpty() {
            return PocketInventory.get(this.owner).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return PocketInventory.get(this.owner).getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return PocketInventory.get(this.owner).removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return PocketInventory.get(this.owner).removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            PocketInventory.get(this.owner).rememberLeggings(this.owner);
            PocketInventory.get(this.owner).setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            PocketInventory.get(this.owner).setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.owner;
        }

        @Override
        public void clearContent() {
            PocketInventory.get(this.owner).clearContent();
        }
    }
}
