package dev.dubhe.anvilcraft.integration.patchouli.page;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.mixin.accessor.ScreenAccessor;
import dev.dubhe.anvilcraft.recipe.anvil.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageTimeWarp extends PageDoubleRecipeRegistry<TimeWarpRecipe> {
    public PageTimeWarp() {
        super(ModRecipeTypes.TIME_WARP_TYPE.get());
    }

    @Override
    protected void drawRecipe(
        GuiGraphics graphics, TimeWarpRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second
    ) {
        PatchouliRenderHelper.renderAnvilWithAnimation(parent, graphics, recipeX + 58, recipeY + 13);
        BlockState state;
        if (recipe.isProduceFluid()) {
            state = Blocks.CAULDRON.defaultBlockState();
        } else {
            state = CauldronUtil.fullState(recipe.getCauldron());
        }
        RenderHelper.renderBlock(
            graphics,
            state,
            recipeX + 58,
            recipeY + 29,
            10,
            12,
            RenderHelper.SINGLE_BLOCK
        );
        RenderHelper.renderBlock(
            graphics,
            ModBlocks.CORRUPTED_BEACON.getDefaultState(),
            recipeX + 58,
            recipeY + 39,
            0,
            12,
            RenderHelper.SINGLE_BLOCK
        );

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 10,
            book.headerColor
        );

        PatchouliRenderHelper.renderArray(graphics, recipeX + 37, recipeY + 25);
        PatchouliRenderHelper.renderArray(graphics, recipeX + 70, recipeY + 25);

        List<Object2IntMap.Entry<Ingredient>> inputs = recipe.getMergedIngredients();
        if (inputs.size() <= 4) {
            PatchouliRenderHelper.render2x2(graphics, recipeX - 8, recipeY + 8);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2 && i * 2 + j < inputs.size(); j++) {
                    PatchouliRenderHelper.renderIngredientWithCount(
                        parent, graphics, inputs.get(i * 2 + j), recipeX - 4 + j * 19, recipeY + 12 + i * 19, mouseX, mouseY
                    );
                }
            }
            PoseStack pose = graphics.pose();
            pose.pushPose();
            if (recipe.isConsumeFluid()) {
                pose.translate(recipeX - 6, recipeY + 52, 100);
                pose.scale(0.5f, 0.5f, 1);
                graphics.drawString(
                    ((ScreenAccessor) parent).anvilcraft$getFont(),
                    Component.translatable(
                        "gui.anvilcraft.category.time_warp.consume_fluid", recipe.getCauldron().getName()),
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
                        "gui.anvilcraft.category.time_warp.produce_fluid", recipe.getCauldron().getName()),
                    0,
                    0,
                    0xFF000000,
                    false
                );
            }
            pose.popPose();
        } else if (inputs.size() <= 6) {
            PatchouliRenderHelper.render2x3(graphics, recipeX - 6, recipeY);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3 && i * 2 + j < inputs.size(); j++) {
                    PatchouliRenderHelper.renderIngredientWithCount(
                        parent, graphics, inputs.get(i * 2 + j), recipeX - 4 + j * 19, recipeY + 4 + i * 19, mouseX, mouseY
                    );
                }
            }
        }

        PatchouliRenderHelper.render1x1(graphics, recipeX + 81, recipeY + 18);
        parent.renderItemStack(graphics, recipeX + 85, recipeY + 22, mouseX, mouseY, recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, TimeWarpRecipe recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 78;
    }
}
