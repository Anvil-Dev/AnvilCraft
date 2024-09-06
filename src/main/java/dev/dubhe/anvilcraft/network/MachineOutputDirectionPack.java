package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.BaseMachineScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.BaseMachineMenu;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

@Getter
public class MachineOutputDirectionPack implements Packet {
    private final Direction direction;

    @Override
    public ResourceLocation getType() {
        return ModNetworks.DIRECTION_PACKET;
    }

    public MachineOutputDirectionPack(Direction direction) {
        this.direction = direction;
    }

    public MachineOutputDirectionPack(@NotNull FriendlyByteBuf buf) {
        this(buf.readEnum(Direction.class));
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(this.getDirection());
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof BaseMachineMenu menu)) return;
            Direction direction = this.getDirection();
            menu.setDirection(direction);
            this.send(player);
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (!(client.screen instanceof BaseMachineScreen<?> screen)) return;
            if (screen.getDirectionButton() == null) return;
            screen.getDirectionButton().setDirection(this.direction);
        });
    }
}
