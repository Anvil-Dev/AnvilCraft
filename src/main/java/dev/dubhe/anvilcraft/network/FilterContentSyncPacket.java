package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.property.FilterContent;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record FilterContentSyncPacket(
    int slotIndex,
    FilterContent filterContent
) implements CustomPacketPayload {
    public static final Type<FilterContentSyncPacket> TYPE = new Type<>(AnvilCraft.of("filter_content_sync"));

    public static final IPayloadHandler<FilterContentSyncPacket> HANDLER = FilterContentSyncPacket::serverHandler;

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContentSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        FilterContentSyncPacket::slotIndex,
        FilterContent.STREAM_CODEC,
        FilterContentSyncPacket::filterContent,
        FilterContentSyncPacket::new
    );

    private static void serverHandler(@NotNull FilterContentSyncPacket packet, @NotNull IPayloadContext ctx) {
        Player player = ctx.player();
        ItemStack item = player.getInventory().getItem(packet.slotIndex());
        if (!item.is(ModItems.FILTER)) return;
        item.set(ModComponents.FILTER_CONTENT, packet.filterContent());
    }

    @Override
    public Type<FilterContentSyncPacket> type() {
        return TYPE;
    }
}
