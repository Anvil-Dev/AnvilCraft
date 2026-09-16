package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record SwapPocketPacket(int slot) implements IServerboundPacket {
    public static final Type<SwapPocketPacket> TYPE = IPacket.type(AnvilCraft.of("swap_pocket"));
    public static final StreamCodec<ByteBuf, SwapPocketPacket> STREAM_CODEC = ByteBufCodecs.VAR_INT
        .map(SwapPocketPacket::new, SwapPocketPacket::slot);

    @Override
    public Type<SwapPocketPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.isAlive() || player.isSpectator() || this.slot < 0 || this.slot >= PocketInventory.capacity(player)
            || player.containerMenu != player.inventoryMenu || !player.containerMenu.getCarried().isEmpty()) return;
        PocketInventory pockets = PocketInventory.get(player);
        ItemStack offhand = player.getOffhandItem();
        player.setItemInHand(InteractionHand.OFF_HAND, pockets.getItem(this.slot));
        pockets.setItem(this.slot, offhand);
        pockets.syncChanges(player);
        player.inventoryMenu.broadcastChanges();
    }
}
