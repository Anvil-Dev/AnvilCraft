package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;

public interface IFilterMenuSever extends IFilterMenu {
    @Nullable IFilterBlockEntity getBlockEntity();
    @Nullable AbstractContainerMenu getMenu();

    @Nullable ServerPlayer getPlayer();

    default void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player0) {
        ServerPlayer player = (ServerPlayer) player0;
        if (slotId >= 0 && slotId < Objects.requireNonNull(getMenu()).slots.size()) {
            Slot slot = Objects.requireNonNull(getMenu()).getSlot(slotId);
            ItemStack slotStack = slot.getItem();
            NonNullList<@Unmodifiable ItemStack> filters = Objects.requireNonNull(getBlockEntity()).getFilter();
            if (slotId < filters.size()) {
                NonNullList<Boolean> disableds = getBlockEntity().getDisabled();
                if (getBlockEntity().isRecord() && getMenu().getCarried().isEmpty() && !filters.get(slotId).isEmpty() && slotStack.isEmpty()) {
                    filters.set(slotId, ItemStack.EMPTY);
                    new SlotFilterChangePack(slotId, ItemStack.EMPTY).send(player);
                    getBlockEntity().getDisabled().set(slotId, true);
                    new SlotDisableChangePack(slotId, true).send(player);
                    getBlockEntity().setChanged();
                } else if (slotStack.isEmpty() && getMenu().getCarried().isEmpty()) {
                    boolean newDisabled = !disableds.get(slotId);
                    if (newDisabled || !getBlockEntity().isRecord()) {
                        disableds.set(slotId, newDisabled);
                        new SlotDisableChangePack(slotId, newDisabled).send(player);
                        getBlockEntity().setChanged();
                    }
                } else if (getBlockEntity().isRecord() && !getMenu().getCarried().isEmpty()) {
                    disableds.set(slotId, false);
                    new SlotDisableChangePack(slotId, false).send(player);
                    filters.set(slotId, getMenu().getCarried().copy());
                    new SlotFilterChangePack(slotId, getMenu().getCarried()).send(player);
                    getBlockEntity().setChanged();
                } else if (!getMenu().getCarried().isEmpty() && disableds.get(slotId)) {
                    disableds.set(slotId, false);
                    new SlotDisableChangePack(slotId, false).send(player);
                    getBlockEntity().setChanged();
                }
            }
        }
    }
    default void setRecord(boolean record) {
        if (record) {
            for (int i = 0; i < Objects.requireNonNull(getBlockEntity()).getFilter().size(); i++) {
                ItemStack slotStack = Objects.requireNonNull(getMenu()).getSlot(i).getItem();
                if (slotStack.isEmpty()) {
                    getBlockEntity().getDisabled().set(i, true);
                    new SlotDisableChangePack(i, true).send(getPlayer());
                    getBlockEntity().setChanged();
                } else {
                    getBlockEntity().getFilter().set(i, slotStack.copy());
                    new SlotFilterChangePack(i, slotStack).send(getPlayer());
                    getBlockEntity().setChanged();
                }
            }
        }
}
}
