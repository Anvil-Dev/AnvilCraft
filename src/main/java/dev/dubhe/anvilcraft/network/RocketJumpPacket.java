package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RocketJumpPacket implements Packet {
    private final double power;

    public RocketJumpPacket(double power) {
        this.power = power;
    }

    public RocketJumpPacket(@NotNull FriendlyByteBuf buf) {
        this.power = buf.readDouble();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.ROCKET_JUMP;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeDouble(this.power);
    }

    @Override
    public void handler() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.execute(() -> minecraft.player.setDeltaMovement(0, this.power, 0));
    }
}
