package dev.dubhe.anvilcraft.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.constant.TexturesConstant;
import dev.dubhe.anvilcraft.inventory.component.sc.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public class RenderUtil {
    public static void renderItemDecorations(
        GuiGraphics graphics,
        Font font,
        ShulkerContainerSlot slot,
        UnlimitedItemStack stack,
        int x,
        int y,
        @Nullable String text
    ) {
        if (stack.isEmpty()) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();

        if (slot.isFolded()) {
            pose.pushPose();
            pose.translate(x - 1, y - 1, -10);
            graphics.blit(
                TexturesConstant.SHULKER_CONTAINER_FOLDED_SLOT,
                0,
                0,
                0,
                0,
                18,
                18,
                18,
                18
            );
            pose.popPose();
        }

        if (stack.getStack().isBarVisible()) {
            int l = stack.getStack().getBarWidth();
            int i = stack.getStack().getBarColor();
            int j = x + 2;
            int k = y + 13;
            graphics.fill(RenderType.guiOverlay(), j, k, j + 13, k + 2, -100, 0xff000000);
            graphics.fill(RenderType.guiOverlay(), j, k, j + l, k + 1, -90, i | 0xff000000);
        }

        if (stack.getCount() != 1 || text != null) {
            pose.pushPose();
            pose.translate(x, y, 200.0F);
            pose.scale(0.75f, 0.75f, 1);
            String s = text == null ? FormattingUtil.compatNumber(stack.getCount()) : text;
            graphics.drawString(font, s, 24 - 2 - font.width(s), 14, 0xffffff, true);
            pose.popPose();
        }

        LocalPlayer localplayer = Minecraft.getInstance().player;
        float f = localplayer == null
                  ? 0.0F
                  : localplayer.getCooldowns().getCooldownPercent(
                      stack.getStack().getItem(),
                      Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)
                  );
        if (f > 0.0F) {
            int i1 = y + Mth.floor(16.0F * (1.0F - f));
            int j1 = i1 + Mth.ceil(16.0F * f);
            graphics.fill(RenderType.guiOverlay(), x, i1, x + 16, j1, Integer.MAX_VALUE);
        }

        pose.popPose();
    }
}
