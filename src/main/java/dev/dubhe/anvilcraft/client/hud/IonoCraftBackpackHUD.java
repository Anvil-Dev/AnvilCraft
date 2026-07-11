package dev.dubhe.anvilcraft.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class IonoCraftBackpackHUD {
    private static final int ROW_HEIGHT = 20;
    private static final int SUPER_CAPACITOR_X = 64;
    private static final ResourceLocation BATTERY_EMPTY = SharedTextures.textureGui("misc/battery_display/battery_empty");
    private static final ResourceLocation BATTERY_FULL = SharedTextures.textureGui("misc/battery_display/battery_full");

    public static void render(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        var config = AnvilCraftClient.CONFIG.ionocraftBackpackHud;
        if (!config.enabled && !config.capacitorCountEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack backpack = IonoCraftBackpackItem.getByPlayer(player);
        boolean renderBackpack = config.enabled && backpack.is(ModItems.IONOCRAFT_BACKPACK);
        Inventory inventory = player.getInventory();
        int capacitorCount = count(inventory, ModItems.CAPACITOR.asStack());
        int superCapacitorCount = count(inventory, ModItems.SUPER_CAPACITOR.asStack());
        boolean renderCapacitors = config.capacitorCountEnabled && (capacitorCount > 0 || superCapacitorCount > 0);
        if (!renderBackpack && !renderCapacitors) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.scale(config.hudScale, config.hudScale, config.hudScale);
        poseStack.translate(config.hudX, config.hudY, 0);
        if (renderBackpack) {
            renderBackpack(guiGraphics, mc.font, backpack);
            poseStack.translate(0, ROW_HEIGHT, 0);
        }
        if (renderCapacitors) {
            renderCapacitorCounts(guiGraphics, mc.font, capacitorCount, superCapacitorCount);
        }

        poseStack.popPose();
    }

    private static void renderBackpack(GuiGraphics guiGraphics, Font font, ItemStack backpack) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        int energy = IonoCraftBackpackItem.getEnergyStored(backpack);
        int percent = Math.round((float) energy / IonoCraftBackpackItem.MAX_ENERGY * 100);
        guiGraphics.renderItem(backpack, 0, 0);

        poseStack.translate(20, 4, 0);
        Component text = Component.translatable("hud.anvilcraft.ionocraft_backpack_power", percent);
        int textWidth = font.width(text);
        guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);

        final int batteryHeight = (int) (percent / 100f * 16);

        poseStack.translate(textWidth + 4, -4, 0);
        guiGraphics.blit(BATTERY_EMPTY, 0, 0, 0, 0, 8, 16, 8, 16);

        poseStack.translate(0, 0, 1);
        guiGraphics.blit(BATTERY_FULL, 0, 16 - batteryHeight, 0, 16 - batteryHeight, 8, batteryHeight, 8, 16);

        poseStack.popPose();
    }

    private static void renderCapacitorCounts(
        GuiGraphics guiGraphics, Font font, int capacitorCount, int superCapacitorCount
    ) {
        guiGraphics.renderItem(ModItems.CAPACITOR.asStack(), 0, 0);
        Component capacitorText = Component.literal("x " + capacitorCount);
        guiGraphics.drawString(font, capacitorText, 20, 4, 0xFFFFFFFF, true);

        guiGraphics.renderItem(ModItems.SUPER_CAPACITOR.asStack(), SUPER_CAPACITOR_X, 0);
        Component superCapacitorText = Component.literal("x " + superCapacitorCount);
        guiGraphics.drawString(font, superCapacitorText, SUPER_CAPACITOR_X + 20, 4, 0xFFFFFFFF, true);
    }

    private static int count(Inventory inventory, ItemStack item) {
        return inventory.items.stream()
            .filter(stack -> ItemStack.isSameItemSameComponents(stack, item))
            .mapToInt(ItemStack::getCount)
            .sum();
    }
}
