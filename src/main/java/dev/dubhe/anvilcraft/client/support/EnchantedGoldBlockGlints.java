package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Iterator;

public class EnchantedGoldBlockGlints {
    private static final EnchantedGoldBlockGlints INSTANCE = new EnchantedGoldBlockGlints();
    private static final float GLINT_TEXTURE_SCALE = 0.0078125F;

    private final RandomSource random = RandomSource.create();

    private EnchantedGoldBlockGlints() {
    }

    public static EnchantedGoldBlockGlints getInstance() {
        return INSTANCE;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        double renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
        double maxDistanceSqr = renderDistance * renderDistance;
        Iterator<BlockPos> iterator = EnchantedGoldBlockPositions.getPositions().iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (pos.distToCenterSqr(cameraPos) > maxDistanceSqr) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.ENCHANTED_GOLD_BLOCK)) {
                iterator.remove();
                continue;
            }
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x(), pos.getY() - cameraPos.y(), pos.getZ() - cameraPos.z());
            VertexConsumer glint = new SheetedDecalTextureGenerator(
                bufferSource.getBuffer(ModRenderTypes.ENCHANTED_GOLD_GLINT),
                poseStack.last(),
                GLINT_TEXTURE_SCALE
            );
            for (Direction direction : Direction.values()) {
                if (!Block.shouldRenderFace(state, level, pos, direction, pos.relative(direction))) {
                    continue;
                }
                random.setSeed(42L);
                for (BakedQuad quad : model.getQuads(state, direction, random, ModelData.EMPTY, RenderType.solid())) {
                    glint.putBulkData(
                        poseStack.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                    );
                }
            }
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, RenderType.solid())) {
                glint.putBulkData(
                    poseStack.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                );
            }
            poseStack.popPose();
        }
    }
}
