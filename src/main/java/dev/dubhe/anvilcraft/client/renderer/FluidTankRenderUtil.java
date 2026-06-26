package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

@UtilityClass
public class FluidTankRenderUtil {
    public static final float TANK_W = 1 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(PoseStack ps, MultiBufferSource mbs, int light, FluidStack fluid, float fill) {
        float height = 1 - 2 * TANK_W;

        float minY = TANK_W;
        float maxY = 1 - TANK_W;

        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            minY = maxY - fill * height;
        } else {
            maxY = minY + fill * height;
        }

        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            TANK_W, minY, TANK_W,
            1 - TANK_W, maxY, 1 - TANK_W,
            mbs, ps, light,
            true, false
        );
    }
}
