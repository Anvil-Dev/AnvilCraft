package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class AgeratumUtil {
    public static final int SLOT_SIZE = 19;
    public static final ResourceLocation SLOT = Ageratum.location("textures/gui/component/slot.png");
    public static final ResourceLocation ARROW = Ageratum.location("textures/gui/component/arrow.png");

    public static void renderArrow(GuiGraphics g, int x, int y) {
        renderArrow(g, x, y, 0);
    }

    public static void renderArrow(GuiGraphics g, int x, int y, float rotation) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + 16, y + 16, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        g.blit(ARROW, -16, -16, 0, 0, 32, 32, 32, 32);
        pose.popPose();
    }

    public static void renderBlock(GuiGraphics g, BlockState blockState, int x, int y) {
        RenderSupport.renderBlock(g, blockState, x, y, 0, 12, RenderSupport.SINGLE_BLOCK);
    }

    public static void renderItem(MDRenderContext context, GuiGraphics g, ItemStack displaying, float mouseX, float mouseY, int x, int y) {
        g.blit(SLOT, x - 8, y - 8, 0, 0, 32, 32, 32, 32);
        g.renderItem(displaying, x, y);
        g.renderItemDecorations(Minecraft.getInstance().font, displaying, x, y);
        AgeratumUtil.renderTooltip(context, displaying, x, y, mouseX, mouseY);
    }

    public static void renderTooltip(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
        }
    }

    public static boolean isHoverItem(int startX, int startY, float mouseX, float mouseY) {
        return isHover(startX, startY, 16, 16, mouseX, mouseY);
    }

    public static boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    public static double getMax(NumberProvider provider) {
        return switch (provider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uniform -> getMax(uniform.max());
            case BinomialDistributionGenerator binomial -> getMax(binomial.n());
            default -> 0;
        };
    }

}
