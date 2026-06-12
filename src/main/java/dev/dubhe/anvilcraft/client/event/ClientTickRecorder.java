package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ClientTickRecorder {
    @Getter
    private static int ticks;

    @ApiStatus.Internal
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre e) {
        if (!Minecraft.getInstance().isPaused()) {
            // 每24小时重置一次（20 tick/s × 3600 s × 24 h = 1,728,000），以保持浮点精度
            ClientTickRecorder.ticks = (ClientTickRecorder.ticks + 1) % 1_728_000;
            // 驱动震波弹跳动画
            SeismicBounceManager.getInstance().tick();
        }
    }
}
