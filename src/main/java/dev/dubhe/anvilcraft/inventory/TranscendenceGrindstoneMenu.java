package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 超限砂轮菜单，根据下槽内容切换祛魔、转移、液态魔咒封存和黄金净化模式。
 */
public class TranscendenceGrindstoneMenu extends AbstractContainerMenu {
    private static final int GOLD_PER_CURSE = RoyalGrindstoneMenu.GOLD_PER_CURSE;
    private static final int PLAYER_INVENTORY_SLOT_START = 3;
    private static final int PLAYER_INVENTORY_SLOT_END = 39;

    private final Container source;
    private final Container modifier;
    private final Container result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final HolderLookup.Provider registries;

    @Getter
    private final IntSet selectedIndexes = new IntArraySet();
    @Getter
    private final List<EnchantmentData> enchantments = new CopyOnWriteArrayList<>();
    @Getter
    private int usedGold;
    @Getter
    private int totalRepairCost;
    @Getter
    private int totalCurseCount;
    @Getter
    private int removedRepairCost;
    @Getter
    private int removedCurseCount;

    public TranscendenceGrindstoneMenu(
        MenuType<TranscendenceGrindstoneMenu> type,
        int containerId,
        Inventory playerInventory
    ) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public TranscendenceGrindstoneMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.TRANSCENDENCE_GRINDSTONE.get(), containerId, playerInventory, access);
    }

    /**
     * 创建超限砂轮菜单。
     *
     * @param type            菜单类型
     * @param containerId     容器 id
     * @param playerInventory 玩家背包
     * @param access          方块位置访问器
     */
    public TranscendenceGrindstoneMenu(
        MenuType<TranscendenceGrindstoneMenu> type,
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access
    ) {
        super(type, containerId);
        this.access = access;
        this.registries = playerInventory.player.level().registryAccess();
        this.source = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                TranscendenceGrindstoneMenu.this.selectedIndexes.clear();
                TranscendenceGrindstoneMenu.this.refreshEnchantments();
                TranscendenceGrindstoneMenu.this.slotsChanged(this);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        this.modifier = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                TranscendenceGrindstoneMenu.this.restrictSelectionForMode();
                TranscendenceGrindstoneMenu.this.slotsChanged(this);
            }
        };

        this.addSlot(new Slot(this.source, 0, 25, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TranscendenceGrindstoneMenu.isValidSource(stack);
            }
        });
        this.addSlot(new Slot(this.modifier, 0, 25, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TranscendenceGrindstoneMenu.this.isValidModifier(stack);
            }
        });
        this.addSlot(new Slot(this.result, 0, 145, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                if (!this.hasItem()) return false;
                if (!TranscendenceGrindstoneMenu.this.isTransferMode()) return true;
                return player.hasInfiniteMaterials()
                       || player.experienceLevel >= TranscendenceGrindstoneMenu.this.getCost();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                TranscendenceGrindstoneMenu.this.takeResult(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    private static boolean isValidSource(ItemStack stack) {
        return stack.isDamageableItem()
               || stack.getOrDefault(DataComponents.REPAIR_COST, 0) > 0
               || hasAnyEnchantments(stack);
    }

    private boolean isValidModifier(ItemStack stack) {
        return isGold(stack)
               || stack.is(Items.BOOK)
               || this.isSmallTank(stack)
               || this.isLargeTank(stack)
               || isTransferTarget(stack);
    }

    private static boolean isTransferTarget(ItemStack stack) {
        return stack.getItem().isEnchantable(stack)
               || stack.isDamageableItem()
               || stack.is(ItemTags.DURABILITY_ENCHANTABLE)
               || stack.is(ItemTags.MINING_ENCHANTABLE)
               || stack.is(ItemTags.WEAPON_ENCHANTABLE)
               || hasAnyEnchantments(stack);
    }

    private static boolean isGold(ItemStack stack) {
        return stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_BLOCK);
    }

    private boolean isSmallTank(ItemStack stack) {
        return FluidTankBlockEntity.isEmptyItem(stack, this.registries);
    }

    private boolean isLargeTank(ItemStack stack) {
        return LargeFluidTankBlockEntity.isEmptyItem(stack, this.registries);
    }

    private static boolean hasAnyEnchantments(ItemStack stack) {
        for (DataComponentType<ItemEnchantments> type : getEnchantmentTypes()) {
            if (!stack.getOrDefault(type, ItemEnchantments.EMPTY).isEmpty()) return true;
        }
        return false;
    }

    private static List<DataComponentType<ItemEnchantments>> getEnchantmentTypes() {
        LinkedHashSet<DataComponentType<ItemEnchantments>> types = new LinkedHashSet<>();
        types.add(DataComponents.ENCHANTMENTS);
        types.add(DataComponents.STORED_ENCHANTMENTS);
        types.add(ModComponents.MERCILESS_ENCHANTMENTS);
        types.addAll(CompatUtil.ENCHANTMENTS_TYPES);
        return List.copyOf(types);
    }

    private Mode getMode() {
        ItemStack stack = this.modifier.getItem(0);
        if (stack.isEmpty()) return Mode.DISENCHANT;
        if (isGold(stack)) return Mode.GOLD;
        if (stack.is(Items.BOOK)) return Mode.BOOK;
        if (this.isSmallTank(stack)) return Mode.SMALL_TANK;
        if (this.isLargeTank(stack)) return Mode.LARGE_TANK;
        if (isTransferTarget(stack)) return Mode.ITEM;
        return Mode.UNSUPPORTED;
    }

    public boolean isGoldMode() {
        return this.getMode() == Mode.GOLD;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isTransferMode() {
        Mode mode = this.getMode();
        return mode == Mode.BOOK || mode == Mode.ITEM;
    }

    private ItemStack createResult() {
        if (this.source.isEmpty()) {
            this.resetGoldStats();
            return ItemStack.EMPTY;
        }
        return switch (this.getMode()) {
            case GOLD -> this.createGoldResult();
            case BOOK, ITEM -> {
                this.resetGoldStats();
                yield this.createTransferResult();
            }
            case SMALL_TANK, LARGE_TANK -> {
                this.resetGoldStats();
                yield this.createTankResult();
            }
            case DISENCHANT -> {
                this.resetGoldStats();
                yield this.hasSelectedEnchantment() ? this.createSourceWithoutSelected() : ItemStack.EMPTY;
            }
            case UNSUPPORTED -> {
                this.resetGoldStats();
                yield ItemStack.EMPTY;
            }
        };
    }

    private ItemStack createGoldResult() {
        final ItemStack sourceStack = this.source.getItem(0);
        final ItemStack goldStack = this.modifier.getItem(0);
        final ItemStack output = sourceStack.copy();
        this.totalRepairCost = sourceStack.getOrDefault(DataComponents.REPAIR_COST, 0);
        this.totalCurseCount = this.countCurses(sourceStack);

        int repairPerItem = goldStack.is(Items.GOLD_BLOCK) ? 9 : 1;
        int usedForRepair = Math.min(this.totalRepairCost / repairPerItem, goldStack.getCount());
        this.removedRepairCost = usedForRepair * repairPerItem;
        this.usedGold = usedForRepair;
        output.set(DataComponents.REPAIR_COST, this.totalRepairCost - this.removedRepairCost);

        this.removedCurseCount = 0;
        if (goldStack.is(Items.GOLD_INGOT)) {
            int remainingGold = goldStack.getCount() - this.usedGold;
            int removableCurses = Math.min(this.totalCurseCount, remainingGold / GOLD_PER_CURSE);
            this.removedCurseCount = this.removeCurses(output, removableCurses);
            this.usedGold += this.removedCurseCount * GOLD_PER_CURSE;
        }

        if (output.is(Items.ENCHANTED_BOOK) && !hasAnyEnchantments(output)) {
            return output.transmuteCopy(Items.BOOK);
        }
        return output;
    }

    private int countCurses(ItemStack stack) {
        int count = 0;
        for (DataComponentType<ItemEnchantments> type : getEnchantmentTypes()) {
            for (Holder<Enchantment> enchantment : stack.getOrDefault(type, ItemEnchantments.EMPTY).keySet()) {
                if (enchantment.is(EnchantmentTags.CURSE)) count++;
            }
        }
        return count;
    }

    private int removeCurses(ItemStack stack, int amount) {
        int removed = 0;
        for (DataComponentType<ItemEnchantments> type : getEnchantmentTypes()) {
            if (removed >= amount) break;
            ItemEnchantments current = stack.getOrDefault(type, ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
            boolean changed = false;
            for (Holder<Enchantment> enchantment : current.keySet()) {
                if (removed >= amount) break;
                if (!enchantment.is(EnchantmentTags.CURSE)) continue;
                mutable.removeIf(enchantment::equals);
                removed++;
                changed = true;
            }
            if (changed) stack.set(type, mutable.toImmutable());
        }
        return removed;
    }

    private ItemStack createTransferResult() {
        List<EnchantmentData> selected = this.getSelectedEnchantments();
        if (selected.isEmpty()) return ItemStack.EMPTY;

        ItemStack target = this.modifier.getItem(0).copyWithCount(1);
        DataComponentType<ItemEnchantments> targetType = DataComponents.ENCHANTMENTS;
        if (target.is(Items.BOOK)) {
            target = target.transmuteCopy(Items.ENCHANTED_BOOK);
            targetType = DataComponents.STORED_ENCHANTMENTS;
        } else if (target.is(Items.ENCHANTED_BOOK)) {
            targetType = DataComponents.STORED_ENCHANTMENTS;
        }

        ItemEnchantments oldEnchantments = target.getOrDefault(targetType, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(oldEnchantments);
        for (EnchantmentData data : selected) {
            mutable.set(data.enchantment(), Math.max(mutable.getLevel(data.enchantment()), data.level()));
        }
        target.set(targetType, mutable.toImmutable());
        return target;
    }

    private ItemStack createTankResult() {
        Mode mode = this.getMode();
        List<EnchantmentData> selected = this.getSelectedEnchantments();
        if (selected.isEmpty() || mode == Mode.SMALL_TANK && selected.size() != 1) {
            return ItemStack.EMPTY;
        }

        int capacity = mode == Mode.SMALL_TANK
                       ? FluidTankBlockEntity.BASE_CAPACITY
                       : LargeFluidTankBlockEntity.BASE_CAPACITY;
        long totalAmount = 0;
        List<FluidStack> fluids = new ArrayList<>();
        for (EnchantmentData data : selected) {
            long amount = getLiquidAmount(data.level());
            if (amount <= 0 || amount > capacity - totalAmount) return ItemStack.EMPTY;

            FluidStack fluid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), (int) amount);
            fluid.set(ModComponents.LIQUID_ENCHANTMENT, data.enchantment().getKey());
            fluids.add(fluid);
            totalAmount += amount;
        }

        ItemStack tank = this.modifier.getItem(0);
        return mode == Mode.SMALL_TANK
               ? FluidTankBlockEntity.fillItem(tank, fluids.getFirst(), this.registries)
               : LargeFluidTankBlockEntity.fillItem(tank, fluids, this.registries);
    }

    private static long getLiquidAmount(int level) {
        if (level <= 0) return 0;
        if (level >= Long.SIZE) return Long.MAX_VALUE;
        return 1L << (level - 1);
    }

    private ItemStack createSourceWithoutSelected() {
        ItemStack output = this.source.getItem(0).copy();
        Map<DataComponentType<ItemEnchantments>, ItemEnchantments.Mutable> mutableByType = new HashMap<>();
        for (EnchantmentData data : this.getSelectedEnchantments()) {
            ItemEnchantments.Mutable mutable = mutableByType.computeIfAbsent(
                data.type(),
                type -> new ItemEnchantments.Mutable(output.getOrDefault(type, ItemEnchantments.EMPTY))
            );
            mutable.removeIf(data.enchantment()::equals);
        }
        for (Map.Entry<DataComponentType<ItemEnchantments>, ItemEnchantments.Mutable> entry : mutableByType.entrySet()) {
            output.set(entry.getKey(), entry.getValue().toImmutable());
        }
        if (output.is(Items.ENCHANTED_BOOK) && !hasAnyEnchantments(output)) {
            return output.transmuteCopy(Items.BOOK);
        }
        return output;
    }

    private List<EnchantmentData> getSelectedEnchantments() {
        List<EnchantmentData> selected = new ArrayList<>();
        for (int index = 0; index < this.enchantments.size(); index++) {
            if (this.selectedIndexes.contains(index)) selected.add(this.enchantments.get(index));
        }
        return selected;
    }

    private void refreshEnchantments() {
        ItemStack input = this.source.getItem(0);
        this.enchantments.clear();
        for (DataComponentType<ItemEnchantments> type : getEnchantmentTypes()) {
            for (var entry : input.getOrDefault(type, ItemEnchantments.EMPTY).entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                if (enchantment.is(EnchantmentTags.CURSE)) continue;
                this.enchantments.add(new EnchantmentData(type, enchantment, entry.getIntValue()));
            }
        }
        this.enchantments.sort(EnchantmentData::compareTo);
    }

    private void resetGoldStats() {
        this.usedGold = 0;
        this.totalRepairCost = 0;
        this.totalCurseCount = 0;
        this.removedRepairCost = 0;
        this.removedCurseCount = 0;
    }

    private void restrictSelectionForMode() {
        if (this.getMode() == Mode.SMALL_TANK && this.selectedIndexes.size() > 1) {
            this.selectedIndexes.clear();
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.result.setItem(0, this.createResult());
    }

    public void select(int index) {
        if (index < 0 || index >= this.enchantments.size() || this.isGoldMode()) return;
        if (this.getMode() == Mode.SMALL_TANK) {
            this.selectedIndexes.clear();
            this.selectedIndexes.add(index);
            this.result.setItem(0, this.createResult());
            return;
        }
        if (this.selectedIndexes.add(index)) this.result.setItem(0, this.createResult());
    }

    public void unselect(int index) {
        if (this.selectedIndexes.remove(index)) this.result.setItem(0, this.createResult());
    }

    public boolean hasSelectedEnchantment() {
        return !this.getSelectedEnchantments().isEmpty();
    }

    public int getCost() {
        if (!this.isTransferMode()) return 0;
        long cost = this.source.getItem(0).getOrDefault(DataComponents.REPAIR_COST, 0);
        for (EnchantmentData data : this.getSelectedEnchantments()) {
            cost += (long) data.enchantment().value().getAnvilCost() * data.level() * this.source.getItem(0).getCount();
        }
        return Math.clamp(cost, 0, Integer.MAX_VALUE);
    }

    private int getExperience() {
        int experience = 0;
        for (EnchantmentData data : this.getSelectedEnchantments()) {
            experience += data.enchantment().value().getMinCost(data.level());
        }
        return experience;
    }

    private void takeResult(Player player, ItemStack output) {
        Mode mode = this.getMode();
        switch (mode) {
            case GOLD -> this.takeGoldResult(player);
            case BOOK, ITEM -> this.takeTransferResult(player, output);
            case SMALL_TANK, LARGE_TANK -> this.takeTankResult();
            case DISENCHANT -> this.takeDisenchantResult();
            case UNSUPPORTED -> {
                return;
            }
            default -> {
                return;
            }
        }
        player.playSound(SoundEvents.GRINDSTONE_USE);
    }

    private void takeGoldResult(Player player) {
        Item goldResult = this.modifier.getItem(0).is(Items.GOLD_BLOCK)
                          ? ModBlocks.CURSED_GOLD_BLOCK.asItem()
                          : ModItems.CURSED_GOLD_INGOT.get();
        int convertedCount = this.usedGold;
        this.source.setItem(0, ItemStack.EMPTY);
        this.consumeModifier(convertedCount);
        if (!player.level().isClientSide && convertedCount > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(goldResult, convertedCount));
        }
    }

    private void takeTransferResult(Player player, ItemStack output) {
        int cost = this.getCost();
        ItemStack remainingSource = this.createSourceWithoutSelected();
        int repairCost = remainingSource.getOrDefault(DataComponents.REPAIR_COST, 0);
        remainingSource.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(repairCost));
        if (!player.level().isClientSide) player.onEnchantmentPerformed(output, cost);
        this.source.setItem(0, remainingSource);
        this.consumeModifier(1);
    }

    private void takeTankResult() {
        this.source.setItem(0, this.createSourceWithoutSelected());
        this.consumeModifier(1);
    }

    private void takeDisenchantResult() {
        int experience = this.getExperience();
        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel && experience > 0) {
                int awarded = experience + level.random.nextInt(experience);
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), awarded);
            }
            level.levelEvent(1042, pos, 0);
        });
        this.source.setItem(0, ItemStack.EMPTY);
    }

    private void consumeModifier(int count) {
        ItemStack stack = this.modifier.getItem(0);
        stack.shrink(count);
        this.modifier.setItem(0, stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack clicked = slot.getItem();
        ItemStack original = clicked.copy();
        if (index == 2) {
            ItemStack moving = clicked.copy();
            if (!this.moveItemStackTo(moving, PLAYER_INVENTORY_SLOT_START, PLAYER_INVENTORY_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, clicked);
            return original;
        }
        if (index < PLAYER_INVENTORY_SLOT_START) {
            if (!this.moveItemStackTo(clicked, PLAYER_INVENTORY_SLOT_START, PLAYER_INVENTORY_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isValidSource(clicked) && !this.getSlot(0).hasItem()) {
            if (!this.moveItemStackTo(clicked, 0, 1, false)) return ItemStack.EMPTY;
        } else if (this.isValidModifier(clicked) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo(clicked, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (clicked.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.TRANSCENDENCE_GRINDSTONE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearInputContainer(player, this.source);
            this.clearInputContainer(player, this.modifier);
        });
    }

    private void clearInputContainer(Player player, Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.removeItemNoUpdate(slot);
            if (stack.isEmpty()) continue;
            if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
                player.drop(stack, false);
            } else if (player.getInventory().player instanceof ServerPlayer) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    public boolean canScroll() {
        return this.enchantments.size() > 6;
    }

    private enum Mode {
        DISENCHANT,
        GOLD,
        BOOK,
        ITEM,
        SMALL_TANK,
        LARGE_TANK,
        UNSUPPORTED
    }
}
