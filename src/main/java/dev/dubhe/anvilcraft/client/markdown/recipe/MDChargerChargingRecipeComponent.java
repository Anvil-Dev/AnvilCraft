package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
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
        AgeratumUtil.renderBlock(context, charger, mouseX, mouseY, 24, 28, 0);

        String keyPower = recipe.getPower() < 0 ? KEY_POWER_CONSUME : KEY_POWER_PRODUCE;
        Component power = Component.translatable(keyPower, Math.abs(recipe.getPower()));
        AgeratumUtil.renderText(graphics, power, 10, 8);

        Component time = Component.translatable(KEY_TIME, 0.05 * recipe.getTime());
        AgeratumUtil.renderText(graphics, time, 10, 58);
    }
}
