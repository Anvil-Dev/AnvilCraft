package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.support.TerminalRemoteOverlay;
import dev.dubhe.anvilcraft.item.TerminalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class TerminalOverlayEventListener {
    private static boolean inserting;
    private static boolean creativePending;
    private static long generation;
    private static int consumedButton = -1;

    private TerminalOverlayEventListener() {
    }

    private static boolean eligible(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> && !(screen instanceof StorageScreen);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (creativePending || eligible(event.getScreen()) && TerminalRemoteOverlay.keyPressed(event.getKeyEvent())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCharacter(ScreenEvent.CharacterTyped.Pre event) {
        if (creativePending || eligible(event.getScreen()) && TerminalRemoteOverlay.charTyped(event.getCharacterEvent())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (creativePending) {
            event.setCanceled(true);
            return;
        }
        if (eligible(event.getScreen()) && event.getScrollDeltaY() != 0
            && TerminalRemoteOverlay.mouseScrolled((int) Math.signum(event.getScrollDeltaY()))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onContainer(ContainerScreenEvent.Render.Foreground event) {
        if (!eligible(event.getContainerScreen())) return;
        Slot slot = event.getContainerScreen().getHoveredSlot();
        ItemStack item = slot == null ? ItemStack.EMPTY : slot.getItem();
        boolean terminal = TerminalRemoteOverlay.isBoundTerminal(item);
        if (terminal && !TerminalRemoteOverlay.isDismissed()) {
            TerminalRemoteOverlay.setHovering(item);
            TerminalRemoteOverlay.updateForTooltip(event.getMouseX(), event.getMouseY());
        } else if (!terminal) {
            TerminalRemoteOverlay.setDismissed(false);
            TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (TerminalRemoteOverlay.isHovering()) TerminalRemoteOverlay.updateForTooltip(event.getX(), event.getY());
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!eligible(event.getScreen())) return;
        TerminalRemoteOverlay.tick();
        TerminalRemoteOverlay.render(event.getGuiGraphics(), Minecraft.getInstance().font, event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!eligible(event.getScreen())) return;
        if (creativePending) {
            consumedButton = event.getButton();
            event.setCanceled(true);
            return;
        }
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;
        var screen = (AbstractContainerScreen<?>) event.getScreen();
        Slot slot = findSlot(screen, event.getMouseX(), event.getMouseY());
        if (slot == null) return;
        if (!TerminalRemoteOverlay.isBoundTerminal(slot.getItem())) {
            if (creativeTransfer(screen, slot, event.getButton())) {
                consumedButton = event.getButton();
                event.setCanceled(true);
            }
            return;
        }
        var carried = screen.getMenu().getCarried();
        if (carried.isEmpty()) {
            if (TerminalRemoteOverlay.isDismissed()
                || TerminalRemoteOverlay.mouseClicked(event.getMouseButtonEvent(), event.isDoubleClick())) {
                consumedButton = event.getButton();
                event.setCanceled(true);
            }
            return;
        }
        var key = InvertedActionEventListener.isInverted() ? client.options.keyAttack : client.options.keyUse;
        if (!inserting && key.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            var target = TerminalRemoteOverlay.terminalIdOf(slot.getItem());
            if (target != null) {
                inserting = true;
                final long requestGeneration = generation;
                final var actor = client.player;
                StorageTerminalClientStub.insert(target, carried.copy()).whenCompleteAsync((result, error) -> {
                    if (requestGeneration != generation) return;
                    inserting = false;
                    if (error == null && result.changed() && client.screen == screen && client.player == actor) {
                        TerminalRemoteOverlay.applyCarriedIfCreative(result.carried());
                    }
                }, client);
            }
        }
        consumedButton = event.getButton();
        event.setCanceled(true);
    }

    private static boolean creativeTransfer(AbstractContainerScreen<?> screen, Slot slot, int button) {
        var client = Minecraft.getInstance();
        if (!(screen instanceof CreativeModeInventoryScreen) || slot.container != client.player.getInventory()
            || !slot.isActive() || !slot.allowModification(client.player)) return false;
        ItemStack carried = screen.getMenu().getCarried();
        if (!(carried.getItem() instanceof TerminalItem terminal)) return false;
        boolean extract = slot.getItem().isEmpty();
        if (button != (extract || !InvertedActionEventListener.isInverted() ? 1 : 0)) return false;
        var target = terminal.targetId(client.player, carried);
        if (target == null) return false;
        int inventorySlot = slot.getSlotIndex();
        int menuSlot = -1;
        for (Slot candidate : client.player.inventoryMenu.slots) {
            if (candidate.container == client.player.getInventory() && candidate.getContainerSlot() == inventorySlot) {
                menuSlot = candidate.index;
                break;
            }
        }
        if (menuSlot < 0) return false;
        final ItemStack terminalSnapshot = carried.copy();
        final var actor = client.player;
        final long requestGeneration = generation;
        creativePending = true;
        StorageTerminalClientStub.creativeTransfer(target, menuSlot, extract, slot.getItem().copy(), terminalSnapshot)
            .whenCompleteAsync((result, error) -> {
                if (requestGeneration != generation) return;
                creativePending = false;
                if (error != null || client.screen != screen || client.player != actor) return;
                if (ItemStack.matches(screen.getMenu().getCarried(), terminalSnapshot)) screen.getMenu().setCarried(result.carried());
                if (result.changed() || !extract) playTerminalSound(terminal, extract);
            }, client);
        return true;
    }

    private static void playTerminalSound(TerminalItem terminal, boolean extract) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        SoundEvent sound = switch (terminal.kind()) {
            case HYPERDIMENSION -> SoundEvents.ENDERMAN_TELEPORT;
            case SHULKER -> extract ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE;
            case LOCAL -> extract ? SoundEvents.BUNDLE_REMOVE_ONE : SoundEvents.BUNDLE_INSERT;
        };
        player.playSound(sound, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrag(ScreenEvent.MouseDragged.Pre event) {
        if (creativePending) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == consumedButton) {
            consumedButton = -1;
            event.setCanceled(true);
        }
    }

    private static @Nullable Slot findSlot(AbstractContainerScreen<?> screen, double x, double y) {
        for (Slot slot : screen.getMenu().slots) {
            int left = screen.getLeftPos() + slot.x;
            int top = screen.getTopPos() + slot.y;
            if (slot.isActive() && x >= left - 1 && x < left + 17 && y >= top - 1 && y < top + 17) return slot;
        }
        return null;
    }

    @SubscribeEvent
    public static void onClose(ScreenEvent.Closing event) {
        clear();
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        generation++;
        inserting = false;
        creativePending = false;
        consumedButton = -1;
        TerminalRemoteOverlay.reset();
        TerminalRemoteOverlay.setDismissed(false);
    }
}
