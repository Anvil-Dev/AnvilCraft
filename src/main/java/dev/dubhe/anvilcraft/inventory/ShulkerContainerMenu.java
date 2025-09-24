package dev.dubhe.anvilcraft.inventory;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.api.container.item.ItemEntry;
import dev.dubhe.anvilcraft.block.entity.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ShulkerContainerSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ShulkerContainerMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    public final ShulkerContainerBlockEntity blockEntity;
    private final Level level;
    private final ContainerStorage storage;

    @SuppressWarnings("DataFlowIssue")
    public ShulkerContainerMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    /**
     * 潜影集装箱菜单
     *
     * @param menuType    菜单类型
     * @param containerId 容器id
     * @param inventory   背包
     * @param blockEntity 方块实体
     */
    public ShulkerContainerMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId);
        this.blockEntity = (ShulkerContainerBlockEntity) blockEntity;
        this.level = inventory.player.level();
        this.storage = ContainerStorages.get().getOrCreateStorage(this.blockEntity.getUUID());

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        this.addContainerSlots();
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 114 + l * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 114 + i * 18, 198));
        }
    }

    private void addContainerSlots() {
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new ShulkerContainerSlot(this.storage, i, j, 114, 18, 18));
            }
        }
    }

    // 致谢： diesieben07 |https://github.com/diesieben07/SevenCommons
    // 必须为 GUI 使用的每个插槽分配一个插槽编号。
    // 对于这个容器，我们可以看到瓷砖库存的插槽以及玩家库存插槽和快捷栏。
    // 每次我们向容器添加 Slot 时，它都会自动增加 slotIndex，这意味着
    // 0 - 8 = 快捷栏插槽（将映射到 InventoryPlayer 插槽编号 0 - 8）
    // 9 - 35 = 玩家库存槽（映射到 InventoryPlayer 槽位编号 9 - 35）
    // 36 - 44 = TileInventory 插槽，映射到我们的 TileEntity 插槽编号 0 - 8）
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 54; // must be the number of slots you have!

    @SuppressWarnings("DuplicatedCode")
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        //noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY; // EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        final ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (this.moveItemToActiveSlot(index)) return ItemStack.EMPTY; // EMPTY_ITEM
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!this.moveItemToPlayer(index)) return ItemStack.EMPTY;
        } else {
            ShulkerContainerMenu.LOGGER.warn("Invalid slotIndex: {}", index);
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

    private boolean moveItemToActiveSlot(int sourceIndex) {
        Slot source = this.slots.get(sourceIndex);
        ItemStack stack = source.getItem();
        if (!this.storage.getEntries().isMaxEntries()) {
            boolean result = this.storage.addItem(stack);
            if (result) {
                stack.setCount(0);
                source.setChanged();
            }
            return result;
        }
        for (ItemEntry entry : this.storage.getEntries().getEntries()) {
            if (stack.is(entry.item())) {
                entry.merge(stack, this.storage.getLevel().getStackPower());
                stack.setCount(0);
                source.setChanged();
                return true;
            }
        }
        return false;
    }

    private boolean moveItemToPlayer(int sourceIndex) {
        boolean result = false;
        Slot source = this.slots.get(sourceIndex);
        if (!(source instanceof ShulkerContainerSlot scSlot)) return false;
        ItemStack stack = scSlot.getItem();
        int originCount = stack.getCount();
        if (stack.isStackable()) {
            for (int i = 0; i < 36; i++) {
                Slot target = this.slots.get(i);
                ItemStack already = target.getItem();
                if (!already.isEmpty() && ItemStack.isSameItemSameComponents(stack, already)) {
                    int totalCount = already.getCount() + stack.getCount();
                    int maxSize = target.getMaxStackSize(already);
                    if (totalCount <= maxSize) {
                        scSlot.remove(stack.getCount());
                        already.setCount(totalCount);
                        target.setChanged();
                        return true;
                    } else if (already.getCount() < maxSize) {
                        stack.shrink(maxSize - already.getCount());
                        already.setCount(maxSize);
                        target.setChanged();
                        result = true;
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            for (int i = 0; i < 36; i++) {
                Slot target = this.slots.get(i);
                ItemStack already = target.getItem();
                if (already.isEmpty() && target.mayPlace(stack)) {
                    int maxSize = target.getMaxStackSize(stack);
                    target.setByPlayer(stack.split(Math.min(stack.getCount(), maxSize)));
                    target.setChanged();
                    return true;
                }
            }
        }
        scSlot.remove(originCount - stack.getCount());
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.SHULKER_CONTAINER.get());
    }
}
