package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.CyclingValueHandler;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ExpCollectorMenu extends AbstractContainerMenu implements ContainerListener, CyclingValueHandler {
    @Getter
    private final ExpCollectorBlockEntity blockEntity;
    private final Level level;

    public ExpCollectorMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        BlockEntity blockEntity
    ) {
        super(menuType, containerId);
        this.blockEntity = (ExpCollectorBlockEntity) blockEntity;
        this.level = inventory.player.level();
        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);
        this.addSlotListener(this);
    }

    public ExpCollectorMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        FriendlyByteBuf extraData
    ) {
        this(
            menuType,
            containerId,
            inventory,
            Objects.requireNonNull(inventory.player.level().getBlockEntity(extraData.readBlockPos()))
        );
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
            ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
            player,
            ModBlocks.EXP_COLLECTOR.get()
        );
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slot, ItemStack stack) {
    }

    @Override
    public void dataChanged(AbstractContainerMenu container, int index, int value) {
    }

    @Override
    public void notify(int index, String name) {
        if (name.contentEquals("rangeRadius")) {
            this.blockEntity.getRangeRadius().fromIndex(index);
        } else if (name.contentEquals("cooldown")) {
            this.blockEntity.getCooldown().fromIndex(index);
        }
    }
}
