package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelFrostedBackground;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.api.input.IMouseHandlerExtension;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import dev.dubhe.anvilcraft.network.HammerChangeBlockPacket;
import dev.dubhe.anvilcraft.network.HammerChangeFlexibleMultiPartBlockPacket;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.util.FullBrightLevelProxy;
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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public class AnvilHammerScreen extends Screen implements IHasHammerEffect {
    public static final int DELAY = 150;
    private static final float ZOOM = 13.5f;
    private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();

    private final Minecraft minecraft = Minecraft.getInstance();
    private final float radialMenuScale = AnvilCraft.CLIENT_CONFIG.anvilHammerRadialMenuScale;
    private final BlockPos targetBlockPos;
    private final BlockState initialBlockState;
    private final ModelRenderTarget initialModelTarget;
    private final Property<?> property;
    private final List<BlockState> possibleStates;
    private final Camera camera;

    private final BlockAndTintGetter fullBrightLevel = new FullBrightLevelProxy(this.minecraft.level);
    private BlockState currentBlockState;
    private ModelRenderTarget currentModelTarget;
    private final long displayTime = System.currentTimeMillis();
    private boolean animationStarted;
    private boolean closingAnimationStarted;
    private @Nullable WheelWidget wheel;
    private @Nullable WheelFrostedBackground frostedBackground;
    private boolean shouldRebuildChunk = true;
    private boolean validate = true;
    private final InteractionHand hand;
    private final BlockHitResult hitVec;

    public AnvilHammerScreen(
        BlockPos targetBlockPos,
        BlockState initialBlockState, Property<?> property, List<BlockState> possibleStates, InteractionHand hand, BlockHitResult hitVec
    ) {
        super(Component.translatable("screen.anvilcraft.anvil_hammer.title"));
        this.targetBlockPos = targetBlockPos;
        this.initialBlockState = initialBlockState;
        this.currentBlockState = initialBlockState;
        this.property = property;
        this.possibleStates = possibleStates;
        this.camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        this.hand = hand;
        this.hitVec = hitVec;
        this.initialModelTarget = this.getModelRenderTarget(initialBlockState);
        this.currentModelTarget = this.initialModelTarget;
    }

    @Override
    protected void init() {
        if (this.frostedBackground != null) this.frostedBackground.close();
        this.frostedBackground = new WheelFrostedBackground();
        List<WheelWidget.RawSection> sections = new ArrayList<>();
        for (BlockState state : this.possibleStates) {
            ModelRenderTarget target = this.getModelRenderTarget(state);
            float scale = state.getBlock() instanceof IMultiPartBlockModelHolder ? 5 : ZOOM;
            sections.add(new WheelWidget.RawSection(propertyName(state, this.property),
                (graphics, pose, width, height) -> this.renderRotatedBlock(pose, target, 0, 0, 100, scale * this.radialMenuScale)));
        }
        this.wheel = new WheelWidget(
            0, 0, this.width, this.height, 55 * this.radialMenuScale, 105 * this.radialMenuScale,
            0, 300, 150, 0x88000000, WheelMenuModel.DEFAULT_SELECTION_EFFECT_COLOR, 5, 0xfdfdfd,
            this.radialMenuScale, sections, (int) (15 * this.radialMenuScale)
        ) {
            private double lastMouseX = Double.NaN;
            private double lastMouseY = Double.NaN;

            @Override
            public void checkMousePos(double mouseX, double mouseY) {
                if (mouseX == this.lastMouseX && mouseY == this.lastMouseY) return;
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                super.checkMousePos(mouseX, mouseY);
            }
        };
        this.wheel.setSelectionEffect(WheelSelectionEffect.ANNULAR_SECTOR);
        this.wheel.setFrostedBackground(this.frostedBackground);
        this.wheel.setCurrentIndex(this.possibleStates.indexOf(this.currentBlockState));
    }

    private static <T extends Comparable<T>> Component propertyName(BlockState state, Property<T> property) {
        return Component.literal(property.getName(state.getValue(property)));
    }

    private ModelRenderTarget getModelRenderTarget(BlockState previewState) {
        if (previewState.getBlock() instanceof IMultiPartBlockModelHolder holder) {
            return holder.getModelRenderTarget(
                Objects.requireNonNull(this.minecraft.level),
                this.targetBlockPos,
                this.initialBlockState,
                previewState
            );
        }
        return new ModelRenderTarget(this.targetBlockPos, previewState);
    }

    private void setCurrentBlockState(BlockState state) {
        this.currentBlockState = state;
        this.currentModelTarget = this.getModelRenderTarget(state);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.wheel == null || this.closingAnimationStarted) return true;
        this.wheel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        this.updateSelection();
        return true;
    }

    private void updateSelection() {
        if (this.wheel == null) return;
        int index = this.wheel.getCurrentSectionIndex();
        if (index >= 0 && index < this.possibleStates.size()) {
            this.setCurrentBlockState(this.possibleStates.get(index));
        }
    }

    @SuppressWarnings({"SameParameterValue", "deprecation"})
    private void renderRotatedBlock(
        PoseStack poseStack,
        ModelRenderTarget modelTarget,
        float x,
        float y,
        float z,
        float scale
    ) {
        final float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        poseStack.pushPose();
        poseStack.translate(-7 * this.radialMenuScale, 7 * this.radialMenuScale, 0);
        poseStack.translate(x, y, z);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(new Matrix4f().scaling(1, -1, 1));
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(this.camera.getEntity().getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(this.camera.getEntity().getYRot() + 180f));
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        BlockPos modelPos = modelTarget.pos();
        BlockState block = modelTarget.state();
        final FluidState fluidState = block.getFluidState();
        MultiBufferSource.BufferSource buffers =
            Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.setupGui3DDiffuseLighting(RenderSupport.L1, RenderSupport.L2);
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blockRenderDispatcher.getBlockModel(block);
        for (RenderType renderType : model.getRenderTypes(block, RANDOM, ModelData.EMPTY)) {
            VertexConsumer bufferBuilder = buffers.getBuffer(renderType);
            blockRenderDispatcher.renderBatched(
                block,
                modelPos,
                this.fullBrightLevel,
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
                modelPos,
                this.fullBrightLevel,
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
        BlockEntity blockEntity = Objects.requireNonNull(this.minecraft.level).getBlockEntity(modelPos);
        if (blockEntity != null && blockEntity.getBlockState().is(block.getBlock())) {
            BlockEntityRenderer<BlockEntity> renderer = this.minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (renderer != null) {
                final Level originalLevel = blockEntity.getLevel();
                final BlockState originalBlockState = blockEntity.getBlockState();
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.wheel == null) return;
        if (this.validate && !this.isValidState(this.currentBlockState)) {
            this.validate = false;
            this.closingAnimationStarted = true;
            this.wheel.onClosing();
        }
        if (!this.shouldRender()) return;
        if (!this.closingAnimationStarted) {
            this.animationStarted = true;
            this.triggerChunkRebuild();
        }
        this.wheel.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.closingAnimationStarted) this.updateSelection();
    }

    private void triggerChunkRebuild() {
        if (!this.shouldRebuildChunk) return;
        this.shouldRebuildChunk = false;
        this.minecraft.levelRenderer.setBlockDirty(this.hiddenBlockPos(), false);
    }

    private boolean isValidState(BlockState selected) {
        BlockState state = Objects.requireNonNull(this.minecraft.level).getBlockState(this.targetBlockPos);
        if (!state.is(selected.getBlock())) return false;
        BlockEntity entity = this.minecraft.level.getBlockEntity(this.targetBlockPos);
        return entity == null || entity.getType().isValid(selected);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (this.frostedBackground != null) {
            this.frostedBackground.close();
            this.frostedBackground = null;
        }
        this.restoreHiddenBlock();
        if (!this.animationStarted) {
            this.setCurrentBlockState(this.currentBlockState.cycle(this.property));
        }
        if (!this.validate) {
            super.removed();
            return;
        }
        if (this.animationStarted) {
            Objects.requireNonNull(Minecraft.getInstance().level).setBlock(
                this.targetBlockPos, this.currentBlockState,
                Block.UPDATE_CLIENTS,
                0
            );
            if (this.currentBlockState.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
                if (this.currentBlockState.hasProperty(BlockStateProperties.FACING)) {
                    PacketDistributor.sendToServer(new HammerChangeFlexibleMultiPartBlockPacket(
                        this.targetBlockPos,
                        this.currentBlockState,
                        this.currentBlockState.getValue(BlockStateProperties.FACING)
                    ));
                } else if (this.currentBlockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    PacketDistributor.sendToServer(new HammerChangeFlexibleMultiPartBlockPacket(
                        this.targetBlockPos,
                        this.currentBlockState,
                        this.currentBlockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    ));
                }
            } else {
                PacketDistributor.sendToServer(new HammerChangeBlockPacket(
                    this.targetBlockPos,
                    this.currentBlockState
                ));
            }
        } else {
            this.runLocalDefaultInteraction();
            PacketDistributor.sendToServer(new HammerUsePacket(this.targetBlockPos, this.hand, this.hitVec));
            super.removed();
            return;
        }
        super.removed();
    }

    private void restoreHiddenBlock() {
        this.shouldRebuildChunk = true;
        BlockPos hiddenPos = this.hiddenBlockPos();
        Minecraft.getInstance().levelRenderer.setBlockDirty(hiddenPos, false);
        if (!this.targetBlockPos.equals(hiddenPos)) {
            Minecraft.getInstance().levelRenderer.setBlockDirty(this.targetBlockPos, false);
        }
    }

    @Override
    public void tick() {
        if (this.closingAnimationStarted) {
            this.minecraft.handleKeybinds();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.minecraft.options.keyUse.matchesMouse(button)) return super.mouseReleased(mouseX, mouseY, button);
        if (this.shouldRender() && !this.closingAnimationStarted && this.wheel != null) {
            this.wheel.checkMousePos(mouseX, mouseY);
            this.updateSelection();
            IMouseHandlerExtension.of(this.minecraft.mouseHandler).anvilcraft$grabMouseWithScreen();
            this.closingAnimationStarted = true;
            this.wheel.onClosing();
        } else {
            this.minecraft.setScreen(null);
        }
        return true;
    }

    @Override
    public boolean shouldRender() {
        if (this.animationStarted) return true;
        return (this.displayTime + DELAY) <= System.currentTimeMillis();
    }

    @Override
    public boolean shouldSkipRebuildBlock() {
        return !this.shouldRebuildChunk;
    }

    private void runLocalDefaultInteraction() {
        Level level = this.minecraft.level;
        if (level == null || this.minecraft.player == null) return;
        BlockState state = level.getBlockState(this.targetBlockPos);
        Block block = state.getBlock();
        if (!(block instanceof DoorBlock
              || block instanceof TrapDoorBlock
              || block instanceof FenceGateBlock
              || block instanceof ButtonBlock
              || block instanceof ComparatorBlock)) {
            return;
        }
        state.useWithoutItem(level, this.minecraft.player, this.hitVec);
    }

    @Override
    public BlockPos renderingBlockPos() {
        return this.currentModelTarget.pos();
    }

    @Override
    public BlockPos hiddenBlockPos() {
        return this.initialModelTarget.pos();
    }

    @Override
    public BlockState renderingBlockState() {
        return this.currentModelTarget.state();
    }

    @Override
    public RenderType renderType() {
        if (IrisState.isShaderEnabled()) {
            return RenderType.translucent();
        }
        return ModRenderTypes.TRANSLUCENT_COLORED_OVERLAY;
    }

}
