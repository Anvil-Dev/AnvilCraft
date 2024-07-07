package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.init.ModNetworks;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Getter
public class PowerGridSyncPack implements Packet {
    private final SimplePowerGrid grid;

    /**
     * 电网同步
     */
    public PowerGridSyncPack(PowerGrid grid) {
        this.grid = new SimplePowerGrid(grid);
    }

    /**
     * @param buf 缓冲区
     */
    public PowerGridSyncPack(@NotNull FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        Tag data = tag.get("data");
        this.grid = SimplePowerGrid.CODEC.decode(NbtOps.INSTANCE, data)
                .getOrThrow(false, ignored -> {}).getFirst();
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
        Minecraft.getInstance().execute(() -> PowerGridRenderer.getGridMap().put(this.grid.getHash(), this.grid));
    }
}
