package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class FluidTankRenderUtil {
    public static final float INSET = 1 / 16F + 0.001F;

    private FluidTankRenderUtil() {
    }

    public static void submit(
        FluidResource resource, int amount, float fill, PoseStack pose, SubmitNodeCollector collector, int light, RenderType type
    ) {
        submit(resource, amount, fill, 0, pose, collector, light, type);
    }

    public static void submit(
        FluidResource resource, int amount, float fill, float insetPixels,
        PoseStack pose, SubmitNodeCollector collector, int light, RenderType type
    ) {
        float inset = INSET + insetPixels / 16F;
        boolean gas = resource.getFluidType().isLighterThanAir();
        float maxY = gas ? 1 - inset : inset + fill * (1 - 2 * inset);
        FluidRenderHelper.submitFluidBox(resource, amount, inset, inset, inset, 1 - inset, maxY, 1 - inset,
            gas ? fill : 1, pose, collector, light, type);
    }
}
