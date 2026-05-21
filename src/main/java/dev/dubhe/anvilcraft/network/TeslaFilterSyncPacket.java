package dev.dubhe.anvilcraft.network;

import com.google.common.collect.Lists;
import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.teslatower.TeslaFilter;
import dev.dubhe.anvilcraft.client.gui.screen.TeslaTowerScreen;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record TeslaFilterSyncPacket(List<FilterData> filters) implements IClientboundPacket {
    public static final Type<TeslaFilterSyncPacket> TYPE = IPacket.type(AnvilCraft.of("tesla_filter_sync"));
    public static final StreamCodec<ByteBuf, TeslaFilterSyncPacket> STREAM_CODEC = StreamCodec.composite(
        FilterData.STREAM_CODEC.apply(ByteBufCodecs.list()),
        TeslaFilterSyncPacket::filters,
        TeslaFilterSyncPacket::new
    );

    public static TeslaFilterSyncPacket create(List<Pair<TeslaFilter, String>> filters) {
        return new TeslaFilterSyncPacket(Lists.transform(filters, FilterData::new));
    }

    @Override
    public Type<TeslaFilterSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof TeslaTowerScreen screen)) return;
        screen.handleSync(Lists.transform(this.filters, data -> Pair.of(data.filter, data.extra)));
    }

    public record FilterData(TeslaFilter filter, String extra) {
        public static final StreamCodec<ByteBuf, FilterData> STREAM_CODEC = StreamCodec.composite(
            TeslaFilter.STREAM_CODEC,
            FilterData::filter,
            ByteBufCodecs.STRING_UTF8,
            FilterData::extra,
            FilterData::new
        );

        public FilterData(Pair<TeslaFilter, String> data) {
            this(data.left(), data.right());
        }
    }
}
