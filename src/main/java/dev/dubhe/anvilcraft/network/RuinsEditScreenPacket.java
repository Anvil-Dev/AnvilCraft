package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.RuinsScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record RuinsEditScreenPacket(BlockPos pos, String drops, boolean fragile, List<String> suggestions) implements IClientboundPacket {
    public static final Type<RuinsEditScreenPacket> TYPE = IPacket.type(AnvilCraft.of("ruins_edit_screen"));
    public static final StreamCodec<ByteBuf, RuinsEditScreenPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, RuinsEditScreenPacket::pos,
        ByteBufCodecs.STRING_UTF8, RuinsEditScreenPacket::drops,
        ByteBufCodecs.BOOL, RuinsEditScreenPacket::fragile,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), RuinsEditScreenPacket::suggestions,
        RuinsEditScreenPacket::new
    );

    @Override
    public Type<RuinsEditScreenPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        RuinsScreen.open(this.pos, this.drops, this.fragile, this.suggestions);
    }
}
