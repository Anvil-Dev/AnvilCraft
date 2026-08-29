package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.UseItemOnBlockRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextureConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 展示四种加工台之间的右键转化关系。
 */
public class UseItemOnBlockCategory
    implements IRecipeCategory<UseItemOnBlockRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final IDrawable mouseRight;
    private final Component title;
    private final Component convertTooltip;

    public UseItemOnBlockCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.STAMPING_PLATFORM.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowDefault(helper);
        this.arrowOut = JeiRenderHelper.getArrowDefault(helper);
        this.mouseRight = helper.drawableBuilder(JeiTextureConstants.texture("mouse_right"), 0, 0, 12, 16)
            .setTextureSize(12, 16)
            .build();
        this.title = Component.translatable("gui.anvilcraft.category.use_item_on_block");
        this.convertTooltip = Component.translatable(
                "gui.anvilcraft.category.use_item_on_block.convert"
            )
            .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public RecipeType<UseItemOnBlockRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.USE_ITEM_ON_BLOCK;
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
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        UseItemOnBlockRecipe recipe,
        IFocusGroup focuses
    ) {
        builder.addSlot(RecipeIngredientRole.CATALYST, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
            .addItemStack(recipe.toolStack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y)
            .addItemStack(recipe.outputStack());
    }

    @Override
    public void draw(
        UseItemOnBlockRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        BlockState inputState = recipe.inputBlock().defaultBlockState();
        if (inputState.hasProperty(IPowerComponent.OVERLOAD)) {
            inputState = inputState.setValue(IPowerComponent.OVERLOAD, false);
        }
        RenderSupport.renderBlock(
            guiGraphics,
            inputState,
            81,
            26,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        this.arrowIn.draw(guiGraphics, 50, 25);
        this.arrowOut.draw(guiGraphics, 96, 25);

        this.mouseRight.draw(guiGraphics, 50, 40);

        JeiSlotUtil.drawSlots(guiGraphics, this.slotDefault, 1, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);

        JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slotDefault, 1);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        UseItemOnBlockRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (mouseX >= 50 && mouseX <= 100 && mouseY >= 15 && mouseY <= 45) {
            tooltip.add(this.convertTooltip);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.USE_ITEM_ON_BLOCK,
            UseItemOnBlockRecipe.getAllRecipes()
        );
    }
}
