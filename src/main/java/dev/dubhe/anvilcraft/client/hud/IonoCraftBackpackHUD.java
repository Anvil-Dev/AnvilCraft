package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public class IonoCraftBackpackHUD {
    private static final int ROW_HEIGHT = 20;
    private static final int SUPER_CAPACITOR_X = 64;
    private static final Identifier BATTERY_EMPTY = SharedTextures.textureGui("misc/battery_display/battery_empty");
    private static final Identifier BATTERY_FULL = SharedTextures.textureGui("misc/battery_display/battery_full");
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        var config = AnvilCraftClient.CONFIG.ionoCraftBackpackHud;
        if (!config.enabled && !config.capacitorCountEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack backpack = IonoCraftBackpackItem.getByPlayer(player);
        boolean renderBackpack = config.enabled && backpack.is(ModItems.IONOCRAFT_BACKPACK);
        Inventory inventory = player.getInventory();
        int capacitorCount = IonoCraftBackpackHUD.count(inventory, ModItems.CAPACITOR.asStack());
        int superCapacitorCount = IonoCraftBackpackHUD.count(inventory, ModItems.SUPER_CAPACITOR.asStack());
        boolean renderCapacitors = config.capacitorCountEnabled && (capacitorCount > 0 || superCapacitorCount > 0);
        if (!renderBackpack && !renderCapacitors) return;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();

        pose.scale(config.hudScale, config.hudScale);
        pose.translate(config.hudX, config.hudY);
        if (renderBackpack) {
            IonoCraftBackpackHUD.renderBackpack(graphics, mc.font, backpack);
            pose.translate(0, IonoCraftBackpackHUD.ROW_HEIGHT);
        }
        if (renderCapacitors) {
            IonoCraftBackpackHUD.renderCapacitorCounts(graphics, mc.font, capacitorCount, superCapacitorCount);
        }

        pose.popMatrix();
    }

    private static void renderBackpack(GuiGraphicsExtractor graphics, Font font, ItemStack backpack) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        int energy = IonoCraftBackpackItem.getEnergyStored(backpack);
        int percent = Math.round((float) energy / IonoCraftBackpackItem.MAX_ENERGY * 100);
        float ratio = Math.clamp((float) energy / IonoCraftBackpackItem.MAX_ENERGY, 0, 1);
        int color = ColorUtil.lerpColor(ratio, IonoCraftBackpackHUD.BAR_COLOR, IonoCraftBackpackHUD.FULL_BAR_COLOR);
        graphics.item(backpack, 0, 0);

        pose.translate(20, 4);
        Component text = Component.translatable("hud.anvilcraft.ionocraft_backpack_power", percent);
        int textWidth = font.width(text);
        graphics.text(font, text, 0, 0, color, true);

        final int batteryHeight = (int) (percent / 100F * 16);

        pose.translate(textWidth + 4, -4);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            IonoCraftBackpackHUD.BATTERY_EMPTY,
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
            IonoCraftBackpackHUD.BATTERY_FULL,
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

        graphics.item(ModItems.SUPER_CAPACITOR.asStack(), IonoCraftBackpackHUD.SUPER_CAPACITOR_X, 0);
        graphics.text(
            font,
            Component.literal("x " + superCapacitorCount),
            IonoCraftBackpackHUD.SUPER_CAPACITOR_X + 20,
            4,
            0xFFFFFFFF,
            true
        );
    }

    private static int count(Inventory inventory, ItemStack item) {
        return inventory.getNonEquipmentItems().stream()
            .filter(stack -> ItemStack.isSameItemSameComponents(stack, item))
            .mapToInt(ItemStack::getCount)
            .sum();
    }
}
