package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Optional;

public class WipBlockEntityRenderer implements BlockEntityRenderer<WipBlockEntity>, ModelSelectionRenderer<WipBlockEntity> {

    public WipBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        WipBlockEntity wipBlockEntity,
        float v,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = wipBlockEntity.getLevel();
        if (level == null) return;
        poseStack.pushPose();
        BlockRenderDispatcher blockRenderDispatcher = minecraft.getBlockRenderer();
        BlockState state = wipBlockEntity.getInitialBlock();
        BakedModel bakedModel = minecraft.getModelManager().getModel(this.model(wipBlockEntity, level));
        RandomSource rand = RandomSource.create(state.getSeed(wipBlockEntity.getBlockPos()));
        ChunkRenderTypeSet types = bakedModel.getRenderTypes(
            state,
            rand,
            ModelData.EMPTY
        );
        for (RenderType renderType : types.asList()) {
            // 走 tesselateBlock 而非 renderModel：前者按方块所在位置逐面采样光照与方向明暗，
            // 后者只能整块共用一个 packedLight，模型会平掉、失去立体感。
            // checkSides 取 false，与原版 FallingBlockRenderer 渲染整体方块模型的做法一致。
            blockRenderDispatcher.getModelRenderer().tesselateBlock(
                level,
                bakedModel,
                state,
                wipBlockEntity.getBlockPos(),
                poseStack,
                buffer.getBuffer(renderType),
                false,
                RandomSource.create(),
                state.getSeed(wipBlockEntity.getBlockPos()),
                packedOverlay,
                ModelData.EMPTY,
                renderType
            );
        }
        poseStack.popPose();
    }

    @Override
    public void collectSelectionModels(WipBlockEntity entity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        Level level = entity.getLevel();
        if (level != null) consumer.accept(this.model(entity, level), pose);
    }

    private ModelResourceLocation model(WipBlockEntity entity, Level level) {
        return Optional.ofNullable(entity.getRecipeId())
            .flatMap(recipeID -> level.getRecipeManager().byKey(recipeID))
            .map(RecipeHolder::value)
            .filter(ProceduralProcessRecipe.class::isInstance)
            .map(ProceduralProcessRecipe.class::cast)
            .flatMap(recipe -> recipe.getDisplayedModelForStep(entity.getStepCount()))
            .map(ModelResourceLocation::standalone)
            .orElseGet(() -> BlockModelShaper.stateToModelLocation(entity.getInitialBlock()));
    }
}
