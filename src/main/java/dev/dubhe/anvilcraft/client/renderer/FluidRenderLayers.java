package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.TankFluidLayer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;

public final class FluidRenderLayers {
    private FluidRenderLayers() {
    }

    public record Layer(FluidResource resource, int amount, double bottom, double top, float opacity, boolean renderBottom) {
        static Layer of(FluidStack fluid, double bottom, double top, float opacity, boolean renderBottom) {
            return new Layer(FluidResource.of(fluid), fluid.getAmount(), bottom, top, opacity, renderBottom);
        }
    }

    public static List<FluidStack> sorted(List<FluidStack> contents) {
        return TankFluidLayer.sorted(contents);
    }

    public static List<Layer> create(List<FluidStack> contents, long capacity) {
        return TankFluidLayer.create(contents, capacity, Layer::of);
    }

    public static void submit(
        Layer layer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        PoseStack pose, SubmitNodeCollector collector, int light, RenderType translucent
    ) {
        float height = maxY - minY;
        FluidRenderHelper.submitFluidBox(layer.resource(), layer.amount(),
            minX, (float) (minY + layer.bottom() * height), minZ,
            maxX, (float) (minY + layer.top() * height), maxZ,
            layer.opacity(), pose, collector, light, translucent, layer.renderBottom(), false);
    }
}
