package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.block.BigRedButtonBlockItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public record SwitchButtonBasePacket(InteractionHand hand, DyeColor color, boolean wasBase) implements IServerboundPacket {
    public static final Type<SwitchButtonBasePacket> TYPE = IPacket.type(AnvilCraft.of("switch_button_base"));
    public static final StreamCodec<ByteBuf, SwitchButtonBasePacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class), SwitchButtonBasePacket::hand,
        DyeColor.STREAM_CODEC, SwitchButtonBasePacket::color,
        ByteBufCodecs.BOOL, SwitchButtonBasePacket::wasBase,
        SwitchButtonBasePacket::new
    );

    @Override
    public Type<SwitchButtonBasePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ItemStack stack = player.getItemInHand(this.hand);
        if (player.isSpectator() || stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.RED) != this.color
            || (stack.get(ModComponents.BUTTON_CAP) == ButtonCap.BASE) != this.wasBase) {
            return;
        }
        BigRedButtonBlockItem.switchBase(stack);
    }
}
