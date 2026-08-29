package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public abstract class BundleLikeItem extends Item {
    public BundleLikeItem(Properties properties) {
        super(properties);
    }

    protected boolean isInvertedAction(Player player) {
        return BundleLikeServerStub.isInvertedAction(player.getGameProfile().getId());
    }

    protected ClickAction computeValidAction(@SuppressWarnings("SameParameterValue") ClickAction action, Player player) {
        return switch (action) {
            case PRIMARY -> this.isInvertedAction(player) ? ClickAction.SECONDARY : ClickAction.PRIMARY;
            case SECONDARY -> this.isInvertedAction(player) ? ClickAction.PRIMARY : ClickAction.SECONDARY;
        };
    }

    /**
     * 是否允许空手点击本物品所在的槽位时取出一个物品（{@link #overrideOtherStackedOnMe} 的空手路径）。
     * 终端类物品未在浮窗内选中物品时不取出，放行 vanilla 拿起终端。
     */
    protected boolean canRemoveOne(Player player) {
        return true;
    }

    protected abstract void removeOne(TransferState state);

    protected abstract void insertOne(TransferState state);

    protected abstract void updateStack(ItemStack stack, TransferState state);

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != this.computeValidAction(ClickAction.SECONDARY, player) || !slot.allowModification(player)) return false;
        ItemStack other = slot.getItem();
        TransferState state = new TransferState(player, other.copy(), stack.copy());
        if (other.isEmpty()) {
            this.removeOne(state);
            ItemStack removed = state.output;
            if (removed == null || removed.isEmpty()) return false;
            slot.set(removed);
            this.playRemoveOneSound(player);
        } else {
            this.insertOne(state);
            ItemStack remain = state.output;
            if (remain == null) return false;
            slot.set(remain);
            this.playInsertSound(player);
        }
        this.updateStack(stack, state);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
        ItemStack stack,
        ItemStack other,
        Slot slot,
        ClickAction action,
        Player player,
        SlotAccess access
    ) {
        if (action != this.computeValidAction(ClickAction.SECONDARY, player) || !slot.allowModification(player)) return false;
        TransferState state = new TransferState(player, other.copy(), stack.copy());
        if (other.isEmpty()) {
            if (!this.canRemoveOne(player)) return false;
            this.removeOne(state);
            ItemStack removed = state.output;
            if (removed == null || removed.isEmpty()) return false;
            access.set(removed);
            this.playRemoveOneSound(player);
            this.broadcastChangesOnContainerMenu(player);
        } else {
            this.insertOne(state);
            ItemStack remain = state.output;
            if (remain == null) return false;
            access.set(remain);
            this.playInsertSound(player);
            this.broadcastChangesOnContainerMenu(player);
        }
        this.updateStack(stack, state);
        return true;
    }

    protected void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    protected void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    protected void broadcastChangesOnContainerMenu(Player player) {
        player.containerMenu.slotsChanged(player.getInventory());
    }

    @RequiredArgsConstructor
    @Data
    protected static class TransferState {
        private final Player player;
        private final ItemStack other;
        private ItemStack stack;
        private @Nullable ItemStack output;

        public TransferState(Player player, ItemStack other, ItemStack stack) {
            this(player, other);
            this.stack = stack;
        }
    }
}
