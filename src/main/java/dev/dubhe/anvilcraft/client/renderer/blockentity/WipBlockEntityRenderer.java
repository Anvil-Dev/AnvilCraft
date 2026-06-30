package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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

public class WipBlockEntityRenderer implements BlockEntityRenderer<WipBlockEntity> {

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
        BakedModel bakedModel = Optional.ofNullable(wipBlockEntity.getRecipeId())
            .flatMap(recipeID -> level.getRecipeManager().byKey(recipeID))
            .map(RecipeHolder::value)
            .filter(ProceduralProcessRecipe.class::isInstance)
            .map(ProceduralProcessRecipe.class::cast)
            .flatMap(ProceduralProcessRecipe::getDisplayedModel)
            .map(ModelResourceLocation::standalone)
            .map(mrl -> minecraft.getModelManager().getModel(mrl))
            .orElse(blockRenderDispatcher.getBlockModel(state));
        RandomSource rand = RandomSource.create(state.getSeed(wipBlockEntity.getBlockPos()));
        ChunkRenderTypeSet types = bakedModel.getRenderTypes(
            state,
            rand,
            ModelData.EMPTY
        );
        for (RenderType renderType : types.asList()) {
            blockRenderDispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(renderType),
                state,
                bakedModel,
                0,
                0,
                0,
                packedLight,
                packedOverlay
            );
        }
        poseStack.popPose();
    }
}
