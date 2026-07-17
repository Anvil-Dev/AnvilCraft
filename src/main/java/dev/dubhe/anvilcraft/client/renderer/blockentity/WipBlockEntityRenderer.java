package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.WipBlockRenderState;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WipBlockEntityRenderer implements BlockEntityRenderer<WipBlockEntity, WipBlockRenderState> {

    private static final Map<Identifier, StandaloneModelKey<BlockStateModel>> MODEL_KEYS = new HashMap<>();

    public static final StandaloneModelKey<BlockStateModel> SPACETIME_SUPERCOMPUTER_WIP = registerModel(
        "block/spacetime_supercomputer_wip"
    );
    public static final StandaloneModelKey<BlockStateModel> ANCIENT_DEBRIS_WIP = registerModel("block/ancient_debris_wip");
    public static final StandaloneModelKey<BlockStateModel> NETHERITE_BLOCK_WIP = registerModel("block/netherite_block_wip");
    public static final StandaloneModelKey<BlockStateModel> HEAVY_IRON_BLOCK_WIP = registerModel("block/heavy_iron_block_wip");
    public static final StandaloneModelKey<BlockStateModel> ANCIENT_SEA_REEF_WIP = registerModel("block/ancient_sea_reef_wip");
    public static final StandaloneModelKey<BlockStateModel> NESTING_SHULKER_BOX = registerModel(
        "block/nesting_shulker_box"
    );
    public static final StandaloneModelKey<BlockStateModel> OVER_NESTING_SHULKER_BOX = registerModel(
        "block/over_nesting_shulker_box"
    );
    public static final StandaloneModelKey<BlockStateModel> SUPERCRITICAL_NESTING_SHULKER_BOX = registerModel(
        "block/supercritical_nesting_shulker_box"
    );

    private static StandaloneModelKey<BlockStateModel> registerModel(String path) {
        Identifier id = AnvilCraft.of(path);
        StandaloneModelKey<BlockStateModel> key = new StandaloneModelKey<>(() -> "AnvilCraft: WIP " + path);
        MODEL_KEYS.put(id, key);
        return key;
    }

    public static @Nullable StandaloneModelKey<BlockStateModel> getModelKey(Identifier id) {
        return MODEL_KEYS.get(id);
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
        Level level = be.getLevel();
        if (level == null) return;

        BlockStateModel model = this.getDisplayedModel(be, level, mc);
        BlockModelRenderState blockModelState = new BlockModelRenderState();
        if (model != null) {
            model.collectParts(
                mc.level,
                be.getBlockPos(),
                be.getBlockState(),
                RandomSource.create(be.getInitialBlock().getSeed(be.getBlockPos())),
                blockModelState.setupModel(new Matrix4f(), false)
            );
        }
        state.setBlockModel(blockModelState);
    }

    private @Nullable BlockStateModel getDisplayedModel(WipBlockEntity be, Level level, Minecraft mc) {
        // Try to get standalone model from recipe's displayedModel field
        Optional<Identifier> displayedModelId = Optional.ofNullable(be.getRecipeId())
            .map(recipeId -> {
                ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
                RecipeHolder<?> holder = RecipesRecord.getRecipes(level).byKey(key);
                if (holder != null && holder.value() instanceof ProceduralProcessRecipe ppr) {
                    return ppr.getDisplayedModelForStep(be.getStepCount()).orElse(null);
                }
                return null;
            });
        if (displayedModelId.isPresent()) {
            StandaloneModelKey<BlockStateModel> modelKey = getModelKey(displayedModelId.get());
            if (modelKey != null) {
                BlockStateModel standaloneModel = mc.getModelManager().getStandaloneModel(modelKey);
                if (standaloneModel != null) {
                    return standaloneModel;
                }
            }
        }
        // Fallback: render the initial block's model
        BlockState initialState = be.getInitialBlock();
        if (initialState != null && !initialState.isAir()) {
            return mc.getModelManager().getBlockStateModelSet().get(initialState);
        }
        return null;
    }

    @Override
    public void submit(
        WipBlockRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (state.getBlockModel() == null) return;
        pose.pushPose();
        state.getBlockModel().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
