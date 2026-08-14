package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 鱼缸客户端渲染扩展。仅在客户端渲染线程调用。
 */
public final class FishTankRenderHooks {
    private static final List<Handler> HANDLERS = new CopyOnWriteArrayList<>();

    private FishTankRenderHooks() {
    }

    public static void register(Handler handler) {
        HANDLERS.add(handler);
    }

    public static boolean showVanillaFire(FishTankBlockEntity tank) {
        for (Handler handler : HANDLERS) {
            if (!handler.showVanillaFire(tank)) return false;
        }
        return true;
    }

    public static void afterRender(
        FishTankBlockEntity tank,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        for (Handler handler : HANDLERS) {
            handler.afterRender(tank, partialTick, poseStack, buffers, packedLight, packedOverlay);
        }
    }

    public interface Handler {
        default boolean showVanillaFire(FishTankBlockEntity tank) {
            return true;
        }

        default void afterRender(
            FishTankBlockEntity tank,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
        ) {
        }
    }
}
