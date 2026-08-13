package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.event.PrimerEnchantmentsEvent;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlotItemHandler;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

public class AutoEnchantingTableMenu extends AbstractContainerMenu {
    @Getter
    private final AutoEnchantingTableBlockEntity blockEntity;
    private final Level level;
    @Getter
    private final Inventory inventory;

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
        this.inventory = inventory;

        // 服务端记录 GUI 打开状态：引物模式下打开 GUI 暂停附魔
        if (!this.level.isClientSide) {
            this.blockEntity.onMenuOpen();
        }

        // 输入/输出槽仅可查看，不允许修改；引物槽可由玩家操作
        this.addSlot(new ReadOnlySlotItemHandler(
            this.blockEntity.getItemHandler(),
            AutoEnchantingTableBlockEntity.SLOT_INPUT,
            7,
            18
        ));
        this.addSlot(new ReadOnlySlotItemHandler(
            this.blockEntity.getItemHandler(),
            AutoEnchantingTableBlockEntity.SLOT_OUTPUT,
            7,
            52
        ));
        this.addSlot(new SlotItemHandler(
            this.blockEntity.getItemHandler(),
            AutoEnchantingTableBlockEntity.SLOT_PRIMER,
            27,
            18
        ) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                AutoEnchantingTableMenu.this.refreshEnchantments();
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        this.refreshEnchantments();
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

    private void refreshEnchantments() {
        this.enchantments.clear();
        ItemStack primer = this.blockEntity.getItemHandler().getStackInSlot(AutoEnchantingTableBlockEntity.SLOT_PRIMER);
        if (!primer.isEmpty() && this.blockEntity.isAllowedPrimer(primer)) {
            PrimerEnchantmentsEvent event = new PrimerEnchantmentsEvent(this.level, primer);
            NeoForge.EVENT_BUS.post(event);
            for (Holder<Enchantment> holder : event.getEnchantments()) {
                this.enchantments.add(new EnchantmentData(
                    DataComponents.ENCHANTMENTS, holder, holder.value().getMaxLevel()));
            }
            this.enchantments.sort(EnchantmentData::compareTo);
        }
        // 根据方块实体记忆的具体附魔重建选中索引
        this.selectedIndexes.clear();
        for (int i = 0; i < this.enchantments.size(); i++) {
            if (this.blockEntity.isSelected(this.enchantments.get(i).enchantment())) {
                this.selectedIndexes.add(i);
            }
        }
    }

    public void select(int index) {
        if (index < 0 || index >= this.enchantments.size()) return;
        EnchantmentData data = this.enchantments.get(index);
        this.blockEntity.selectEnchantment(data.enchantment());
        this.selectedIndexes.add(index);
    }

    public void unselect(int index) {
        if (index < 0 || index >= this.enchantments.size()) return;
        EnchantmentData data = this.enchantments.get(index);
        this.blockEntity.unselectEnchantment(data.enchantment());
        this.selectedIndexes.remove(index);
    }

    public void setLiquidLevel(int level) {
        this.blockEntity.setLiquidLevel(level);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack;
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

    @Override
    public void removed(Player player) {
        if (!this.level.isClientSide) {
            this.blockEntity.onMenuClose();
        }
        super.removed(player);
    }
}
