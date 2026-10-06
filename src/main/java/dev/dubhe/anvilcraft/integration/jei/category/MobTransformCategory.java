package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.MobTransformJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobTransformCategory implements IRecipeCategory<MobTransformJeiRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 82;

    private static final long RESULT_CYCLE_MILLIS = 1500L;
    private static final String INPUT_ITEM_SLOT = "input_item";
    private static final int INPUT_ITEM_X = 53;
    private static final int OUTPUT_ITEM_X = 91;
    private static final int HELD_ITEM_Y = 10;
    private static final int INPUT_ENTITY_X = 27;
    private static final int OUTPUT_ENTITY_X = 135;
    private static final float BEACON_BEAM_BASE_Y = 48.0f;
    private static final float BEACON_BEAM_LENGTH = 4.8f;
    private static final Quaternionf ENTITY_ANGLE = new Quaternionf().rotationXYZ(0.43633232f, 0.0f, (float) Math.PI);
    private static final EntityPreviewSettings DEFAULT_PREVIEW_SETTINGS = new EntityPreviewSettings(1.0f, 0, 0);
    private static final Map<EntityType<?>, EntityPreviewSettings> ENTITY_PREVIEW_SETTINGS = Map.of(
        EntityType.PHANTOM, new EntityPreviewSettings(0.55f, 0, 0),
        EntityType.ELDER_GUARDIAN, new EntityPreviewSettings(1.0f, 0, 16),
        EntityType.GIANT, new EntityPreviewSettings(3.0f, 13, -7),
        EntityType.ALLAY, new EntityPreviewSettings(1.4f, -6, 0),
        EntityType.VEX, new EntityPreviewSettings(1.4f, -6, 0),
        EntityType.IRON_GOLEM, new EntityPreviewSettings(1.35f, 0, 0),
        EntityType.WARDEN, new EntityPreviewSettings(1.4f, 0, 0)
    );

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable arrowDefault;
    private final Component title;
    private final Map<EntityType<?>, LivingEntity> entityCache = new HashMap<>();
    private @Nullable Level cachedLevel;

    public MobTransformCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.mob_transform");
    }

    @Override
    public IRecipeType<MobTransformJeiRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobTransformJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputItems().size(); i++) {
            ItemIngredientPredicate ingredient = recipe.inputItems().get(i);
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_ITEM_X + i * 19, HELD_ITEM_Y)
                .setSlotName(i == 0 ? INPUT_ITEM_SLOT : INPUT_ITEM_SLOT + "_" + i);
            for (var item : ingredient.getItems()) slot.add(item.withCount(ingredient.count()));
        }
        if (!recipe.outputItem().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_ITEM_X, HELD_ITEM_Y)
                .add(recipe.outputItem());
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION)
            .add(ModBlocks.CORRUPTED_BEACON.asStack());
    }

    @Override
    public void draw(
        MobTransformJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {
        BlockState beacon = ModBlocks.CORRUPTED_BEACON
            .get()
            .defaultBlockState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);
        RenderSupport.renderBlockAt(guiGraphics, beacon, 81, 48, 12);
        renderBeaconBeam(guiGraphics);

        this.arrowDefault.draw(guiGraphics, 54, 31);
        this.arrowDefault.draw(guiGraphics, 92, 31);
        for (int i = 0; i < recipe.inputItems().size(); i++) {
            this.slotDefault.draw(guiGraphics, INPUT_ITEM_X - 1 + i * 19, HELD_ITEM_Y - 1);
        }
        if (!recipe.outputItem().isEmpty()) {
            this.slotDefault.draw(guiGraphics, OUTPUT_ITEM_X - 1, HELD_ITEM_Y - 1);
        }

        ItemStack inputHeldItem = recipeSlotsView.findSlotByName(INPUT_ITEM_SLOT)
            .flatMap(IRecipeSlotView::getDisplayedItemStack)
            .orElse(ItemStack.EMPTY);
        this.renderEntity(guiGraphics, recipe.input(), INPUT_ENTITY_X, 61, 1, inputHeldItem);

        TransformResult displayedResult = getDisplayedResult(recipe);
        this.renderEntity(
            guiGraphics,
            displayedResult.resultEntityType(),
            OUTPUT_ENTITY_X,
            61,
            -1,
            recipe.outputItem()
        );

        Component chanceText = getChanceText(recipe, displayedResult);
        if (chanceText != null) {
            Minecraft minecraft = Minecraft.getInstance();
            int textX = (WIDTH - minecraft.font.width(chanceText)) / 2;
            guiGraphics.text(minecraft.font, chanceText, textX, 71, 0xFF555555, false);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        MobTransformJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (mouseX >= 0 && mouseX <= 42 && mouseY >= 8 && mouseY <= 66) {
            tooltip.add(recipe.input().getDescription());
        } else if (mouseX >= 120 && mouseX <= WIDTH && mouseY >= 8 && mouseY <= 66) {
            TransformResult result = getDisplayedResult(recipe);
            tooltip.add(result.resultEntityType().getDescription());
            Component chanceText = getChanceText(recipe, result);
            if (chanceText != null) tooltip.add(chanceText);
        }
    }

    @Override
    public @Nullable Identifier getIdentifier(MobTransformJeiRecipe recipe) {
        return recipe.id();
    }

    private void renderEntity(
        GuiGraphicsExtractor guiGraphics,
        EntityType<?> entityType,
        int x,
        int y,
        int inwardDirection,
        ItemStack heldItem
    ) {
        LivingEntity entity = this.getEntity(entityType);
        if (entity == null) return;
        entity.setItemInHand(InteractionHand.MAIN_HAND, heldItem.copy());
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        var renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        EntityPreviewSettings settings = ENTITY_PREVIEW_SETTINGS.getOrDefault(entityType, DEFAULT_PREVIEW_SETTINGS);
        final Vector2f center = guiGraphics.pose().transformPosition(
            x + settings.inwardOffset() * inwardDirection, y + settings.yoffset(), new Vector2f());
        final float guiScale = (float) Math.hypot(guiGraphics.pose().m00(), guiGraphics.pose().m01());
        float maxSize = Math.max(entity.getBbWidth(), entity.getBbHeight());
        int baseScale = Mth.clamp((int) (30.0f / maxSize), 2, 24);
        int scale = Mth.clamp(Math.round(baseScale * settings.scaleMultiplier()), 2, 32);
        int extent = Mth.ceil(Math.max(48, maxSize * scale + 8) * guiScale);
        guiGraphics.entity(renderState, scale * guiScale, new Vector3f(), ENTITY_ANGLE, null,
            Mth.floor(center.x) - extent, Mth.floor(center.y) - extent,
            Mth.floor(center.x) + extent, Mth.floor(center.y) + extent);
    }

    private static void renderBeaconBeam(GuiGraphicsExtractor guiGraphics) {
        PoseStack pose = new PoseStack();
        pose.translate(0, (20 - BEACON_BEAM_BASE_Y) / 12, 0);
        GuiRenderExtras.submitStructure(guiGraphics, BlockAndTintGetter.EMPTY, BlockPos.ZERO, BlockPos.ZERO,
            70, -12, 92, 52, 12, false, false, pose,
            (collector, matrices) -> {
                matrices.pushPose();
                matrices.translate(0.5, 0.5, 0.5);
                collector.submitCustomGeometry(matrices, ModRenderTypes.CORRUPTED_BEACON_BEAM,
                    (last, vertices) -> CorruptedBeaconRenderer.renderBeam(
                        vertices, last.pose(), BEACON_BEAM_LENGTH, 0.65F));
                matrices.popPose();
            });
    }

    private @Nullable LivingEntity getEntity(EntityType<?> entityType) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) return null;
        if (this.cachedLevel != level) {
            this.entityCache.clear();
            this.cachedLevel = level;
        }
        return this.entityCache.computeIfAbsent(entityType, type -> {
            Entity entity = type.create(level, EntitySpawnReason.LOAD);
            if (!(entity instanceof LivingEntity livingEntity)) return null;
            livingEntity.setYRot(205.0f);
            livingEntity.yBodyRot = 205.0f;
            livingEntity.yHeadRot = 205.0f;
            return livingEntity;
        });
    }

    private static TransformResult getDisplayedResult(MobTransformJeiRecipe recipe) {
        List<TransformResult> results = recipe.results();
        int index = (int) ((Util.getMillis() / RESULT_CYCLE_MILLIS) % results.size());
        return results.get(index);
    }

    private static @Nullable Component getChanceText(
        MobTransformJeiRecipe recipe,
        TransformResult result
    ) {
        if (recipe.hasHeldItem()) {
            return Component.translatable(
                "gui.anvilcraft.category.mob_transform.chance_per_item",
                recipe.chancePercentPerItem()
            );
        }
        if (recipe.results().size() > 1 || result.probability() != 1.0) {
            return Component.translatable(
                "gui.anvilcraft.category.chance",
                Math.round(result.probability() * 100.0)
            );
        }
        return null;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<MobTransformJeiRecipe> recipes = new ArrayList<>();
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM.get())
            .stream()
            .map(MobTransformJeiRecipe::ofStandard)
            .forEach(recipes::add);
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM.get())
            .stream()
            .map(MobTransformJeiRecipe::ofWithItem)
            .forEach(recipes::add);
        registration.addRecipes(AnvilCraftJeiPlugin.MOB_TRANSFORM, recipes);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MOB_TRANSFORM, ModBlocks.CORRUPTED_BEACON);
    }

    private record EntityPreviewSettings(float scaleMultiplier, int yoffset, int inwardOffset) {
    }
}
