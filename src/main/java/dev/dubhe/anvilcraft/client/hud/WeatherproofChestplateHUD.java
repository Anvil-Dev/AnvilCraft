package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.WeatherproofChestplateItem;
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

public class WeatherproofChestplateHUD {
    private static final int ROW_HEIGHT = 20;
    private static final int SUPER_CAPACITOR_X = 64;
    private static final Identifier BATTERY_EMPTY = SharedTextures.textureGui("misc/battery_display/battery_empty");
    private static final Identifier BATTERY_FULL = SharedTextures.textureGui("misc/battery_display/battery_full");

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        var config = AnvilCraftClient.CONFIG.weatherproofChestplateHud;
        if (!config.enabled && !config.capacitorCountEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack backpack = WeatherproofChestplateItem.getByPlayer(player);
        boolean renderBackpack = config.enabled && backpack.is(ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE);
        java.util.List<ItemStack> inventory = dev.dubhe.anvilcraft.inventory.PocketInventory.carriedItems(player);
        int capacitorCount = WeatherproofChestplateHUD.count(inventory, ModItems.CAPACITOR.asStack());
        int superCapacitorCount = WeatherproofChestplateHUD.count(inventory, ModItems.SUPER_CAPACITOR.asStack());
        boolean renderCapacitors = config.capacitorCountEnabled && (capacitorCount > 0 || superCapacitorCount > 0);
        if (!renderBackpack && !renderCapacitors) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();

        pose.scale(config.hudScale, config.hudScale);
        pose.translate(config.hudX, config.hudY);
        if (renderBackpack) {
            WeatherproofChestplateHUD.renderBackpack(graphics, mc.font, backpack);
            pose.translate(0, WeatherproofChestplateHUD.ROW_HEIGHT);
        }
        if (renderCapacitors) {
            WeatherproofChestplateHUD.renderCapacitorCounts(graphics, mc.font, capacitorCount, superCapacitorCount);
        }

        pose.popMatrix();
    }

    private static void renderBackpack(GuiGraphicsExtractor graphics, Font font, ItemStack backpack) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        int energy = WeatherproofChestplateItem.getEnergyStored(backpack);
        int percent = Math.round((float) energy / WeatherproofChestplateItem.MAX_ENERGY * 100);
        int color = 0xFFFFFFFF;
        graphics.item(backpack, 0, 0);

        pose.translate(20, 4);
        Component text = Component.translatable("hud.anvilcraft.weatherproof_chestplate_power", percent);
        int textWidth = font.width(text);
        graphics.text(font, text, 0, 0, color, true);

        final int batteryHeight = (int) (percent / 100F * 16);

        pose.translate(textWidth + 4, -4);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            WeatherproofChestplateHUD.BATTERY_EMPTY,
            0,
            0,
            0,
            0,
            8,
            16,
            8,
            16
        );

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            WeatherproofChestplateHUD.BATTERY_FULL,
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

    private static void renderCapacitorCounts(
        GuiGraphicsExtractor graphics,
        Font font,
        int capacitorCount,
        int superCapacitorCount
    ) {
        graphics.item(ModItems.CAPACITOR.asStack(), 0, 0);
        graphics.text(font, Component.literal("x " + capacitorCount), 20, 4, 0xFFFFFFFF, true);

        graphics.item(ModItems.SUPER_CAPACITOR.asStack(), WeatherproofChestplateHUD.SUPER_CAPACITOR_X, 0);
        graphics.text(
            font,
            Component.literal("x " + superCapacitorCount),
            WeatherproofChestplateHUD.SUPER_CAPACITOR_X + 20,
            4,
            0xFFFFFFFF,
            true
        );
    }

    private static int count(java.util.List<ItemStack> inventory, ItemStack item) {
        return inventory.stream()
            .filter(stack -> stack.is(item.getItem()))
            .mapToInt(ItemStack::getCount)
            .sum();
    }
}
