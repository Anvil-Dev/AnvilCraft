package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LargeCauldronRenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LargeCauldronRenderState.FluidLayerRenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LargeCauldronRenderState.ItemRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class LargeCauldronBlockEntityRenderer
    implements BlockEntityRenderer<LargeCauldronBlockEntity, LargeCauldronRenderState> {
    public static final StandaloneModelKey<BlockStateModel> FIRE = new StandaloneModelKey<>(
        () -> "AnvilCraft: Large Cauldron Fire Model"
    );
    private static final float WALL = 0.25F + 0.001F;
    private static final float MIN_XZ = -1.0F + LargeCauldronBlockEntityRenderer.WALL;
    private static final float MAX_XZ = 2.0F - LargeCauldronBlockEntityRenderer.WALL;
    private static final float MIN_Y = -0.5F + 0.001F;
    private static final float MAX_Y = 1.75F - 0.001F;
    private static final float CONTENT_HEIGHT = LargeCauldronBlockEntityRenderer.MAX_Y - LargeCauldronBlockEntityRenderer.MIN_Y;
    private static final float INPUT_CELL_SPACING = 0.68F;
    private static final float FIRE_MODEL_SURFACE_Y = 1.0F - (1.0F / 16.0F + 0.001F);
    private static final int[][] SLOT_OFFSETS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private final ItemModelResolver itemModelResolver;

    public LargeCauldronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public LargeCauldronRenderState createRenderState() {
        return new LargeCauldronRenderState();
    }

    @Override
    public void extractRenderState(
        LargeCauldronBlockEntity cauldron,
        LargeCauldronRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(cauldron, state, partialTicks, cameraPosition, breakProgress);
        state.getItems().clear();
        state.getFluids().clear();
        state.setFire(null);
        state.setFill(0.0F);
        if (!cauldron.isMainPart() || cauldron.getLevel() == null) return;

        LargeCauldronFluidHandler fluids = cauldron.getFluids();
        float fill = Mth.clamp(
            (float) fluids.getTotalAmount() / LargeCauldronFluidHandler.TOTAL_CAPACITY,
            0.0F,
            1.0F
        );
        state.setFill(fill);
        float itemY = Mth.clamp(
            LargeCauldronBlockEntityRenderer.MIN_Y + LargeCauldronBlockEntityRenderer.CONTENT_HEIGHT * fill - 0.08F,
            LargeCauldronBlockEntityRenderer.MIN_Y + 0.06F, LargeCauldronBlockEntityRenderer.MAX_Y - 0.12F
        );
        float bob = fill > 0 ? Mth.sin(ClientTickRecorder.getTicks() / 12.0F) * 0.025F : 0.0F;
        this.extractItems(cauldron, state, itemY + bob);
        for (int layer = 0; layer < fluids.size(); layer++) {
            FluidResource resource = fluids.getResource(layer);
            int amount = fluids.getAmountAsInt(layer);
            if (!resource.isEmpty() && amount > 0) {
                state.getFluids().add(new FluidLayerRenderState(resource, amount));
            }
        }
        if (cauldron.isIgnited()) {
            state.setFire(FeatureRendererSupport.initialize(LargeCauldronBlockEntityRenderer.FIRE, cauldron));
        }
    }

    private void extractItems(LargeCauldronBlockEntity cauldron, LargeCauldronRenderState state, float itemY) {
        ResourceHandler<ItemResource> inputs = cauldron.getInputHandler();
        for (int slot = 0; slot < inputs.size(); slot++) {
            ItemStack stack = LargeCauldronBlockEntityRenderer.toStack(inputs, slot);
            if (stack.isEmpty()) continue;
            float x = LargeCauldronBlockEntityRenderer.SLOT_OFFSETS[slot][0] * LargeCauldronBlockEntityRenderer.INPUT_CELL_SPACING + 0.5F;
            float z = LargeCauldronBlockEntityRenderer.SLOT_OFFSETS[slot][1] * LargeCauldronBlockEntityRenderer.INPUT_CELL_SPACING + 0.5F;
            state.getItems().add(this.createItemState(stack, x, itemY, z, slot * 37.0F));
        }

        ResourceHandler<ItemResource> outputs = cauldron.getOutputHandler();
        for (int slot = 0; slot < outputs.size(); slot++) {
            ItemStack stack = LargeCauldronBlockEntityRenderer.toStack(outputs, slot);
            if (stack.isEmpty()) continue;
            float angle = slot * 2.3999631F;
            float radius = 0.08F + slot % 3 * 0.07F;
            float x = 0.5F + Mth.cos(angle) * radius;
            float z = 0.5F + Mth.sin(angle) * radius;
            state.getItems().add(this.createItemState(stack, x, itemY - 0.08F, z, slot * 29.0F));
        }
    }

    private ItemRenderState createItemState(ItemStack stack, float x, float y, float z, float rotation) {
        ItemClusterRenderState item = FeatureRendererSupport.initialize(stack, this.itemModelResolver);
        return new ItemRenderState(item, x, y, z, rotation);
    }

    private static ItemStack toStack(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }

    @Override
    public void submit(
        LargeCauldronRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        for (ItemRenderState item : state.getItems()) {
            poseStack.pushPose();
            poseStack.translate(item.x(), item.y(), item.z());
            poseStack.mulPose(Axis.YP.rotationDegrees(item.rotation()));
            poseStack.mulPose(Axis.XP.rotationDegrees(65.0F));
            item.item().item.submit(
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
            );
            poseStack.popPose();
        }
        this.submitFluids(state, poseStack, submitNodeCollector);

        BlockModelRenderState fire = state.getFire();
        if (fire == null) return;
        poseStack.pushPose();
        float surfaceY = LargeCauldronBlockEntityRenderer.MIN_Y + LargeCauldronBlockEntityRenderer.CONTENT_HEIGHT * state.getFill();
        poseStack.translate(-1.0F, surfaceY - LargeCauldronBlockEntityRenderer.FIRE_MODEL_SURFACE_Y * 3.0F, -1.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        fire.submit(
            poseStack,
            submitNodeCollector,
            LightCoordsUtil.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0
        );
        poseStack.popPose();
    }

    private void submitFluids(
        LargeCauldronRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector
    ) {
        float minY = LargeCauldronBlockEntityRenderer.MIN_Y;
        for (FluidLayerRenderState layer : state.getFluids()) {
            float maxY = minY + LargeCauldronBlockEntityRenderer.CONTENT_HEIGHT * layer.amount() / LargeCauldronFluidHandler.TOTAL_CAPACITY;
            FluidResource resource = layer.resource();
            FluidModel model = FluidRenderHelper.getModel(
                Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
                resource.getFluid()
            );
            var tintSource = model.fluidTintSource();
            int tintColor = tintSource == null ? -1 : tintSource.colorAsStack(resource.toStack(1));
            TextureAtlasSprite sprite = model.stillMaterial().sprite();
            float layerMinY = minY;
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                BaseFluidHandlerHolderRenderer.FLUID_RENDER_TYPE,
                (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                    sprite,
                    resource,
                    LargeCauldronBlockEntityRenderer.MIN_XZ,
                    layerMinY,
                    LargeCauldronBlockEntityRenderer.MIN_XZ,
                    LargeCauldronBlockEntityRenderer.MAX_XZ,
                    maxY,
                    LargeCauldronBlockEntityRenderer.MAX_XZ,
                    tintColor,
                    buffer,
                    pose,
                    state.lightCoords,
                    true,
                    false
                )
            );
            minY = maxY;
        }
    }

    @Override
    public AABB getRenderBoundingBox(LargeCauldronBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0, 4.0, 1.0);
    }
}
