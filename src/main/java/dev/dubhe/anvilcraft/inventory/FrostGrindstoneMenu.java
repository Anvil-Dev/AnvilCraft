package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import dev.dubhe.anvilcraft.util.ListUtil;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class FrostGrindstoneMenu extends AbstractContainerMenu {
    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            FrostGrindstoneMenu.this.slotsChanged(this);
        }
    };
    private final Container result = new ResultContainer();
    private final ContainerLevelAccess access;

    @Getter
    private final IntSet selectedIndexes = new IntArraySet();
    @Getter
    private final List<EnchantmentData> enchantments = new CopyOnWriteArrayList<>();

    public FrostGrindstoneMenu(MenuType<FrostGrindstoneMenu> type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public FrostGrindstoneMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.FROST_GRINDSTONE.get(), containerId, playerInventory, access);
    }

    /**
     * 浮霜砂轮菜单
     *
     * @param type            菜单类型
     * @param containerId     容器id
     * @param playerInventory 背包
     * @param access          检查
     */
    public FrostGrindstoneMenu(
        MenuType<FrostGrindstoneMenu> type,
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access
    ) {
        super(type, containerId);
        this.access = access;
        this.addSlot(new Slot(this.input, 0, 25, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                for (DataComponentType<ItemEnchantments> type : CompatUtil.ENCHANTMENTS_TYPES) {
                    if (!stack.getOrDefault(type, ItemEnchantments.EMPTY).isEmpty()) return true;
                }
                return stack.isDamageableItem()
                       || stack.isEnchanted()
                       || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                       || !stack.getOrDefault(ModComponents.MERCILESS_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                refreshEnchantments();
            }
        });
        this.addSlot(new Slot(this.result, 0, 145, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                access.execute((level, pos) -> {
                    if (level instanceof ServerLevel serverLevel) {
                        ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), this.getExperienceAmount(level));
                    }

                    level.levelEvent(1042, pos, 0);
                });

                FrostGrindstoneMenu.this.input.setItem(0, ItemStack.EMPTY);
                FrostGrindstoneMenu.this.refreshEnchantments();
                FrostGrindstoneMenu.this.selectedIndexes.clear();
                FrostGrindstoneMenu.this.result.setItem(0, ItemStack.EMPTY);
            }

            /**
             * Returns the total amount of XP stored in all the input slots of this container.
             * The return value is randomized, so that it returns between 100% and 200% of the total XP.
             */
            private int getExperienceAmount(Level level) {
                int xp = this.getExperience();
                return xp > 0 ? xp + level.random.nextInt(xp) : 0;
            }

            /**
             * Returns the total amount of XP stored in the enchantments of this stack.
             */
            private int getExperience() {
                int xp = 0;
                for (int selected : FrostGrindstoneMenu.this.selectedIndexes) {
                    EnchantmentData data = ListUtil.safelyGet(FrostGrindstoneMenu.this.enchantments, selected).orElse(null);
                    if (data == null) continue;

                    Holder<Enchantment> holder = data.enchantment();
                    if (holder.is(EnchantmentTags.CURSE)) continue;

                    xp += holder.value().getMinCost(data.level());
                }

                return xp;
            }
        });
        int row;
        for (row = 0; row < 3; row++) {
            for (int colomn = 0; colomn < 9; colomn++) {
                this.addSlot(new Slot(playerInventory, colomn + row * 9 + 9, 8 + colomn * 18, 84 + row * 18));
            }
        }

        for (row = 0; row < 9; row++) {
            this.addSlot(new Slot(playerInventory, row, 8 + row * 18, 142));
        }
    }

    private ItemStack createResult() {
        if (!this.hasSelectedEnchantment() || this.input.isEmpty()) return ItemStack.EMPTY;
        ItemStack inputItem = this.input.getItem(0).copy();

        Map<DataComponentType<ItemEnchantments>, ItemEnchantments.Mutable> mutableMap = new HashMap<>();
        int cost = 0;
        for (int i = 0; i < this.enchantments.size(); i++) {
            EnchantmentData data = ListUtil.safelyGet(this.enchantments, i).orElse(null);
            if (data == null) continue;

            ItemEnchantments.Mutable newMut = mutableMap.computeIfAbsent(
                data.type(),
                type -> new ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
            );
            if (this.selectedIndexes.contains(i)) continue;
            newMut.set(data.enchantment(), data.level());

            cost = AnvilMenu.calculateIncreasedRepairCost(cost);
        }

        for (Map.Entry<DataComponentType<ItemEnchantments>, ItemEnchantments.Mutable> entry : mutableMap.entrySet()) {
            inputItem.set(entry.getKey(), entry.getValue().toImmutable());
        }
        inputItem.set(DataComponents.REPAIR_COST, cost);

        ItemEnchantments.Mutable stored = mutableMap.get(DataComponents.STORED_ENCHANTMENTS);
        if (inputItem.is(Items.ENCHANTED_BOOK) && stored != null && stored.keySet().isEmpty()) {
            return inputItem.transmuteCopy(Items.BOOK);
        } else {
            return inputItem;
        }

    }

    private void refreshEnchantments() {
        ItemStack input = this.getSlot(0).getItem();
        this.enchantments.clear();
        this.addEnchantments(
            input,
            DataComponents.ENCHANTMENTS,
            DataComponents.STORED_ENCHANTMENTS,
            ModComponents.MERCILESS_ENCHANTMENTS
        );
        for (DataComponentType<ItemEnchantments> type : CompatUtil.ENCHANTMENTS_TYPES) {
            this.addEnchantments(input, type);
        }
        this.enchantments.sort(EnchantmentData::compareTo);
    }

    private void addEnchantments(ItemStack input, DataComponentType<ItemEnchantments> type) {
        for (var entry : input.getOrDefault(type, ItemEnchantments.EMPTY).entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (enchantment.is(EnchantmentTags.CURSE)) continue;
            this.enchantments.add(new EnchantmentData(type, enchantment, entry.getIntValue()));
        }
    }

    @SafeVarargs
    private void addEnchantments(ItemStack input, DataComponentType<ItemEnchantments>... types) {
        for (DataComponentType<ItemEnchantments> type : types) {
            this.addEnchantments(input, type);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.getSlot(0).getItem().isEmpty()) {
            this.selectedIndexes.clear();
        }
        this.getSlot(1).set(this.createResult());
    }

    public void select(int index) {
        int size = this.selectedIndexes.size();
        this.selectedIndexes.add(index);
        if (this.selectedIndexes.size() != size) this.getSlot(1).set(this.createResult());
    }

    public void unselect(int index) {
        int size = this.selectedIndexes.size();
        this.selectedIndexes.remove(index);
        if (this.selectedIndexes.size() != size) this.getSlot(1).set(this.createResult());
    }

    public boolean hasSelectedEnchantment() {
        return !this.selectedIndexes.isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack clickedItem = slot.getItem();
        stack = clickedItem.copy();
        if (index >= 0 && index <= 1) {
            if (!this.moveItemStackTo(stack, 2, 38, false)) return ItemStack.EMPTY;
            if (index == 1) {
                slot.onTake(player, clickedItem);
            } else {
                int remain = clickedItem.getCount() - clickedItem.getMaxStackSize();
                this.getSlot(index).setByPlayer(remain > 0 ? clickedItem.copyWithCount(remain) : ItemStack.EMPTY);
            }
        } else {
            if (!this.getSlot(0).mayPlace(clickedItem)) return ItemStack.EMPTY;
            if (!this.getSlot(0).hasItem()) {
                this.getSlot(0).setByPlayer(stack);
                this.getSlot(index).setByPlayer(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.FROST_GRINDSTONE.get());
    }

    /**
     * 移除
     *
     * @param player 玩家
     */
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.input));
    }

    protected void clearContainer(Player player, Container container) {
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
