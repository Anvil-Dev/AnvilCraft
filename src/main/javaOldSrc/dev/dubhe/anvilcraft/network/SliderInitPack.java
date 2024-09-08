package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.SliderScreen;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

@Getter
public class SliderInitPack implements CustomPacketPayload {
    public static final Type<SliderInitPack> TYPE = new Type<>(AnvilCraft.of("slider_init"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderInitPack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SliderInitPack::getValue,
            ByteBufCodecs.INT, SliderInitPack::getMin,
            ByteBufCodecs.INT, SliderInitPack::getMax,
            SliderInitPack::new
    );
    public static final IPayloadHandler<SliderInitPack> HANDLER = SliderInitPack::clientHandler;

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(SliderInitPack data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof SliderScreen screen)) return;
            screen.setMin(data.min);
            screen.setMax(data.max);
            screen.setValue(data.value);
        });
    }
}
