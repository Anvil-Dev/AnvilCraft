package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlotItemHandler;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoEnchantingTableMenu extends AbstractContainerMenu {
    @Getter
    private final AutoEnchantingTableBlockEntity blockEntity;
    private final Level level;
    private ItemStack lastInputStack = ItemStack.EMPTY;

    @Getter
    private final IntSet selectedIndexes = new IntArraySet();
    @Getter
    private final List<EnchantmentData> enchantments = new CopyOnWriteArrayList<>();

    public AutoEnchantingTableMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        BlockEntity machine
    ) {
        super(menuType, containerId);
        this.blockEntity = (AutoEnchantingTableBlockEntity) machine;
        this.level = inventory.player.level();

        // 输入/输出槽仅可查看，不允许修改；引物槽可由玩家操作
        this.addSlot(new ReadOnlySlotItemHandler(
            this.blockEntity.getItemHandler(), AutoEnchantingTableBlockEntity.SLOT_INPUT, 7, 18));
        this.addSlot(new ReadOnlySlotItemHandler(
            this.blockEntity.getItemHandler(), AutoEnchantingTableBlockEntity.SLOT_OUTPUT, 7, 52));
        this.addSlot(new SlotItemHandler(
            this.blockEntity.getItemHandler(), AutoEnchantingTableBlockEntity.SLOT_PRIMER, 27, 18));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public AutoEnchantingTableMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        FriendlyByteBuf extraData
    ) {
        this(
            menuType, containerId, inventory,
            Objects.requireNonNull(inventory.player.level().getBlockEntity(extraData.readBlockPos()))
        );
    }

    @Override
    public void broadcastChanges() {
        ItemStack current = this.blockEntity.getItemHandler().getStackInSlot(AutoEnchantingTableBlockEntity.SLOT_INPUT);
        if (!ItemStack.matches(current, this.lastInputStack)) {
            this.lastInputStack = current.copy();
            this.refreshEnchantments();
        }
        super.broadcastChanges();
    }

    private void refreshEnchantments() {
        ItemStack input = this.blockEntity.getItemHandler().getStackInSlot(AutoEnchantingTableBlockEntity.SLOT_INPUT);
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
        this.selectedIndexes.removeIf(index -> index < 0 || index >= this.enchantments.size());
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

    public void select(int index) {
        if (index < 0 || index >= this.enchantments.size()) return;
        this.selectedIndexes.add(index);
    }

    public void unselect(int index) {
        this.selectedIndexes.remove(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack slotItem = slot.getItem();
        itemStack = slotItem.copy();
        if (index < 3) {
            // 输入/输出槽只读，无法快速取出；仅引物槽可快速取回背包
            if (!slot.mayPickup(player)) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(slotItem, 3, 39, true)) return ItemStack.EMPTY;
        } else {
            // 输入/输出槽只读（mayPlace 为 false），快速放入只会进入引物槽
            if (!this.moveItemStackTo(slotItem, 0, 3, false)) return ItemStack.EMPTY;
        }
        if (slotItem.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (slotItem.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotItem);
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
            ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
            player,
            ModBlocks.AUTO_ENCHANTING_TABLE.get()
        );
    }
}
