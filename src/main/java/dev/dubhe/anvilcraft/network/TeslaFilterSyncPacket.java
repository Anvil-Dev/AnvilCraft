package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.client.gui.screen.TeslaTowerScreen;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TeslaFilterSyncPacket implements CustomPacketPayload {
    public static final Type<TeslaFilterSyncPacket> TYPE = new Type<>(AnvilCraft.of("tesla_filter_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeslaFilterSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(TeslaFilterSyncPacket::encode, TeslaFilterSyncPacket::new);
    public static final IPayloadHandler<TeslaFilterSyncPacket> HANDLER = TeslaFilterSyncPacket::clientHandler;

    private final List<Pair<TeslaFilter, String>> filters;

    public TeslaFilterSyncPacket(List<Pair<TeslaFilter, String>> filters) {
        this.filters = filters;
    }

    public TeslaFilterSyncPacket(RegistryFriendlyByteBuf buf) {
        List<String> ids = buf.readList(FriendlyByteBuf::readUtf);
        List<String> args = buf.readList(FriendlyByteBuf::readUtf);
        ArrayList<Pair<TeslaFilter, String>> filters = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            filters.add(Pair.of(TeslaFilter.getFilter(ids.get(i)), args.get(i)));
        }
        this.filters = filters;
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeCollection(filters.stream().map(it -> it.left().getId()).toList(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(filters.stream().map(Pair::right).toList(), FriendlyByteBuf::writeUtf);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public static void clientHandler(TeslaFilterSyncPacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof TeslaTowerScreen screen) {
                screen.handleSync(data.filters);
            }
        });
    }
}
