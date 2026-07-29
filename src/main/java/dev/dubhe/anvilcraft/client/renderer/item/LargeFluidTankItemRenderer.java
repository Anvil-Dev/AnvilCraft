package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// 在物品形态的大型储罐里按高度分层渲染多种流体
public class LargeFluidTankItemRenderer extends BaseFluidTankItemRenderer {
    /// 大型储罐是 3x3x3 多方块，物品模型按整体缩放，壁厚也按整体计
    private static final float TANK_W = 4 / 16F + 0.001F;

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
        List<FluidStack> fluids = new ArrayList<>(FluidTankItemTooltip.readMultiTankFluids(stack));
        if (fluids.isEmpty()) return null;
        fluids.sort(Comparator
            .comparingInt(FluidStack::getAmount)
            .reversed()
            .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()));

        long totalAmount = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        long renderAmount = FluidTankItemTooltip.isMultiTankEnhanced(stack)
            ? Math.max(totalAmount, LargeFluidTankBlockEntity.INFINITY_THRESHOLD)
            : LargeFluidTankBlockEntity.BASE_CAPACITY;

        List<FluidTankItemRenderState.Layer> layers = new ArrayList<>();
        double bottom = 0;
        for (FluidStack fluid : fluids) {
            if (bottom >= 1) break;
            double top = Math.min(1, bottom + (double) fluid.getAmount() / renderAmount);
            layers.add(new FluidTankItemRenderState.Layer(
                FluidResource.of(fluid),
                (float) bottom,
                (float) top
            ));
            bottom = top;
        }
        if (layers.isEmpty()) return null;

        FluidTankItemRenderState state = new FluidTankItemRenderState();
        state.setLayers(List.copyOf(layers));
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
        float height = 3 - 2 * TANK_W;
        for (FluidTankItemRenderState.Layer layer : argument.getLayers()) {
            FluidResource resource = layer.resource();
            FluidModel model = FluidRenderHelper.getModel(
                Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
                resource.getFluid()
            );
            var tintSource = model.fluidTintSource();
            int tintColor = tintSource != null ? tintSource.colorAsStack(resource.toStack(1)) : -1;
            TextureAtlasSprite sprite = model.stillMaterial().sprite();
            float minY = TANK_W - 1 + layer.bottom() * height;
            float maxY = TANK_W - 1 + layer.top() * height;
            collector.submitCustomGeometry(
                poseStack,
                FluidTankItemRenderState.FLUID_RENDER_TYPE,
                (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                    sprite,
                    resource,
                    TANK_W - 1,
                    minY,
                    TANK_W - 1,
                    2 - TANK_W,
                    maxY,
                    2 - TANK_W,
                    tintColor,
                    buffer,
                    pose,
                    lightCoords,
                    true,
                    false
                )
            );
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
