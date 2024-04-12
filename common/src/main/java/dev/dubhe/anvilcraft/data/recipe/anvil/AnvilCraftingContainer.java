package dev.dubhe.anvilcraft.data.recipe.anvil;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

@Getter
public class AnvilCraftingContainer
    implements Container, StackedContentsCompatible {
    private final Level level;
    private final BlockPos pos;
    private final FallingBlockEntity entity;
    @Setter
    private boolean isAnvilDamage = false;

    /**
     * 初始化 AnvilCraftingContainer
     *
     * @param level  世界
     * @param pos    位置
     * @param entity 铁砧实体
     */
    public AnvilCraftingContainer(Level level, BlockPos pos, FallingBlockEntity entity) {
        this.level = level;
        this.pos = pos;
        this.entity = entity;
    }

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
    public void setItem(int slot, @NotNull ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public void fillStackedContents(@NotNull StackedContents contents) {
    }
}
