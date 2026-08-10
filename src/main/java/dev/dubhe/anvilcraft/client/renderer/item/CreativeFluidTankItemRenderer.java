package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class CreativeFluidTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String TAG_INFINITY_FLUID = "infinityFluid";
    private static final String TAG_FLUID = "Fluid";

    @Nullable
    private static CreativeFluidTankItemRenderer instance;

    private CreativeFluidTankItemRenderer(
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        EntityModelSet entityModelSet
    ) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    public static CreativeFluidTankItemRenderer getInstance() {
        if (instance == null) {
            Minecraft minecraft = Minecraft.getInstance();
            instance = new CreativeFluidTankItemRenderer(
                minecraft.getBlockEntityRenderDispatcher(),
                minecraft.getEntityModels()
            );
        }
        return instance;
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (!stack.is(ModBlocks.CREATIVE_FLUID_TANK.asItem())) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
        renderModel(itemRenderer, stack, poseStack, buffer, packedLight, packedOverlay, model);

        FluidStack fluid = readFluid(stack);
        if (fluid.isEmpty()) return;
        // 创造流体储罐内流体恒为无限，显示为满罐
        FluidTankRenderUtil.drawFluidInTank(poseStack, buffer, packedLight, fluid, 1.0f);
    }

    private static FluidStack readFluid(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) return FluidStack.EMPTY;
        CompoundTag infinityFluid = data.copyTag().getCompound(TAG_INFINITY_FLUID);
        if (!infinityFluid.contains(TAG_FLUID, CompoundTag.TAG_COMPOUND)) return FluidStack.EMPTY;

        Minecraft minecraft = Minecraft.getInstance();
        HolderLookup.Provider registries;
        if (minecraft.level != null) {
            registries = minecraft.level.registryAccess();
        } else if (minecraft.getConnection() != null) {
            registries = minecraft.getConnection().registryAccess();
        } else {
            return FluidStack.EMPTY;
        }
        return FluidStack.parseOptional(registries, infinityFluid.getCompound(TAG_FLUID));
    }

    private static void renderModel(
        ItemRenderer itemRenderer,
        ItemStack stack,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay,
        BakedModel model
    ) {
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                VertexConsumer vertices = ItemRenderer.getFoilBuffer(
                    buffer,
                    renderType,
                    true,
                    stack.hasFoil()
                );
                itemRenderer.renderModelLists(
                    pass,
                    stack,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    vertices
                );
            }
        }
    }
}
