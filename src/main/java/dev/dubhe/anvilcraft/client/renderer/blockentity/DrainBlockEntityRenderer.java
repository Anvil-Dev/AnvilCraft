package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 排水口渲染器：像储罐一样渲染内部流体；向下排水时，从排水口下方一直渲染一条<b>流动水柱</b>
 * 到当前排水目标层（{@link DrainBlockEntity#getColumnBottomY()}），仅为视觉效果、非真实方块，
 * 避免流动水扩散与无限水。
 */
public class DrainBlockEntityRenderer implements BlockEntityRenderer<DrainBlockEntity> {
    /** 水柱横截面相对方块内缩（像素/16） */
    private static final float COLUMN_INSET = 5.0f / 16.0f;

    @SuppressWarnings("unused")
    public DrainBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        DrainBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay
    ) {
        FluidStack fluid = be.getTank().getFluid();
        // 1) 内部流体（像储罐）
        if (!fluid.isEmpty()) {
            float fill = Mth.clamp((float) fluid.getAmount() / be.getTank().getCapacity(), 0, 1);
            FluidTankRenderUtil.drawFluidInTank(ms, buffer, light, fluid, fill);
        }
        // 2) 向下排水柱（仅渲染）：从排水口底面渲染流动水到目标层
        int bottomY = be.getColumnBottomY();
        if (bottomY != Integer.MIN_VALUE && !fluid.isEmpty()) {
            renderColumn(be, fluid, bottomY, ms, buffer, light);
        }
    }

    /**
     * 在排水口正下方渲染一条竖直流动水柱。方块局部坐标以排水口自身为 (0..1)，
     * 水柱从 y=0（排水口底面）向下延伸到目标层底部（相对本方块的 y 偏移）。
     */
    private void renderColumn(
        DrainBlockEntity be, FluidStack fluid, int bottomY, PoseStack ms, MultiBufferSource buffer, int light
    ) {
        int drainY = be.getBlockPos().getY();
        // 目标层底部相对本方块的 y（本方块底面为 0，向下为负）
        float minY = (bottomY - drainY);      // 例如下方 3 格 → -3
        float maxY = 0.0f;                     // 排水口底面
        if (minY >= maxY) {
            return;
        }
        // 侧面用流动贴图（向下流动感），顶面用静止贴图
        net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions ext =
            net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
        net.minecraft.client.renderer.texture.TextureAtlasSprite flowing =
            net.minecraft.client.Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getFlowingTexture(fluid));
        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            COLUMN_INSET, minY, COLUMN_INSET,
            1 - COLUMN_INSET, maxY, 1 - COLUMN_INSET,
            buffer, ms, light,
            false /*renderBottom*/, false,
            flowing
        );
    }

    /** 扩大渲染包围盒以包含向下的水柱，否则水柱在方块本体 AABB 之外会被视锥剔除、看不见。 */
    @Override
    public AABB getRenderBoundingBox(DrainBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        AABB self = new AABB(pos);
        int bottomY = be.getColumnBottomY();
        if (bottomY == Integer.MIN_VALUE || bottomY >= pos.getY()) {
            return self;
        }
        // 从方块本体一直向下扩展到水柱底部
        return self.expandTowards(0, bottomY - pos.getY(), 0);
    }

    /** 长水柱可能横跨多个渲染区段，不能只按排水口本体所在区段做视锥剔除。 */
    @Override
    public boolean shouldRenderOffScreen(DrainBlockEntity be) {
        return true;
    }

    /** 按摄像机到整根水柱的最近距离判断，而不是只计算到排水口方块中心的距离。 */
    @Override
    public boolean shouldRender(DrainBlockEntity be, Vec3 cameraPos) {
        double viewDistance = getViewDistance();
        return getRenderBoundingBox(be).distanceToSqr(cameraPos) <= viewDistance * viewDistance;
    }
}
