package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LargeFluidTankRenderUtil {
    public static final float INSET = 4 / 16F + 0.001F;

    private LargeFluidTankRenderUtil() {
    }

    public record Layer(FluidResource resource, int amount, double bottom, double top) {
    }

    public static List<Layer> layers(List<FluidStack> contents, boolean enhanced) {
        List<FluidStack> fluids = contents.stream().filter(fluid -> !fluid.isEmpty())
            .sorted(Comparator.comparingInt(FluidStack::getAmount).reversed()
                .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()))
            .toList();
        long total = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        long capacity = enhanced ? Math.max(total, LargeFluidTankBlockEntity.INFINITY_THRESHOLD)
            : LargeFluidTankBlockEntity.BASE_CAPACITY;
        List<Layer> layers = new ArrayList<>();
        double bottom = 0;
        for (FluidStack fluid : fluids) {
            if (bottom >= 1) break;
            double top = Math.min(1, bottom + (double) fluid.getAmount() / capacity);
            layers.add(new Layer(FluidResource.of(fluid), fluid.getAmount(), bottom, top));
            bottom = top;
        }
        return List.copyOf(layers);
    }

    public static void submit(
        Layer layer, boolean expandGas, PoseStack pose, SubmitNodeCollector collector, int light, RenderType translucent
    ) {
        FluidResource resource = layer.resource();
        var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(), resource.getFluid());
        var tint = model.fluidTintSource();
        int color = tint == null ? -1 : tint.colorAsStack(resource.toStack(layer.amount()));
        var sprite = model.stillMaterial().sprite();
        float height = 3 - 2 * INSET;
        boolean gas = expandGas && resource.getFluidType().isLighterThanAir();
        float minY = gas ? INSET - 1 : (float) (INSET - 1 + layer.bottom() * height);
        float maxY = gas ? 2 - INSET : (float) (INSET - 1 + layer.top() * height);
        float opacity = gas ? (float) (layer.top() - layer.bottom()) : 1;
        RenderType type = resource.is(NeoForgeMod.MILK.get()) ? RenderTypes.cutoutMovingBlock() : translucent;
        collector.submitCustomGeometry(pose, type, (submittedPose, output) -> FluidRenderHelper.INSTANCE.renderFluidBox(
            sprite, resource, INSET - 1, minY, INSET - 1, 2 - INSET, maxY, 2 - INSET,
            color, output, submittedPose, light, true, false, opacity));
    }
}
