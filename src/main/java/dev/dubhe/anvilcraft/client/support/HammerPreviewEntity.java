package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** 保存候选朝向的渲染快照，使图标和世界虚影都可在还原方块实体后提交。 */
public record HammerPreviewEntity(
    @Nullable BlockEntityRenderState model, @Nullable CachedBlockEntityRenderState cached, @Nullable AABB bounds
) {
    private static final ScopedValue<Boolean> EXTRACTING = ScopedValue.newInstance();

    public static boolean isExtracting() {
        return EXTRACTING.isBound();
    }

    public static @Nullable CachedBlockEntityRenderState extractCached(BlockEntity entity, float partialTick, Camera camera) {
        return ScopedValue.where(EXTRACTING, true).call(
            () -> CachedBlockEntityRenderDispatcher.INSTANCE.tryExtractRenderState(entity, partialTick, camera));
    }

    public static @Nullable HammerPreviewEntity extract(
        @Nullable BlockEntity entity, BlockState preview, float partialTick, Camera camera, boolean icon
    ) {
        if (entity == null || !entity.getBlockState().is(preview.getBlock()) || !entity.getType().isValid(preview)) {
            return null;
        }
        BlockState original = entity.getBlockState();
        try {
            entity.setBlockState(preview);
            return ScopedValue.where(EXTRACTING, true).call(() -> {
                var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
                BlockEntityRenderState model = null;
                AABB bounds = null;
                if (renderer != null) {
                    model = renderer.createRenderState();
                    renderer.extractRenderState(entity, model, partialTick, camera.position(), null);
                    model.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                    if (icon) {
                        for (var part : ModelBlockSelection.rendererParts(entity, partialTick)) {
                            bounds = bounds == null ? part.bounds() : bounds.minmax(part.bounds());
                        }
                    }
                }
                var cached = icon ? null : CachedBlockEntityRenderDispatcher.INSTANCE.tryExtractRenderState(entity, partialTick, camera);
                return new HammerPreviewEntity(model, cached, bounds);
            });
        } finally {
            entity.setBlockState(original);
        }
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (this.model != null) {
            Minecraft.getInstance().getBlockEntityRenderDispatcher().submit(this.model, pose, collector, camera);
        }
        if (this.cached != null) {
            CachedBlockEntityRenderDispatcher.INSTANCE.submit(this.cached, pose, collector, camera);
        }
    }
}
