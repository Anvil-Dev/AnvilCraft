package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.HyperdimensionStorageStationUpgradeRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class HyperdimensionStorageStationUpgradeCategory implements IRecipeCategory<HyperdimensionStorageStationUpgradeRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    public static final int HYPERCUBE_COUNT = 16;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable arrowIn;
    private final IDrawable arrowOutputFromBelow;
    private final ITickTimer timer;
    private final Component title;
    private final Component dropOnTopTooltip;
    private final Component strikeTooltip;
    private final Component requiresExpansion;

    public HyperdimensionStorageStationUpgradeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.HYPERDIMENSION_STORAGE_STATION.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutputFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.title = Component.translatable("gui.anvilcraft.category.hyperdimension_storage_station_upgrade");
        this.dropOnTopTooltip =
            Component.translatable("gui.anvilcraft.category.hyperdimension_storage_station_upgrade.drop_on_top")
                .withStyle(ChatFormatting.GOLD);
        this.strikeTooltip =
            Component.translatable("gui.anvilcraft.category.hyperdimension_storage_station_upgrade.strike")
                .withStyle(ChatFormatting.GOLD);
        this.requiresExpansion =
            Component.translatable("gui.anvilcraft.category.hyperdimension_storage_station_upgrade.requires_expansion")
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public RecipeType<HyperdimensionStorageStationUpgradeRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.HYPERDIMENSION_STORAGE_STATION_UPGRADE;
    }

    @Override
    public Component getTitle() {
        return title;
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
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HyperdimensionStorageStationUpgradeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 9, 8)
            .addItemStack(ModBlocks.SINGULARITY_CRYSTAL.asStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(dropOnTopTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, 31, 8)
            .addItemStack(new ItemStack(ModBlocks.HYPERCUBE, HYPERCUBE_COUNT))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(dropOnTopTooltip));
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.INPUT,
            "container",
            69,
            32,
            25,
            25,
            ModBlocks.SHULKER_CONTAINER.get()
        );
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 8)
            .addItemStack(ModBlocks.HYPERDIMENSION_STORAGE_STATION.asStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(strikeTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, 31, 41)
            .addItemStack(ModBlocks.SPACE_OVERCOMPRESSOR.asStack(4))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(requiresExpansion));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, HyperdimensionStorageStationUpgradeRecipe recipe, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        HyperdimensionStorageStationUpgradeRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.SHULKER_CONTAINER.getDefaultState().setValue(ShulkerContainerBlock.HALF, OpenedCube3x3PartHalf.MID_CENTER),
            81,
            44,
            10,
            8,
            RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 52, 30);
        arrowOutputFromBelow.draw(guiGraphics, 94, 29);

        slotDefault.draw(guiGraphics, 8, 7);
        slotDefault.draw(guiGraphics, 30, 7);
        slotDefault.draw(guiGraphics, 127, 7);
        slotDefault.draw(guiGraphics, 30, 40);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.HYPERDIMENSION_STORAGE_STATION_UPGRADE,
            HyperdimensionStorageStationUpgradeRecipe.getAllRecipes()
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.HYPERDIMENSION_STORAGE_STATION_UPGRADE);
        registration.addRecipeCatalyst(ModBlocks.SHULKER_CONTAINER.asStack(), AnvilCraftJeiPlugin.HYPERDIMENSION_STORAGE_STATION_UPGRADE);
        registration.addRecipeCatalyst(ModBlocks.SINGULARITY_CRYSTAL.asStack(), AnvilCraftJeiPlugin.HYPERDIMENSION_STORAGE_STATION_UPGRADE);
        registration.addRecipeCatalyst(ModBlocks.HYPERCUBE.asStack(), AnvilCraftJeiPlugin.HYPERDIMENSION_STORAGE_STATION_UPGRADE);
    }
}
