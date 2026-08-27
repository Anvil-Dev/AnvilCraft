package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.setting.mode.BalanceMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 修改玩家主 / 副手持有的终端物品自身保存的物品均衡模式。
 * 均衡模式随终端物品保存，而不是玩家全局设置。
 */
public record TerminalBalanceModePacket(BalanceMode mode) implements IServerboundPacket {
    public static final Type<TerminalBalanceModePacket> TYPE = IPacket.type(AnvilCraft.of("terminal_balance_mode"));
    public static final StreamCodec<ByteBuf, TerminalBalanceModePacket> STREAM_CODEC = BalanceMode.STREAM_CODEC
        .map(TerminalBalanceModePacket::new, TerminalBalanceModePacket::mode);

    @Override
    public Type<TerminalBalanceModePacket> type() {
        return TerminalBalanceModePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = serverPlayer.getMainHandItem();
        if (!TerminalBalanceModePacket.isTerminal(stack)) {
            stack = serverPlayer.getOffhandItem();
        }
        if (!TerminalBalanceModePacket.isTerminal(stack)) {
            return;
        }
        stack.set(ModComponents.TERMINAL_BALANCE_MODE, this.mode);
        serverPlayer.getInventory().setChanged();
        serverPlayer.containerMenu.broadcastChanges();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isTerminal(ItemStack stack) {
        return stack.is(ModItems.LOCAL_TERMINAL)
               || stack.is(ModItems.SHULKER_TERMINAL)
               || stack.is(ModItems.HYPERDIMENSION_TERMINAL);
    }
}
