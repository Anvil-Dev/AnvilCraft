package dev.dubhe.anvilcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundEventListener;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.init.ModInspections;
import it.unimi.dsi.fastutil.objects.Object2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ModInspectionClient {
    public static final ModInspectionClient INSTANCE = new ModInspectionClient();
    private final Map<ResourceLocation, InspectionAction> inspectionActionMap = new HashMap<>();
    private final Object2BooleanMap<ResourceLocation> inspectionState = new Object2BooleanAVLTreeMap<>();

    public static void initializeClient() {
        INSTANCE.registerActionClient(AnvilCraft.of("silencer"), (p, r, c, d) -> {
            Map<ClientLevel, List<SoundEventListener>> map = SoundHelper.INSTANCE.getEventListeners();
            List<SoundEventListener> listeners = map.get(Minecraft.getInstance().level);
            MultiBufferSource.BufferSource buf = r.renderBuffers.bufferSource();
            VertexConsumer vertex = buf.getBuffer(RenderType.LINES);
            if (listeners == null || listeners.isEmpty()) return;
            listeners.stream().filter(it -> it instanceof IHasAffectRange)
                .map(it -> ((IHasAffectRange) it).shape())
                .forEach(it -> TooltipRenderHelper.renderOutline(
                    p,
                    vertex,
                    c.x,
                    c.y,
                    c.z,
                    BlockPos.ZERO,
                    Shapes.create(it),
                    0xff00ffcc
                ));
            buf.endBatch();
        });
    }

    /**
     * 注册检查项
     * <p>
     * 检查项需同时在 {@link ModInspections} 和 {@link dev.dubhe.anvilcraft.client.ModInspectionClient} 中注册
     * </p>
     * <p>
     * 对于 {@link ModInspections}
     * 使用 {@link ModInspections#registerActionServer(ResourceLocation)} 注册检查项
     * </p>
     *
     * @see ModInspections
     */
    public void registerActionClient(ResourceLocation id, InspectionAction action) {
        synchronized (inspectionActionMap) {
            if (inspectionActionMap.containsKey(id)) {
                throw new IllegalArgumentException("Duplicated inspection action id:" + id);
            }
            inspectionActionMap.put(id, action);
            inspectionState.put(id, false);
        }
    }

    public void changeStateClient(ResourceLocation id, boolean state) {
        log.info("{} inspection {}.", state ? "Disabling" : "Enabling", id);
        inspectionState.put(id, state);
    }

    public void onRenderInspectionAction(
        PoseStack poseStack,
        LevelRenderer renderer,
        Vec3 camera,
        DeltaTracker deltaTracker
    ) {
        inspectionActionMap.forEach((id, action) -> {
            if (inspectionState.getOrDefault(id, false)) {
                action.onRenderInspection(
                    poseStack,
                    renderer,
                    camera,
                    deltaTracker
                );
            }
        });
    }

    @FunctionalInterface
    public interface InspectionAction {
        /**
         * 当检查项启用时 将调用此方法渲染
         */
        void onRenderInspection(
            PoseStack poseStack,
            LevelRenderer renderer,
            Vec3 camera,
            DeltaTracker deltaTracker
        );
    }
}
