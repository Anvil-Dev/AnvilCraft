package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.api.input.IMouseHandlerExtension;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.integration.create.VisualizationUnsupported;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import dev.dubhe.anvilcraft.network.HammerChangeBlockPacket;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.util.FullBrightLevelProxy;
import dev.dubhe.anvilcraft.util.MathUtil;
import dev.dubhe.anvilcraft.util.VertexConsumerWithPose;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static dev.dubhe.anvilcraft.util.RenderHelper.L1;
import static dev.dubhe.anvilcraft.util.RenderHelper.L2;

@ParametersAreNonnullByDefault
public class AnvilHammerScreen extends Screen implements IHasHammerEffect {
    public static final int RADIUS = 80;
    public static final int DELAY = 150; //ms
    public static final int ANIMATION_T = 300; //ms
    public static final int CLOSING_ANIMATION_T = 150; //ms
    public static final float ZOOM = 13.5f;
    public static final int IGNORE_CURSOR_MOVE_LENGTH = 15;

    private static final MethodHandle PROPERTY_TOSTRING;

    private static final int RING_COLOR = 0x88000000;
    private static final int RING_INNER_DIAMETER = 55;
    private static final int RING_OUTER_DIAMETER = 105;

    private static final int SELECTION_EFFECT_COLOR = 0xddFFFF00;
    private static final int SELECTION_EFFECT_RADIUS = 20;

    private static final float TEXT_SCALE = 1f;
    private static final int TEXT_COLOR = 0xfdfdfd;

    private static final Vector2f ROTATION_START = new Vector2f(0, 1);
    private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();

    /// Nonlinear, should bigger than 1, 1 means no animation
    private static final float SELECTION_ANIMATION_SPEED_FACTOR = 5.0f;

    static {
        MethodType mt = MethodType.methodType(
            String.class,
            Comparable.class
        );
        try {
            PROPERTY_TOSTRING = MethodHandles.lookup()
                .findVirtual(
                    Property.class,
                    "getName",
                    mt
                );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final Minecraft minecraft = Minecraft.getInstance();
    private final BlockPos targetBlockPos;
    private final Property<?> property;
    private final List<BlockState> possibleStates;
    private final Camera camera;

    private final Level replacementLevel = VisualizationUnsupported.wrap(Objects.requireNonNull(minecraft.level));
    private final BlockAndTintGetter fullBrightLevel = new FullBrightLevelProxy(minecraft.level);
    private BlockState currentBlockState;
    private final List<SelectionItem> items = new ArrayList<>();
    private long displayTime = System.currentTimeMillis();
    private boolean animationStarted = false;
    private boolean closingAnimationStarted = false;
    private Vector2f centerPos;
    private Vector2f selectionEffectPosFromCenter = MathUtil.copy(ROTATION_START).mul(RADIUS);
    /// *rad*
    private float targetAngle = 0f;
    private boolean shouldRebuildChunk = true;
    private boolean validate = true;
    private final InteractionHand hand;

    public AnvilHammerScreen(
        BlockPos targetBlockPos,
        BlockState initialBlockState,
        Property<?> property,
        List<BlockState> possibleStates,
        InteractionHand hand
    ) {
        super(Component.translatable("screen.anvilcraft.anvil_hammer.title"));
        this.targetBlockPos = targetBlockPos;
        this.currentBlockState = initialBlockState;
        this.property = property;
        this.possibleStates = possibleStates;
        this.camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        this.hand = hand;
    }

    @Override
    protected void init() {
        items.clear();
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        this.centerPos = new Vector2f(centerX, centerY);
        float degreeEachRotation = 360f / possibleStates.size();
        for (int i = 0; i < possibleStates.size(); i++) {
            BlockState state = possibleStates.get(i);
            float rotation = degreeEachRotation * i;
            Vector2f rotated = MathUtil.rotationDegrees(ROTATION_START, rotation)
                .mul(-1, 1)
                .mul(RADIUS)
                .add(centerX, centerY);
            try {
                float detectionStart = (float) (Math.toRadians(rotation - degreeEachRotation / 2f) + Math.PI);
                float detectionEnd = (float) (Math.toRadians(rotation + degreeEachRotation / 2f) + Math.PI);
                detectionStart = detectionStart % (float) (Math.PI * 2);
                detectionEnd = detectionEnd % (float) (Math.PI * 2);
                items.add(
                    new SelectionItem(
                        rotated,
                        (float) (Math.toRadians(rotation) % (Math.PI * 2)),
                        detectionStart,
                        detectionEnd,
                        state,
                        state.getBlock() instanceof IMultiPartBlockModelHolder holder
                            ? holder.mapRealModelHolderBlock(state)
                            : state,
                        Component.literal(
                            "%s".formatted(
                                PROPERTY_TOSTRING.invokeWithArguments(
                                    property,
                                    state.getValue(property)
                                )
                            )
                        )
                    )
                );
            } catch (Throwable ignored) {
            }
        }
        SelectionItem selected = items.stream()
            .filter(it -> it.state == currentBlockState)
            .findFirst()
            .orElseThrow();
        targetAngle = selected.angle;
        selectionEffectPosFromCenter = MathUtil.rotate(
            MathUtil.copy(ROTATION_START)
                .mul(RADIUS),
            -targetAngle
        ).mul(1, -1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int selectionIdx = possibleStates.indexOf(currentBlockState);
        if (scrollY > 0) {
            if (selectionIdx == possibleStates.size() - 1) {
                selectionIdx = 0;
            } else {
                selectionIdx++;
            }
        } else if (scrollY < 0) {
            if (selectionIdx == 0) {
                selectionIdx = possibleStates.size() - 1;
            } else {
                selectionIdx--;
            }
        }
        currentBlockState = possibleStates.get(selectionIdx);
        targetAngle = items.stream()
            .filter(it -> it.state == currentBlockState)
            .findFirst()
            .orElseThrow()
            .angle;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (closingAnimationStarted) return true;
        float screenCenterX = width / 2f;
        float screenCenterY = height / 2f;
        Vector2f cursorVec2 = new Vector2f(
            (float) mouseX - screenCenterX,
            (float) mouseY - screenCenterY
        );
        if (cursorVec2.length() < IGNORE_CURSOR_MOVE_LENGTH) {
            return true;
        }
        Vector2f rotationStart = new Vector2f(0, 1);
        cursorVec2.normalize();
        double rot = Math.acos(rotationStart.dot(cursorVec2) / (rotationStart.length() * cursorVec2.length()));
        double rotation = cursorVec2.x < 0 ? Math.PI - rot : Math.PI + rot;
        items.stream()
            .filter(it -> {
                if (it.detectionAngleStart > it.detectionAngleEnd) {
                    return rotation >= it.detectionAngleStart;
                }
                return rotation >= it.detectionAngleStart && rotation <= it.detectionAngleEnd;
            })
            .findFirst()
            .ifPresent(it -> {
                targetAngle = it.angle;
                currentBlockState = it.state;
            });
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @SuppressWarnings("unused")
    public void renderClosingAnimation(GuiGraphics guiGraphics, int mouseX, int mouseY, float particalTick) {
        if (!closingAnimationStarted) return;
        float delta = displayTime + CLOSING_ANIMATION_T - System.currentTimeMillis();
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        float progress = delta / CLOSING_ANIMATION_T;
        if (progress >= 1 || progress <= 0) {
            minecraft.setScreen(null);
        }
        renderProgressAnimation(guiGraphics, progress, centerX, centerY);
    }

    @SuppressWarnings({"SameParameterValue", "deprecation"})
    private void renderRotatedBlock(
        PoseStack poseStack,
        BlockState block,
        float x,
        float y,
        float z,
        float scale
    ) {
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        poseStack.pushPose();
        poseStack.translate(-7, 7, 0);
        poseStack.translate(x, y, z);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(new Matrix4f().scaling(1, -1, 1));
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getEntity().getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getEntity().getYRot() + 180f));
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        FluidState fluidState = block.getFluidState();
        MultiBufferSource.BufferSource buffers =
            Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.setupGui3DDiffuseLighting(L1, L2);
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blockRenderDispatcher.getBlockModel(block);
        for (RenderType renderType : model.getRenderTypes(block, RANDOM, ModelData.EMPTY)) {
            VertexConsumer bufferBuilder = buffers.getBuffer(renderType);
            blockRenderDispatcher.renderBatched(
                block,
                targetBlockPos,
                fullBrightLevel,
                poseStack,
                bufferBuilder,
                true,
                RANDOM,
                ModelData.EMPTY,
                renderType
            );
        }
        buffers.endLastBatch();
        if (!fluidState.isEmpty()) {
            if (block.getBlock() instanceof LiquidBlock) {
                block = block.setValue(LiquidBlock.LEVEL, block.getFluidState().getAmount());
            }
            blockRenderDispatcher.renderLiquid(
                targetBlockPos,
                fullBrightLevel,
                new VertexConsumerWithPose(
                    buffers.getBuffer(ItemBlockRenderTypes.getRenderLayer(fluidState)),
                    poseStack.last(),
                    BlockPos.ZERO
                ),
                block,
                fluidState
            );
            buffers.endLastBatch();
        }
        BlockEntity blockEntity = Objects.requireNonNull(minecraft.level).getBlockEntity(targetBlockPos);
        if (blockEntity != null && blockEntity.getBlockState().is(block.getBlock())) {
            BlockEntityRenderer<BlockEntity> renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (renderer != null) {
                Level originalLevel = blockEntity.getLevel();
                BlockState originalBlockState = blockEntity.getBlockState();
                blockEntity.setLevel(replacementLevel);
                blockEntity.setBlockState(block);
                renderer.render(
                    blockEntity,
                    partialTick,
                    poseStack,
                    buffers,
                    LightTexture.FULL_BLOCK,
                    OverlayTexture.NO_OVERLAY
                );
                if (originalLevel != null) {
                    blockEntity.setLevel(originalLevel);
                }
                blockEntity.setBlockState(originalBlockState);
            }
        }
        poseStack.popPose();
    }

    private void renderProgressAnimation(GuiGraphics guiGraphics, float progress, float centerX, float centerY) {
        progress = (float) (-Math.pow(progress, 2) + 2 * progress);
        if (progress == 0) return;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        renderRing(
            guiGraphics,
            this.width / 2f,
            this.height / 2f,
            RING_COLOR,
            RING_INNER_DIAMETER * progress,
            RING_OUTER_DIAMETER * progress
        );
        poseStack.popPose();
        float finalProgress = progress;
        items.stream()
            .filter(it -> it.state == currentBlockState)
            .findFirst()
            .ifPresent(it -> {
                Vector2f center = new Vector2f(
                    (it.center.x - centerX) / RADIUS,
                    (it.center.y - centerY) / RADIUS
                ).mul(RADIUS * finalProgress)
                    .add(centerX, centerY);
                renderSelectionEffect(
                    guiGraphics,
                    center.x,
                    center.y,
                    SELECTION_EFFECT_COLOR,
                    SELECTION_EFFECT_RADIUS
                );
            });
        for (SelectionItem value : items) {
            Vector2f center = new Vector2f(
                (value.center.x - centerX) / RADIUS,
                (value.center.y - centerY) / RADIUS
            ).mul(RADIUS * progress)
                .add(centerX, centerY);
            float x = center.x;
            float y = center.y;
            renderRotatedBlock(
                poseStack,
                value.modelBlock,
                x,
                y,
                100,
                ZOOM
            );
            int textAlpha = (int) (progress * 0xff) << 24;
            poseStack.pushPose();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = (y - offsetY - 20) / coordinateScale;
            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(coordinateScale, coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY, 0);
            poseStack.scale(TEXT_SCALE / coordinateScale, TEXT_SCALE / coordinateScale, TEXT_SCALE / coordinateScale);
            guiGraphics.drawCenteredString(
                minecraft.font,
                value.description,
                0,
                0,
                textAlpha | 0xfdfdfd
            );
            poseStack.popPose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        if (validate && !isValidState(this.currentBlockState)) {
            validate = false;
            displayTime = System.currentTimeMillis();
            closingAnimationStarted = true;
        }
        renderClosingAnimation(guiGraphics, mouseX, mouseY, partialTick);
        if (!shouldRender()) {
            return;
        }
        if (closingAnimationStarted) return;
        if (!animationStarted) {
            animationStarted = true;
            displayTime = System.currentTimeMillis();
        }
        PoseStack poseStack = guiGraphics.pose();
        float delta = displayTime + ANIMATION_T - System.currentTimeMillis();
        if (delta > 0) {
            triggerChunkRebuild();
            float progress = 1 - (delta / ANIMATION_T);
            progress = (float) (-Math.pow(progress, 2) + 2 * progress);
            if (progress == 0) return;
            renderProgressAnimation(guiGraphics, progress, centerX, centerY);
            return;
        }
        renderRing(
            guiGraphics,
            this.width / 2f,
            this.height / 2f,
            RING_COLOR,
            RING_INNER_DIAMETER,
            RING_OUTER_DIAMETER
        );
        renderSelection(guiGraphics);
        for (SelectionItem value : items) {
            float x = value.center.x;
            float y = value.center.y;
            renderRotatedBlock(
                poseStack,
                value.modelBlock,
                x,
                y,
                -100,
                ZOOM
            );
            poseStack.pushPose();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = (y - offsetY - 20) / coordinateScale;

            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(coordinateScale, coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY, 0);
            poseStack.scale(TEXT_SCALE / coordinateScale, TEXT_SCALE / coordinateScale, TEXT_SCALE / coordinateScale);
            guiGraphics.drawCenteredString(
                minecraft.font,
                value.description,
                0,
                0,
                (0xff << 24) | TEXT_COLOR
            );
            poseStack.popPose();
        }
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }

    private void renderSelection(GuiGraphics guiGraphics) {

        float selectionEffectAngle =
            MathUtil.angle(
                MathUtil.copy(ROTATION_START).negate(),
                selectionEffectPosFromCenter
            );

        float diffAngle = targetAngle - selectionEffectAngle;

        if (diffAngle > Math.PI) {
            diffAngle -= (float) (Math.PI * 2);
        } else if (diffAngle < -Math.PI) {
            diffAngle += (float) (Math.PI * 2);
        }

        selectionEffectPosFromCenter =
            MathUtil.rotate(
                selectionEffectPosFromCenter,
                diffAngle / SELECTION_ANIMATION_SPEED_FACTOR
            );

        Vector2f pos =
            MathUtil.copy(selectionEffectPosFromCenter)
                .mul(1, -1)
                .add(centerPos);

        renderSelectionEffect(
            guiGraphics,
            pos.x,
            pos.y,
            SELECTION_EFFECT_COLOR,
            SELECTION_EFFECT_RADIUS
        );
    }

    private void triggerChunkRebuild() {
        if (!shouldRebuildChunk) return;
        shouldRebuildChunk = false;
        Minecraft.getInstance().levelRenderer.setBlockDirty(
            targetBlockPos,
            false
        );
    }

    public static void renderSelectionEffect(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float radius
    ) {
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix4f = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        float x1 = centerX - radius - 5;
        float y1 = centerY - radius - 5;
        float x2 = centerX + radius + 5;
        float y2 = centerY + radius + 5;
        bufferBuilder.addVertex(matrix4f, x1, y1, -200).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, -200).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, -200).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, -200).setColor(color);

        Window window = Minecraft.getInstance().getWindow();
        float guiScale = (float) window.getGuiScale();
        RenderSystem.setShader(ModShaders::getSelectionShader);

        ModShaders.getSelectionShader()
            .safeGetUniform("Center")
            .set(centerX * guiScale, centerY * guiScale);
        ModShaders.getSelectionShader()
            .safeGetUniform("FramebufferSize")
            .set((float) window.getWidth(), (float) window.getHeight());
        ModShaders.getSelectionShader()
            .safeGetUniform("Radius")
            .set(radius * guiScale);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }

    private boolean isValidState(BlockState selected) {
        BlockState state = Objects.requireNonNull(minecraft.level).getBlockState(targetBlockPos);
        if (!state.is(selected.getBlock())) return false;
        BlockEntity entity = minecraft.level.getBlockEntity(targetBlockPos);
        return entity == null || entity.getType().isValid(selected);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (!animationStarted) {
            currentBlockState = currentBlockState.cycle(property);
        }
        if (!validate) {
            super.removed();
            return;
        }
        if (animationStarted) {
            Objects.requireNonNull(Minecraft.getInstance().level).setBlock(
                targetBlockPos,
                currentBlockState,
                Block.UPDATE_CLIENTS,
                0
            );
            PacketDistributor.sendToServer(
                new HammerChangeBlockPacket(
                    targetBlockPos,
                    currentBlockState
                )
            );
        } else {
            PacketDistributor.sendToServer(
                new HammerUsePacket(
                    targetBlockPos,
                    hand
                )
            );
            super.removed();
            return;
        }
        Minecraft.getInstance().levelRenderer.setBlockDirty(
            targetBlockPos,
            false
        );
        super.removed();
    }

    @Override
    public void tick() {
        if (closingAnimationStarted) {
            minecraft.handleKeybinds();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (shouldRender() && !closingAnimationStarted) {
            IMouseHandlerExtension.of(minecraft.mouseHandler).anvilCraft$grabMouseWithScreen();
            displayTime = System.currentTimeMillis();
            closingAnimationStarted = true;
        } else {
            minecraft.setScreen(null);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldRender() {
        if (animationStarted) return true;
        return (displayTime + DELAY) <= System.currentTimeMillis();
    }

    @Override
    public boolean shouldSkipRebuildBlock() {
        return !shouldRebuildChunk;
    }

    public static void renderRing(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerDiameter,
        float outerDiameter
    ) {
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix4f = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        float x1 = centerX - outerDiameter - 5;
        float y1 = centerY - outerDiameter - 5;
        float x2 = centerX + outerDiameter + 5;
        float y2 = centerY + outerDiameter + 5;
        bufferBuilder.addVertex(matrix4f, x1, y1, -300).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, -300).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, -300).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, -300).setColor(color);

        Window window = Minecraft.getInstance().getWindow();
        float guiScale = (float) window.getGuiScale();
        RenderSystem.setShader(ModShaders::getRingShader);

        ModShaders.getRingShader()
            .safeGetUniform("Center")
            .set(centerX * guiScale, centerY * guiScale);
        ModShaders.getRingShader()
            .safeGetUniform("InnerDiameter")
            .set(innerDiameter * guiScale);
        ModShaders.getRingShader()
            .safeGetUniform("OuterDiameter")
            .set(outerDiameter * guiScale);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }

    @Override
    public BlockPos renderingBlockPos() {
        return targetBlockPos;
    }

    @Override
    public BlockState renderingBlockState() {
        return currentBlockState;
    }

    @Override
    public RenderType renderType() {
        if (IrisState.isShaderEnabled()) {
            return RenderType.translucent();
        }
        return ModRenderTypes.TRANSLUCENT_COLORED_OVERLAY;
    }

    private record SelectionItem(
        Vector2f center,
        float angle,
        float detectionAngleStart,
        float detectionAngleEnd,
        BlockState state,
        BlockState modelBlock,
        Component description
    ) {
    }
}
