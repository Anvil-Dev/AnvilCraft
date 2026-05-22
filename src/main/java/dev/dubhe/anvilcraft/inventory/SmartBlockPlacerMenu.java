package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.component.BookOnlySlot;
import dev.dubhe.anvilcraft.inventory.component.StructureDiskOnlySlot;
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

import java.util.Objects;

public class SmartBlockPlacerMenu extends AbstractContainerMenu {
    @Nullable
    private final SmartBlockPlacerBlockEntity blockEntity;
    private final Level level;

    public SmartBlockPlacerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity machine) {
        super(menuType, containerId);
        this.blockEntity = (SmartBlockPlacerBlockEntity) machine;
        this.level = inventory.player.level();

        // 添加Structure Disk物品栏槽位（1个槽位）
        // Smart Block Placer 需要限制结构大小不超过 5x5x5
        int diskSlotX = 8;
        int diskSlotY = 119;
        this.addSlot(new StructureDiskOnlySlot(
            this.blockEntity.getDiskInventory(),
            0,
            diskSlotX,
            diskSlotY,
            true,  // enforceSizeLimit: 强制限制 5x5x5
            // 提取条件：只有当书槽位为空时才能取出磁盘
            () -> this.blockEntity.getBookInventory().getItem(0).isEmpty()
        ));
        
        // 添加蓝图模式书物品栏槽位（输入，1个槽位，只在蓝图模式下显示）
        int bookSlotX = 46;
        int bookSlotY = 86;
        this.addSlot(new BookOnlySlot(
            this.blockEntity.getBookInventory(),
            0,
            bookSlotX,
            bookSlotY,
            // 可见性条件：只有当结构磁盘槽位有物品时才可见
            () -> !this.blockEntity.getDiskInventory().getItem(0).isEmpty()
        ));
        
        // 添加蓝图模式输出书物品栏槽位（输出，1个槽位，只在蓝图模式下显示）
        int outputBookSlotX = 84;
        int outputBookSlotY = 86;
        this.addSlot(new dev.dubhe.anvilcraft.inventory.component.WrittenBookOnlySlot(
            this.blockEntity.getOutputBookInventory(),
            0,
            outputBookSlotX,
            outputBookSlotY,
            // 可见性条件：只有当结构磁盘槽位有物品时才可见
            () -> !this.blockEntity.getDiskInventory().getItem(0).isEmpty()
        ));

        // 添加玩家物品栏（主物品栏3行9列）
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 48 + col * 18, 119 + row * 18));
            }
        }

        // 添加快捷栏（1行9列）
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 48 + col * 18, 177));
        }
    }

    @SuppressWarnings("resource")
    public SmartBlockPlacerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, Objects.requireNonNull(
            inventory.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    @Nullable
    public SmartBlockPlacerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    // Slot索引常量
    private static final int STRUCTURE_DISK_SLOT_COUNT = 1;                 // Structure Disk物品栏1个槽位
    private static final int BOOK_SLOT_COUNT = 1;                           // 书物品栏(输入)1个槽位
    private static final int OUTPUT_BOOK_SLOT_COUNT = 1;                    // 输出书物品栏1个槽位
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;  // 主物品栏3行9列
    private static final int HOTBAR_SLOT_COUNT = 9;             // 快捷栏1行9列
    private static final int VANILLA_SLOT_COUNT = PLAYER_INVENTORY_SLOT_COUNT + HOTBAR_SLOT_COUNT;
    private static final int TOTAL_SLOT_COUNT = STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT + VANILLA_SLOT_COUNT;

    @SuppressWarnings("checkstyle:RightCurly")
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            itemstack = originalStack.copy();

            // Structure Disk槽位（索引0）的物品移动到玩家物品栏
            if (index < STRUCTURE_DISK_SLOT_COUNT) {
                // 检查书槽位是否有书，如果有则不允许取出磁盘
                if (this.blockEntity != null && !this.blockEntity.getBookInventory().getItem(0).isEmpty()) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(originalStack, 
                    STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT, TOTAL_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Book槽位（索引1）的物品移动到玩家物品栏
            else if (index < STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT) {
                if (!this.moveItemStackTo(originalStack, 
                    STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT, TOTAL_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Output Book槽位（索引2）的物品移动到玩家物品栏
            else if (index < STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT) {
                if (!this.moveItemStackTo(originalStack, 
                    STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 玩家物品栏的物品移动
            else if (index < TOTAL_SLOT_COUNT) {
                // 检查是否是蓝图模式
                boolean isBlueprintMode = this.blockEntity != null && !this.blockEntity.getDiskInventory().getItem(0).isEmpty();
                
                if (originalStack.is(ModItems.STRUCTURE_DISK.get())) {
                    // Structure Disk尝试移动到Disk槽位
                    if (!this.moveItemStackTo(originalStack, 0, STRUCTURE_DISK_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isBlueprintMode 
                    && (originalStack.is(net.minecraft.world.item.Items.WRITTEN_BOOK)
                    || originalStack.is(net.minecraft.world.item.Items.WRITABLE_BOOK)
                    || originalStack.is(net.minecraft.world.item.Items.BOOK))) {
                    // 蓝图模式下的书尝试移动到Book槽位（输入）
                    if (!this.moveItemStackTo(originalStack, 
                        STRUCTURE_DISK_SLOT_COUNT, STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // 其他物品在玩家物品栏内部移动（主物品栏<->快捷栏）
                    // 非蓝图模式下,书也会走这个分支
                    // 玩家物品栏始终从索引 3 开始（disk + book + outputBook）
                    int playerInventoryStart = STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT;
                    int playerInventoryEnd = 
                        STRUCTURE_DISK_SLOT_COUNT + BOOK_SLOT_COUNT + OUTPUT_BOOK_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
                    
                    if (index >= playerInventoryEnd) {
                        // 从快捷栏移动到主物品栏
                        if (!this.moveItemStackTo(originalStack, playerInventoryStart, playerInventoryEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // 从主物品栏移动到快捷栏
                        if (!this.moveItemStackTo(originalStack, playerInventoryEnd, TOTAL_SLOT_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (originalStack.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, originalStack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null) {
            return false;
        }
        return stillValid(
            ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
            player,
            ModBlocks.SMART_BLOCK_PLACER.get()
        );
    }
}
