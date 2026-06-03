package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public class IonoCraftBackpackHUD {
    private static final Identifier BATTERY_EMPTY = SharedTextures.textureGui("misc/battery_display/battery_empty");
    private static final Identifier BATTERY_FULL = SharedTextures.textureGui("misc/battery_display/battery_full");

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        if (!AnvilCraftClient.CONFIG.ionoCraftBackpackHud.enabled) {
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
        final int percent = Math.round((float) flightTime / AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime * 100);

        final Font font = mc.font;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();

        int x = AnvilCraftClient.CONFIG.ionoCraftBackpackHud.hudX;
        int y = AnvilCraftClient.CONFIG.ionoCraftBackpackHud.hudY;
        float scale = AnvilCraftClient.CONFIG.ionoCraftBackpackHud.hudScale;

        pose.scale(scale, scale);
        pose.translate(x, y);
        graphics.item(itemStack, 0, 0);

        pose.translate(20, 4);
        Component text = Component.translatable("hud.anvilcraft.ionocraft_backpack_power", percent);
        int textWidth = font.width(text);
        graphics.text(font, text, 0, 0, 0xFFFFFFFF, true);

        final int batteryHeight = (int) (percent / 100F * 16);

        pose.translate(textWidth + 4, -4);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BATTERY_EMPTY,
            0,
            0,
            8,
            16,
            8,
            16,
            8,
            16
        );

        pose.translate(0, 0);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BATTERY_FULL,
            0,
            16 - batteryHeight,
            0,
            16 - batteryHeight,
            8,
            batteryHeight,
            8,
            16
        );

        pose.popMatrix();
    }
}
