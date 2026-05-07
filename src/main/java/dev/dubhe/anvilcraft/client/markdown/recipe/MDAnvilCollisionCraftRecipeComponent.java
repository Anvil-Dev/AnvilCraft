package dev.dubhe.anvilcraft.client.markdown.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class MDAnvilCollisionCraftRecipeComponent extends MDRecipeComponent {
    public static final int ANVIL_X = 30;
    public static final int ANVIL_Y = 50;
    public static final int MOVING_ANVIL_X = ANVIL_X + 36;
    public static final int MOVING_ANVIL_X_DELTA = 3;
    public static final int MOVING_ANVIL_Y = ANVIL_Y;

    public static final int HIT_BLOCK_X = MOVING_ANVIL_X + 24;
    public static final int HIT_BLOCK_Y = MOVING_ANVIL_Y;
    public static final int EXPLOSION_X = HIT_BLOCK_X - 6;
    public static final int EXPLOSION_Y = HIT_BLOCK_Y - 8;

    public static final int OUTPUT_ITEM_X = HIT_BLOCK_X + 80;
    public static final int OUTPUT_ITEM_Y = HIT_BLOCK_Y;
    public static final int OUTPUT_ARROW_X = HIT_BLOCK_X + 24;
    public static final int OUTPUT_ARROW_Y = HIT_BLOCK_Y - 8;

    public static final int TRANSFORM_X = 160;
    public static final int TRANSFORM_ARROW_X = TRANSFORM_X - 16;
    public static final int TRANSFORM_INPUT_Y = 26;
    public static final int TRANSFORM_ARROW_Y = TRANSFORM_INPUT_Y + 16;
    public static final int TRANSFORM_OUTPUT_Y = TRANSFORM_INPUT_Y + 54;
    public static final int TRANSFORM_INFO_X = TRANSFORM_X - 16;
    public static final int TRANSFORM_INFO_Y = TRANSFORM_OUTPUT_Y + 20;

    public static final int INFO_X = 12;
    public static final int INFO_Y = 100;
    public static final int INFO_Y_OFFSET = 8;

    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/ageratum/256back.png");
    public static final ResourceLocation EXPLOSION =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/jei/explosion.png");

    private final AnvilCollisionCraftRecipe recipe;

    public MDAnvilCollisionCraftRecipeComponent(AnvilCollisionCraftRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 256, 128, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics guiGraphics = context.graphics();
        PoseStack pose = guiGraphics.pose();
        // 此配方需要的铁砧
        Ingredient anvil = Ingredient.of(
            recipe.anvil().getBlocks().stream()
                .map(blockHolder -> new ItemStack(blockHolder.value()))
        );
        AgeratumUtil.renderItemWithoutSlot(context, anvil, mouseX, mouseY, ANVIL_X, ANVIL_Y);
        for (int i = 0; i < 7; i++) {
            RenderSupport.renderItemWithTransparency(
                new ItemStack(Blocks.ANVIL),
                pose,
                MOVING_ANVIL_X - i * MOVING_ANVIL_X_DELTA,
                MOVING_ANVIL_Y,
                1f - (float) i / 10
            );
        }
        // 被撞击的方块
        Ingredient hitBlock = Ingredient.of(recipe.hitBlock().getBlocks().stream()
            .map(blockHolder -> new ItemStack(blockHolder.value()))
        );
        AgeratumUtil.renderItemWithoutSlot(context, hitBlock, mouseX, mouseY, HIT_BLOCK_X, HIT_BLOCK_Y);

        guiGraphics.blit(EXPLOSION, EXPLOSION_X, EXPLOSION_Y, 0, 0, 32, 32, 32, 32);

        // 输出物品
        if (!recipe.outputItems().isEmpty()) {
            AgeratumUtil.renderArrow(guiGraphics, OUTPUT_ARROW_X, OUTPUT_ARROW_Y);
            AgeratumUtil.renderItems(context, recipe.outputItems(), mouseX, mouseY, OUTPUT_ITEM_X, OUTPUT_ITEM_Y);
        }

        // 转换方块
        if (!recipe.transformBlocks().isEmpty()) {
            List<BlockTransform> blockTransforms = recipe.transformBlocks();

            for (BlockTransform blockTransform : blockTransforms) {
                AgeratumUtil.renderBlock(context, blockTransform.inputBlock(), mouseX, mouseY, TRANSFORM_X, TRANSFORM_INPUT_Y, 20);
                AgeratumUtil.renderArrow(guiGraphics, TRANSFORM_ARROW_X, TRANSFORM_ARROW_Y, 90);
                AgeratumUtil.renderBlock(context, blockTransform.outputBlock(), mouseX, mouseY, TRANSFORM_X, TRANSFORM_OUTPUT_Y, 20);
                AgeratumUtil.renderText(
                    guiGraphics,
                    Component.translatable("gui.anvilcraft.category.anvil_collision.maxcount", blockTransform.maxCount()),
                    TRANSFORM_INFO_X, TRANSFORM_INFO_Y
                );
            }
        }

        AgeratumUtil.renderText(
            guiGraphics,
            Component.translatable("gui.anvilcraft.category.anvil_collision.consume", recipe.consume()),
            INFO_X, INFO_Y
        );

        AgeratumUtil.renderText(
            guiGraphics,
            Component.translatable("gui.anvilcraft.category.anvil_collision.speed", recipe.speed()),
            INFO_X, INFO_Y + INFO_Y_OFFSET
        );
    }
}
