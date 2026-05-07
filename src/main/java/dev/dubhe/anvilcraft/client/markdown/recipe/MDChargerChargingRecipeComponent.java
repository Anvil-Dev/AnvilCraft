package dev.dubhe.anvilcraft.client.markdown.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class MDChargerChargingRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/ageratum/128back.png");

    public static final String KEY_CATEGORY = "gui.anvilcraft.category.charger_charging";
    public static final String KEY_POWER_CONSUME = KEY_CATEGORY + ".power_consume";
    public static final String KEY_POWER_PRODUCE = KEY_CATEGORY + ".power_produce";
    public static final String KEY_TIME = KEY_CATEGORY + ".time";

    private final ChargerChargingRecipe recipe;

    public MDChargerChargingRecipeComponent(ChargerChargingRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 128, 64, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics graphics = context.graphics();

        AgeratumUtil.renderItem(context, recipe.getIngredient(), mouseX, mouseY, 50, 24);
        AgeratumUtil.renderArrow(graphics, 64, 16);
        AgeratumUtil.renderItem(context, recipe.getResult(), mouseX, mouseY, 96, 24);

        BlockState charger = recipe.getProcessingBlock().defaultBlockState().setValue(ChargerBlock.OVERLOAD, false);
        AgeratumUtil.renderBlock(graphics, charger, 24, 28, 0);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(10, 8, 0);
        pose.scale(0.8f, 0.8f, 1.0f);
        graphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable(recipe.getPower() < 0 ? KEY_POWER_CONSUME : KEY_POWER_PRODUCE,
                Math.abs(recipe.getPower())),
            0, 0, 0xFF000000, false);
        pose.translate(0, 50, 0);
        graphics.drawString(Minecraft.getInstance().font,
            Component.translatable(KEY_TIME, 0.05 * recipe.getTime()),
            0, 0, 0xFF000000, false);
        pose.popPose();
    }
}
