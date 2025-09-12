package dev.dubhe.anvilcraft.inventory;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RoyalGrindstoneMenu extends AbstractContainerMenu {
    // Map<repairMaterial, Pair<perUnitRepair, resultMaterial>>
    public static final Map<Item, Pair<Integer, Item>> REPAIR_COST_RECIPES = new HashMap<>();
    public static final Item DEFAULT_REPAIR_MATERIAL = Items.GOLD_INGOT;
    public static final int GOLD_PER_CURSE = 16;
    private final Container repairToolSlots;
    private final Container resultToolSlots;
    private final Container repairMaterialSlots;
    private final Container resultMaterialSlots;
    private final ContainerLevelAccess access;

    public int usedGold = 0;
    public int totalRepairCost = 0;
    public int totalCurseCount = 0;
    public int removedRepairCost = 0;
    public int removedCurseCount = 0;
    public Item repairMaterial = null;
    public Pair<Integer, Item> currentRecipe = null;

    static {
        REPAIR_COST_RECIPES.put(Items.GOLD_INGOT, new Pair<>(1, ModItems.CURSED_GOLD_INGOT.get()));
        REPAIR_COST_RECIPES.put(Items.GOLD_BLOCK, new Pair<>(9, ModBlocks.CURSED_GOLD_BLOCK.asItem()));
    }

    public RoyalGrindstoneMenu(MenuType<RoyalGrindstoneMenu> type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public RoyalGrindstoneMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.ROYAL_GRINDSTONE.get(), containerId, playerInventory, access);
    }

    /**
     * 皇家砂轮菜单
     *
     * @param type            菜单类型
     * @param containerId     容器id
     * @param playerInventory 背包
     * @param access          检查
     */
    public RoyalGrindstoneMenu(
        MenuType<RoyalGrindstoneMenu> type,
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access) {
        super(type, containerId);
        this.repairToolSlots = new SimpleContainer(1) {
            public void setChanged() {
                super.setChanged();
                RoyalGrindstoneMenu.this.slotsChanged(this);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        this.resultToolSlots = new ResultContainer();
        this.repairMaterialSlots = new SimpleContainer(1) {
            public void setChanged() {
                super.setChanged();
                RoyalGrindstoneMenu.this.slotsChanged(this);
            }
        };
        this.resultMaterialSlots = new ResultContainer() {
            public void setChanged() {
                RoyalGrindstoneMenu.this.slotsChanged(this);
            }
        };
        this.access = access;
        this.addSlot(new Slot(this.repairToolSlots, 0, 25, 34) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
            }
        });
        this.addSlot(new Slot(this.repairMaterialSlots, 0, 89, 22) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isRepairMaterial(stack);
            }
        });
        this.addSlot(new Slot(this.resultToolSlots, 2, 145, 34) {
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                player.playSound(SoundEvents.GRINDSTONE_USE);
                if (currentRecipe != null) {
                    resultMaterialSlots.setItem(2, new ItemStack(currentRecipe.getSecond(), usedGold + resultMaterialSlots.getItem(2).getCount()));
                    repairMaterialSlots.setItem(0, new ItemStack(repairMaterial, repairMaterialSlots.getItem(0).getCount() - usedGold));
                }
                repairToolSlots.setItem(0, ItemStack.EMPTY);
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
        this.totalRepairCost = 0;
        this.totalCurseCount = 0;
        this.removedRepairCost = 0;
        this.removedCurseCount = 0;
        ItemStack repairTool = repairToolSlots.getItem(0);
        ItemStack repairMaterialSlotItem = repairMaterialSlots.getItem(0);
        ItemStack resultMaterialSlotItem = resultMaterialSlots.getItem(0);
        this.repairMaterial = repairMaterialSlotItem.getItem();
        ItemStack result = repairTool.copy();
        this.currentRecipe = REPAIR_COST_RECIPES.getOrDefault(repairMaterialSlotItem.getItem(), null);
        int repairCost = repairTool.getOrDefault(DataComponents.REPAIR_COST, 0);
        this.totalRepairCost = repairCost;
        DataComponentType<ItemEnchantments> enchantmentComponent = result.is(Items.ENCHANTED_BOOK)
                                                                   ? DataComponents.STORED_ENCHANTMENTS
                                                                   : DataComponents.ENCHANTMENTS;
        ItemEnchantments enchantments = result.get(enchantmentComponent);
        ItemEnchantments.Mutable mutEnch = null;
        if (enchantments != null) {
            this.totalCurseCount = (int) enchantments.keySet()
                .stream()
                .filter(it -> it.is(EnchantmentTags.CURSE))
                .count();
            mutEnch = new ItemEnchantments.Mutable(enchantments);
        }
        if (repairTool.isEmpty()
            || repairMaterialSlotItem.isEmpty()
            || this.currentRecipe == null) {
            this.usedGold = 0;
            return ItemStack.EMPTY;
        }
        int repairMaterialUsable = Math.min(repairMaterialSlotItem.getCount(), currentRecipe.getSecond().getDefaultMaxStackSize() - resultMaterialSlotItem.getCount());
        int perUnitRepair = this.currentRecipe.getFirst();
        int maxUnitsByCost = repairCost / perUnitRepair;
        this.usedGold = Math.min(maxUnitsByCost, repairMaterialUsable);
        int maxRemovable = perUnitRepair * this.usedGold;
        repairMaterialUsable -= this.usedGold;
        removedRepairCost = Math.min(repairCost, maxRemovable);
        int remainRepairCost = repairCost - removedRepairCost;
        result.set(DataComponents.REPAIR_COST, remainRepairCost);
        if (repairMaterialSlotItem.is(DEFAULT_REPAIR_MATERIAL)
            && repairMaterialSlotItem.getCount() - this.usedGold >= GOLD_PER_CURSE
            && mutEnch != null) {
            Iterator<Holder<Enchantment>> iterator = mutEnch.keySet().iterator();
            while (iterator.hasNext() && repairMaterialUsable >= GOLD_PER_CURSE) {
                Holder<Enchantment> curseEnchantment = iterator.next();
                if (!curseEnchantment.is(EnchantmentTags.CURSE)) continue;
                iterator.remove();
                this.usedGold += GOLD_PER_CURSE;
                repairMaterialUsable -= GOLD_PER_CURSE;
                removedCurseCount += 1;
            }
            result.set(enchantmentComponent, mutEnch.toImmutable());
            if (result.is(Items.ENCHANTED_BOOK) && !EnchantmentHelper.hasAnyEnchantments(result)) result = result.transmuteCopy(Items.BOOK);
        }
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
                        this.getSlot(0).setByPlayer(itemStack.copyWithCount(1));
                        itemStack.shrink(1);
                        this.getSlot(index).setByPlayer(itemStack.isEmpty() ? ItemStack.EMPTY : itemStack);
                    } else {
                        return ItemStack.EMPTY;
                    }
                } else if (isRepairMaterial(itemStack)) {
                    if (!this.getSlot(1).hasItem()) {
                        this.getSlot(1).setByPlayer(itemStack);
                        this.getSlot(index).setByPlayer(ItemStack.EMPTY);
                    } else if ((isRepairMaterial(gold = this.getSlot(1).getItem()))
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
        return stillValid(this.access, player, ModBlocks.ROYAL_GRINDSTONE.get());
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container.equals(this.repairMaterialSlots)
            || container.equals(this.repairToolSlots)) resultToolSlots.setItem(2, createResult());
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

    private boolean isRepairMaterial(ItemStack stack) {
        return REPAIR_COST_RECIPES.containsKey(stack.getItem());
    }
}
