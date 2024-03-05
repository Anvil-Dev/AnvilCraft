package dev.dubhe.anvilcraft.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record AnvilCraftingContainer(Level level, BlockPos pos, FallingBlockEntity entity)
        implements Container, StackedContentsCompatible {

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
    }
}
