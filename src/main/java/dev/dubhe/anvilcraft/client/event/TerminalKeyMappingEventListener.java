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
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class TerminalKeyMappingEventListener {
    private TerminalKeyMappingEventListener() {
    }

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (Minecraft.getInstance().screen == null && event.getAction() == InputConstants.PRESS) {
            tryOpenTerminal(InputConstants.getKey(event.getKeyEvent()));
        }
    }

    @SubscribeEvent
    public static void onMousePressed(InputEvent.MouseButton.Post event) {
        if (Minecraft.getInstance().screen == null && event.getAction() == InputConstants.PRESS) {
            tryOpenTerminal(InputConstants.Type.MOUSE.getOrCreate(event.getMouseButtonInfo().button()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
        if (!isInventoryScreen(event.getScreen()) || !ModKeyMappings.OPEN_TERMINAL.get().isActiveAndMatches(key)
            || event.getScreen().children().stream()
                .anyMatch(child -> child instanceof EditBox edit && edit.isVisible() && edit.isFocused())) {
            return;
        }
        if (event.getScreen() instanceof InventoryScreen) {
            for (var child : event.getScreen().children()) {
                if (child instanceof RecipeBookComponent<?> book && book.keyPressed(event.getKeyEvent())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (tryOpenTerminal(key)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (isInventoryScreen(event.getScreen())
            && tryOpenTerminal(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) event.setCanceled(true);
    }

    private static boolean isInventoryScreen(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    private static boolean tryOpenTerminal(InputConstants.Key key) {
        var client = Minecraft.getInstance();
        if (!ModKeyMappings.OPEN_TERMINAL.get().isActiveAndMatches(key) || client.player == null
            || client.getConnection() == null || !client.player.containerMenu.getCarried().isEmpty()) return false;
        for (ItemStack stack : TerminalItem.getAll(client.player)) {
            if (stack.getItem() instanceof TerminalItem terminal) {
                terminal.openStorage(client.player, stack);
                return true;
            }
        }
        return false;
    }
}
