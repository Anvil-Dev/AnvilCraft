package dev.dubhe.anvilcraft.client.gui.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 仓储屏幕提供给 JEI 等客户端扩展的背包视图，原版背包仍负责实际同步。 */
public final class StorageMenu extends AbstractContainerMenu {
    private final AbstractContainerMenu inventoryMenu;
    private final BlockPos sourcePos;

    public StorageMenu(Player player, BlockPos sourcePos) {
        super(null, player.inventoryMenu.containerId);
        this.inventoryMenu = player.inventoryMenu;
        this.sourcePos = sourcePos.immutable();
        for (int index = 0; index < this.inventoryMenu.slots.size(); index++) {
            this.addSlot(this.createSlot(index, false));
        }
    }

    public BlockPos getSourcePos() {
        return this.sourcePos;
    }

    public void setFlipped(boolean flipped) {
        for (int index = 9; index < 45; index++) {
            Slot slot = this.createSlot(index, flipped);
            slot.index = index;
            this.slots.set(index, slot);
        }
    }

    private Slot createSlot(int index, boolean flipped) {
        Slot original = this.inventoryMenu.getSlot(index);
        boolean visible = index >= 9 && index < 45;
        int inventoryIndex = index >= 36 && index < 45 ? index - 36 : index;
        int x = visible ? (flipped ? 8 : 114) + inventoryIndex % 9 * 18 : -1000;
        int y = visible ? inventoryIndex < 9 ? 198 : 140 + (inventoryIndex - 9) / 9 * 18 : -1000;
        return new Slot(original.container, original.getContainerSlot(), x, y) {
            @Override
            public boolean isActive() {
                return visible;
            }
        };
    }

    @Override
    public ItemStack getCarried() {
        return this.inventoryMenu.getCarried();
    }

    @Override
    public void setCarried(ItemStack stack) {
        this.inventoryMenu.setCarried(stack);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive();
    }

    @Override
    public void setSynchronizer(@Nullable ContainerSynchronizer synchronizer) {
    }
}
