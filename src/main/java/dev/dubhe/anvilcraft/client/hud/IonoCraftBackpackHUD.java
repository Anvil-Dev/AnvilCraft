package dev.dubhe.anvilcraft.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class IonoCraftBackpackHUD {
    private static final ResourceLocation BATTERY_EMPTY = AnvilCraft.of("widget/battery_display/battery_empty");
    private static final ResourceLocation BATTERY_FULL = AnvilCraft.of("widget/battery_display/battery_full");

    public static void render(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        if (!AnvilCraft.config.ionoCraftBackpackHud.enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack itemStack = IonoCraftBackpackItem.getByPlayer(player);
        if (!itemStack.is(ModItems.IONOCRAFT_BACKPACK)) {
            return;
        }
        int flightTime = IonoCraftBackpackItem.getFlightTime(itemStack);
        int percent = Math.round((float) flightTime / AnvilCraft.config.ionoCraftBackpackMaxFlightTime * 100);

        Font font = mc.font;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int x = AnvilCraft.config.ionoCraftBackpackHud.hudX;
        int y = AnvilCraft.config.ionoCraftBackpackHud.hudY;
        float scale = AnvilCraft.config.ionoCraftBackpackHud.hudScale;

        poseStack.scale(scale, scale, scale);
        poseStack.translate(x, y, 0);
        guiGraphics.renderItem(itemStack, 0, 0);

        poseStack.translate(20, 4, 0);
        Component text = Component.translatable("hud.anvilcraft.ionocraft_backpack_power", percent);
        int textWidth = font.width(text);
        guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);

        int batteryHeight = (int) (percent / 100f * 16);

        poseStack.translate(textWidth + 4, -4, 0);
        guiGraphics.blitSprite(BATTERY_EMPTY, 0, 0, 8, 16);

        poseStack.translate(0, 0, 1);
        guiGraphics.blitSprite(BATTERY_FULL, 8, 16, 0, 16 - batteryHeight, 0, 16 - batteryHeight, 8, batteryHeight);

        poseStack.popPose();
    }
}
