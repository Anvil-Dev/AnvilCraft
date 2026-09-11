package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.blockentity.StorageFluidPortBlockEntityRenderer;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

/**
 * 仓储流体端口物品渲染器：先画方块模型，再在其玻璃窗口内画内部流体，与流体储罐物品一致。
 *
 * <p>窗口内缩沿用 {@link StorageFluidPortBlockEntityRenderer#WINDOW_INSET_PIXELS}，
 * 保证物品栏 / 手中看到的水位与放置在世界中时一致。</p>
 */
public class StorageFluidPortItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_FLUID = "Fluid";

    @Nullable
    private static StorageFluidPortItemRenderer instance;

    private StorageFluidPortItemRenderer(
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        EntityModelSet entityModelSet
    ) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    public static StorageFluidPortItemRenderer getInstance() {
        if (instance == null) {
            Minecraft minecraft = Minecraft.getInstance();
            instance = new StorageFluidPortItemRenderer(
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
        if (!stack.is(ModBlocks.STORAGE_FLUID_PORT.asItem())) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
        renderModel(itemRenderer, stack, poseStack, buffer, packedLight, packedOverlay, model);

        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || blockEntityData.isEmpty()) {
            return;
        }
        CompoundTag tankTag = blockEntityData.copyTag().getCompound(TAG_TANK);
        if (!tankTag.contains(TAG_FLUID, CompoundTag.TAG_COMPOUND)) {
            return;
        }

        HolderLookup.Provider registries = registries();
        if (registries == null) {
            return;
        }
        FluidStack fluid = FluidStack.parseOptional(registries, tankTag.getCompound(TAG_FLUID));
        if (fluid.isEmpty()) {
            return;
        }
        float fill = Mth.clamp(
            (float) fluid.getAmount() / StorageFluidPortBlockEntity.CAPACITY_MB,
            0.0F,
            1.0F
        );
        FluidTankRenderUtil.drawFluidInTank(
            poseStack, buffer, packedLight, fluid, fill,
            StorageFluidPortBlockEntityRenderer.WINDOW_INSET_PIXELS
        );
    }

    @Nullable
    private static HolderLookup.Provider registries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return null;
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
