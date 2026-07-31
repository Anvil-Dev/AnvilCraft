package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public class MDPortalConversionRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/128back.png");
    public static final String FALL_THROUGH = "gui.anvilcraft.category.portal_conversion.fall_through";
    private final PortalConversionRecipe recipe;

    public MDPortalConversionRecipeComponent(PortalConversionRecipe recipe, boolean enableAlignCenter) {
        super(MDPortalConversionRecipeComponent.TEXTURE, 128, 64, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor graphics = context.graphics();
        AgeratumUtil.renderBlock(context, this.recipe.getInput(), mouseX, mouseY, 28, 20);
        AgeratumUtil.renderArrow(graphics, 46, 10);
        AgeratumUtil.renderBlock(context, this.recipe.getResults(), mouseX, mouseY, 96, 20);

        // 在没有其他tooltip的情况下添加一个tooltip，显示传送门类型的名称
        if (AgeratumUtil.isHover(0, 0, 128, 64, mouseX, mouseY)) {
            if (context.tooltips().isEmpty()) {
                context.tooltips().add(new MDRenderContext.Tooltip(
                    List.of(Component.translatable(
                        MDPortalConversionRecipeComponent.FALL_THROUGH,
                        this.recipe.getPortalType().getPortalName()
                    )), Optional.empty()
                ));
            }
        }
    }
}
