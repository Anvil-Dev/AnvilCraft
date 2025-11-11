package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.TranscendiumRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class TranscendiumRecipeCategory implements IRecipeCategory<TranscendiumRecipe> {
    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotChance;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;

    public TranscendiumRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.TRANSCENDIUM_BLOCK);
        this.slotDefault = JeiRenderHelper.getSlotDefault(guiHelper);
        this.slotChance = JeiRenderHelper.getSlotChoice(guiHelper);
        this.arrowIn = JeiRenderHelper.getArrowInput(guiHelper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(guiHelper);
        this.timer = guiHelper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<TranscendiumRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft.category.transcendium_recipe");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 162;
    }

    @Override
    public int getHeight() {
        return 64;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TranscendiumRecipe recipe, IFocusGroup focuses) {
        int recipeId = recipe.recipeId();
        // 输入
        builder.addInputSlot(21, 24)
            .addItemStack(ModItems.CHARGED_NEUTRONIUM_INGOT.asStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                switch (recipeId) {
                    case 0 -> tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.enchantments_amount_0").withStyle(ChatFormatting.GOLD));
                    case 1 -> tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.enchantments_amount_1_10").withStyle(ChatFormatting.GOLD));
                    case 2 -> tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.enchantments_amount_11_14").withStyle(ChatFormatting.GOLD));
                    case 3 -> tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.enchantments_amount_15").withStyle(ChatFormatting.GOLD));
                    case 4 -> tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.enchantments_amount_>16").withStyle(ChatFormatting.GOLD));
                }
            });
        switch (recipeId) {
            case 0 -> builder.addOutputSlot(125, 24).addItemStack(ModItems.TRANSCENDIUM_INGOT.asStack(4));
            case 1 -> {
                builder.addOutputSlot(116, 15).addItemStack(ModItems.NEUTRONIUM_INGOT.asStack())
                    .addRichTooltipCallback((recipeSlotView, tooltip) ->
                        tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.probability").withStyle(ChatFormatting.GRAY)));
                builder.addOutputSlot(134, 15).addItemStack(ModItems.TRANSCENDIUM_INGOT.asStack(4));
                builder.addOutputSlot(116, 33).addItemStack(ModItems.TRANSCENDIUM_NUGGET.asStack())
                    .addRichTooltipCallback((recipeSlotView, tooltip) ->
                        tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.amount_is_3").withStyle(ChatFormatting.GOLD)));
            }
            case 2 -> {
                builder.addOutputSlot(116, 15).addItemStack(ModItems.NEUTRONIUM_INGOT.asStack());
                builder.addOutputSlot(134, 15).addItemStack(ModItems.TRANSCENDIUM_INGOT.asStack(4));
                builder.addOutputSlot(116, 33).addItemStack(ModItems.TRANSCENDIUM_NUGGET.asStack())
                    .addRichTooltipCallback((recipeSlotView, tooltip) ->
                        tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.amount_is_3").withStyle(ChatFormatting.GOLD)));
            }
            case 3 -> builder.addOutputSlot(125, 15).addItemStack(ModItems.NEUTRONIUM_INGOT.asStack());
            case 4 -> {
                builder.addOutputSlot(116, 15).addItemStack(ModItems.NEUTRONIUM_INGOT.asStack());
                builder.addOutputSlot(134, 15).addItemStack(ModItems.TRANSCENDIUM_NUGGET.asStack())
                    .addRichTooltipCallback((recipeSlotView, tooltip) ->
                        tooltip.add(Component.translatable("gui.anvilcraft.category.transcendium_recipe.amount_is_1").withStyle(ChatFormatting.GOLD)));
            }
        }
    }

    @Override
    public void draw(TranscendiumRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int recipeId = recipe.recipeId();
        slotDefault.draw(guiGraphics, 20, 23);
        arrowIn.draw(guiGraphics, 54, 30);
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(guiGraphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 20, 12, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(guiGraphics, ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.getDefaultState(), 81, 40, 10, 12, RenderSupport.SINGLE_BLOCK);
        arrowOut.draw(guiGraphics, 92, 29);
        switch (recipeId) {
            case 0 -> slotDefault.draw(guiGraphics, 124, 23);
            case 1 -> {
                slotChance.draw(guiGraphics, 115, 14);
                slotDefault.draw(guiGraphics, 133, 14);
                slotDefault.draw(guiGraphics, 115, 32);
            }
            case 2 -> {
                slotDefault.draw(guiGraphics, 115, 14);
                slotDefault.draw(guiGraphics, 133, 14);
                slotDefault.draw(guiGraphics, 115, 32);
            }
            case 3 -> {
                slotDefault.draw(guiGraphics, 124, 14);
                RenderSupport.renderBlock(guiGraphics, ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState(), 133, 45, 0, 12, RenderSupport.SINGLE_BLOCK);
            }
            case 4 -> {
                slotDefault.draw(guiGraphics, 115, 14);
                slotDefault.draw(guiGraphics, 133, 14);
                RenderSupport.renderBlock(guiGraphics, ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState(), 133, 45, 0, 12, RenderSupport.SINGLE_BLOCK);
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, TranscendiumRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        int recipeId = recipe.recipeId();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.addAll(TooltipUtil.tooltip(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get()));
            }
        }
        switch (recipeId) {
            case 3, 4 -> {
                if (mouseX >= 124 && mouseX <= 140) {
                    if (mouseY >= 39 && mouseY <= 57) {
                        tooltip.addAll(TooltipUtil.tooltip(ModBlocks.TRANSCENDIUM_BLOCK.get()));
                    }
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE, TranscendiumRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.CURSED_GOLD_BLOCK.asStack(), AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
    }
}
