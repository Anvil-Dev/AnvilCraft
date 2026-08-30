package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.MeshRecipeGroup;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public class MeshRecipeCategory implements IRecipeCategory<MeshRecipeGroup> {
    public static final int WIDTH = 162;
    public static final int ROW_START = 44;

    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final IDrawable icon;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;

    public MeshRecipeCategory(IGuiHelper helper) {
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.icon = new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), Blocks.SCAFFOLDING.defaultBlockState());
        this.title = Component.translatable("gui.anvilcraft.category.mesh");
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
    }

    @Override
    public IRecipeType<MeshRecipeGroup> getRecipeType() {
        return AnvilCraftJeiPlugin.MESH;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return MeshRecipeCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return MeshRecipeCategory.ROW_START + MeshRecipeGroup.maxRows * 18;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MeshRecipeGroup recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 14).addItemStacks(
            Arrays.stream(recipe.ingredient().getItems()).map(ItemStackTemplate::create).toList()
        );

        for (int i = 0; i < recipe.results().size(); i++) {
            MeshRecipeGroup.Result result = recipe.results().get(i);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (i % 9) * 18, 1 + MeshRecipeCategory.ROW_START
                                                                                                     + 18 * (i / 9)).add(
                result.item());
            JeiRecipeUtil.addTooltips(slot, result.item().count(), result.provider());
        }
    }

    @Override
    public void draw(
        MeshRecipeGroup recipe,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.SCAFFOLDING.defaultBlockState(), 71, 25, 20);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 7 + anvilYOffset, 20);

        this.arrowIn.draw(graphics, 55, 17);
        this.slotDefault.draw(graphics, 36, 13);

        for (int row = 0; row < MeshRecipeGroup.maxRows; row++) {
            for (int column = 0; column < 9; column++) {
                this.slotProbability.draw(graphics, column * 18, MeshRecipeCategory.ROW_START + row * 18);
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.MESH, MeshRecipeGroup.getAllRecipesGrouped());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.MESH);
        registration.addCraftingStation(AnvilCraftJeiPlugin.MESH, Items.SCAFFOLDING);
    }
}
