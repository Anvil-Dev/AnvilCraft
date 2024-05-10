package dev.dubhe.anvilcraft.client.renderer.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

public class TooltipRenderHelper {

    public static void renderTooltipWithItemIcon(
            GuiGraphics thiz,
            Font font,
            ItemStack itemStack,
            @NotNull List<Component> lines,
            int x,
            int y
    ) {
        ClientTooltipPositioner tooltipPositioner = DefaultTooltipPositioner.INSTANCE;
        List<ClientTooltipComponent> components = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        if (components.isEmpty()) return;
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent component : components) {
            width = Math.max(component.getWidth(font), width);
            height += component.getHeight();
        }

        Vector2ic vector2ic = tooltipPositioner.positionTooltip(
                thiz.guiWidth(),
                thiz.guiHeight(),
                x,
                y,
                width,
                height
        );
        int vx = vector2ic.x();
        int vy = vector2ic.y();
        thiz.pose().pushPose();

        int finalVy = vy;
        int finalWidth = width;
        int finalHeight = height + 16;
        thiz.drawManaged(() -> TooltipRenderUtil.renderTooltipBackground(
                thiz,
                vx,
                finalVy,
                finalWidth,
                finalHeight,
                400
        ));
        thiz.pose().translate(0.0F, 0.0F, 400.0F);

        thiz.renderFakeItem(itemStack, vx, vy);

        vy += 16;

        ClientTooltipComponent component;
        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.renderText(font, vx, q, thiz.pose().last().pose(), thiz.bufferSource());
            q += component.getHeight() + (i == 0 ? 2 : 0);
        }

        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.renderImage(font, vx, q, thiz);
            q += component.getHeight() + (i == 0 ? 2 : 0);
        }

        thiz.pose().popPose();
    }
}
