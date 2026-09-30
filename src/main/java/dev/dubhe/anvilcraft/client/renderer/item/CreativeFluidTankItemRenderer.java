package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/// 在物品形态的流体储罐里额外渲染出内部流体
public class CreativeFluidTankItemRenderer extends BaseBlockItemRenderer<FluidTankItemRenderState> {
    public CreativeFluidTankItemRenderer() {
        super(ModBlocks.CREATIVE_FLUID_TANK.get().defaultBlockState(), 0.0F, 1.0F);
    }

    @Override
    public @Nullable FluidTankItemRenderState extractArgument(ItemStack stack) {
        if (!stack.is(ModBlocks.CREATIVE_FLUID_TANK.asItem())) return null;
        var client = Minecraft.getInstance();
        var registries = client.level != null ? client.level.registryAccess()
            : client.getConnection() != null ? client.getConnection().registryAccess() : null;
        var fluid = FluidTankItemTooltip.readCreativeTank(stack, registries);
        if (fluid.isEmpty()) return null;
        var state = new FluidTankItemRenderState();
        state.setResource(FluidResource.of(fluid));
        state.setAmount(fluid.getAmount());
        state.setFill(1);
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
        this.submitShell(poseStack, collector, lightCoords, overlayCoords, hasFoil, outlineColor);
        if (argument == null) return;
        FluidResource resource = argument.getResource();
        if (resource == null || resource.isEmpty()) return;
        FluidTankRenderUtil.submit(resource, argument.getAmount(), argument.getFill(), poseStack, collector,
            lightCoords, FluidTankItemRenderState.FLUID_RENDER_TYPE);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<FluidTankItemRenderState> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked.INSTANCE);

        @Override
        public CreativeFluidTankItemRenderer bake(BakingContext context) {
            return new CreativeFluidTankItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }
    }
}
