package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3x2fStack;

import java.util.Comparator;
import java.util.List;

public class MassInjectCategory implements IRecipeCategory<RecipeHolder<MassInjectRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private static final String KEY_MASS_VALUE = "gui.anvilcraft.category.mass_inject.mass_value";
    private static final String KEY_MASS_NEEDED = "gui.anvilcraft.category.mass_inject.mass_needed";
    private static final String KEY_ITEMS_NEEDED = "gui.anvilcraft.category.mass_inject.items_needed";

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOutputFromBelow;

    public MassInjectCategory(IGuiHelper helper) {
        this.icon = new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.SPACE_OVERCOMPRESSOR.getDefaultState());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.mass_inject");
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutputFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
    }

    @Override
    public IRecipeHolderType<MassInjectRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MASS_INJECT;
    }

    @Override
    public Component getTitle() {
        return this.title;
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
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MassInjectRecipe> recipeHolder, IFocusGroup focuses) {
        MassInjectRecipe recipe = recipeHolder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).add(recipe.getIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 24).add(ModItems.NEUTRONIUM_INGOT.asStack())
            .addRichTooltipCallback((_, tooltip) -> tooltip.add(
                Component.translatable(KEY_MASS_NEEDED, SpaceOvercompressorBlockEntity.DISPLAYED_MASS).withStyle(ChatFormatting.GOLD)
            ));
    }

    @Override
    public void draw(
        RecipeHolder<MassInjectRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final MassInjectRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 20);
        RenderSupport.renderBlock(graphics, ModBlocks.SPACE_OVERCOMPRESSOR.getDefaultState(), 81, 40, 20);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutputFromBelow.draw(graphics, 92, 29);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, 1);
        JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, 1);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable(KEY_MASS_VALUE, recipe.displayMassValue()),
            0,
            10,
            0xFF000000,
            false
        );
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable(KEY_ITEMS_NEEDED, Math.ceilDiv(SpaceOvercompressorBlockEntity.NEUTRONIUM_INGOT_MASS, recipe.getMass())),
            0,
            70,
            0xFF000000,
            false
        );
        pose.popMatrix();
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<MassInjectRecipe>> recipes =
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MASS_INJECT.get());
        recipes.sort(Comparator.comparingInt(recipe -> recipe.value().getMass()));
        registration.addRecipes(AnvilCraftJeiPlugin.MASS_INJECT, recipes.reversed());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MASS_INJECT, ModBlocks.SPACE_OVERCOMPRESSOR.asStack());
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.MASS_INJECT);
    }
}
