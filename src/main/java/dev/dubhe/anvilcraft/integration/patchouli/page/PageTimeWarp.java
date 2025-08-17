package dev.dubhe.anvilcraft.integration.patchouli.page;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.mixin.accessor.ScreenAccessor;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PageTimeWarp extends PageAnvilItemProcess<TimeWarpRecipe> {
    public PageTimeWarp() {
        super(
            ModRecipeTypes.TIME_WARP_TYPE.get(),
            TimeWarpRecipe::getInputItems,
            TimeWarpRecipe::getResultItems,
            PageTimeWarp::getCauldron,
            recipe -> ModBlocks.CORRUPTED_BEACON.getDefaultState());
    }

    @Override
    protected void drawExtra(
        GuiGraphics graphics, TimeWarpRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second
    ) {
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        if (inputs.size() > 4) return;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (recipe.isConsumeFluid()) {
            pose.translate(recipeX - 6, recipeY + 52, 100);
            pose.scale(0.5f, 0.5f, 1);
            graphics.drawString(
                ((ScreenAccessor) parent).anvilcraft$getFont(),
                Component.translatable(
                    "gui.anvilcraft.category.time_warp.consume_fluid",
                    hasCauldron.getConsume(),
                    Component.translatable("fluid." + hasCauldron.getTransform().toString().replace(':', '.'))),
                0,
                0,
                0xFF000000,
                false
            );
        } else if (recipe.isProduceFluid()) {
            pose.translate(recipeX - 6, recipeY + 52, 100);
            pose.scale(0.5f, 0.5f, 1);
            graphics.drawString(
                ((ScreenAccessor) parent).anvilcraft$getFont(),
                Component.translatable(
                    "gui.anvilcraft.category.time_warp.produce_fluid",
                    -hasCauldron.getConsume(),
                    Component.translatable("fluid." + hasCauldron.getTransform().toString().replace(':', '.'))),
                0,
                0,
                0xFF000000,
                false
            );
        }
        pose.popPose();
    }

    static BlockState getCauldron(TimeWarpRecipe recipe) {
        if (recipe.isProduceFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        } else {
            return CauldronUtil.fullState(BuiltInRegistries.BLOCK.get(recipe.getHasCauldron().getFluid().withSuffix("_cauldron")));
        }
    }
}
