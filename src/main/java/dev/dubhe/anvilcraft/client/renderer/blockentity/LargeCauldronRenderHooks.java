package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 大型炼药锅客户端渲染扩展。仅在客户端渲染线程调用；不得在公共模组入口初始化其它模组的客户端 API。
 */
public final class LargeCauldronRenderHooks {
    private static final List<Handler> HANDLERS = new CopyOnWriteArrayList<>();

    private LargeCauldronRenderHooks() {
    }

    public static void register(Handler handler) {
        HANDLERS.add(handler);
    }

    public static boolean showVanillaFire(LargeCauldronBlockEntity cauldron) {
        for (Handler handler : HANDLERS) {
            if (!handler.showVanillaFire(cauldron)) return false;
        }
        return true;
    }

    public static void afterRender(
        LargeCauldronBlockEntity cauldron,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        for (Handler handler : HANDLERS) {
            handler.afterRender(cauldron, partialTick, poseStack, buffers, packedLight, packedOverlay);
        }
    }

    public interface Handler {
        default boolean showVanillaFire(LargeCauldronBlockEntity cauldron) {
            return true;
        }

        default void afterRender(
            LargeCauldronBlockEntity cauldron,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
        ) {
        }
    }
}
