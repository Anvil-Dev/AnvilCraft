package dev.dubhe.anvilcraft.integration.patchouli.page;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliUtil;
import dev.dubhe.anvilcraft.mixin.accessor.ScreenAccessor;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageAnvilCollisionCraft extends PageDoubleRecipeRegistry<AnvilCollisionCraftRecipe> {
    private static final int COLLISION_LENGTH = 44;
    private static final int COLLISION_TIME = 30;

    public PageAnvilCollisionCraft() {
        super(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());
    }

    @Override
    protected void drawRecipe(
        GuiGraphics graphics,
        AnvilCollisionCraftRecipe recipe,
        int recipeX,
        int recipeY,
        int mouseX,
        int mouseY,
        boolean second
    ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(recipeX, recipeY + 18, 50);
        pose.scale(0.8f, 0.8f, 1);

        // 铁砧
        List<BlockState> anvils = recipe.anvil().constructStatesForRender();
        if (!anvils.isEmpty()) {
            BlockState state = anvils.get((parent.ticksInBook / COLLISION_TIME) % anvils.size());
            renderAnvil(parent, graphics, state, recipe.consume());
        }
        for (int i = 0; i < anvils.size(); i++) {
            RenderSupport.renderBlock(graphics, anvils.get(i), 14 * i, 26, 0, 12, RenderSupport.SINGLE_BLOCK);
        }

        // 撞击方块
        List<BlockState> hitBlocks = recipe.hitBlock().constructStatesForRender();
        if (parent.ticksInBook % COLLISION_TIME <= 20 && !hitBlocks.isEmpty()) {
            int scale = 12;
            BlockState state = hitBlocks.get((parent.ticksInBook / COLLISION_TIME) % hitBlocks.size());
            if (state.is(ModBlocks.GIANT_ANVIL)) {
                scale = 5;
                state = ModBlocks.GIANT_ANVIL.getDefaultState()
                    .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                    .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER);
            }
            RenderSupport.renderBlock(graphics, state, COLLISION_LENGTH + 5, 0, 0, scale, RenderSupport.SINGLE_BLOCK);
        }

        PatchouliRenderHelper.renderArray(graphics, COLLISION_LENGTH + 16, 0);

        pose.scale(0.5f, 0.5f, 1);
        graphics.drawString(
            ((ScreenAccessor) parent).getFont(),
            Component.translatable("gui.anvilcraft.category.anvil_collision_craft_speed", recipe.speed()),
            -4,
            26,
            0xFF000000,
            false
        );
        pose.popPose();


        // 标题
        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 10,
            book.headerColor
        );

        // 产出物
        List<ChanceItemStack> results = recipe.outputItems();
        if (!results.isEmpty()) {
            PatchouliRenderHelper.render2x2(graphics, recipeX + COLLISION_LENGTH + 12, recipeY);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2 && i * 2 + j < results.size(); j++) {
                    ChanceItemStack itemStack = results.get(i * 2 + j);
                    parent.renderItemStack(graphics, recipeX + COLLISION_LENGTH + 16 + j * 19, recipeY + 4 + i * 19,
                        mouseX,
                        mouseY,
                        PatchouliUtil.getStack(itemStack)
                    );
                }
            }
        }

        // 转化方块
        if (!recipe.transformBlocks().isEmpty()) {
            for (int i = 0; i < recipe.transformBlocks().size() && i < 2; i++) {
                int x = recipeX + COLLISION_LENGTH + 22 + i * 15;
                int y = recipeY + 7;
                pose.pushPose();
                pose.translate(x, y, 10);
                pose.scale(0.8f, 0.8f, 1);
                BlockTransform transformBlock = recipe.transformBlocks().get(i);

                // 被转化方块
                BlockStatePredicate inputBlock = transformBlock.inputBlock();
                List<BlockState> states = inputBlock.constructStatesForRender();
                BlockState state = states.get((parent.ticksInBook / COLLISION_TIME) % states.size());
                RenderSupport.renderBlock(graphics, state, 0, 0, 0, 12, RenderSupport.SINGLE_BLOCK);

                // 转化出方块
                BlockState outputBlockState = transformBlock.outputBlock().state();
                RenderSupport.renderBlock(graphics, outputBlockState, 0, 30, 0, 12, RenderSupport.SINGLE_BLOCK);
                pose.popPose();

                // 箭头
                pose.pushPose();
                pose.translate(x + 2, y + 11, 0);
                pose.scale(0.4f, 0.8f, 1);
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                PatchouliRenderHelper.renderArray(graphics, 0, 0);
                pose.popPose();

                // 最大转化量
                pose.pushPose();
                pose.translate(x + 4, y + 5, 100);
                pose.scale(0.5f, 0.5f, 1);
                graphics.drawString(
                    ((ScreenAccessor) parent).getFont(),
                    Integer.toString(transformBlock.maxCount()),
                    0,
                    0,
                    0xFFEEEEEE,
                    false
                );
                graphics.drawString(
                    ((ScreenAccessor) parent).getFont(),
                    Integer.toString(transformBlock.maxCount()),
                    0,
                    48,
                    0xFFEEEEEE,
                    false
                );
                pose.popPose();
            }
        }
    }

    private void renderAnvil(GuiBookEntry parent, GuiGraphics guiGraphics, BlockState anvil, Boolean consume) {
        int time = parent.ticksInBook % COLLISION_TIME;
        float anvilXOffset;
        if (time < 10) {
            anvilXOffset = 0;
        } else if (time < 20) {
            float returnTime = time - 10;
            anvilXOffset = (float) (COLLISION_LENGTH * Math.pow(returnTime / 10.0, 2));
        } else {
            float returnTime = time - 20;
            anvilXOffset = (float) (COLLISION_LENGTH * Math.pow(1 - returnTime / 10.0, 2));
        }
        if (!(consume && time > 20)) {
            RenderSupport.renderBlock(
                guiGraphics, anvil,
                anvilXOffset,
                0,
                20,
                12, RenderSupport.SINGLE_BLOCK
            );
        }
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, AnvilCollisionCraftRecipe recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 87;
    }
}
