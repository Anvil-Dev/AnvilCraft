package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.ShulkerContainerUpgradeRecipe;
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

public class ShulkerContainerUpgradeCategory implements IRecipeCategory<ShulkerContainerUpgradeRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    public static final int NETHERITE_BLOCK_COUNT = 6;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable arrowIn;
    private final IDrawable arrowOutputFromBelow;
    private final ITickTimer timer;
    private final Component title;
    private final Component dropOnTopTooltip;
    private final Component strikeTooltip;

    public ShulkerContainerUpgradeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.SHULKER_CONTAINER.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutputFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.title = Component.translatable("gui.anvilcraft.category.shulker_container_upgrade");
        this.dropOnTopTooltip =
            Component.translatable("gui.anvilcraft.category.shulker_container_upgrade.drop_on_top")
                .withStyle(ChatFormatting.GOLD);
        this.strikeTooltip =
            Component.translatable("gui.anvilcraft.category.shulker_container_upgrade.strike")
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public RecipeType<ShulkerContainerUpgradeRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.SHULKER_CONTAINER_UPGRADE;
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
    public void setRecipe(
        IRecipeLayoutBuilder builder, ShulkerContainerUpgradeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 9, 8)
            .addItemStack(ModBlocks.SPACE_OVERCOMPRESSOR.asStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(dropOnTopTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, 31, 8)
            .addItemStack(new ItemStack(Blocks.NETHERITE_BLOCK, NETHERITE_BLOCK_COUNT))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(dropOnTopTooltip));
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.INPUT,
            "crate",
            69,
            32,
            25,
            25,
            ModBlocks.LARGE_CRATE.get()
        );
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 8)
            .addItemStack(ModBlocks.SHULKER_CONTAINER.asStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(strikeTooltip));
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, ShulkerContainerUpgradeRecipe recipe, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        ShulkerContainerUpgradeRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
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
            ModBlocks.LARGE_CRATE.getDefaultState().setValue(LargeCrateBlock.HALF, Cube3x3PartHalf.MID_CENTER),
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
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SHULKER_CONTAINER_UPGRADE,
            ShulkerContainerUpgradeRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.LARGE_CRATE.asStack(), AnvilCraftJeiPlugin.SHULKER_CONTAINER_UPGRADE);
        registration.addRecipeCatalyst(
            ModBlocks.SHULKER_CONTAINER.asStack(), AnvilCraftJeiPlugin.SHULKER_CONTAINER_UPGRADE);
        registration.addRecipeCatalyst(
            ModBlocks.SPACE_OVERCOMPRESSOR.asStack(), AnvilCraftJeiPlugin.SHULKER_CONTAINER_UPGRADE);
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.SHULKER_CONTAINER_UPGRADE);
    }
}