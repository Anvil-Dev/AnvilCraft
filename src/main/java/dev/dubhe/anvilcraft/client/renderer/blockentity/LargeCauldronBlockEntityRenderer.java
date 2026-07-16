package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class LargeCauldronBlockEntityRenderer implements BlockEntityRenderer<LargeCauldronBlockEntity> {
    private static final ModelResourceLocation FIRE =
        ModelResourceLocation.standalone(AnvilCraft.of("block/fire_cauldron_fire4"));
    private static final float WALL = 0.25F + 0.001F;
    private static final float MIN_XZ = -1.0F + WALL;
    private static final float MAX_XZ = 2.0F - WALL;
    private static final float MIN_Y = -0.5F + 0.001F;
    private static final float MAX_Y = 1.75F - 0.001F;
    private static final float CONTENT_HEIGHT = MAX_Y - MIN_Y;
    private static final float INPUT_CELL_SPACING = 0.68F;
    private static final float FIRE_MODEL_SURFACE_Y = 1.0F - (1.0F / 16.0F + 0.001F);
    private static final int[][] SLOT_OFFSETS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private final BlockRenderDispatcher dispatcher;

    public LargeCauldronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public AABB getRenderBoundingBox(LargeCauldronBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.0, 4.0, 1.0);
    }

    @Override
    public void render(
        LargeCauldronBlockEntity cauldron,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        if (!cauldron.isMainPart() || cauldron.getLevel() == null) return;
        LargeCauldronFluidHandler fluids = cauldron.getFluids();
        float fill = Mth.clamp(
            (float) fluids.getTotalAmount()
            / (LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.TANK_CAPACITY),
            0.0F,
            1.0F
        );
        float itemY = Mth.clamp(MIN_Y + CONTENT_HEIGHT * fill - 0.08F, MIN_Y + 0.06F, MAX_Y - 0.12F);
        this.drawItems(cauldron, pose, buffers, light, overlay, itemY, fill);
        this.drawFluids(fluids, pose, buffers, light);
        if (cauldron.isIgnited()) {
            this.drawFire(pose, buffers, overlay, MIN_Y + CONTENT_HEIGHT * fill);
        }
    }

    private void drawItems(
        LargeCauldronBlockEntity cauldron,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay,
        float itemY,
        float fill
    ) {
        float bob = fill > 0 ? Mth.sin(ClientTickRecorder.getTicks() / 12.0F) * 0.025F : 0.0F;
        IItemHandler inputs = cauldron.getInputHandler();
        for (int slot = 0; slot < inputs.getSlots(); slot++) {
            ItemStack stack = inputs.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            float x = SLOT_OFFSETS[slot][0] * INPUT_CELL_SPACING + 0.5F;
            float z = SLOT_OFFSETS[slot][1] * INPUT_CELL_SPACING + 0.5F;
            renderItem(cauldron, stack, pose, buffers, light, overlay, x, itemY + bob, z, slot * 37.0F);
        }

        IItemHandler outputs = cauldron.getOutputHandler();
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            ItemStack stack = outputs.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            float angle = slot * 2.3999631F;
            float radius = 0.08F + slot % 3 * 0.07F;
            float x = 0.5F + Mth.cos(angle) * radius;
            float z = 0.5F + Mth.sin(angle) * radius;
            renderItem(cauldron, stack, pose, buffers, light, overlay, x, itemY + bob - 0.08F, z, slot * 29.0F);
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
    }

    private static void renderItem(
        LargeCauldronBlockEntity cauldron,
        ItemStack stack,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay,
        float x,
        float y,
        float z,
        float rotation
    ) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.XP.rotationDegrees(65.0F));
        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.GROUND,
            light,
            overlay,
            pose,
            buffers,
            cauldron.getLevel(),
            (int) cauldron.getBlockPos().asLong()
        );
        pose.popPose();
    }

    private void drawFluids(
        LargeCauldronFluidHandler handler,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        List<FluidStack> layers = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty()) layers.add(fluid);
        }

        float minY = MIN_Y;
        float totalCapacity = LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.TANK_CAPACITY;
        for (FluidStack layer : layers) {
            float maxY = minY + CONTENT_HEIGHT * layer.getAmount() / totalCapacity;
            FluidRenderHelper.INSTANCE.renderFluidBox(
                layer,
                MIN_XZ,
                minY,
                MIN_XZ,
                MAX_XZ,
                maxY,
                MAX_XZ,
                buffers,
                pose,
                light,
                true,
                false
            );
            minY = maxY;
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
    }

    private void drawFire(PoseStack pose, MultiBufferSource buffers, int overlay, float surfaceY) {
        pose.pushPose();
        pose.translate(-1.0F, surfaceY - FIRE_MODEL_SURFACE_Y * 3.0F, -1.0F);
        pose.scale(3.0F, 3.0F, 3.0F);
        this.dispatcher.getModelRenderer().renderModel(
            pose.last(),
            buffers.getBuffer(RenderType.CUTOUT),
            null,
            this.dispatcher.getBlockModelShaper().getModelManager().getModel(FIRE),
            1.0F,
            1.0F,
            1.0F,
            LightTexture.FULL_BRIGHT,
            overlay,
            ModelData.EMPTY,
            RenderType.cutout()
        );
        pose.popPose();
    }
}
