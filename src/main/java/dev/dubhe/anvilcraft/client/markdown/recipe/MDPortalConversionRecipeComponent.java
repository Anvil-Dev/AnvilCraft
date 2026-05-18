package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public class MDPortalConversionRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/gui/ageratum/128back.png");
    public static final String FALL_THROUGH = "gui.anvilcraft.category.portal_conversion.fall_through";
    private final PortalConversionRecipe recipe;

    public MDPortalConversionRecipeComponent(PortalConversionRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 128, 64, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics graphics = context.graphics();
        AgeratumUtil.renderBlock(context, recipe.getInput(), mouseX, mouseY, 28, 20, 0);
        AgeratumUtil.renderArrow(graphics, 46, 10);
        AgeratumUtil.renderBlock(context, recipe.getResults(), mouseX, mouseY, 96, 20, 0);

        // 在没有其他tooltip的情况下添加一个tooltip，显示传送门类型的名称
        if (AgeratumUtil.isHover(0, 0, 128, 64, mouseX, mouseY)) {
            if (context.tooltips().isEmpty()) {
                context.tooltips().add(new MDRenderContext.Tooltip(
                    List.of(Component.translatable(FALL_THROUGH, recipe.getPortalType().getPortalName())), Optional.empty()
                ));
            }
        }
    }
}
