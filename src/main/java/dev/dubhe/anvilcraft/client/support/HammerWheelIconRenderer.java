package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

public final class HammerWheelIconRenderer {
    private final ModelRenderTarget target;
    private final boolean multipart;
    private WeakReference<IconLevel> level;

    public HammerWheelIconRenderer(ClientLevel level, ModelRenderTarget target, BlockState selected) {
        this.target = target;
        this.multipart = selected.getBlock() instanceof IMultiPartBlockModelHolder;
        this.level = new WeakReference<>(new IconLevel(level, target));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(GuiGraphicsExtractor graphics, Vec2 camera) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float menuScale = AnvilCraft.CLIENT_CONFIG.anvilHammerRadialMenuScale;
        float scale = (this.multipart ? 5 : 13.5F) * menuScale;
        float x = -7 * menuScale + scale / 2;
        float y = 7 * menuScale - scale / 2;
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(camera.x));
        pose.mulPose(Axis.YP.rotationDegrees(camera.y + 180));

        BlockEntity entity = client.level.getBlockEntity(this.target.pos());
        BlockEntityRenderer renderer = entity != null && entity.getBlockState().is(this.target.state().getBlock())
            ? client.getBlockEntityRenderDispatcher().getRenderer(entity) : null;
        BlockEntityRenderState entityState = renderer == null ? null : renderer.createRenderState();
        if (entityState != null) {
            BlockState original = entity.getBlockState();
            try {
                entity.setBlockState(this.target.state());
                renderer.extractRenderState(entity, entityState, client.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                    client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.pos, null);
                entityState.lightCoords = LightCoordsUtil.pack(15, 0);
            } finally {
                entity.setBlockState(original);
            }
        }

        graphics.nextStratum();
        graphics.pose().pushMatrix();
        float resolution = Math.max(1, Math.max(
            (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11())));
        graphics.pose().scale(1 / resolution, 1 / resolution);
        IconLevel iconLevel = this.level.get();
        if (iconLevel == null || iconLevel.parent != client.level) {
            iconLevel = new IconLevel(client.level, this.target);
            this.level = new WeakReference<>(iconLevel);
        }
        float extent = scale * 4;
        GuiRenderExtras.submitStructure(graphics, iconLevel, BlockPos.ZERO, BlockPos.ZERO,
            (x - extent) * resolution, (y - extent) * resolution, (x + extent) * resolution, (y + extent) * resolution,
            scale * resolution, false, false, pose, (collector, modelPose) -> {
                if (entityState != null) {
                    renderer.submit(entityState, modelPose, collector,
                        client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState);
                }
            });
        graphics.pose().popMatrix();
    }

    private static final class IconLevel extends LevelLike {
        private final ClientLevel parent;
        private final ModelRenderTarget target;

        private IconLevel(ClientLevel parent, ModelRenderTarget target) {
            super(parent);
            this.parent = parent;
            this.target = target;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(BlockPos.ZERO) ? this.target.state() : Blocks.AIR.defaultBlockState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public CardinalLighting cardinalLighting() {
            return this.parent.cardinalLighting();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return this.parent.getBlockTint(this.target.pos().offset(pos), color);
        }
    }
}
