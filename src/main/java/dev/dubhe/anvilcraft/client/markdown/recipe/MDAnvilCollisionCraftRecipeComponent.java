package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class MDAnvilCollisionCraftRecipeComponent extends MDRecipeComponent {
    public static final int ANVIL_X = 30;
    public static final int ANVIL_Y = 50;
    public static final int MOVING_ANVIL_X = MDAnvilCollisionCraftRecipeComponent.ANVIL_X + 36;
    public static final int MOVING_ANVIL_X_DELTA = 3;
    public static final int MOVING_ANVIL_Y = MDAnvilCollisionCraftRecipeComponent.ANVIL_Y;

    public static final int HIT_BLOCK_X = MDAnvilCollisionCraftRecipeComponent.MOVING_ANVIL_X + 24;
    public static final int HIT_BLOCK_Y = MDAnvilCollisionCraftRecipeComponent.MOVING_ANVIL_Y;
    public static final int EXPLOSION_X = MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_X - 6;
    public static final int EXPLOSION_Y = MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_Y - 8;

    public static final int OUTPUT_ITEM_X = MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_X + 80;
    public static final int OUTPUT_ITEM_Y = MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_Y;
    public static final int OUTPUT_ARROW_X = MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_X + 24;
    public static final int OUTPUT_ARROW_Y = MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_Y - 8;

    public static final int TRANSFORM_X = 160;
    public static final int TRANSFORM_ARROW_X = MDAnvilCollisionCraftRecipeComponent.TRANSFORM_X - 16;
    public static final int TRANSFORM_INPUT_Y = 26;
    public static final int TRANSFORM_ARROW_Y = MDAnvilCollisionCraftRecipeComponent.TRANSFORM_INPUT_Y + 16;
    public static final int TRANSFORM_OUTPUT_Y = MDAnvilCollisionCraftRecipeComponent.TRANSFORM_INPUT_Y + 54;
    public static final int TRANSFORM_INFO_X = MDAnvilCollisionCraftRecipeComponent.TRANSFORM_X - 16;
    public static final int TRANSFORM_INFO_Y = MDAnvilCollisionCraftRecipeComponent.TRANSFORM_OUTPUT_Y + 20;

    public static final int INFO_X = 12;
    public static final int INFO_Y = 100;
    public static final int INFO_Y_OFFSET = 8;

    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/256back.png");
    public static final Identifier EXPLOSION = AnvilCraft.of("textures/gui/jei/explosion.png");

    private final AnvilCollisionCraftRecipe recipe;

    public MDAnvilCollisionCraftRecipeComponent(AnvilCollisionCraftRecipe recipe, boolean enableAlignCenter) {
        super(MDAnvilCollisionCraftRecipeComponent.TEXTURE, 256, 128, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor guiGraphics = context.graphics();
        // 此配方需要的铁砧
        Ingredient anvil = Ingredient.of(this.recipe.anvil().getBlocks().stream().map(Holder::value)
        );
        AgeratumUtil.renderItemWithoutSlot(context, anvil, mouseX, mouseY, MDAnvilCollisionCraftRecipeComponent.ANVIL_X, MDAnvilCollisionCraftRecipeComponent.ANVIL_Y);
        for (int i = 0; i < 7; i++) {
            GuiRenderExtras.itemWithTransparency(
                guiGraphics,
                new ItemStack(Blocks.ANVIL),
                MDAnvilCollisionCraftRecipeComponent.MOVING_ANVIL_X - i * MDAnvilCollisionCraftRecipeComponent.MOVING_ANVIL_X_DELTA,
                MDAnvilCollisionCraftRecipeComponent.MOVING_ANVIL_Y,
                1f - (float) i / 10
            );
        }
        // 被撞击的方块
        Ingredient hitBlock = Ingredient.of(this.recipe.hitBlock().getBlocks().stream().map(Holder::value)
        );
        AgeratumUtil.renderItemWithoutSlot(context, hitBlock, mouseX, mouseY, MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_X, MDAnvilCollisionCraftRecipeComponent.HIT_BLOCK_Y);

        guiGraphics.blit(MDAnvilCollisionCraftRecipeComponent.EXPLOSION, MDAnvilCollisionCraftRecipeComponent.EXPLOSION_X, MDAnvilCollisionCraftRecipeComponent.EXPLOSION_Y, 0, 0, 32, 32, 32, 32);

        // 输出物品
        if (!this.recipe.outputItems().isEmpty()) {
            AgeratumUtil.renderArrow(guiGraphics, MDAnvilCollisionCraftRecipeComponent.OUTPUT_ARROW_X, MDAnvilCollisionCraftRecipeComponent.OUTPUT_ARROW_Y);
            AgeratumUtil.renderItems(context, this.recipe.outputItems(), mouseX, mouseY, MDAnvilCollisionCraftRecipeComponent.OUTPUT_ITEM_X,
                                     MDAnvilCollisionCraftRecipeComponent.OUTPUT_ITEM_Y
            );
        }

        // 转换方块
        if (!this.recipe.transformBlocks().isEmpty()) {
            List<BlockTransform> blockTransforms = this.recipe.transformBlocks();

            for (BlockTransform blockTransform : blockTransforms) {
                AgeratumUtil.renderBlock(context, blockTransform.inputBlock(), mouseX, mouseY,
                                         MDAnvilCollisionCraftRecipeComponent.TRANSFORM_X,
                                         MDAnvilCollisionCraftRecipeComponent.TRANSFORM_INPUT_Y
                );
                AgeratumUtil.renderArrow(guiGraphics, MDAnvilCollisionCraftRecipeComponent.TRANSFORM_ARROW_X, MDAnvilCollisionCraftRecipeComponent.TRANSFORM_ARROW_Y, 90);
                AgeratumUtil.renderBlock(context, blockTransform.outputBlock(), mouseX, mouseY,
                                         MDAnvilCollisionCraftRecipeComponent.TRANSFORM_X,
                                         MDAnvilCollisionCraftRecipeComponent.TRANSFORM_OUTPUT_Y
                );
                AgeratumUtil.renderText(
                    guiGraphics,
                    Component.translatable("gui.anvilcraft.category.anvil_collision.maxcount", blockTransform.maxCount()),
                    MDAnvilCollisionCraftRecipeComponent.TRANSFORM_INFO_X, MDAnvilCollisionCraftRecipeComponent.TRANSFORM_INFO_Y
                );
            }
        }

        AgeratumUtil.renderText(
            guiGraphics,
            Component.translatable("gui.anvilcraft.category.anvil_collision.consume", this.recipe.consume()),
            MDAnvilCollisionCraftRecipeComponent.INFO_X, MDAnvilCollisionCraftRecipeComponent.INFO_Y
        );

        AgeratumUtil.renderText(
            guiGraphics,
            Component.translatable("gui.anvilcraft.category.anvil_collision.speed", this.recipe.speed()),
            MDAnvilCollisionCraftRecipeComponent.INFO_X, MDAnvilCollisionCraftRecipeComponent.INFO_Y + MDAnvilCollisionCraftRecipeComponent.INFO_Y_OFFSET
        );
    }
}
