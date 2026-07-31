package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/// 宝石工艺台自动填充网络包
/// 用于将客户端的自动填充请求发送到服务端执行
public record JewelCraftingAutoFillPacket() implements IServerboundPacket {
    public static final Type<JewelCraftingAutoFillPacket> TYPE = IPacket.type(AnvilCraft.of("jewel_crafting_auto_fill"));
    public static final StreamCodec<ByteBuf, JewelCraftingAutoFillPacket> STREAM_CODEC = StreamCodec.unit(
        new JewelCraftingAutoFillPacket()
    );

    @Override
    public Type<JewelCraftingAutoFillPacket> type() {
        return JewelCraftingAutoFillPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof JewelCraftingMenu menu)) return;
        menu.autoFill();
    }
}
