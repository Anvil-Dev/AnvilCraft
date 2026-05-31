package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.FilterOnlySlot;
import dev.dubhe.anvilcraft.network.multiple.TradingStationPackets;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@Getter
public class TradingStationMenu extends AbstractContainerMenu {
    private final TradingStationBlockEntity be;
    private final Level level;

    public TradingStationMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(
            menuType,
            containerId,
            inventory,
            inventory.player.level().getBlockEntity(extraData.readBlockPos(), ModBlockEntities.TRADING_STATION.get()).orElseThrow()
        );
    }

    public TradingStationMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, TradingStationBlockEntity be) {
        super(menuType, containerId);

        this.be = be;
        be.setOwner(inventory.player.getGameProfile().getId());
        this.level = inventory.player.level();

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 4; ++column) {
                this.addSlot(new SlotItemHandler(
                    this.be.getHandler(),
                    row * 4 + column,
                    98 + column * 18,
                    18 + row * 18
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return TradingStationMenu.this.be.canPlace(stack) && super.mayPlace(stack);
                    }
                });
            }
        }

        this.addSlot(new FilterOnlySlot(this.be.getFilters(), 0, 8, 25) {
            @Override
            public void set(ItemStack stack) {
                if (TradingStationMenu.this.level.isClientSide) {
                    PacketDistributor.sendToServer(new TradingStationPackets.SyncFilter(
                        TradingStationMenu.this.be.getBlockPos(),
                        0,
                        stack
                    ));
                }
                super.set(stack);
            }
        });
        this.addSlot(new FilterOnlySlot(this.be.getFilters(), 1, 26, 25) {
            @Override
            public void set(ItemStack stack) {
                if (TradingStationMenu.this.level.isClientSide) {
                    PacketDistributor.sendToServer(new TradingStationPackets.SyncFilter(
                        TradingStationMenu.this.be.getBlockPos(),
                        1,
                        stack
                    ));
                }
                super.set(stack);
            }
        });
        this.addSlot(new FilterOnlySlot(this.be.getFilters(), 2, 69, 25) {
            @Override
            public void set(ItemStack stack) {
                if (TradingStationMenu.this.level.isClientSide) {
                    PacketDistributor.sendToServer(new TradingStationPackets.SyncFilter(
                        TradingStationMenu.this.be.getBlockPos(),
                        2,
                        stack
                    ));
                }
                super.set(stack);
            }
        });
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

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 12; // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        // noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        final ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (this.moveItemToActiveSlot(sourceStack)) {
                return ItemStack.EMPTY; // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!this.moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT + 3) {
            // This is a filter slot so just skip it
            return ItemStack.EMPTY;
        } else {
            System.out.println("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    // 移动物品到可用槽位
    private boolean moveItemToActiveSlot(ItemStack stack) {
        int count = stack.getCount();
        for (int index = TE_INVENTORY_FIRST_SLOT_INDEX; index < 48; index++) {
            // 只有对应槽位可以放入物品时才向槽位里快速移动物品
            if (this.canPlace(stack, index)) {
                this.moveItemStackTo(stack, index, index + 1, false);
                if (stack.isEmpty()) break;
            }
        }
        return stack.getCount() >= count;
    }

    // 是否可以向槽位中放入物品
    protected boolean canPlace(ItemStack stack, int index) {
        return !(this.getSlot(index) instanceof SlotItemHandler) || this.be.isProviding(stack) || this.be.isRequesting(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(this.level, this.be.getBlockPos()), player, ModBlocks.TRADING_STATION.get());
    }
}
