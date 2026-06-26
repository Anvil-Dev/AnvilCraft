package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@UtilityClass
public class FluidTankRenderUtil {
    public static final float TANK_W = 1 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(PoseStack ps, MultiBufferSource mbs, int light, FluidStack fluid, float fill) {
        // From Modern Industrialization
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));

        int color = renderProps.getTintColor(fluid);

        // Top and bottom positions of the fluid inside the tank
        float height = 1 - 2 * TANK_W;

        float minY = TANK_W;
        float maxX = 1 - TANK_W;
        float maxY = 1 - TANK_W;
        float maxZ = 1 - TANK_W;

        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            minY = maxY - fill * height;
        } else {
            maxY = minY + fill * height;
        }

        renderFluidCube(ps, mbs, light, sprite, color, TANK_W, minY, TANK_W, maxX, maxY, maxZ, fluid);
    }

    public static void renderFluidCube(
        PoseStack ps,
        MultiBufferSource mbs,
        int light,
        TextureAtlasSprite sprite,
        int color,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        @Nullable FluidStack fluid
    ) {
        boolean opaque = false;
        if (fluid != null) {
            var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
            if (renderProps instanceof ModClientFluidTypeExtensionImpl ext) {
                opaque = ext.isOpaque();
            }
        }
        RenderType renderType = opaque ? RenderType.cutout() : RenderType.translucent();
        VertexConsumer consumer = mbs.getBuffer(renderType);
        Matrix4f pose = ps.last().pose();
        LargeFluidTankBlockEntityRenderer.RenderManager renderManager =
            new LargeFluidTankBlockEntityRenderer.RenderManager(consumer, light, sprite, pose, color, minX, minY, minZ, maxX, maxY, maxZ);
        renderManager.render();
    }
}
