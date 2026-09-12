package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.item.TerminalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class TerminalKeyMappingEventListener {
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (Minecraft.getInstance().screen == null && event.getAction() == InputConstants.PRESS) {
            TerminalKeyMappingEventListener.tryOpenTerminal(InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    @SubscribeEvent
    public static void onMousePressed(InputEvent.MouseButton.Post event) {
        if (Minecraft.getInstance().screen == null && event.getAction() == InputConstants.PRESS) {
            TerminalKeyMappingEventListener.tryOpenTerminal(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (!TerminalKeyMappingEventListener.isInventoryScreen(event.getScreen())
            || !ModKeyMappings.OPEN_TERMINAL.get().isActiveAndMatches(key)
            || event.getScreen().children().stream().anyMatch(child -> child instanceof EditBox editBox && editBox.isFocused())) {
            return;
        }
        if (event.getScreen() instanceof InventoryScreen inventoryScreen
            && inventoryScreen.getRecipeBookComponent().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
            return;
        }
        if (TerminalKeyMappingEventListener.tryOpenTerminal(key)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (TerminalKeyMappingEventListener.isInventoryScreen(event.getScreen())
            && TerminalKeyMappingEventListener.tryOpenTerminal(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            event.setCanceled(true);
        }
    }

    private static boolean isInventoryScreen(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    private static boolean tryOpenTerminal(InputConstants.Key key) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ModKeyMappings.OPEN_TERMINAL.get().isActiveAndMatches(key)
            || minecraft.player == null
            || minecraft.getConnection() == null
            || !minecraft.player.containerMenu.getCarried().isEmpty()) {
            return false;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof TerminalItem terminal) {
                terminal.openStorage(minecraft.player, stack);
                return true;
            }
        }
        return false;
    }
}
