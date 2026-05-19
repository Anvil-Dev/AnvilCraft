package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NeutronIrradiationCategory implements IRecipeCategory<RecipeHolder<NeutronIrradiationRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final IDrawable icon;
    private final Component title;
    private final ITickTimer timer;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public NeutronIrradiationCategory(IGuiHelper helper) {
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.icon = new DrawableBlockStateIcon(
            Blocks.CAULDRON.defaultBlockState(),
            ModBlocks.NEUTRON_IRRADIATOR.get().defaultBlockState()
        );
        this.title = Component.translatable("gui.anvilcraft.category.neutron_irradiation");
        this.timer = helper.createTickTimer(30, 60, true);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public IRecipeHolderType<NeutronIrradiationRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.NEUTRON_IRRADIATION;
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
        IRecipeLayoutBuilder builder, RecipeHolder<NeutronIrradiationRecipe> recipeHolder, IFocusGroup focuses) {
        NeutronIrradiationRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        }
    }

    @Override
    public void draw(
        RecipeHolder<NeutronIrradiationRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        NeutronIrradiationRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 12 + anvilYOffset, 20);
        Block material = recipe.getHasCauldron().getFluidCauldron();
        RenderSupport.renderBlock(graphics, CauldronUtil.fullState(material), 81, 30, 20);

        RenderSupport.renderBlock(graphics, ModBlocks.NEUTRON_IRRADIATOR.getDefaultState(), 81, 40, 20);

        this.arrowIn.draw(graphics, 54, 20);
        this.arrowOut.draw(graphics, 92, 19);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
            }
        } else {
            BlockState cauldronState = recipe.getHasCauldron().getTransformCauldron().defaultBlockState();
            RenderSupport.renderBlock(graphics, cauldronState, 133, 30, 20);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<NeutronIrradiationRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        NeutronIrradiationRecipe recipe = recipeHolder.value();
        if (MathUtil.isInRange(mouseX, 72, 90)) {
            if (MathUtil.isInRange(mouseY, 24, 43)) {
                Component text;
                text = Blocks.CAULDRON.getName();
                tooltip.add(text);
            }
            if (MathUtil.isInRange(mouseY, 34, 53)) {
                tooltip.add(ModBlocks.NEUTRON_IRRADIATOR.get().getName());
            }
        }
        if (MathUtil.isInRange(mouseX, mouseY, 124, 24, 140, 42)) {
            Identifier id = this.getIdentifier(recipeHolder);
            if (id == null) return;
            if (!recipe.getResultItems().isEmpty()) return;
            tooltip.addAll(TooltipUtil.recipeIDTooltip(recipe.getHasCauldron().getTransformCauldron(), id));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.NEUTRON_IRRADIATION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.NEUTRON_IRRADIATION.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.NEUTRON_IRRADIATION);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.NEUTRON_IRRADIATION);
        registration.addCraftingStation(AnvilCraftJeiPlugin.NEUTRON_IRRADIATION, ModBlocks.NEUTRON_IRRADIATOR);
    }
}
