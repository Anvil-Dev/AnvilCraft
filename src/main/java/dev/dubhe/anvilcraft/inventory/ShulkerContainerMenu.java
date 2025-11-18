package dev.dubhe.anvilcraft.inventory;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.block.entity.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public class ShulkerContainerMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    public final ShulkerContainerBlockEntity blockEntity;
    private final Level level;
    public final ContainerStorage storage;

    private IntSet slotsOrder;

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
        this.slotsOrder = IntSets.fromTo(0, this.storage.getItems().size());

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        this.addContainerSlots();

        this.scrollTo(0.0F);
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

    public float applyOrder(IntSet order, float scrollOffs) {
        int i = this.getRowIndexForScroll(scrollOffs);
        this.slotsOrder = order;

        this.scrollTo(scrollOffs);
        return this.getScrollForRowIndex(i);
    }

    protected int calculateRowCount() {
        return Mth.positiveCeilDiv(this.storage.getItems().size(), 9) - 6;
    }

    protected int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int) ((double) (scrollOffs * (float) this.calculateRowCount()) + 0.5), 0);
    }

    protected float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float) rowIndex / (float) this.calculateRowCount(), 0.0F, 1.0F);
    }

    public float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float) (input / (double) this.calculateRowCount()), 0.0F, 1.0F);
    }

    /**
     * Updates the gui slot's ItemStacks based on scroll position.
     */
    public void scrollTo(float pos) {
        int row = this.getRowIndexForScroll(pos);

        int[] order = this.slotsOrder.toIntArray();
        for (int rowDiff = 0; rowDiff < 6; rowDiff++) {
            for (int column = 0; column < 9; column++) {
                int index = column + (rowDiff + row) * 9;
                int slotIndex = VANILLA_SLOT_COUNT + column + rowDiff * 9;
                ShulkerContainerSlot slot = Util.cast(
                    this.getSlot(slotIndex),
                    () -> new IllegalStateException("Slot not ShulkerContainerSlot in index " + slotIndex)
                );
                if (index >= 0 && index < this.storage.getItems().size()) {
                    if (index < order.length) {
                        slot.setIndex(order[index]);
                    } else {
                        slot.setIndex(-1);
                    }
                } else {
                    slot.setIndex(-1);
                }
            }
        }
    }

    public boolean canScroll() {
        return this.storage.getItems().size() > 54;
    }

    // 致谢： diesieben07 |https://github.com/diesieben07/SevenCommons
    // 必须为 GUI 使用的每个插槽分配一个插槽编号。
    // 对于这个容器，我们可以看到瓷砖库存的插槽以及玩家库存插槽和快捷栏。
    // 每次我们向容器添加 Slot 时，它都会自动增加 slotIndex，这意味着
    // 0 - 8 = 快捷栏插槽（将映射到 InventoryPlayer 插槽编号 0 - 8）
    // 9 - 35 = 玩家库存槽（映射到 InventoryPlayer 槽位编号 9 - 35）
    // 36 - 89 = TileInventory 插槽（映射到我们的 TileEntity 插槽编号 0 - 53 （在行0的情况下    ））
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
        // noinspection ConstantValue
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
        if (!this.storage.isMaxItems()) {
            int result = this.storage.addItem(stack);
            if (result != stack.getCount()) {
                stack.setCount(result);
                result = -1;
            }
            source.setChanged();
            return result == -1;
        }
        int maxSize = this.storage.getMaxStackSize(stack);
        for (UnlimitedItemStack entry : this.storage.getItems()) {
            if (!ItemStack.isSameItemSameComponents(stack, entry.getStack())) continue;
            if (entry.getCount() >= maxSize) {
                return false;
            } else if (stack.getCount() + entry.getCount() > maxSize) {
                entry.setCount(maxSize);
                stack.setCount(stack.getCount() - (maxSize - entry.getCount()));
                source.setChanged();
                return true;
            } else {
                entry.grow(stack.getCount());
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
                    scSlot.remove(maxSize);
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

    // region 修复快速移动死锁的问题
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        try {
            this.doClick(slotId, button, clickType, player);
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Container click");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Click info");
            crashreportcategory.setDetail("Menu Type", () -> ModMenuTypes.SHULKER_CONTAINER.get().toString());
            crashreportcategory.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
            crashreportcategory.setDetail("Slot Count", this.slots.size());
            crashreportcategory.setDetail("Slot", slotId);
            crashreportcategory.setDetail("Button", button);
            crashreportcategory.setDetail("Type", clickType);
            throw new ReportedException(crashreport);
        }
    }

    private void doClick(int slotId, int button, ClickType clickType, Player player) {
        Inventory inventory = player.getInventory();
        if (clickType == ClickType.QUICK_CRAFT) {
            int status = this.quickcraftStatus;
            this.quickcraftStatus = AbstractContainerMenu.getQuickcraftHeader(button);
            if ((status != 1 || this.quickcraftStatus != 2) && status != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == AbstractContainerMenu.QUICKCRAFT_HEADER_START) {
                this.quickcraftType = AbstractContainerMenu.getQuickcraftType(button);
                if (AbstractContainerMenu.isValidQuickcraftType(this.quickcraftType, player)) {
                    this.quickcraftStatus = AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE) {
                Slot slot = this.slots.get(slotId);
                ItemStack carried = this.getCarried();
                if (
                    AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
                    && slot.mayPlace(carried)
                    && (this.quickcraftType == 2 || carried.getCount() > this.quickcraftSlots.size())
                    && this.canDragTo(slot)
                ) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == AbstractContainerMenu.QUICKCRAFT_HEADER_END) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int slotIndex = this.quickcraftSlots.iterator().next().index;
                        this.resetQuickCraft();
                        this.doClick(slotIndex, this.quickcraftType, ClickType.PICKUP, player);
                        return;
                    }

                    ItemStack carriedCopy = this.getCarried().copy();
                    if (carriedCopy.isEmpty()) {
                        this.resetQuickCraft();
                        return;
                    }

                    int carriedCount = this.getCarried().getCount();

                    for (Slot slot : this.quickcraftSlots) {
                        ItemStack carried = this.getCarried();
                        if (
                            slot != null
                            && AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
                            && slot.mayPlace(carried)
                            && (
                                this.quickcraftType == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE
                                || carried.getCount() >= this.quickcraftSlots.size()
                            )
                            && this.canDragTo(slot)
                        ) {
                            int count = slot.hasItem() ? slot.getItem().getCount() : 0;
                            int maxCount = Math.min(carriedCopy.getMaxStackSize(), slot.getMaxStackSize(carriedCopy));
                            int placingCount = Math.min(
                                AbstractContainerMenu.getQuickCraftPlaceCount(
                                    this.quickcraftSlots,
                                    this.quickcraftType,
                                    carriedCopy
                                ) + count,
                                maxCount
                            );
                            carriedCount -= placingCount - count;
                            carriedCount += slot.safeInsert(carriedCopy.copyWithCount(placingCount)).getCount();
                        }
                    }

                    carriedCopy.setCount(carriedCount);
                    this.setCarried(carriedCopy);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != AbstractContainerMenu.QUICKCRAFT_HEADER_START) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
            ClickAction action = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (slotId == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (action == ClickAction.PRIMARY) {
                        player.drop(this.getCarried(), true);
                        this.setCarried(ItemStack.EMPTY);
                    } else {
                        player.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (slotId < 0) return;

                Slot slot = this.slots.get(slotId);
                if (!slot.mayPickup(player)) return;

                this.quickMoveStack(player, slotId);
            } else {
                if (slotId < 0) return;

                Slot slot = this.slots.get(slotId);
                ItemStack stackInSlot = slot.getItem();
                ItemStack stackCarried = this.getCarried();
                player.updateTutorialInventoryAction(stackCarried, slot.getItem(), action);
                if (!this.tryItemClickBehaviourOverride(player, action, slot, stackInSlot, stackCarried)) {
                    if (stackInSlot.isEmpty()) {
                        if (!stackCarried.isEmpty()) {
                            int placeCount = action == ClickAction.PRIMARY ? stackCarried.getCount() : 1;
                            this.setCarried(slot.safeInsert(stackCarried, placeCount));
                        }
                    } else if (slot.mayPickup(player)) {
                        if (stackCarried.isEmpty()) {
                            int pickupCount = action == ClickAction.PRIMARY ? stackInSlot.getCount() : (stackInSlot.getCount() + 1) / 2;
                            Optional<ItemStack> pickupResult = slot.tryRemove(pickupCount, Integer.MAX_VALUE, player);
                            pickupResult.ifPresent(stack -> {
                                this.setCarried(stack);
                                slot.onTake(player, stack);
                            });
                        } else if (slot.mayPlace(stackCarried)) {
                            if (ItemStack.isSameItemSameComponents(stackInSlot, stackCarried)) {
                                int placeCount = action == ClickAction.PRIMARY ? stackCarried.getCount() : 1;
                                this.setCarried(slot.safeInsert(stackCarried, placeCount));
                            } else if (stackCarried.getCount() <= slot.getMaxStackSize(stackCarried)) {
                                this.setCarried(stackInSlot);
                                slot.setByPlayer(stackCarried);
                            }
                        } else if (ItemStack.isSameItemSameComponents(stackInSlot, stackCarried)) {
                            Optional<ItemStack> pickupResult = slot.tryRemove(
                                stackInSlot.getCount(),
                                stackCarried.getMaxStackSize() - stackCarried.getCount(),
                                player
                            );
                            pickupResult.ifPresent(stack -> {
                                stackCarried.grow(stack.getCount());
                                slot.onTake(player, stack);
                            });
                        }
                    }
                }

                slot.setChanged();
            }
        } else if (
            clickType == ClickType.SWAP
            && (button >= 0 && button < 9 || button == 40)
            && slotId < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT // no swap to TE now
        ) {
            ItemStack itemstack2 = inventory.getItem(button);
            Slot slot5 = this.slots.get(slotId);
            ItemStack itemstack7 = slot5.getItem();
            if (!itemstack2.isEmpty() || !itemstack7.isEmpty()) {
                if (itemstack2.isEmpty()) {
                    if (slot5.mayPickup(player)) {
                        inventory.setItem(button, itemstack7);
                        slot5.setByPlayer(ItemStack.EMPTY);
                        slot5.onTake(player, itemstack7);
                    }
                } else if (itemstack7.isEmpty()) {
                    if (slot5.mayPlace(itemstack2)) {
                        int j2 = slot5.getMaxStackSize(itemstack2);
                        if (itemstack2.getCount() > j2) {
                            slot5.setByPlayer(itemstack2.split(j2));
                        } else {
                            inventory.setItem(button, ItemStack.EMPTY);
                            slot5.setByPlayer(itemstack2);
                        }
                    }
                } else if (slot5.mayPickup(player) && slot5.mayPlace(itemstack2)) {
                    int k2 = slot5.getMaxStackSize(itemstack2);
                    if (itemstack2.getCount() > k2) {
                        slot5.setByPlayer(itemstack2.split(k2));
                        slot5.onTake(player, itemstack7);
                        if (!inventory.add(itemstack7)) {
                            player.drop(itemstack7, true);
                        }
                    } else {
                        inventory.setItem(button, itemstack7);
                        slot5.setByPlayer(itemstack2);
                        slot5.onTake(player, itemstack7);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.hasInfiniteMaterials() && this.getCarried().isEmpty() && slotId >= 0) {
            Slot slot4 = this.slots.get(slotId);
            if (slot4.hasItem()) {
                ItemStack itemstack5 = slot4.getItem();
                this.setCarried(itemstack5.copyWithCount(itemstack5.getMaxStackSize()));
            }
        } else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && slotId >= 0) {
            Slot slot3 = this.slots.get(slotId);
            int j1 = button == 0 ? 1 : slot3.getItem().getCount();
            ItemStack itemstack6 = slot3.safeTake(j1, Integer.MAX_VALUE, player);
            player.drop(itemstack6, true);
        } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot slot2 = this.slots.get(slotId);
            ItemStack itemstack4 = this.getCarried();
            if (!itemstack4.isEmpty() && (!slot2.hasItem() || !slot2.mayPickup(player))) {
                int l1 = button == 0 ? 0 : this.slots.size() - 1;
                int i2 = button == 0 ? 1 : -1;

                for (int l2 = 0; l2 < 2; l2++) {
                    for (int l3 = l1; l3 >= 0 && l3 < this.slots.size() && itemstack4.getCount() < itemstack4.getMaxStackSize(); l3 += i2) {
                        Slot slot8 = this.slots.get(l3);
                        if (slot8.hasItem()
                            && canItemQuickReplace(slot8, itemstack4, true)
                            && slot8.mayPickup(player)
                            && this.canTakeItemForPickAll(itemstack4, slot8)) {
                            ItemStack itemstack11 = slot8.getItem();
                            if (l2 != 0 || itemstack11.getCount() != itemstack11.getMaxStackSize()) {
                                ItemStack itemstack12 = slot8.safeTake(
                                    itemstack11.getCount(), itemstack4.getMaxStackSize() - itemstack4.getCount(), player);
                                itemstack4.grow(itemstack12.getCount());
                            }
                        }
                    }
                }
            }
        }
    }
    // endregion
}
