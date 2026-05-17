package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.ISoundEventListener;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.init.ModInspections;
import it.unimi.dsi.fastutil.objects.Object2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class InspectionSupport {
    public static final InspectionSupport INSTANCE = new InspectionSupport();
    private final Map<Identifier, InspectionAction> inspectionActionMap = new HashMap<>();
    private final Object2BooleanMap<Identifier> inspectionState = new Object2BooleanAVLTreeMap<>();

    public static void initializeClient() {
        INSTANCE.registerActionClient(AnvilCraft.of("silencer"), (p, r, c, d) -> {
            Map<ResourceKey<Level>, List<ISoundEventListener>> map = SoundHelper.INSTANCE.getEventListeners();
            List<ISoundEventListener> listeners = map.get(Minecraft.getInstance().level.dimension());
            MultiBufferSource.BufferSource buf = r.renderBuffers.bufferSource();
            VertexConsumer vertex = buf.getBuffer(RenderTypes.lines());
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
                    0xff00Ffcc
                ));
            buf.endBatch();
        });
    }

    /**
     * 注册检查项
     *
     * <p>检查项需同时在 {@link ModInspections} 和 {@link InspectionSupport} 中注册</p>
     *
     * <p>对于 {@link ModInspections}，使用 {@link ModInspections#registerActionServer(Identifier)} 注册检查项</p>
     *
     * @see ModInspections
     */
    public void registerActionClient(Identifier id, InspectionAction action) {
        synchronized (this.inspectionActionMap) {
            if (this.inspectionActionMap.containsKey(id)) {
                throw new IllegalArgumentException("Duplicated inspection action id:" + id);
            }
            this.inspectionActionMap.put(id, action);
            this.inspectionState.put(id, false);
        }
    }

    public void changeStateClient(Identifier id, boolean state) {
        log.info("{} inspection {}.", state ? "Disabling" : "Enabling", id);
        this.inspectionState.put(id, state);
    }

    public void onRenderInspectionAction(
        PoseStack poseStack,
        SubmitNodeCollector renderer,
        Vec3 camera,
        DeltaTracker deltaTracker
    ) {
        this.inspectionActionMap.forEach((id, action) -> {
            if (this.inspectionState.getOrDefault(id, false)) {
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
            SubmitNodeCollector renderer,
            Vec3 camera,
            DeltaTracker deltaTracker
        );
    }
}
