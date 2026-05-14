package dev.dubhe.anvilcraft.integration.jei.category.anvil;

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
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NeutronIrradiationCategory implements IRecipeCategory<RecipeHolder<NeutronIrradiationRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final Component title;
    private final ITickTimer timer;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public NeutronIrradiationCategory(IGuiHelper helper) {
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        slotProbability = JeiRenderHelper.getSlotProbability(helper);
        title = Component.translatable("gui.anvilcraft.category.neutron_irradiation");
        timer = helper.createTickTimer(30, 60, true);
        arrowIn = JeiRenderHelper.getArrowInput(helper);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public RecipeType<RecipeHolder<NeutronIrradiationRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.NEUTRON_IRRADIATION;
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
        return new DrawableBlockStateIcon(
            Blocks.CAULDRON.defaultBlockState(),
            ModBlocks.NEUTRON_IRRADIATOR
                .get()
                .defaultBlockState()
        );
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
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        NeutronIrradiationRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        Block material = recipe.getHasCauldron().getFluidCauldron();
        RenderSupport.renderBlock(
            guiGraphics,
            CauldronUtil.fullState(material),
            81,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        BlockState block = ModBlocks.NEUTRON_IRRADIATOR
            .get()
            .defaultBlockState();

        RenderSupport.renderBlock(
            guiGraphics,
            block,
            81,
            40,
            0,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        arrowIn.draw(guiGraphics, 54, 20);
        arrowOut.draw(guiGraphics, 92, 19);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
            }
        } else {
            BlockState cauldronState = recipe.getHasCauldron().getTransformCauldron().defaultBlockState();

            RenderSupport.renderBlock(guiGraphics, cauldronState, 133, 30, 0, 12, RenderSupport.SINGLE_BLOCK);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<NeutronIrradiationRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        NeutronIrradiationRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 24 && mouseY <= 43) {
                Component text;
                text = Blocks.CAULDRON.getName();
                tooltip.add(text);
            }
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.add(ModBlocks.NEUTRON_IRRADIATOR.get().getName());
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                Component text;
                if (recipe.getResultItems().isEmpty()) {
                    text = recipe.getHasCauldron().getTransformCauldron().getName();
                    tooltip.add(text);
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.NEUTRON_IRRADIATION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.NEUTRON_IRRADIATION_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.NEUTRON_IRRADIATION);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.NEUTRON_IRRADIATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftJeiPlugin.NEUTRON_IRRADIATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.NEUTRON_IRRADIATOR), AnvilCraftJeiPlugin.NEUTRON_IRRADIATION);
    }
}
