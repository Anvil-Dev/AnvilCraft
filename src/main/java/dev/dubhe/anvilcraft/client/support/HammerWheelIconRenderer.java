package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

public final class HammerWheelIconRenderer {
    private final ModelRenderTarget target;
    private WeakReference<IconLevel> level;

    public HammerWheelIconRenderer(ClientLevel level, ModelRenderTarget target) {
        this.target = target;
        this.level = new WeakReference<>(new IconLevel(level, target));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(GuiGraphicsExtractor graphics, Vec2 camera) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        final float menuScale = AnvilCraft.CLIENT_CONFIG.ui.anvilHammerWheelScale;
        IconLevel iconLevel = this.level.get();
        if (iconLevel == null || iconLevel.parent != client.level) {
            iconLevel = new IconLevel(client.level, this.target);
            this.level = new WeakReference<>(iconLevel);
        }
        AABB bounds = iconLevel.bounds;
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
                for (var part : ModelBlockSelection.rendererParts(entity, 0)) bounds = bounds.minmax(part.bounds());
            } finally {
                entity.setBlockState(original);
            }
        }

        var rotation = pose.last().pose();
        float width = (float) (Math.abs(rotation.m00()) * bounds.getXsize()
            + Math.abs(rotation.m10()) * bounds.getYsize() + Math.abs(rotation.m20()) * bounds.getZsize());
        float height = (float) (Math.abs(rotation.m01()) * bounds.getXsize()
            + Math.abs(rotation.m11()) * bounds.getYsize() + Math.abs(rotation.m21()) * bounds.getZsize());
        final float scale = 20 * menuScale / Math.max(1, Math.max(width, height));
        var center = bounds.getCenter();
        pose.translate(0.5 - center.x, 0.5 - center.y, 0.5 - center.z);

        graphics.nextStratum();
        graphics.pose().pushMatrix();
        float resolution = Math.max(1, Math.max(
            (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11())));
        graphics.pose().scale(1 / resolution, 1 / resolution);
        float extent = 16 * menuScale * resolution;
        GuiRenderExtras.submitStructure(graphics, iconLevel, iconLevel.getMinPos().orElseThrow(), iconLevel.getMaxPos().orElseThrow(),
            -extent, -extent, extent, extent,
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
        private final AABB bounds;

        private IconLevel(ClientLevel parent, ModelRenderTarget target) {
            super(parent);
            this.parent = parent;
            this.target = target;
            if (target.state().getBlock() instanceof AbstractMultiPartBlock<?> block) {
                this.addParts(block, target.state());
            } else {
                this.setBlockState(BlockPos.ZERO, target.state());
            }
            var client = Minecraft.getInstance();
            var renderer = new ModelBlockRenderer(false, true, client.getBlockColors());
            var collector = new AABB.Builder();
            for (BlockPos pos : BlockPos.betweenClosed(this.getMinPos().orElseThrow(), this.getMaxPos().orElseThrow())) {
                BlockState state = this.getBlockState(pos);
                if (state.isAir()) continue;
                renderer.tesselateBlock((x, y, z, quad, instance) -> {
                    for (int vertex = 0; vertex < 4; vertex++) {
                        collector.include(new Vector3f(quad.position(vertex)).add(x, y, z));
                    }
                }, pos.getX(), pos.getY(), pos.getZ(), this, pos, state,
                    client.getModelManager().getBlockStateModelSet().get(state), state.getSeed(pos));
            }
            this.bounds = collector.isDefined() ? collector.build() : new AABB(BlockPos.ZERO);
        }

        private <P extends Enum<P>> void addParts(AbstractMultiPartBlock<P> block, BlockState state) {
            for (P part : block.getParts()) {
                this.setBlockState(BlockPos.ZERO.offset(block.offsetFrom(state, part)), block.placedState(part, state));
            }
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
