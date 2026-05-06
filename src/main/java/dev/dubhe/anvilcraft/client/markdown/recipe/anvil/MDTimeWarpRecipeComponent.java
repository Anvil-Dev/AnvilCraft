package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDTimeWarpRecipeComponent extends MDBaseAnvilRecipeComponent {
    public static final int INFO_X = 12;
    public static final int INFO_Y = 106;
    @Getter
    @Nullable
    private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable
    private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    @Getter
    @Nullable
    private final BlockState outputCauldron;

    @Getter
    private final TimeWarpRecipe recipe;

    public MDTimeWarpRecipeComponent(TimeWarpRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlockStates = List.of(
            getInputCauldron(recipe),
            ModBlocks.CORRUPTED_BEACON.getDefaultState()
        );
        outputCauldron = !resultItems.isEmpty() ? null : getResultCauldron(recipe);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        super.renderRecipe(context, mouseX, mouseY);
        GuiGraphics graphics = context.graphics();
        if (outputCauldron != null) {
            AgeratumUtil.renderBlock(graphics, outputCauldron, 90, 29, 10);
        }

        Block material = recipe.getHasCauldron().getFluidCauldron();
        if (recipe.isConsumeFluid()) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(INFO_X, INFO_Y, 100);
            graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.time_warp.consume_fluid",
                    recipe.getHasCauldron().consume(),
                    material.getName()),
                0,
                0,
                0xFF000000,
                false);
            pose.popPose();
        } else if (recipe.isProduceFluid()) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(INFO_X, INFO_Y, 100);
            graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.time_warp.produce_fluid",
                    -recipe.getHasCauldron().consume(),
                    recipe.getHasCauldron().getTransformCauldron().getName()),
                0,
                0,
                0xFF000000,
                false);
            pose.popPose();
        }
    }

    public static BlockState getInputCauldron(TimeWarpRecipe recipe) {
        Block material = recipe.getHasCauldron().getFluidCauldron();
        return CauldronUtil.fullState(material);
    }

    static BlockState getResultCauldron(TimeWarpRecipe recipe) {
        Block result = recipe.getHasCauldron().getTransformCauldron();
        if (recipe.isConsumeFluid()) {
            return CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
        } else if (recipe.isProduceFluid()) {
            return CauldronUtil.getStateFromContentAndLevel(result, 1);
        } else {
            return CauldronUtil.fullState(result);
        }
    }
}
