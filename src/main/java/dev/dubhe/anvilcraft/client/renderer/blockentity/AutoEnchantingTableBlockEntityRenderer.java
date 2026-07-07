package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.AutoEnchantingTableBlockEntityRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.jspecify.annotations.Nullable;

public class AutoEnchantingTableBlockEntityRenderer
    implements BlockEntityRenderer<AutoEnchantingTableBlockEntity,
    AutoEnchantingTableBlockEntityRenderState> {
    private final SpriteGetter sprites;
    private final BookModel bookModel;

    public AutoEnchantingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public AutoEnchantingTableBlockEntityRenderState createRenderState() {
        return new AutoEnchantingTableBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
        AutoEnchantingTableBlockEntity blockEntity,
        AutoEnchantingTableBlockEntityRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.time = blockEntity.time + partialTicks;
        float or = blockEntity.rot - blockEntity.oldRot;

        while (or >= (float) Math.PI) {
            or -= (float) (Math.PI * 2);
        }

        while (or < (float) -Math.PI) {
            or += (float) (Math.PI * 2);
        }

        state.rotY = blockEntity.oldRot + or * partialTicks;

        final FluidStacksResourceHandler fluidHandler = blockEntity.getFluidHandler();
        FluidResource resource = fluidHandler.getResource(0);
        state.amount.setValue(fluidHandler.getAmountAsInt(0));
        state.fluid = resource.getFluid();
        state.fluidStack = resource.toStack(1);
        state.fluidResource = resource;
    }

    @Override
    public void submit(
        AutoEnchantingTableBlockEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.75F, 0.5F);
        float rotY = state.rotY;
        poseStack.mulPose(Axis.YP.rotation(-rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        poseStack.translate(0.0F, 0.1F + Mth.sin(state.time * 0.1F) * 0.01F, 0.0F);
        BookModel.State animationState = BookModel.State.forAnimation(state.time, 0, 0, 0);
        submitNodeCollector.submitModel(
            this.bookModel,
            animationState,
            poseStack,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            EnchantTableRenderer.BOOK_TEXTURE,
            this.sprites,
            0,
            state.breakProgress
        );
        poseStack.popPose();

        if (state.amount.getValue() > 0) {
            poseStack.pushPose();
            FluidModel model = FluidRenderHelper.getModel(
                Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
                state.fluid
            );
            var tintSource = model.fluidTintSource();
            if (tintSource != null) {
                TextureAtlasSprite sprite = model.stillMaterial().sprite();
                int tintColor = tintSource.colorAsStack(state.fluidStack);
                final int[] numbers = splitNumber(state.getAmount().getValue());
                AnvilCraft.LOGGER.debug("split: ");
                for (int i : numbers) {
                    AnvilCraft.LOGGER.debug("  {}", i);
                }
                Direction[] directions = { Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };
                for (int i = 0; i < numbers.length; i++) {
                    renderFluid(directions[i], numbers[i], submitNodeCollector, poseStack, sprite, state, tintColor);
                }
            }
            poseStack.popPose();
        }
    }

    private static void renderFluid(
        Direction face,
        int amount,
        SubmitNodeCollector submitNodeCollector,
        PoseStack poseStack,
        TextureAtlasSprite sprite,
        AutoEnchantingTableBlockEntityRenderState state,
        int tintColor
    ) {
        float y = 0.375f;
        if (amount > 0 && amount <= 2000) {
            y += 0.0625f;
        } else if (amount > 2000 && amount <= 4000) {
            y += 0.125f;
        } else if (amount > 4000 && amount <= 6000) {
            y += 0.1875f;
        } else if (amount > 6000 && amount <= 8000) {
            y += 0.25f;
        }
        float[] pos = new float[6];
        if (face == Direction.EAST || face == Direction.WEST) {
            pos[0] = 0;
            pos[1] = 0.375f;
            pos[2] = 0.4375f;
            pos[3] = 1;
            pos[4] = y;
            pos[5] = 0.5625f;
        } else if (face == Direction.SOUTH || face == Direction.NORTH) {
            pos[0] = 0.4375f;
            pos[1] = 0.375f;
            pos[2] = 0;
            pos[3] = 0.5625f;
            pos[4] = y;
            pos[5] = 1;
        }
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            BaseFluidHandlerHolderRenderer.FLUID_RENDER_TYPE,
            (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                face,
                sprite,
                state.fluidResource,
                pos[0], pos[1], pos[2],
                pos[3], pos[4], pos[5],
                tintColor,
                buffer,
                pose,
                state.lightCoords,
                false,
                false
            )
        );
    }

    private static int[] splitNumber(int total) {
        if (total <= 0) {
            return new int[0];
        }

        int full = total / 8000;
        int remainder = total % 8000;
        int length = remainder == 0 ? full : full + 1;
        int[] result = new int[length];

        for (int i = 0; i < full; i++) {
            result[i] = 8000;
        }
        if (remainder != 0) {
            result[full] = remainder;
        }
        return result;
    }
}
