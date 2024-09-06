package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.init.ModNetworks;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Getter
public class PowerGridRemovePack implements Packet {
    private final int grid;

    /**
     * 电网移除
     */
    public PowerGridRemovePack(@NotNull PowerGrid grid) {
        this.grid = grid.hashCode();
    }

    /**
     * @param buf 缓冲区
     */
    public PowerGridRemovePack(@NotNull FriendlyByteBuf buf) {
        this.grid = buf.readInt();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.POWER_GRID_REMOVE;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(this.grid);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft.getInstance().execute(() -> PowerGridRenderer.getGridMap().remove(this.grid));
    }
}
