package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.gui.screen.MultiphaseScreen;
import dev.dubhe.anvilcraft.client.gui.screen.MultitoolScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ResonatorScreen;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.support.AmuletSelectorSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import dev.dubhe.anvilcraft.network.UsePillBoxPacket;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientEventListener {
    @SubscribeEvent
    public static void blockHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
            level,
            Minecraft.getInstance().renderBuffers().bufferSource(),
            event.getPoseStack(),
            event.getCamera()
        );
    }

    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
            && (event.getBlockState().is(ModBlocks.ACCELERATION_RING) || event.getBlockState().is(ModBlocks.DEFLECTION_RING))
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        SoundHelper.INSTANCE.clear();
    }

    private static int lastSwitchPhasePressAction = 0;

    @SubscribeEvent
    public static void onKeyPress(Key event) {
        if (ModKeyMappings.TOGGLE_GOGGLE.get().isDown()) AnvilHammerItem.goggleEnabled = !AnvilHammerItem.goggleEnabled;
        if (Minecraft.getInstance().level == null) return;

        // 以下是界面部分

        switchPhase:
        if (event.getKey() == ModKeyMappings.SWITCH_PHASE.get().getKey().getValue()) {
            if (event.getAction() == InputConstants.REPEAT
                && Minecraft.getInstance().screen == null
            ) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                ItemStack stack = player.getMainHandItem();
                if (stack.has(ModComponents.MULTIPHASE) && !stack.get(ModComponents.MULTIPHASE).isEmpty()) {
                    Minecraft.getInstance().setScreen(new MultiphaseScreen(InteractionHand.MAIN_HAND));
                    return;
                }
                stack = player.getOffhandItem();
                if (stack.has(ModComponents.MULTIPHASE) && !stack.get(ModComponents.MULTIPHASE).isEmpty()) {
                    Minecraft.getInstance().setScreen(new MultiphaseScreen(InteractionHand.OFF_HAND));
                }
            }
            if (event.getAction() != InputConstants.RELEASE) {
                lastSwitchPhasePressAction = event.getAction();
                break switchPhase;
            }
            if (lastSwitchPhasePressAction == InputConstants.PRESS) {
                PacketDistributor.sendToServer(new MultiphasePackets.SwitchPhase());
            } else if (
                lastSwitchPhasePressAction == InputConstants.REPEAT
                && Minecraft.getInstance().screen instanceof MultiphaseScreen screen
            ) {
                screen.wheel.onClosing();
            }
        }

        if (event.getKey() == ModKeyMappings.SWITCH_RESONATE_MODE.get().getKey().getValue()) {
            if (event.getAction() == InputConstants.PRESS && Minecraft.getInstance().screen == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof ResonatorItem) {
                    Minecraft.getInstance().setScreen(new ResonatorScreen(
                        InteractionHand.MAIN_HAND,
                        ResonatorItem.getMode(stack)
                    ));
                    return;
                }
                stack = player.getOffhandItem();
                if (stack.getItem() instanceof ResonatorItem) {
                    Minecraft.getInstance().setScreen(new ResonatorScreen(
                        InteractionHand.OFF_HAND,
                        ResonatorItem.getMode(stack)
                    ));
                }
            } else if (event.getAction() == InputConstants.RELEASE && Minecraft.getInstance().screen instanceof ResonatorScreen screen) {
                screen.wheel.onClosing();
            }
        }

        if (event.getKey() == ModKeyMappings.SWITCH_RESONATE_MODE.get().getKey().getValue()) {
            if (event.getAction() == InputConstants.PRESS && Minecraft.getInstance().screen == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) {
                    return;
                }
                ItemStack stack = player.getMainHandItem();
                if (stack.is(ModItems.MULTITOOL_ITEM)) {
                    Minecraft.getInstance().setScreen(new MultitoolScreen(InteractionHand.MAIN_HAND, MultitoolItem.getMode(stack)));
                }
            } else if (event.getAction() == InputConstants.RELEASE && Minecraft.getInstance().screen instanceof MultitoolScreen screen) {
                screen.getWheel().onClosing();
            }
        }

        if (event.getKey() == ModKeyMappings.USE_PILL_BOX.get().getKey().getValue()) {
            if (event.getAction() == InputConstants.PRESS) {
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.send(new UsePillBoxPacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (AmuletSelectorSupport.hasHoveringItem()) {
            int amount = (int) event.getScrollDeltaY();
            AmuletSelectorSupport.mouseScrolled(-amount);
            event.setCanceled(true);
        } else if (AnvilCraftClient.pillSelectorSupport.hasItem()) {
            int amount = (int) event.getScrollDeltaY();
            AnvilCraftClient.pillSelectorSupport.mouseScrolled(-amount);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderContainerScreenEvent(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        Slot slot = screen.getSlotUnderMouse();
        if (slot != null) {
            ItemStack item = slot.getItem();
            if (item.is(ModItems.PILL_BOX)) {
                AnvilCraftClient.pillSelectorSupport.setPillBox(item);
                return;
            }
        }
        AnvilCraftClient.pillSelectorSupport.setPillBox(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        GuiGraphics guiGraphics = event.getGraphics();
        int x = event.getX();
        int y = event.getY();

        ItemStack itemStack = event.getItemStack();
        if (itemStack.is(ModItems.AMULET_BOX)) {
            event.setY(y + 13);
            AmuletSelectorSupport.setCurrentHoveringItemStack(itemStack);
            AmuletSelectorSupport.render(guiGraphics, x, y);
        } else if (itemStack.is(ModItems.PILL_BOX)) {
            event.setY(y + 13);
            AnvilCraftClient.pillSelectorSupport.render(guiGraphics, x, y);
        } else {
            AmuletSelectorSupport.setCurrentHoveringItemStack(ItemStack.EMPTY);
        }
    }
}
