package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.inventory.component.FilterOnlySlot;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class ItemDetectorMenu extends AbstractContainerMenu implements IFilterMenu {

    private final ItemDetectorBlockEntity blockEntity;

    public ItemDetectorMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity machine) {
        super(menuType, containerId);
        ItemCollectorMenu.checkContainerSize(inventory, 9);

        this.blockEntity = (ItemDetectorBlockEntity) machine;

        this.addPlayerHotbar(inventory);
        this.addPlayerInventory(inventory);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new FilterOnlySlot(
                    this.blockEntity.getFilter(), i * 3 + j, 98 + j * 18, 18 + i * 18));
            }
        }

        this.addDataSlot(DataSlot.forContainer(this.blockEntity.getDataAccess(), 0));
        this.addDataSlot(DataSlot.forContainer(this.blockEntity.getDataAccess(), 1));
    }

    public ItemDetectorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(extraData.readBlockPos())));
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

    // 功劳归于：: diesieben07 | https://github.com/diesieben07/SevenCommons
    // 必须为 GUI 使用的每个插槽分配一个插槽编号.
    // 对于这个容器，我们可以看到过滤槽以及玩家库存插槽和快捷栏.
    // 每次我们向容器添加 Slot 时，它都会自动增加 slotIndex，这意味着
    //  0 - 8 = 快捷栏插槽（将映射到 InventoryPlayer 插槽编号 0 - 8）
    //  9 - 35 = 玩家物品栏（映射到 InventoryPlayer 插槽编号 9 - 35）
    //  36 - 44 = 过滤槽，映射到我们的 TileEntity 插槽编号 0 - 8）
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int FILTER_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    // THIS YOU HAVE TO DEFINE!
    private static final int FILTER_SLOT_COUNT = 9; // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Check if the slot clicked is one of the vanilla container slots
        if (index >= VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        Slot sourceSlot = this.getSlot(index);
        // noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        } // EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        // This is a vanilla container slot so try to set filter
        for (int j = 0; j < FILTER_SLOT_COUNT; j++) {
            Slot slot = this.getSlot(FILTER_FIRST_SLOT_INDEX + j);
            if (!(slot instanceof FilterOnlySlot filterSlot)) continue;
            if (!filterSlot.getItem().is(sourceStack.getItem())) continue;
            filterSlot.set(sourceStack.copy());
            return ItemStack.EMPTY;
        }
        for (int j = 0; j < FILTER_SLOT_COUNT; j++) {
            Slot slot = this.getSlot(FILTER_FIRST_SLOT_INDEX + j);
            if (!(slot instanceof FilterOnlySlot filterSlot)) continue;
            if (!filterSlot.getItem().isEmpty()) continue;
            filterSlot.set(sourceStack.copy());
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP
            && slotId >= FILTER_FIRST_SLOT_INDEX
            && slotId < FILTER_FIRST_SLOT_INDEX + FILTER_SLOT_COUNT
            && this.getSlot(slotId) instanceof FilterOnlySlot filterSlot
            && (button >= 0 && button < HOTBAR_SLOT_COUNT || button == Inventory.SLOT_OFFHAND)) {
            filterSlot.set(player.getInventory().getItem(button).copy());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public void setFilterMode(ItemDetectorBlockEntity.Mode mode) {
        this.setData(ItemDetectorBlockEntity.DATASLOT_ID_FILTER_MODE, mode.ordinal());
    }

    public void setRange(int range) {
        this.setData(ItemDetectorBlockEntity.DATASLOT_ID_RANGE, range);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean isFilterEnabled() {
        return true;
    }

    @Override
    public void setFilterEnabled(boolean enable) {
    }

    @Override
    public IFilterBlockEntity getFilterBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public boolean isSlotDisabled(int slot) {
        return false;
    }

    @Override
    public void setSlotDisabled(int slot, boolean disable) {
    }

    @Override
    public int getFilterSlotIndex(Slot slot) {
        return slot.getContainerSlot();
    }

    @Override
    public void flush() {
        IFilterMenu.super.flush();
    }

}
