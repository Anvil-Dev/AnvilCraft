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
        FluidTankRenderUtil.drawFluidInTank(ps, mbs, light, fluid, fill, 0.0f);
    }

    /**
     * 在方块内绘制流体。
     *
     * @param insetPixels 在默认内缩基础上额外向内收缩的像素数，用于适配更小的玻璃窗口
     */
    public static void drawFluidInTank(
        PoseStack ps,
        MultiBufferSource mbs,
        int light,
        FluidStack fluid,
        float fill,
        float insetPixels
    ) {
        float inset = FluidTankRenderUtil.TANK_W + insetPixels / 16f;
        float height = 1 - 2 * inset;

        float maxY = 1 - inset;

        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            // Gas always fills the whole tank; the amount is conveyed by opacity.
            FluidRenderHelper.INSTANCE.renderFluidBox(
                fluid,
                inset, inset, inset,
                1 - inset, maxY, 1 - inset,
                mbs, ps, light,
                true, fill
            );
            return;
        }

        maxY = inset + fill * height;

        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            inset, inset, inset,
            1 - inset, maxY, 1 - inset,
            mbs, ps, light,
            true, false
        );
    }
}
