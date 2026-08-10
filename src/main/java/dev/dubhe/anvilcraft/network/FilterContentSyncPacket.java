package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record FilterContentSyncPacket(int slotIndex, FilterContent filterContent) implements IServerboundPacket {
    public static final Type<FilterContentSyncPacket> TYPE = new Type<>(AnvilCraft.of("filter_content_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContentSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        FilterContentSyncPacket::slotIndex,
        FilterContent.STREAM_CODEC,
        FilterContentSyncPacket::filterContent,
        FilterContentSyncPacket::new
    );

    @Override
    public Type<FilterContentSyncPacket> type() {
        return FilterContentSyncPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ItemStack stack = player.getInventory().getItem(this.slotIndex());
        if (!stack.is(ModItems.FILTER)) return;
        stack.set(ModComponents.FILTER_CONTENT, this.filterContent());
    }
}
