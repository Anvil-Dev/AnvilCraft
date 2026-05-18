package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.MathUtil;
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
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

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
    public IRecipeType<TranscendiumRecipe> getRecipeType() {
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
            .add(ModItems.CHARGED_NEUTRONIUM_INGOT.asStack())
            .addRichTooltipCallback((_, tooltip) -> {
                switch (recipeId) {
                    case 0 -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.transcendium_recipe.enchantments_amount_0"
                    ).withStyle(ChatFormatting.GOLD));
                    case 1 -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.transcendium_recipe.enchantments_amount_1_10"
                    ).withStyle(ChatFormatting.GOLD));
                    case 2 -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.transcendium_recipe.enchantments_amount_11_14"
                    ).withStyle(ChatFormatting.GOLD));
                    case 3 -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.transcendium_recipe.enchantments_amount_15"
                    ).withStyle(ChatFormatting.GOLD));
                    case 4 -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.transcendium_recipe.enchantments_amount_>16"
                    ).withStyle(ChatFormatting.GOLD));
                    default -> {
                    }
                }
            });
        switch (recipeId) {
            case 0 -> builder.addOutputSlot(125, 24).add(ModItems.TRANSCENDIUM_INGOT.asStack(4));
            case 1 -> {
                builder.addOutputSlot(116, 15).add(ModItems.NEUTRONIUM_INGOT.asStack())
                    .addRichTooltipCallback((_, tooltip) ->
                        tooltip.add(Component.translatable(
                            "gui.anvilcraft.category.transcendium_recipe.probability"
                        ).withStyle(ChatFormatting.GRAY)));
                builder.addOutputSlot(134, 15).add(ModItems.TRANSCENDIUM_INGOT.asStack(4));
                builder.addOutputSlot(116, 33).add(ModItems.TRANSCENDIUM_NUGGET.asStack())
                    .addRichTooltipCallback((_, tooltip) ->
                        tooltip.add(Component.translatable(
                            "gui.anvilcraft.category.transcendium_recipe.amount_is_3"
                        ).withStyle(ChatFormatting.GOLD)));
            }
            case 2 -> {
                builder.addOutputSlot(116, 15).add(ModItems.NEUTRONIUM_INGOT.asStack());
                builder.addOutputSlot(134, 15).add(ModItems.TRANSCENDIUM_INGOT.asStack(4));
                builder.addOutputSlot(116, 33).add(ModItems.TRANSCENDIUM_NUGGET.asStack())
                    .addRichTooltipCallback((_, tooltip) ->
                        tooltip.add(Component.translatable(
                            "gui.anvilcraft.category.transcendium_recipe.amount_is_3"
                        ).withStyle(ChatFormatting.GOLD)));
            }
            case 3 -> builder.addOutputSlot(125, 15).add(ModItems.NEUTRONIUM_INGOT.asStack());
            case 4 -> {
                builder.addOutputSlot(116, 15).add(ModItems.NEUTRONIUM_INGOT.asStack());
                builder.addOutputSlot(134, 15).add(ModItems.TRANSCENDIUM_NUGGET.asStack())
                    .addRichTooltipCallback((_, tooltip) ->
                        tooltip.add(Component.translatable(
                            "gui.anvilcraft.category.transcendium_recipe.amount_is_1"
                        ).withStyle(ChatFormatting.GOLD)));
            }
            default -> {
            }
        }
    }

    @Override
    public void draw(
        TranscendiumRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final int recipeId = recipe.recipeId();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 12);
        RenderSupport.renderBlock(graphics, ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.getDefaultState(), 81, 40, 12);
        this.slotDefault.draw(graphics, 20, 23);
        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOut.draw(graphics, 92, 29);
        switch (recipeId) {
            case 0 -> this.slotDefault.draw(graphics, 124, 23);
            case 1 -> {
                this.slotChance.draw(graphics, 115, 14);
                this.slotDefault.draw(graphics, 133, 14);
                this.slotDefault.draw(graphics, 115, 32);
            }
            case 2 -> {
                this.slotDefault.draw(graphics, 115, 14);
                this.slotDefault.draw(graphics, 133, 14);
                this.slotDefault.draw(graphics, 115, 32);
            }
            case 3 -> {
                this.slotDefault.draw(graphics, 124, 14);
                RenderSupport.renderBlock(graphics, ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState(), 133, 45, 12);
            }
            case 4 -> {
                this.slotDefault.draw(graphics, 115, 14);
                this.slotDefault.draw(graphics, 133, 14);
                RenderSupport.renderBlock(graphics, ModBlocks.TRANSCENDIUM_BLOCK.getDefaultState(), 133, 45, 12);
            }
            default -> {
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, TranscendiumRecipe recipe, IRecipeSlotsView view, double mouseX, double mouseY) {
        int recipeId = recipe.recipeId();
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            tooltip.addAll(TooltipUtil.tooltip(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get()));
        }
        switch (recipeId) {
            case 3, 4 -> {
                if (MathUtil.isInRange(mouseX, mouseY, 124, 39, 140, 57)) {
                    tooltip.addAll(TooltipUtil.tooltip(ModBlocks.TRANSCENDIUM_BLOCK.get()));
                }
            }
            default -> {
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE, TranscendiumRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE);
        registration.addCraftingStation(AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE, ModItems.NEUTRONIUM_INGOT);
        registration.addCraftingStation(AnvilCraftJeiPlugin.TRANSCENDIUM_RECIPE, ModBlocks.OVERHEATED_EMBER_METAL_BLOCK);
    }
}
