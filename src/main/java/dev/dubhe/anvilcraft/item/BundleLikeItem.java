package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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

    /**
     * 是否允许本物品进行 BundleLike 交互。四个分支（{@link #overrideStackedOnOther} 的
     * 取出/放入与 {@link #overrideOtherStackedOnMe} 的取出/放入）都会先经过此判定。
     * 默认允许；子类可按 {@link TransferState#getType()} 与玩家状态细化
     * （如终端类物品仅在浮窗内选中物品时才允许空手点击终端槽取出）。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canRemoveOne(TransferState state) {
        return true;
    }

    /**
     * 手拿物品点击槽内本物品时是否允许把物品放入（vanilla 语义为交换）。
     * 默认允许（收纳类物品放入）；终端类物品不本地存储内容，覆写为 false
     * 放行 vanilla 交换。
     */
    protected boolean canInsertInto(TransferState state) {
        return true;
    }

    protected abstract void removeOne(TransferState state);

    protected abstract void insertOne(TransferState state);

    protected abstract void updateStack(ItemStack stack, TransferState state);

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // 客户端预测执行（Render thread）：真实处理在服务端 clicked 中执行。
            // 仅当本次操作确实是 BundleLike 且子类允许（如终端有绑定且存储有物品可取出）
            // 时才放行（返回 true 阻止 vanilla fallback 把终端放回槽、避免点击包携带的
            // carried 变空导致服务端无法执行）；其余（空槽+左键放回、有物品+右键交换、
            // 无绑定/无物品可取时）返回 false 交给 vanilla fallback，与 BundleItem 语义一致。
            ItemStack other = slot.getItem();
            TransferState state = new TransferState(TransferType.BUNDLE_HOVER_ITEM, player, other.copy(), stack.copy());
            boolean result;
            if (other.isEmpty()) {
                result = action == BundleLikeItem.computeValidAction(ClickAction.SECONDARY, player)
                    && this.canRemoveOne(state);
            } else {
                result = action == BundleLikeItem.computeValidAction(ClickAction.SECONDARY, player);
            }
            return result;
        }
        if (!slot.allowModification(serverPlayer)) return false;
        ItemStack other = slot.getItem();
        TransferState state = new TransferState(TransferType.BUNDLE_HOVER_ITEM, serverPlayer, other.copy(), stack.copy());
        if (other.isEmpty()) {
            if (!this.canRemoveOne(state)) return false;
            if (action != BundleLikeItem.computeValidAction(ClickAction.SECONDARY, serverPlayer)) return false;
            this.removeOne(state);
            ItemStack removed = state.output;
            if (removed == null || removed.isEmpty()) return false;
            slot.set(removed);
            this.playRemoveOneSound(serverPlayer);
        } else {
            if (action != BundleLikeItem.computeValidAction(ClickAction.SECONDARY, serverPlayer)) return false;
            this.insertOne(state);
            ItemStack remain = state.output;
            if (remain == null) return false;
            slot.set(remain);
            this.playInsertSound(serverPlayer);
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
        if (!slot.allowModification(player)) return false;
        TransferState state = new TransferState(TransferType.ITEM_HOVER_BUNDLE, player, other.copy(), stack.copy());
        if (other.isEmpty()) {
            if (!this.canRemoveOne(state)) return false;
            if (action != BundleLikeItem.computeValidAction(ClickAction.SECONDARY, player)) return false;
            this.removeOne(state);
            ItemStack removed = state.output;
            if (removed == null || removed.isEmpty()) return false;
            access.set(removed);
            this.playRemoveOneSound(player);
            this.broadcastChangesOnContainerMenu(player);
        } else {
            // 手拿物品：默认放入（收纳类）；终端类放行 vanilla 交换
            if (!this.canInsertInto(state)) return false;
            if (action != BundleLikeItem.computeValidAction(ClickAction.SECONDARY, player)) return false;
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
        this.playSound(entity, SoundEvents.BUNDLE_REMOVE_ONE);
    }

    protected void playInsertSound(Entity entity) {
        this.playSound(entity, SoundEvents.BUNDLE_INSERT);
    }

    /**
     * 播放音效：服务端 {@code Player.playSound} 会排除玩家本人（听不到），
     * 这里改用 {@code level.playSound(null, ...)} 广播给附近所有玩家（含操作者）。
     */
    protected static void playSound(Entity entity, SoundEvent sound) {
        entity.level().playSound(
            null,
            entity,
            sound,
            entity.getSoundSource(),
            0.8F,
            0.8F + entity.level().getRandom().nextFloat() * 0.4F
        );
    }

    protected void broadcastChangesOnContainerMenu(Player player) {
        player.containerMenu.slotsChanged(player.getInventory());
    }

    protected static ClickAction computeValidAction(ClickAction action, Player player) {
        return switch (action) {
            case PRIMARY -> BundleLikeItem.isInvertedAction(player) ? ClickAction.SECONDARY : ClickAction.PRIMARY;
            case SECONDARY -> BundleLikeItem.isInvertedAction(player) ? ClickAction.PRIMARY : ClickAction.SECONDARY;
        };
    }

    protected static boolean isInvertedAction(Player player) {
        return BundleLikeServerStub.isInvertedAction(player.getGameProfile().getId());
    }

    @RequiredArgsConstructor
    @Data
    protected static class TransferState {
        private final TransferType type;
        private final Player player;
        private final ItemStack other;
        private ItemStack stack;
        private @Nullable ItemStack output;

        public TransferState(TransferType type, Player player, ItemStack other, ItemStack stack) {
            this(type, player, other);
            this.stack = stack;
        }
    }

    protected enum TransferType {
        BUNDLE_HOVER_ITEM,
        ITEM_HOVER_BUNDLE,
    }
}
