package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.rendering.IAnimatedPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * 支持平滑位移渲染的 BlockEntityRenderer 基类。
 * <p>
 * 与 {@link AnimatedPositionBlockEntity} 配合使用，
 * 自动将方块模型渲染在插值后的偏移位置，BlockState 本身不变。
 * </p>
 *
 * <pre>{@code
 * // 注册示例：
 * @Override
 * public void blockEntityRenderers(BindingsRegistry registry) {
 *     registry.register(
 *         ModBlockEntities.MY_BLOCK.get(),
 *         MyRenderer::new
 *     );
 * }
 *
 * // 渲染器实现：
 * public class MyRenderer extends AnimatedPositionRenderer<MyBlockEntity> {
 *     public MyRenderer(BlockEntityRendererProvider.Context ctx) { super(ctx); }
 * }
 * }</pre>
 *
 * @param <T> 实现了 {@link IAnimatedPosition} 的 BlockEntity 类型
 */
public abstract class AnimatedPositionRenderer<T extends BlockEntity & IAnimatedPosition>
    implements BlockEntityRenderer<T> {

    protected final BlockRenderDispatcher blockRenderer;

    public AnimatedPositionRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
        T blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        // ---- 获取插值后的位移 ----
        float dx = blockEntity.getRenderOffsetX(partialTick);
        float dy = blockEntity.getRenderOffsetY(partialTick);
        float dz = blockEntity.getRenderOffsetZ(partialTick);

        // ---- 如果没有位移，走默认渲染（避免无意义的 push/pop） ----
        //     这里仍然 push，因为子类可能还有其他变换
        poseStack.pushPose();

        // ---- 应用位移 ----
        if (dx != 0.0f || dy != 0.0f || dz != 0.0f) {
            poseStack.translate(dx, dy, dz);
        }

        // ---- 渲染方块的默认模型 ----
        renderBlockModel(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // ---- 子类扩展点：额外渲染（粒子、光束、自定义模型等） ----
        renderExtra(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * 渲染方块自身的模型。
     * <p>
     * 默认使用 Minecraft 的 BlockRenderDispatcher 渲染该位置的 BlockState。
     * 子类可 override 以渲染自定义模型或替换渲染方式。
     * </p>
     */
    protected void renderBlockModel(
        T blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            blockEntity.getBlockState(),
            poseStack,
            bufferSource,
            packedLight,
            packedOverlay,
            net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
            null
        );
    }

    /**
     * 额外渲染扩展点。
     * <p>
     * 在方块模型渲染之后调用，位移已应用，所有坐标都是相对于偏移位置的。
     * 默认不做任何额外渲染。
     * </p>
     */
    protected void renderExtra(
        T blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        // 子类按需 override
    }

    // ======================================================================
    //  渲染边界（重要：必须扩大以包含位移范围）
    // ======================================================================

    /**
     * 获取渲染边界。
     * <p>
     * 默认：以方块位置为中心，向每个方向扩展 1 格（因为位移通常不超过 1 格）。
     * 如果位移可能超过 1 格，子类应 override 此方法返回更大的 AABB。
     * </p>
     */
    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        // 获取当前位置到目标的距离，判断最大可能的偏移范围
        float maxOffset = Math.max(
            Math.abs(blockEntity.getOffsetX()) + 0.5f,
            Math.max(
                Math.abs(blockEntity.getOffsetY()) + 0.5f,
                Math.abs(blockEntity.getOffsetZ()) + 0.5f
            )
        );
        maxOffset = Math.max(maxOffset, 1.0f); // 至少 1 格
        return new AABB(blockEntity.getBlockPos()).inflate(maxOffset);
    }
}
