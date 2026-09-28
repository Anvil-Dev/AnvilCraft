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
        boolean gas = resource.getFluidType().isLighterThanAir();
        float maxY = gas ? 1 - INSET : INSET + fill * (1 - 2 * INSET);
        FluidRenderHelper.submitFluidBox(resource, amount, INSET, INSET, INSET, 1 - INSET, maxY, 1 - INSET,
            gas ? fill : 1, pose, collector, light, type);
    }
}
