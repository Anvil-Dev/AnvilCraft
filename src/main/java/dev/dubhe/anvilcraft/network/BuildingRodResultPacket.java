package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.building.BuildingRodClient;
import dev.dubhe.anvilcraft.client.building.BuildingRodItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record BuildingRodResultPacket(boolean complete, ItemStack placed, long seed, List<Change> changes) implements IClientboundPacket {
    public record Change(BlockPos pos, BlockState state) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Change> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Change::pos, ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Change::state, Change::new);
    }

    public BuildingRodResultPacket(boolean complete, ItemStack placed) {
        this(complete, placed, 0, List.of());
    }

    public BuildingRodResultPacket(boolean complete) {
        this(complete, ItemStack.EMPTY);
    }

    public static final Type<BuildingRodResultPacket> TYPE = IPacket.type(AnvilCraft.of("building_rod_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildingRodResultPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, BuildingRodResultPacket::complete,
        ItemStack.OPTIONAL_STREAM_CODEC, BuildingRodResultPacket::placed,
        ByteBufCodecs.LONG, BuildingRodResultPacket::seed, Change.CODEC.apply(ByteBufCodecs.list()), BuildingRodResultPacket::changes,
        BuildingRodResultPacket::new);

    @Override
    public Type<BuildingRodResultPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (this.complete) {
            BuildingRodClient.cancel();
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("message.anvilcraft.building_rod.placed"), false);
        } else if (!this.placed.isEmpty()) {
            BuildingRodItemRenderer.placedOffhand(this.placed);
        } else {
            BuildingRodClient.placementConfirmed(this.seed, this.changes);
            BuildingRodItemRenderer.placed();
        }
    }
}
