package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.power.generator.ChargerBlock;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class MDChargerChargingRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/128back.png");

    public static final String KEY_CATEGORY = "gui.anvilcraft.category.charger_charging";
    public static final String KEY_POWER_CONSUME = MDChargerChargingRecipeComponent.KEY_CATEGORY + ".power_consume";
    public static final String KEY_POWER_PRODUCE = MDChargerChargingRecipeComponent.KEY_CATEGORY + ".power_produce";
    public static final String KEY_TIME = MDChargerChargingRecipeComponent.KEY_CATEGORY + ".time";

    private final ChargerChargingRecipe recipe;

    public MDChargerChargingRecipeComponent(ChargerChargingRecipe recipe, boolean enableAlignCenter) {
        super(MDChargerChargingRecipeComponent.TEXTURE, 128, 64, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor graphics = context.graphics();

        AgeratumUtil.renderItem(context, this.recipe.ingredient(), mouseX, mouseY, 50, 24);
        AgeratumUtil.renderArrow(graphics, 64, 16);
        AgeratumUtil.renderItem(context, this.recipe.result(), mouseX, mouseY, 96, 24);

        BlockState charger = this.recipe.getProcessingBlock().defaultBlockState().setValue(ChargerBlock.OVERLOAD, false);
        AgeratumUtil.renderBlock(context, charger, mouseX, mouseY, 24, 28);

        String keyPower = this.recipe.power() < 0 ? MDChargerChargingRecipeComponent.KEY_POWER_CONSUME : MDChargerChargingRecipeComponent.KEY_POWER_PRODUCE;
        Component power = Component.translatable(keyPower, Math.abs(this.recipe.power()));
        AgeratumUtil.renderText(graphics, power, 10, 8);

        Component time = Component.translatable(MDChargerChargingRecipeComponent.KEY_TIME, 0.05 * this.recipe.power());
        AgeratumUtil.renderText(graphics, time, 10, 48);
    }
}
