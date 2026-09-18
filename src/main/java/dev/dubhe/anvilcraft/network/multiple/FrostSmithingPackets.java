package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.entity.player.Player;

public class FrostSmithingPackets {
    @SuppressWarnings("SameParameterValue")
    private static <T extends IPacket> Type<T> of(String path) {
        return IPacket.type(AnvilCraft.of("frost_smithing_" + path));
    }

    public record ClickButton(boolean left) implements IServerboundPacket {
        public static final Type<ClickButton> TYPE = FrostSmithingPackets.of("click_button");
        public static final StreamCodec<ByteBuf, ClickButton> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClickButton::left,
            ClickButton::new
        );

        @Override
        public Type<ClickButton> type() {
            return TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (!(player.containerMenu instanceof FrostSmithingMenu menu)) return;
            menu.turn(this.left);
        }
    }
}
