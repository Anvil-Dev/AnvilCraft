package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class ExpCollectorBlockEntityRenderer implements BlockEntityRenderer<ExpCollectorBlockEntity> {
    public ExpCollectorBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        ExpCollectorBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        IFluidHandler fluidHandler = blockEntity.getFluidHandler();
        FluidStack fluidStack = fluidHandler.getFluidInTank(0);
        if (fluidStack.isEmpty()) {
            return;
        }
        IClientFluidTypeExtensions renderProps = IClientFluidTypeExtensions.of(ModFluids.EXP_FLUID.get());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture());

        float fillRatio = (float) fluidStack.getAmount() / fluidHandler.getTankCapacity(0);
        float fill = 0.15f + 0.6f * fillRatio;

        // fill的取值范围是0.15f到0.75f
        FluidTankRenderUtil.renderFluidCube(
            poseStack,
            bufferSource,
            packedLight,
            sprite,
            renderProps.getTintColor(),
            0.07f, 0.15f, 0.07f,
            0.93f, fill, 0.93f
        );
    }
}
