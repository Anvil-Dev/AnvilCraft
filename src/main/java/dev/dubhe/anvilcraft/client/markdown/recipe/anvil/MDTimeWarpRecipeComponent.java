package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.List;

public class MDTimeWarpRecipeComponent extends MDBaseAnvilRecipeComponent {
    public static final int INFO_X = 12;
    public static final int INFO_Y = 106;
    @Getter
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    @Getter
    private final TimeWarpRecipe recipe;

    public MDTimeWarpRecipeComponent(TimeWarpRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        this.ingredients = recipe.getDisplayInputItems();
        this.resultItems = recipe.getResultItems();
        this.inputBlockStates = List.of(
            MDTimeWarpRecipeComponent.getInputCauldron(recipe),
            ModBlocks.CORRUPTED_BEACON.getDefaultState()
        );
        this.recipe = recipe;
    }

    protected BlockState getOutputBlockState() {
        if (this.resultItems.isEmpty()) {
            return MDTimeWarpRecipeComponent.getResultCauldron(this.recipe);
        }
        return super.getOutputBlockState();
    }

    @Override
    protected void extractAnvilRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        super.extractAnvilRecipeRenderState(context, mouseX, mouseY);
        GuiGraphicsExtractor graphics = context.graphics();

        if (!this.recipe.getCatalysts().isEmpty()) {
            AgeratumUtil.renderText(graphics, Component.translatable("gui.anvilcraft.category.catalyst"),
                MDTimeWarpRecipeComponent.INFO_X, MDTimeWarpRecipeComponent.INFO_Y - 10);
        }

        if (this.recipe.isConsumeFluid()) {
            Component text = Component.translatable(
                "gui.anvilcraft.category.time_warp.consume_fluid",
                this.recipe.getHasCauldron().consume(),
                HasCauldron.getDefaultCauldron(this.recipe.getHasCauldron().fluid()).getName()
            );
            AgeratumUtil.renderText(graphics, text, MDTimeWarpRecipeComponent.INFO_X, MDTimeWarpRecipeComponent.INFO_Y);
        } else if (this.recipe.isProduceFluid()) {
            Component text = Component.translatable(
                "gui.anvilcraft.category.time_warp.produce_fluid",
                getDisplayedElement(this.recipe.getHasCauldron().transforms()).amount(),
                HasCauldron.getDefaultCauldron(getDisplayedElement(this.recipe.getHasCauldron().transforms()).fluid().value()).getName()
            );
            AgeratumUtil.renderText(graphics, text, MDTimeWarpRecipeComponent.INFO_X, MDTimeWarpRecipeComponent.INFO_Y);
        }
    }

    public static BlockState getInputCauldron(TimeWarpRecipe recipe) {
        Block material = HasCauldron.getDefaultCauldron(recipe.getHasCauldron().fluid());
        return CauldronUtil.fullState(material);
    }

    public static BlockState getResultCauldron(TimeWarpRecipe recipe) {
        List<FluidStackTemplate> transforms = recipe.getHasCauldron().transforms();
        Block result = transforms.isEmpty()
                       ? HasCauldron.getDefaultCauldron(recipe.getHasCauldron().fluid())
                       : HasCauldron.getDefaultCauldron(getDisplayedElement(transforms).fluid().value());
        if (recipe.isConsumeFluid()) {
            return CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
        } else if (recipe.isProduceFluid()) {
            return CauldronUtil.getStateFromContentAndLevel(result, 1);
        } else {
            return CauldronUtil.fullState(result);
        }
    }
}
