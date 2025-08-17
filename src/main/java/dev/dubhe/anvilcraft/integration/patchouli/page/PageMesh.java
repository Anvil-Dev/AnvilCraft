package dev.dubhe.anvilcraft.integration.patchouli.page;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageMesh extends PageDoubleRecipeRegistry<MeshRecipe> {
    public PageMesh() {
        super(ModRecipeTypes.MESH_TYPE.get());
    }

    @Override
    protected void drawRecipe(GuiGraphics graphics, MeshRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY, boolean second) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(recipeX + 49, recipeY + 6, 10);
        pose.scale(0.8f, 0.8f, 1);
        PatchouliRenderHelper.renderAnvilWithAnimation(parent, graphics, 0, 0);
        RenderHelper.renderBlock(graphics, Blocks.SCAFFOLDING.defaultBlockState(), 0, 16, 0, 12, RenderHelper.SINGLE_BLOCK);
        pose.popPose();

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 10,
            book.headerColor
        );

        pose.pushPose();
        pose.translate(recipeX + 28, recipeY + 10, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(15));
        PatchouliRenderHelper.renderArray(graphics, 0, 0);
        pose.popPose();

        ItemIngredientPredicate input = recipe.getInputItems().get((parent.ticksInBook / 20) % recipe.getInputItems().size());
        PatchouliRenderHelper.render1x1(graphics, recipeX - 1, recipeY + 2);
        PatchouliRenderHelper.renderIngredient(parent, graphics, input, recipeX + 3, recipeY + 6, mouseX, mouseY);

        List<ChanceItemStack> results = recipe.getResultItems();
        if (results.size() <= 5) {
            PatchouliRenderHelper.render1x5(graphics, recipeX - 1, recipeY + 29);
            for (int i = 0; i < results.size(); i++) {
                parent.renderItemStack(graphics, recipeX + 3 + i * 19, recipeY + 42, mouseX, mouseY, results.get(i).getStack());
            }
        } else if (results.size() <= 8) {
            PatchouliRenderHelper.render2x5(graphics, recipeX - 1, recipeY + 29);
            for (int i = 0; i < 5; i++) {
                parent.renderItemStack(graphics, recipeX + 3 + i * 19, recipeY + 33, mouseX, mouseY, results.get(i).getStack());
            }
            for (int i = 0; i < results.size() - 5; i++) {
                parent.renderItemStack(graphics, recipeX + 3 + i * 19, recipeY + 52, mouseX, mouseY, results.get(i + 5).getStack());
            }
        }
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, MeshRecipe recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 87;
    }
}
