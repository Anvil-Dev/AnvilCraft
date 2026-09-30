package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FluidRenderLayers {
    private FluidRenderLayers() {
    }

    public record Layer(FluidResource resource, int amount, double bottom, double top, float opacity, boolean renderBottom) {
    }

    public static List<FluidStack> sorted(List<FluidStack> contents) {
        return contents.stream().filter(fluid -> !fluid.isEmpty())
            .sorted(Comparator.comparingInt(FluidStack::getAmount).reversed()
                .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()))
            .toList();
    }

    public static List<Layer> create(List<FluidStack> contents, long capacity) {
        if (capacity <= 0) return List.of();
        List<Layer> layers = new ArrayList<>();
        List<FluidStack> gases = new ArrayList<>();
        double bottom = 0;
        for (FluidStack fluid : contents) {
            if (fluid.isEmpty()) continue;
            if (fluid.getFluidType().isLighterThanAir()) {
                gases.add(fluid);
            } else if (bottom < 1) {
                double top = Math.min(1, bottom + (double) fluid.getAmount() / capacity);
                layers.add(new Layer(FluidResource.of(fluid), fluid.getAmount(), bottom, top, 1, true));
                bottom = top;
            }
        }
        if (!gases.isEmpty() && bottom < 1) {
            double height = (1 - bottom) / gases.size();
            boolean renderBottom = bottom <= 0;
            for (FluidStack gas : gases) {
                double top = Math.min(1, bottom + height);
                layers.add(new Layer(FluidResource.of(gas), gas.getAmount(), bottom, top,
                    (float) ((double) gas.getAmount() / capacity), renderBottom));
                bottom = top;
                renderBottom = false;
            }
        }
        return List.copyOf(layers);
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
