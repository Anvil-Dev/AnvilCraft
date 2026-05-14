package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;  // 主物品栏3行9列
    private static final int HOTBAR_SLOT_COUNT = 9;             // 快捷栏1行9列
    private static final int VANILLA_SLOT_COUNT = PLAYER_INVENTORY_SLOT_COUNT + HOTBAR_SLOT_COUNT;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            itemstack = originalStack.copy();

            // 判断点击的是主物品栏还是快捷栏
            if (index < PLAYER_INVENTORY_SLOT_COUNT) {
                // 点击的是主物品栏（索引0-26），移动到快捷栏（索引27-35）
                if (!this.moveItemStackTo(originalStack, PLAYER_INVENTORY_SLOT_COUNT, VANILLA_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < VANILLA_SLOT_COUNT) {
                // 点击的是快捷栏（索引27-35），移动到主物品栏（索引0-26）
                if (!this.moveItemStackTo(originalStack, 0, PLAYER_INVENTORY_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
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
