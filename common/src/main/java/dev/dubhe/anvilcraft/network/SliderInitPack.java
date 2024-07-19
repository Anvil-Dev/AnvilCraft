package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.SliderScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SliderInitPack implements Packet {
    private final int value;
    private final int min;
    private final int max;

    /**
     * @param value 当前值
     * @param min 最小值
     * @param max 最大值
     */
    public SliderInitPack(int value, int min, int max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

    /**
     * @param buf 缓冲区
     */
    public SliderInitPack(@NotNull FriendlyByteBuf buf) {
        this.value = buf.readInt();
        this.min = buf.readInt();
        this.max = buf.readInt();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.SLIDER_INIT;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(this.value);
        buf.writeInt(this.min);
        buf.writeInt(this.max);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (!(client.screen instanceof SliderScreen screen)) return;
            screen.setMin(this.min);
            screen.setMax(this.max);
            screen.setValue(this.value);
        });
    }
}
