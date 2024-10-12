package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingSpectralBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class SpectralBlockRenderer extends EntityRenderer<FallingSpectralBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    /**
     * 上升方块渲染器
     */
    public SpectralBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            FallingSpectralBlockEntity entity,
            float entityYaw,
            float partialTicks,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight
    ) {
        BlockState blockState = entity.getBlockState();
        if (blockState.getRenderShape() == RenderShape.MODEL) {
            Level level = entity.level();
            if (blockState != level.getBlockState(entity.blockPosition())
                    && blockState.getRenderShape() != RenderShape.INVISIBLE
            ) {
                poseStack.pushPose();
                BlockPos blockPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
                poseStack.translate(-0.5, -1.0, -0.5);
                this.dispatcher.getModelRenderer()
                        .tesselateBlock(
                                level,
                                this.dispatcher.getBlockModel(blockState),
                                blockState,
                                blockPos,
                                poseStack,
                                buffer.getBuffer(RenderType.translucent()),
                                false,
                                RandomSource.create(),
                                blockState.getSeed(entity.getStartPos()),
                                OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            }
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FallingSpectralBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}