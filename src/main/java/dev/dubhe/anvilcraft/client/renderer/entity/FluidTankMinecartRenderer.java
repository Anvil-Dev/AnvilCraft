package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidTankMinecartRenderer extends MinecartRenderer<FluidTankMinecartEntity> {
    public FluidTankMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }

    @Override
    protected void renderMinecartContents(
        FluidTankMinecartEntity entity,
        float partialTicks,
        BlockState state,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        super.renderMinecartContents(entity, partialTicks, state, poseStack, buffer, packedLight);
        FluidStack fluid = entity.getSyncedFluid();
        if (fluid.isEmpty()) return;
        float fill = Mth.clamp((float) fluid.getAmount() / entity.getCapacity(), 0.0F, 1.0F);
        FluidTankRenderUtil.drawFluidInTank(poseStack, buffer, packedLight, fluid, fill);
    }
}
