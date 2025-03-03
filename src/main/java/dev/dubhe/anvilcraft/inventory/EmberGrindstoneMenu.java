package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

public class EmberGrindstoneMenu extends AbstractContainerMenu {
    public static final int GOLD_PER_CURSE = 16;
    private final Container repairToolSlots;
    private final Container resultToolSlots;
    private final Container repairMaterialSlots;
    private final Container resultMaterialSlots;
    private final ContainerLevelAccess access;

    public int usedGold = 0;
    public int removedRepairCost = 0;
    public int removedCurseCount = 0;

    public EmberGrindstoneMenu(MenuType<EmberGrindstoneMenu> type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public EmberGrindstoneMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.EMBER_GRINDSTONE.get(), containerId, playerInventory, access);
    }

    /**
     * 皇家砂轮菜单
     *
     * @param type            菜单类型
     * @param containerId     容器id
     * @param playerInventory 背包
     * @param access          检查
     */
    public EmberGrindstoneMenu(
        MenuType<EmberGrindstoneMenu> type,
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access) {
        super(type, containerId);
        this.repairToolSlots = new SimpleContainer(1) {
            public void setChanged() {
                super.setChanged();
                EmberGrindstoneMenu.this.slotsChanged(this);
            }
        };
        this.resultToolSlots = new ResultContainer();
        this.repairMaterialSlots = new SimpleContainer(1) {
            public void setChanged() {
                super.setChanged();
                EmberGrindstoneMenu.this.slotsChanged(this);
            }
        };
        this.resultMaterialSlots = new ResultContainer();
        this.access = access;
        this.addSlot(new Slot(this.repairToolSlots, 0, 25, 34) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
            }
        });
        this.addSlot(new Slot(this.repairMaterialSlots, 0, 89, 22) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(Items.GOLD_INGOT);
            }
        });
        this.addSlot(new Slot(this.resultToolSlots, 2, 145, 34) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                player.playSound(SoundEvents.GRINDSTONE_USE);
                repairToolSlots.setItem(0, ItemStack.EMPTY);
                repairMaterialSlots.setItem(
                    0,
                    new ItemStack(
                        Items.GOLD_INGOT, repairMaterialSlots.getItem(0).getCount() - usedGold));
                resultMaterialSlots.setItem(
                    2,
                    new ItemStack(
                        ModItems.CURSED_GOLD_INGOT.get(),
                        usedGold + resultMaterialSlots.getItem(2).getCount()));
            }
        });
        this.addSlot(new Slot(this.resultMaterialSlots, 2, 89, 47) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private ItemStack createResult() {
        ItemStack repairTool = repairToolSlots.getItem(0);
        ItemStack repairMaterial = repairMaterialSlots.getItem(0);
        if (repairTool.isEmpty() || repairMaterial.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = repairTool.copy();
        ItemEnchantments.Mutable curses = new ItemEnchantments.Mutable(result.getEnchantments());
        curses.removeIf(it -> !it.is(EnchantmentTags.CURSE));
        Map<Holder<Enchantment>, Integer> curseMap = Maps.asMap(curses.keySet(), curses::getLevel);
        int curseNumber = curseMap.size();
        int repairCost = repairTool.getOrDefault(DataComponents.REPAIR_COST, 0);
        int goldCount = repairMaterial.getCount();
        int goldUsed = 0;
        while (goldCount > 0 && repairCost > 0 && goldUsed <= 64) {
            result.set(DataComponents.REPAIR_COST, repairCost - 1);
            repairCost -= 1;
            goldCount -= 1;
            goldUsed += 1;
        }
        if (result.getOrDefault(DataComponents.REPAIR_COST, 0) <= 0) {
            result.remove(DataComponents.REPAIR_COST);
        }
        int removedCurseCound = 0;
        Iterator<Holder<Enchantment>> iterator = curseMap.keySet().iterator();
        while (goldCount >= GOLD_PER_CURSE && curseNumber > 0 && goldUsed < 64) {
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(result.getEnchantments());
            enchantments.removeIf(it -> it == iterator.next());
            ItemStack itemStack = result.copy();
            itemStack.remove(DataComponents.ENCHANTMENTS);
            itemStack.remove(DataComponents.STORED_ENCHANTMENTS);
            EnchantmentHelper.setEnchantments(itemStack, enchantments.toImmutable());
            result = itemStack.copy();
            curseNumber -= 1;
            goldUsed += GOLD_PER_CURSE;
            goldCount -= GOLD_PER_CURSE;
            removedCurseCound += 1;
        }
        if (result.is(Items.ENCHANTED_BOOK) && !EnchantmentHelper.hasAnyEnchantments(result)) {
            result = new ItemStack(Items.BOOK, result.getCount());
        }
        this.usedGold = goldUsed;
        this.removedRepairCost = repairTool.getOrDefault(DataComponents.REPAIR_COST, 0) - repairCost;
        this.removedCurseCount = removedCurseCound;
        return result;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemStack;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack clickedItem = slot.getItem();
            itemStack = clickedItem.copy();
            if (index >= 0 && index <= 3) {
                if (!this.moveItemStackTo(itemStack, 4, 39, false)) {
                    return ItemStack.EMPTY;
                } else {
                    if (index == 2) {
                        slot.onTake(player, clickedItem);
                    }
                    int surplus = clickedItem.getCount() - clickedItem.getMaxStackSize();
                    ItemStack stack = surplus > 0 ? clickedItem.copyWithCount(surplus) : ItemStack.EMPTY;
                    this.getSlot(index).setByPlayer(stack);
                }
            } else {
                ItemStack gold;
                if (itemStack.isDamageableItem() || itemStack.is(Items.ENCHANTED_BOOK) || itemStack.isEnchanted()) {
                    if (!this.getSlot(0).hasItem()) {
                        this.getSlot(0).setByPlayer(itemStack);
                        this.getSlot(index).setByPlayer(ItemStack.EMPTY);
                    } else {
                        return ItemStack.EMPTY;
                    }
                } else if (itemStack.is(Items.GOLD_INGOT)) {
                    if (!this.getSlot(1).hasItem()) {
                        this.getSlot(1).setByPlayer(itemStack);
                        this.getSlot(index).setByPlayer(ItemStack.EMPTY);
                    } else if ((gold = this.getSlot(1).getItem()).is(Items.GOLD_INGOT)
                        && gold.getCount() < gold.getMaxStackSize()) {
                        int canSet = gold.getMaxStackSize() - gold.getCount();
                        canSet = Math.min(itemStack.getCount(), canSet);
                        gold.grow(canSet);
                        itemStack.shrink(canSet);
                        this.getSlot(1).setByPlayer(gold);
                        this.getSlot(index).setByPlayer(itemStack);
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, ModBlocks.EMBER_GRINDSTONE.get());
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        resultToolSlots.setItem(2, createResult());
    }

    /**
     * 移除
     *
     * @param player 玩家
     */
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.access.execute((level, blockPos) -> {
            this.clearContainer(player, this.repairToolSlots);
            this.clearContainer(player, this.repairMaterialSlots);
            this.clearContainer(player, this.resultMaterialSlots);
        });
    }

    protected void clearContainer(Player player, @NotNull Container container) {
        int i;
        if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer) player).hasDisconnected()) {
            for (i = 0; i < container.getContainerSize(); ++i) {
                player.drop(container.removeItemNoUpdate(i), false);
            }

        } else {
            for (i = 0; i < container.getContainerSize(); ++i) {
                Inventory inventory = player.getInventory();
                if (inventory.player instanceof ServerPlayer) {
                    inventory.placeItemBackInInventory(container.removeItemNoUpdate(i));
                }
            }
        }
    }
}
