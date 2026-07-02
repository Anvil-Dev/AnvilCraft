package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.ScreenShakeManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/// 屏幕震动 —— 在 ComputeCameraAngles 阶段往摄像机角度叠加衰减抖动。
/// 由超新星爆发、巨型铁砧撼地等事件通过 {@link ScreenShakeManager} 触发。
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ScreenShakeEventListener {

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ScreenShakeManager manager = ScreenShakeManager.getInstance();
        if (!manager.isActive()) return;
        if (Minecraft.getInstance().isPaused()) return;
        float partialTick = (float) event.getPartialTick();
        float[] offsets = manager.computeAngleOffsets(partialTick);
        if (offsets == null) return;
        event.setYaw(event.getYaw() + offsets[0]);
        event.setPitch(event.getPitch() + offsets[1]);
        event.setRoll(event.getRoll() + offsets[2]);
    }
}
