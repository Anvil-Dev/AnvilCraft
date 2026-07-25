package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/// 在物品形态的流体储罐里额外渲染出内部流体
public class FluidTankItemRenderer extends BaseFluidTankItemRenderer {
    /// 与方块实体渲染保持一致的壁厚，避免与外壳 Z-fighting
    private static final float TANK_W = 1 / 16F + 0.001F;

    public FluidTankItemRenderer() {
        super(ModBlocks.FLUID_TANK.get().defaultBlockState(), 0.0F, 1.0F);
    }

    @Override
    public @Nullable FluidTankItemRenderState extractArgument(ItemStack stack) {
        if (!stack.is(ModBlocks.FLUID_TANK.asItem())) return null;
        FluidTankItemTooltip.SingleTankData data = FluidTankItemTooltip.readSingleTank(stack);
        if (data == null || data.fluid().isEmpty()) return null;
        int capacity = data.enhanced()
            ? FluidTankBlockEntity.INFINITY_THRESHOLD
            : FluidTankBlockEntity.BASE_CAPACITY;
        FluidTankItemRenderState state = new FluidTankItemRenderState();
        state.setResource(FluidResource.of(data.fluid()));
        state.setFill(Math.max(0.025F, Mth.clamp((float) data.fluid().getAmount() / capacity, 0.0F, 1.0F)));
        return state;
    }

    @Override
    public void submit(
        @Nullable FluidTankItemRenderState argument,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int lightCoords,
        int overlayCoords,
        boolean hasFoil,
        int outlineColor
    ) {
        this.submitShell(poseStack, collector, lightCoords, overlayCoords, outlineColor);
        if (argument == null) return;
        FluidResource resource = argument.getResource();
        if (resource == null || resource.isEmpty()) return;
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        int tintColor = tintSource != null ? tintSource.colorAsStack(resource.toStack(1)) : -1;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        float maxY = TANK_W + (1 - 2 * TANK_W) * argument.getFill();
        collector.submitCustomGeometry(
            poseStack,
            FluidTankItemRenderState.FLUID_RENDER_TYPE,
            (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                sprite,
                resource,
                TANK_W,
                TANK_W,
                TANK_W,
                1 - TANK_W,
                maxY,
                1 - TANK_W,
                tintColor,
                buffer,
                pose,
                lightCoords,
                true,
                false
            )
        );
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<FluidTankItemRenderState> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked.INSTANCE);

        @Override
        public FluidTankItemRenderer bake(BakingContext context) {
            return new FluidTankItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }
    }
}
