package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
    protected boolean canRemoveOne(Player player) {
        // 未在浮窗内通过滚轮选中物品时，点击终端不应取出存储中的物品（放行 vanilla 拿起终端）
        return false;
    }

    @Override
    protected void removeOne(BundleLikeItem.TransferState state) {
        if (!(state.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        UUID targetId = StorageServerStub.terminalTargetId(player, state.getStack());
        state.setOutput(targetId == null ? null : StorageServerStub.extractFromTerminal(player, targetId, 1));
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
