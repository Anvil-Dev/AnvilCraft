package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

@Getter
public class AutoEnchantingTableMenu extends AbstractContainerMenu {
    private final AutoEnchantingTableBlockEntity blockEntity;
    private final Container container;

    private final Level level;

    public AutoEnchantingTableMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        FriendlyByteBuf extraData
    ) {
        this(menuType, containerId, inventory, (AutoEnchantingTableBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AutoEnchantingTableMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        AutoEnchantingTableBlockEntity blockEntity
    ) {
        super(menuType, containerId);
        this.level = inventory.player.level();
        this.blockEntity = blockEntity;
        this.container = blockEntity;

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        this.addSlot(new Slot(this.container, 0, 7,  18) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        this.addSlot(new Slot(this.container, 1, 7,  52) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        this.addSlot(new Slot(this.container, 2, 27,  18));
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

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        AnvilCraft.LOGGER.debug("index: {}", slotIndex);
        if (slotIndex == 36 || slotIndex == 37) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(slotIndex);
        if (slotIndex == 38) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty()) {
                if (!this.moveItemStackTo(item,
                    1, 36, true)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (slotIndex >= 0 && slotIndex <= 35) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty()) {
                if (!this.moveItemStackTo(item,
                    38, 39, true)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
