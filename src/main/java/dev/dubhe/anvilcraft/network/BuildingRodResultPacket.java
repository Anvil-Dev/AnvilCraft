package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.building.BuildingRodClient;
import dev.dubhe.anvilcraft.client.building.BuildingRodItemRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record BuildingRodResultPacket(boolean complete, ItemStack placed) implements IClientboundPacket {
    public BuildingRodResultPacket(boolean complete) {
        this(complete, ItemStack.EMPTY);
    }

    public static final Type<BuildingRodResultPacket> TYPE = IPacket.type(AnvilCraft.of("building_rod_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildingRodResultPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, BuildingRodResultPacket::complete,
        ItemStack.OPTIONAL_STREAM_CODEC, BuildingRodResultPacket::placed, BuildingRodResultPacket::new);

    @Override
    public Type<BuildingRodResultPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (this.complete) {
            BuildingRodClient.cancel();
            player.displayClientMessage(Component.translatable("message.anvilcraft.building_rod.placed"), true);
        } else if (!this.placed.isEmpty()) {
            BuildingRodItemRenderer.placedOffhand(this.placed);
        } else {
            BuildingRodItemRenderer.placed();
        }
    }
}
