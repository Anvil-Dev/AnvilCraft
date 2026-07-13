package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.ControlValveScreen;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

public record ControlValveFilterPacket(int index, FluidStack fluid) implements ISensitiveBiPacket {
    public static final Type<ControlValveFilterPacket> TYPE = IPacket.type(AnvilCraft.of("control_valve_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlValveFilterPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ControlValveFilterPacket::index,
        FluidStack.OPTIONAL_STREAM_CODEC,
        ControlValveFilterPacket::fluid,
        ControlValveFilterPacket::new
    );

    @Override
    public Type<ControlValveFilterPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof ControlValveScreen screen) {
            screen.setFilter(this.index, this.fluid);
        }
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof ControlValveMenu menu) || menu.getBlockEntity() == null) return;
        menu.getBlockEntity().setFilter(this.index, this.fluid);
        menu.getBlockEntity().sendUpdate();
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, this);
        }
    }
}
