package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.WipBlockRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class WipBlockEntityRenderer
    implements BlockEntityRenderer<WipBlockEntity, WipBlockRenderState>, ModelSelectionRenderer<WipBlockEntity> {

    private static final Map<Identifier, StandaloneModelKey<BlockStateModel>> MODEL_KEYS = new HashMap<>();

    public static final StandaloneModelKey<BlockStateModel> SPACETIME_SUPERCOMPUTER_WIP = WipBlockEntityRenderer.registerModel(
        "block/spacetime_supercomputer_wip"
    );
    public static final StandaloneModelKey<BlockStateModel> ANCIENT_DEBRIS_WIP = WipBlockEntityRenderer.registerModel(
        "block/ancient_debris_wip");
    public static final StandaloneModelKey<BlockStateModel> NETHERITE_BLOCK_WIP = WipBlockEntityRenderer.registerModel(
        "block/netherite_block_wip");
    public static final StandaloneModelKey<BlockStateModel> HEAVY_IRON_BLOCK_WIP = WipBlockEntityRenderer.registerModel(
        "block/heavy_iron_block_wip");
    public static final StandaloneModelKey<BlockStateModel> ANCIENT_SEA_REEF_WIP = WipBlockEntityRenderer.registerModel(
        "block/ancient_sea_reef_wip");
    public static final StandaloneModelKey<BlockStateModel> NESTING_SHULKER_BOX = WipBlockEntityRenderer.registerModel(
        "block/nesting_shulker_box"
    );
    public static final StandaloneModelKey<BlockStateModel> OVER_NESTING_SHULKER_BOX = WipBlockEntityRenderer.registerModel(
        "block/over_nesting_shulker_box"
    );
    public static final StandaloneModelKey<BlockStateModel> SUPERCRITICAL_NESTING_SHULKER_BOX = WipBlockEntityRenderer.registerModel(
        "block/supercritical_nesting_shulker_box"
    );
    public static final StandaloneModelKey<BlockStateModel> SPACETIME_SUPERCOMPUTER_WIP_2 = WipBlockEntityRenderer.registerModel(
        "block/spacetime_supercomputer_wip_2"
    );
    public static final StandaloneModelKey<BlockStateModel> SPACETIME_SUPERCOMPUTER_WIP_3 = WipBlockEntityRenderer.registerModel(
        "block/spacetime_supercomputer_wip_3"
    );
    public static final StandaloneModelKey<BlockStateModel> NETHERITE_BLOCK_WIP_2 = WipBlockEntityRenderer.registerModel(
        "block/netherite_block_wip_2"
    );
    public static final StandaloneModelKey<BlockStateModel> HEAVY_IRON_BLOCK_WIP_2 = WipBlockEntityRenderer.registerModel(
        "block/heavy_iron_block_wip_2"
    );
    public static final StandaloneModelKey<BlockStateModel> ANCIENT_SEA_REEF_WIP_2 = WipBlockEntityRenderer.registerModel(
        "block/ancient_sea_reef_wip_2"
    );
    public static final StandaloneModelKey<BlockStateModel> SHULKER_BOX_WIP = WipBlockEntityRenderer.registerModel(
        "block/shulker_box_wip"
    );
    public static final StandaloneModelKey<BlockStateModel> SHULKER_BOX_WIP_2 = WipBlockEntityRenderer.registerModel(
        "block/shulker_box_wip_2"
    );

    private static StandaloneModelKey<BlockStateModel> registerModel(String path) {
        Identifier id = AnvilCraft.of(path);
        StandaloneModelKey<BlockStateModel> key = new StandaloneModelKey<>(() -> "AnvilCraft: WIP " + path);
        WipBlockEntityRenderer.MODEL_KEYS.put(id, key);
        return key;
    }

    public static @Nullable StandaloneModelKey<BlockStateModel> getModelKey(Identifier id) {
        return WipBlockEntityRenderer.MODEL_KEYS.get(id);
    }

    public WipBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public WipBlockRenderState createRenderState() {
        return new WipBlockRenderState();
    }

    @Override
    public void extractRenderState(
        WipBlockEntity be,
        WipBlockRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        BlockStateModel model = this.getDisplayedModel(be, level, mc);
        BlockModelRenderState blockModelState = new BlockModelRenderState();
        if (model != null) {
            model.collectParts(
                level,
                be.getBlockPos(),
                be.getBlockState(),
                RandomSource.create(be.getInitialBlock().getSeed(be.getBlockPos())),
                blockModelState.setupModel(new Matrix4f(), false)
            );
        }
        state.setBlockModel(blockModelState);
    }

    private @Nullable BlockStateModel getDisplayedModel(WipBlockEntity be, Level level, Minecraft mc) {
        SelectionModel selected = this.getDisplayedSelection(be, level);
        if (selected instanceof SelectionModel.State state && state.state().isAir()) return null;
        return switch (selected) {
            case SelectionModel.Standalone standalone -> mc.getModelManager().getStandaloneModel(
                WipBlockEntityRenderer.getModelKeyForSelection(standalone));
            case SelectionModel.State state -> mc.getModelManager().getBlockStateModelSet().get(state.state());
        };
    }

    @SuppressWarnings("unchecked")
    private static StandaloneModelKey<BlockStateModel> getModelKeyForSelection(SelectionModel.Standalone model) {
        return (StandaloneModelKey<BlockStateModel>) model.key();
    }

    private SelectionModel getDisplayedSelection(WipBlockEntity be, Level level) {
        Identifier recipeId = be.getRecipeId();
        if (recipeId != null) {
            RecipeHolder<?> holder = RecipesRecord.getRecipes(level).byKey(ResourceKey.create(Registries.RECIPE, recipeId));
            if (holder != null && holder.value() instanceof ProceduralProcessRecipe recipe) {
                var modelKey = recipe.getDisplayedModelForStep(be.getStepCount()).map(WipBlockEntityRenderer::getModelKey).orElse(null);
                if (modelKey != null) return SelectionModel.standalone(modelKey);
            }
        }
        return new SelectionModel.State(be.getInitialBlock());
    }

    @Override
    public void collectSelectionModels(WipBlockEntity be, float partialTick, PoseStack pose, ModelConsumer consumer) {
        if (be.getLevel() != null) consumer.accept(this.getDisplayedSelection(be, be.getLevel()), pose);
    }

    @Override
    public void submit(
        WipBlockRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        pose.pushPose();
        state.getBlockModel().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
