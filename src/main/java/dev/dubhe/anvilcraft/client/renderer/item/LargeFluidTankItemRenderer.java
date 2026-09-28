package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.renderer.LargeFluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/// 在物品形态的大型储罐里按高度分层渲染多种流体
public class LargeFluidTankItemRenderer extends BaseFluidTankItemRenderer {
    public LargeFluidTankItemRenderer() {
        super(
            ModBlocks.LARGE_FLUID_TANK.get().defaultBlockState()
                .setValue(LargeFluidTankBlock.HALF, Cube3x3PartHalf.MID_CENTER),
            -1.0F,
            2.0F
        );
    }

    @Override
    public @Nullable FluidTankItemRenderState extractArgument(ItemStack stack) {
        if (!stack.is(ModBlocks.LARGE_FLUID_TANK.asItem())) return null;
        var client = Minecraft.getInstance();
        var registries = client.level != null ? client.level.registryAccess()
            : client.getConnection() != null ? client.getConnection().registryAccess() : null;
        if (registries == null) return null;
        var layers = LargeFluidTankRenderUtil.layers(
            FluidTankItemTooltip.readMultiTankFluids(stack, registries), FluidTankItemTooltip.isMultiTankEnhanced(stack));
        if (layers.isEmpty()) return null;

        FluidTankItemRenderState state = new FluidTankItemRenderState();
        state.setLayers(layers);
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
        for (var layer : argument.getLayers()) {
            LargeFluidTankRenderUtil.submit(layer, false, poseStack, collector, lightCoords, FluidTankItemRenderState.FLUID_RENDER_TYPE);
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<FluidTankItemRenderState> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked.INSTANCE);

        @Override
        public LargeFluidTankItemRenderer bake(BakingContext context) {
            return new LargeFluidTankItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }
    }
}
