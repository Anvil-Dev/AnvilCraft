package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 终端类物品的通用 BundleLike 行为：物品本身不本地存储内容，
 * 点击交互直接把物品放入 / 取出终端连接的存储站
 * （本地终端→大型板条箱、潜影终端→潜影目标、超维终端→绑定存储站）。
 */
public abstract class TerminalItem extends BundleLikeItem {
    protected TerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canRemoveOne(BundleLikeItem.TransferState state) {
        // BUNDLE_HOVER_ITEM（终端拖到空槽上右键）：不检查浮窗选中状态，直接放行，
        // 取出目标存储中排序第一的物品 / 放入。
        // ITEM_HOVER_BUNDLE（空手点击终端槽）：服务端不取出，放行 vanilla 拿起终端；
        // 浮窗内滚轮选中后的取出由客户端 mouseClicked 分支处理。
        return state.getType() == BundleLikeItem.TransferType.BUNDLE_HOVER_ITEM;
    }

    @Override
    protected void removeOne(BundleLikeItem.TransferState state) {
        if (!(state.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        // BUNDLE_HOVER_ITEM（终端拖到空槽上右键）：不检查浮窗选中状态，
        // 直接取出目标存储中排序第一的物品；ITEM_HOVER_BUNDLE（空手点击终端槽）
        // 的选中检查由 canRemoveOne 在调用前完成。
        UUID targetId = StorageServerStub.terminalTargetId(player, state.getStack());
        state.setOutput(targetId == null ? null : StorageServerStub.extractFromTerminal(player, targetId, 64));
    }

    @Override
    protected void insertOne(BundleLikeItem.TransferState state) {
        if (!(state.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack other = state.getOther();
        UUID targetId = StorageServerStub.terminalTargetId(player, state.getStack());
        int inserted = targetId == null ? 0 : StorageServerStub.insertIntoTerminal(player, targetId, other, other.getCount());
        ItemStack remain = other.copy();
        remain.shrink(inserted);
        state.setOutput(remain);
    }

    @Override
    protected void updateStack(ItemStack stack, BundleLikeItem.TransferState state) {
        // 终端不本地存储内容，物品直接进入连接的存储站，无需写回
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
