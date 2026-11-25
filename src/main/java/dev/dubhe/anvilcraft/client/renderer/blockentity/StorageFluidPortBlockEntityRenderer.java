package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 仓储流体端口渲染器：在中央玻璃窗口内渲染内部流体，液体按液面高度、气体按不透明度表示余量。
 *
 * <p>端口被标记后，还会在玻璃窗上方渲染一枚零厚度的扁平流体图标，取空后依然保留。
 * 图标与 26.1.2 用同一套画法：按面方向画贴图四边形，贴图与配色取自流体自身。</p>
 */
public class StorageFluidPortBlockEntityRenderer implements BlockEntityRenderer<StorageFluidPortBlockEntity> {
    /** 中央玻璃窗口是 2..14，比储罐更小，故在默认内缩基础上再收 2.0 像素 */
    public static final float WINDOW_INSET_PIXELS = 2.0f;
    /** 玻璃窗内缩：外框 1 像素 + 窗口再收 2 像素 */
    private static final float WINDOW_EDGE = 3.0f / 16.0f;
    /** 标记图标的半宽（格），1.8 像素见方 */
    private static final float MARK_HALF = 0.9f / 16.0f;
    /** 标记图标相对方块面的距离（格），1.9 像素 */
    private static final float MARK_DEPTH = 1.9f / 16.0f;
    /** 图标在玻璃窗口上方的额外抬升（格），0.1 像素 */
    private static final float MARK_RISE = 0.1f / 16.0f;
    /** 图标中心在面内的偏移：落在玻璃窗口上方 */
    private static final float MARK_CENTER = 0.5f + WINDOW_EDGE + MARK_HALF + MARK_RISE;

    public StorageFluidPortBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        StorageFluidPortBlockEntity port,
        float tickDelta,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int light,
        int overlay
    ) {
        FluidStack fluid = port.getFluid();
        if (!fluid.isEmpty()) {
            float fill = Mth.clamp((float) fluid.getAmount() / StorageFluidPortBlockEntity.CAPACITY_MB, 0, 1);
            FluidTankRenderUtil.drawFluidInTank(
                poseStack, bufferSource, light, fluid, fill, WINDOW_INSET_PIXELS
            );
        }
        FluidStack marked = port.getMarkedFluid();
        if (marked.isEmpty()) {
            return;
        }
        // 罐里没有流体但端口认这一种：每个面都画一枚扁平流体图标
        for (Direction direction : Direction.values()) {
            StorageFluidPortBlockEntityRenderer.renderMarkedIcon(
                poseStack, bufferSource, light, marked, direction
            );
        }
    }

    /**
     * 在指定面画一枚贴面的扁平流体图标，位置在玻璃窗口上方。
     *
     * <p>零厚度四边形，贴面放置，颜色与贴图取自流体本身，与窗口里的流体观感一致。</p>
     */
    private static void renderMarkedIcon(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int light,
        FluidStack fluid,
        Direction direction
    ) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ext.getStillTexture(fluid));
        int color = ext.getTintColor(fluid);
        // 流体自发光：取方块光与原版流体亮度的较大者
        int blockLight = Math.max((light >> 4) & 0xF, fluid.getFluidType().getLightLevel());
        int iconLight = (light & 0xF00000) | blockLight << 4;
        boolean opaque = (ext instanceof ModClientFluidTypeExtensionImpl impl && impl.isOpaque())
            || fluid.is(NeoForgeMod.MILK.value());
        VertexConsumer consumer = bufferSource.getBuffer(opaque ? RenderType.cutout() : RenderType.translucent());
        float half = MARK_HALF;
        float left = 0.5f - half;
        float right = 0.5f + half;
        // 水平面上图标落在窗口上方；上下两面沿 -Z 偏移，保证同一位置看过去不会压在窗口上
        float center = direction.getAxis().isHorizontal() ? MARK_CENTER : 1 - MARK_CENTER;
        float down = center - half;
        float up = center + half;
        boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float depth = positive ? 1 - MARK_DEPTH : MARK_DEPTH;
        FluidRenderHelper.renderStillTiledFace(
            direction, left, down, right, up, depth, consumer, poseStack, iconLight, color, sprite
        );
    }
}