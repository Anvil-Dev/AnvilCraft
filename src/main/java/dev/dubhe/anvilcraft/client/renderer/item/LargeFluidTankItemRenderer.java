package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LargeFluidTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_FLUIDS = "Fluids";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_ENHANCED = "Enhanced";

    private static LargeFluidTankItemRenderer instance;

    private LargeFluidTankItemRenderer(
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        EntityModelSet entityModelSet
    ) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    public static LargeFluidTankItemRenderer getInstance() {
        if (instance == null) {
            Minecraft minecraft = Minecraft.getInstance();
            instance = new LargeFluidTankItemRenderer(
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
        if (!stack.is(ModBlocks.LARGE_FLUID_TANK.asItem())) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
        renderModel(itemRenderer, stack, poseStack, buffer, packedLight, packedOverlay, model);

        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || blockEntityData.isEmpty()) return;
        CompoundTag tankTag = blockEntityData.copyTag().getCompound(TAG_TANK);
        if (!tankTag.contains(TAG_FLUIDS, Tag.TAG_LIST)) return;

        Minecraft minecraft = Minecraft.getInstance();
        HolderLookup.Provider registries;
        if (minecraft.level != null) {
            registries = minecraft.level.registryAccess();
        } else if (minecraft.getConnection() != null) {
            registries = minecraft.getConnection().registryAccess();
        } else {
            return;
        }

        boolean enhanced = tankTag.getBoolean(TAG_ENHANCED);
        List<FluidStack> fluids = new ArrayList<>();
        ListTag fluidsTag = tankTag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < fluidsTag.size(); i++) {
            CompoundTag fluidTag = fluidsTag.getCompound(i);
            FluidStack fluid = FluidStack.parseOptional(registries, fluidTag.getCompound(TAG_FLUID));
            if (!fluid.isEmpty()) {
                fluids.add(fluid);
            }
        }
        if (fluids.isEmpty()) return;

        fluids.sort(Comparator
            .comparingInt(FluidStack::getAmount)
            .reversed()
            .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()));

        long totalAmount = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        long renderAmount = enhanced
            ? Math.max(totalAmount, LargeFluidTankBlockEntity.INFINITY_THRESHOLD)
            : LargeFluidTankBlockEntity.BASE_CAPACITY;

        float tankW = 4 / 16f + 0.001f;
        float height = 3 - 2 * tankW;
        double layerBottom = 0;
        for (FluidStack fluid : fluids) {
            if (layerBottom >= 1) break;
            double layerTop = Math.min(1, layerBottom + (double) fluid.getAmount() / renderAmount);
            float minX = tankW - 1;
            float minY = (float) (tankW - 1 + layerBottom * height);
            float minZ = tankW - 1;
            float maxX = 2 - tankW;
            float maxY = (float) (tankW - 1 + layerTop * height);
            float maxZ = 2 - tankW;
            FluidRenderHelper.INSTANCE.renderFluidBox(
                fluid,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                buffer, poseStack, packedLight,
                true, false
            );
            layerBottom = layerTop;
        }
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
