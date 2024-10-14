package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.MeshRecipeGroup;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.util.RenderHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.Lazy;

import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MeshRecipeCategory implements IRecipeCategory<MeshRecipeGroup> {


    public static final int WIDTH = 162;
    public static final int ROW_START = 44;

    private final Lazy<IDrawable> background;
    private final IDrawable slot;
    private final IDrawable icon;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;

    public MeshRecipeCategory(IGuiHelper helper) {
        this.background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, ROW_START + MeshRecipeGroup.maxRows * 18));
        this.slot = helper.getSlotDrawable();
        this.icon =
                new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), Blocks.SCAFFOLDING.defaultBlockState());
        this.title = Component.translatable("gui.anvilcraft.category.mesh");
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
    }

    @Override
    public RecipeType<MeshRecipeGroup> getRecipeType() {
        return AnvilCraftJeiPlugin.MESH;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background.get();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MeshRecipeGroup recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 14).addIngredients(recipe.ingredient());

        for (int i = 0; i < recipe.results().size(); i++) {
            MeshRecipeGroup.Result result = recipe.results().get(i);
            IRecipeSlotBuilder slot = builder.addSlot(
                            RecipeIngredientRole.OUTPUT, 1 + (i % 9) * 18, 1 + ROW_START + 18 * (i / 9))
                    .addItemStack(result.item);
            JeiRecipeUtil.addTooltips(slot, result.provider);
        }
    }

    @Override
    public void draw(
            MeshRecipeGroup recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                81,
                12 + anvilYOffset,
                20,
                12,
                RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, Blocks.SCAFFOLDING.defaultBlockState(), 81, 30, 10, 12, RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 22);
        slot.draw(guiGraphics, 36, 13);

        for (int row = 0; row < MeshRecipeGroup.maxRows; row++) {
            for (int column = 0; column < 9; column++) {
                slot.draw(guiGraphics, column * 18, ROW_START + row * 18);
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.MESH, MeshRecipeGroup.getAllRecipesGrouped());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.MESH);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.MESH);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.MESH);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.MESH);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.MESH);
        registration.addRecipeCatalyst(new ItemStack(Items.SCAFFOLDING), AnvilCraftJeiPlugin.MESH);
    }
}
