package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
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
public class PowerGridSyncPack implements Packet {
    private final PowerGrid.SimplePowerGrid grid;

    /**
     * 电网同步
     */
    public PowerGridSyncPack(PowerGrid grid) {
        this.grid = new PowerGrid.SimplePowerGrid(grid);
    }

    /**
     * @param buf 缓冲区
     */
    public PowerGridSyncPack(@NotNull FriendlyByteBuf buf) {
        this.grid = new PowerGrid.SimplePowerGrid(buf);
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.POWER_GRID_SYNC;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        this.grid.encode(buf);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft.getInstance().execute(() -> PowerGridRenderer.getGrids().put(this.grid.getHash(), this.grid));
    }
}
