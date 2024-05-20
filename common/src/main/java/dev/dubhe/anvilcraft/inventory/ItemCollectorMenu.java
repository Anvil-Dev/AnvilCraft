package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemCollectorMenu extends AbstractContainerMenu implements IFilterMenu, ContainerListener {
    @Getter
    private final ItemCollectorBlockEntity blockEntity;
    private final Level level;

    /**
     * 物品收集器 ScreenHandler
     */
    public ItemCollectorMenu(
            @Nullable MenuType<?> menuType,
            int containerId,
            Inventory inventory,
            @NotNull BlockEntity machine
    ) {
        super(menuType, containerId);
        ItemCollectorMenu.checkContainerSize(inventory, 9);

        this.blockEntity = (ItemCollectorBlockEntity) machine;
        this.level = inventory.player.level();

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new ItemDepositorySlot(
                        this.blockEntity.getDepository(),
                        i * 3 + j,
                        98 + j * 18,
                        18 + i * 18
                ));
            }
        }

        this.onChanged();
        this.addSlotListener(this);
    }

    public ItemCollectorMenu(
            @Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData
    ) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private void onChanged() {

    }


    @Override
    public IFilterBlockEntity getFilterBlockEntity() {
        return blockEntity;
    }

    @Override
    public void setItem(int slotId, int stateId, @NotNull ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        this.onChanged();
    }


    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 9;  // must be the number of slots you have!

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        //noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        final ItemStack copyOfSourceStack = sourceStack.copy();
        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(
                    sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.ITEM_COLLECTOR.get()
        );
    }

    @Override
    public void slotChanged(
            @NotNull AbstractContainerMenu containerToSend, int dataSlotIndex, @NotNull ItemStack stack
    ) {
        onChanged();
    }

    @Override
    public void dataChanged(@NotNull AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {

    }

    @Override
    public void flush() {
        this.onChanged();
    }

    /**
     *
     */
    public void notify(int index, String name) {
        if (name.contentEquals("rangeRadius")) {
            blockEntity.getRangeRadius().fromIndex(index);
        } else {
            if (name.contentEquals("cooldown")) {
                blockEntity.getCooldown().fromIndex(index);
            }
        }
    }
}
