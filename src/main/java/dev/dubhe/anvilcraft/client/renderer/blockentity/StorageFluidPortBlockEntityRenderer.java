package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class StorageFluidPortBlockEntityRenderer
    extends BaseFluidHandlerHolderRenderer<StorageFluidPortBlockEntity, FluidHandlerRenderState> {
    /// 玻璃窗口相对面中心的高度，取自窗口半宽 3 像素
    private static final float WINDOW_EDGE = 3 / 16F;
    /// 标记图标半宽（像素折算），比窗口窄一圈
    private static final float ICON_HALF = 0.9F / 16F;
    /// 标记图标离面的距离：贴面往内收 1.9 像素，避免与凹槽边沿打架
    private static final float ICON_DEPTH = 1.9F / 16F;
    /// 图标在玻璃窗口上方的额外抬升
    private static final float ICON_RISE = 0.1F / 16F;
    /// 图标中心在面内的偏移：落在窗口上方
    private static final float ICON_CENTER = 0.5F + StorageFluidPortBlockEntityRenderer.WINDOW_EDGE
        + StorageFluidPortBlockEntityRenderer.ICON_HALF + StorageFluidPortBlockEntityRenderer.ICON_RISE;

    public StorageFluidPortBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public FluidHandlerRenderState createRenderState() {
        return new FluidHandlerRenderState();
    }

    @Override
    protected void updateTankW(
        StorageFluidPortBlockEntity port, FluidHandlerRenderState state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(3 / 16F + 0.001F);
    }

    @Override
    public void extractRenderState(
        StorageFluidPortBlockEntity port, FluidHandlerRenderState state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        super.extractRenderState(port, state, partialTick, cameraPosition, breakProgress);
        FluidStack stored = port.getFluid();
        // 标记与罐内内容彼此独立：罐里有流体时也要画出标记图标，与 1.21.1 一致
        FluidStack marked = port.getMarkedFluid();
        state.setMarkedResource(marked.isEmpty() ? null : FluidResource.of(marked));
        state.setFill(Mth.clamp((float) stored.getAmount() / StorageFluidPortBlockEntity.CAPACITY_MB, 0, 1));
    }

    @Override
    public void submit(FluidHandlerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var fluid = state.getResource();
        if (fluid != null && state.getFill() > 0) {
            FluidTankRenderUtil.submit(
                fluid, state.getAmount(), state.getFill(), 2, pose, collector, state.lightCoords, FLUID_RENDER_TYPE);
        }
        var marked = state.getMarkedResource();
        if (marked != null) {
            // 端口认的流体：无论罐里有没有流体，每个面都画一枚扁平图标
            for (Direction direction : Direction.values()) {
                StorageFluidPortBlockEntityRenderer.submitMarkedIcon(
                    marked, state.getAmount(), direction, pose, collector, state.lightCoords);
            }
        }
    }

    /// 在指定面画一枚贴面的扁平流体图标，位置在玻璃窗口上方。
    ///
    /// <p>零厚度四边形，贴面放置，颜色与纹理取自流体本身，与储罐窗口里的流体观感一致。</p>
    private static void submitMarkedIcon(
        FluidResource fluid,
        int amount,
        Direction direction,
        PoseStack pose,
        SubmitNodeCollector collector,
        int light
    ) {
        var model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(), fluid.getFluid());
        var tint = model.fluidTintSource();
        int color = tint == null ? -1 : tint.colorAsStack(fluid.toStack(Math.max(1, amount)));
        var sprite = model.stillMaterial().sprite();
        RenderType type = FluidRenderHelper.renderType(fluid, FLUID_RENDER_TYPE);
        float half = StorageFluidPortBlockEntityRenderer.ICON_HALF;
        float left = 0.5F - half;
        float right = 0.5F + half;
        // 水平面上图标落在窗口上方；上下两面沿 -Z 偏移，保证同一位置看过去不会压在窗口上
        float center = direction.getAxis().isHorizontal()
            ? StorageFluidPortBlockEntityRenderer.ICON_CENTER
            : 1 - StorageFluidPortBlockEntityRenderer.ICON_CENTER;
        float down = center - half;
        float up = center + half;
        boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float depth = positive ? 1 - StorageFluidPortBlockEntityRenderer.ICON_DEPTH
            : StorageFluidPortBlockEntityRenderer.ICON_DEPTH;
        collector.submitCustomGeometry(pose, type, (submittedPose, output) ->
            FluidRenderHelper.renderStillTiledFace(
                direction, left, down, right, up, depth, output, submittedPose, light, color, sprite));
    }
}
