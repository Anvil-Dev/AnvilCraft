package dev.dubhe.anvilcraft.client.gui.screen.cfa;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyTextureBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * 锻星砧界面的画中画渲染器，用于绘制真实天体和巨构模型。
 */
public final class CfaPreviewPipRenderer extends PictureInPictureRenderer<CfaPreviewPipRenderer.State> {
    private static final float UI_AXIAL_TILT = 25.0f;
    private final @Nullable SkullModelBase playerHeadModel;

    public CfaPreviewPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
        this.playerHeadModel = SkullBlockRenderer.createModel(
            Minecraft.getInstance().getEntityModels(),
            SkullBlock.Types.PLAYER
        );
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);

        SubmitNodeStorage nodes = new SubmitNodeStorage();
        switch (state.content()) {
            case BodyContent body -> this.submitBody(body, poseStack, nodes);
            case ModelContent model -> this.submitModelPreview(model, poseStack, nodes);
        }
        this.renderSubmittedFeatures(minecraft, nodes);
    }

    private void submitModelPreview(ModelContent content, PoseStack poseStack, SubmitNodeCollector collector) {
        // PIP 基类已经翻转 Z 轴，此处与原版物品预览保持一致，恢复模型的正确朝向和绕序。
        poseStack.scale(1.0f, -1.0f, -1.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(content.rotationDegrees()));
        // 束星环模型以原点为几何中心，不能按普通方块模型平移半格。
        this.submitStandalone(content.model(), false, content.seed(), poseStack, collector, true);
    }

    private void submitBody(BodyContent content, PoseStack poseStack, SubmitNodeCollector collector) {
        CelestialBodyData body = content.body();
        // PIP 基类已经翻转 Z 轴，再翻转 Y/Z 可得到与界面坐标一致的正向天体。
        poseStack.scale(1.0f, -1.0f, -1.0f);
        if (body instanceof SpecialCelestialBodyData special && special.isErrorPlanet()) {
            poseStack.scale(0.25f, 0.25f, 0.25f);
        }
        if (body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            poseStack.scale(1.5f, 1.5f, 1.5f);
        }
        float rotation = content.animationTick()
            * CelestialBodyData.getVisualRotationSpeed(body.rotationSpeed());
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(UI_AXIAL_TILT));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        if (body instanceof SpecialCelestialBodyData special && special.needsCustomModel()) {
            if (special.isPlayerHead()) {
                this.submitPlayerHead(special, poseStack, collector);
            } else {
                this.submitComplexBody(special, content.seed(), poseStack, collector);
            }
        } else if (body instanceof StarData star) {
            this.submitStar(star, content.seed(), poseStack, collector);
        } else {
            this.submitPlanet(body, poseStack, collector);
        }
        poseStack.popPose();
        this.submitCelestialRing(body, rotation, poseStack, collector);
    }

    private void submitComplexBody(
        SpecialCelestialBodyData special,
        long seed,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        StandaloneModelKey<BlockStateModel> model = CelestialBodyPreviewRenderer.resolveSpecialModel(special);
        this.submitStandalone(model, false, seed, poseStack, collector, false);
        if (special.hasAtmosphere() && special.temperature() != null) {
            this.submitAtmosphere(special.temperature(), poseStack, collector);
        }
    }

    private void submitPlayerHead(
        SpecialCelestialBodyData special,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        if (this.playerHeadModel == null || special.playerHeadProfile() == null) return;
        ResolvableProfile profile = ResolvableProfile.CODEC
            .parse(NbtOps.INSTANCE, special.playerHeadProfile())
            .result()
            .orElse(null);
        if (profile == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.25f, 0.5f);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        SkullBlockRenderer.submitSkull(
            0.0f,
            poseStack,
            collector,
            LightCoordsUtil.FULL_BRIGHT,
            this.playerHeadModel,
            Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).renderType(),
            0,
            null
        );
        poseStack.popPose();
    }

    private void submitStar(
        StarData star,
        long seed,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            this.submitStandalone(CFARenderer.BODY_BLACK_HOLE, true, seed, poseStack, collector, true);
            return;
        }
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
            this.submitStandalone(CFARenderer.BODY_NEUTRON_STAR, false, seed, poseStack, collector, true);
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            float magneticTilt = star.magneticFieldStrength() >= 5 ? 15.0f : 10.0f;
            poseStack.mulPose(Axis.XP.rotationDegrees(magneticTilt));
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            this.submitStandalone(CFARenderer.BODY_NEUTRON_STAR_JET, true, seed, poseStack, collector, true);
            poseStack.popPose();
            return;
        }

        this.submitStandalone(CFARenderer.BODY_STAR, false, seed, poseStack, collector, true);
        final float[] color = CelestialBodyRenderer.getStarColor(star);
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.scale(1.005f, 1.005f, 1.005f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        collector.submitCustomGeometry(
            poseStack,
            ModRenderTypes.STAR_COLOR_OVERLAY,
            (pose, consumer) -> CelestialBodyRenderer.renderColorCube(
                pose,
                consumer,
                color[0],
                color[1],
                color[2],
                1.0f,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            )
        );
        poseStack.popPose();

        for (int i = 0; i < 10; i++) {
            float progress = i / 10.0f;
            float scale = 1.0f + progress * 0.6f;
            final float alpha = (1.2f - 1.125f * progress) / 10.0f;
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            this.submitTranslucentCube(color, alpha, poseStack, collector);
            poseStack.popPose();
        }
    }

    private void submitPlanet(
        CelestialBodyData body,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        Identifier texture = CelestialBodyTextureBakery.getOrBakeBody(body);
        if (texture != null) {
            collector.submitCustomGeometry(
                poseStack,
                ModRenderTypes.STAR_CUTOUT.apply(texture),
                (pose, consumer) -> CelestialBodyRenderer.renderPlanetBody(
                    pose,
                    consumer,
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
                )
            );
        }

        Temperature atmosphere = null;
        if (body instanceof RockyPlanetData rocky && rocky.hasAtmosphere()) {
            atmosphere = rocky.temperature();
        } else if (body instanceof SpecialCelestialBodyData special && special.hasAtmosphere()) {
            atmosphere = special.temperature();
        }
        if (atmosphere != null) {
            this.submitAtmosphere(atmosphere, poseStack, collector);
        }

    }

    /** 独立提交天体环，避免主体的半格平移和大气层缩放污染天体环姿态。 */
    private void submitCelestialRing(
        CelestialBodyData body,
        float rotation,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        if (body.ringType() == RingType.NONE) return;
        Identifier ringTexture = CelestialBodyTextureBakery.getOrBakeRing(body);
        if (ringTexture == null) return;

        float ringScale = switch (body) {
            case RockyPlanetData ignored -> 1.35f;
            case GiantPlanetData ignored -> 1.3f;
            default -> 1.4f;
        };
        poseStack.pushPose();
        poseStack.scale(ringScale, ringScale, ringScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(UI_AXIAL_TILT));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        collector.submitCustomGeometry(
            poseStack,
            ModRenderTypes.CELESTIAL_RING.apply(ringTexture),
            (pose, consumer) -> CelestialBodyRenderer.renderRing(
                pose,
                consumer,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            )
        );
        poseStack.popPose();
    }

    private void submitAtmosphere(
        Temperature temperature,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        final float[] color = CelestialBodyRenderer.getAtmosphereColor(temperature);
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.scale(1.125f, 1.125f, 1.125f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        collector.submitCustomGeometry(
            poseStack,
            ModRenderTypes.CELESTIAL_ATMOSPHERE,
            (pose, consumer) -> CelestialBodyRenderer.renderAtmosphereCube(
                pose,
                consumer,
                color,
                0.2f,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            )
        );
        poseStack.popPose();
    }

    private void submitTranslucentCube(
        float[] color,
        float alpha,
        PoseStack poseStack,
        SubmitNodeCollector collector
    ) {
        collector.submitCustomGeometry(
            poseStack,
            ModRenderTypes.CELESTIAL_ATMOSPHERE,
            (pose, consumer) -> CelestialBodyRenderer.renderColorCube(
                pose,
                consumer,
                color[0],
                color[1],
                color[2],
                alpha,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            )
        );
    }

    private void submitStandalone(
        StandaloneModelKey<BlockStateModel> modelKey,
        boolean translucent,
        long seed,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        boolean fullBright
    ) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getStandaloneModel(modelKey);
        if (model == null) return;
        BlockModelRenderState renderState = new BlockModelRenderState();
        model.collectParts(
            BlockAndTintGetter.EMPTY,
            BlockPos.ZERO,
            Blocks.AIR.defaultBlockState(),
            RandomSource.create(seed),
            renderState.setupModel(new Matrix4f(), translucent)
        );
        if (fullBright) {
            renderState.submitModel(
                translucent ? ModRenderTypes.TRANSLUCENT_BLOCK : ModRenderTypes.CUTOUT_BLOCK,
                poseStack,
                collector,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0
            );
        } else {
            renderState.submit(
                poseStack,
                collector,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0
            );
        }
    }

    private void renderSubmittedFeatures(Minecraft minecraft, SubmitNodeStorage nodes) {
        RenderBuffers renderBuffers = minecraft.renderBuffers();
        GameRenderer gameRenderer = minecraft.gameRenderer;
        FeatureRenderDispatcher dispatcher = new FeatureRenderDispatcher(
            nodes,
            minecraft.getModelManager(),
            this.bufferSource,
            minecraft.getAtlasManager(),
            renderBuffers.outlineBufferSource(),
            renderBuffers.crumblingBufferSource(),
            minecraft.font,
            gameRenderer.getGameRenderState()
        );
        dispatcher.renderAllFeatures();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0f;
    }

    @Override
    protected String getTextureLabel() {
        return "celestial forging anvil preview";
    }

    @Override
    public boolean canBeReusedFor(State state, int textureWidth, int textureHeight) {
        return false;
    }

    /** 界面预览内容。 */
    public sealed interface Content permits BodyContent, ModelContent {
    }

    /** 天体预览内容。 */
    public record BodyContent(CelestialBodyData body, float animationTick, long seed) implements Content {
    }

    /** 独立方块模型预览内容。 */
    public record ModelContent(
        StandaloneModelKey<BlockStateModel> model,
        float rotationDegrees,
        long seed
    ) implements Content {
    }

    /**
     * 画中画渲染状态。
     */
    public record State(
        Content content,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) implements PictureInPictureRenderState {
        public State(
            Content content,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
        ) {
            this(
                content,
                x0,
                y0,
                x1,
                y1,
                scale,
                pose,
                scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
            );
        }

        public static State body(
            GuiGraphicsExtractor graphics,
            CelestialBodyData body,
            float animationTick,
            long seed,
            int x,
            int y,
            int width,
            int height,
            float scale
        ) {
            return new State(
                new BodyContent(body, animationTick, seed),
                x,
                y,
                x + width,
                y + height,
                scale,
                graphics.pose().get(new Matrix3x2f()),
                graphics.peekScissorStack()
            );
        }

        public static State model(
            GuiGraphicsExtractor graphics,
            StandaloneModelKey<BlockStateModel> model,
            float rotationDegrees,
            long seed,
            int x,
            int y,
            int width,
            int height,
            float scale
        ) {
            return new State(
                new ModelContent(model, rotationDegrees, seed),
                x,
                y,
                x + width,
                y + height,
                scale,
                graphics.pose().get(new Matrix3x2f()),
                graphics.peekScissorStack()
            );
        }
    }
}
