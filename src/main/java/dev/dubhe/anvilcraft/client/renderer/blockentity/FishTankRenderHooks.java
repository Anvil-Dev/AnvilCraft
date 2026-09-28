package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 鱼缸客户端扩展：提取阶段读取世界数据，提交阶段仅绘制已捕获的数据。 */
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

    public static List<AfterRender> extract(FishTankBlockEntity tank, float partialTick) {
        if (HANDLERS.isEmpty()) return List.of();
        List<AfterRender> renders = new ArrayList<>();
        for (Handler handler : HANDLERS) {
            AfterRender render = handler.extract(tank, partialTick);
            if (render != null) renders.add(render);
        }
        return List.copyOf(renders);
    }

    public static void afterRender(List<AfterRender> renders, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        for (AfterRender render : renders) render.submit(pose, collector, light, overlay);
    }

    public interface Handler {
        default boolean showVanillaFire(FishTankBlockEntity tank) {
            return true;
        }

        /** 返回的绘制回调应只捕获本阶段提取的数据，不在提交阶段读取方块实体或世界。 */
        default @Nullable AfterRender extract(FishTankBlockEntity tank, float partialTick) {
            return null;
        }
    }

    @FunctionalInterface
    public interface AfterRender {
        void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay);
    }
}
