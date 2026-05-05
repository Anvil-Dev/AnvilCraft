package dev.dubhe.anvilcraft.client.event;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.network.SwitchMultitoolModePacket;
import dev.dubhe.anvilcraft.network.SwitchResonateModePacket;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nullable;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class WheelLifecycleEventListener {
    private static final WheelScreenController CONTROLLER = new WheelScreenController();

    private static long multiphaseKeyTime = -1L;
    private static boolean multiphaseKeyWasDown = false;
    private static Optional<WheelMenuModel> multiphaseWheelCache = null;

    private static long resonatorKeyTime = -1L;
    private static boolean resonatorKeyWasDown = false;
    private static Optional<WheelMenuModel> resonatorWheelCache = null;

    private static long multitoolKeyTime = -1L;
    private static boolean multitoolKeyWasDown = false;
    private static Optional<WheelMenuModel> multitoolWheelCache = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        long gameTime = level.getGameTime();
        WheelLifecycleEventListener.openMultiphaseWheel(gameTime);
        WheelLifecycleEventListener.openResonatorWheel(gameTime);
        WheelLifecycleEventListener.openMultitoolWheel(gameTime);
    }

    private static void openMultiphaseWheel(long gameTime) {
        if (
            WheelLifecycleEventListener.multiphaseKeyTime > 0
            && gameTime - WheelLifecycleEventListener.multiphaseKeyTime > 4
        ) {
            if (WheelLifecycleEventListener.multiphaseWheelCache == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                InteractionHand hand = InteractionHand.MAIN_HAND;
                ItemStack stack = player.getMainHandItem();
                if (!stack.has(ModComponents.MULTIPHASE)) {
                    hand = InteractionHand.OFF_HAND;
                    stack = player.getOffhandItem();
                }
                if (!stack.has(ModComponents.MULTIPHASE)) return;
                MultiphaseRef ref = stack.get(ModComponents.MULTIPHASE);
                if (ref.isEmpty()) return;
                PacketDistributor.sendToServer(new MultiphasePackets.SingleSync(stack.get(ModComponents.MULTIPHASE).id().get()));
                WheelLifecycleEventListener.multiphaseWheelCache = Optional.ofNullable(
                    WheelLifecycleEventListener.getMultiphaseWheel(hand, stack, ref.toMultiphase())
                );
            }
            if (WheelLifecycleEventListener.multiphaseWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.multiphaseWheelCache.get());
            WheelLifecycleEventListener.multiphaseKeyWasDown = true;
        }
    }

    private static void openResonatorWheel(long gameTime) {
        if (
            WheelLifecycleEventListener.resonatorKeyTime > 0
            && gameTime - WheelLifecycleEventListener.resonatorKeyTime > 4
        ) {
            if (WheelLifecycleEventListener.resonatorWheelCache == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                InteractionHand hand = InteractionHand.MAIN_HAND;
                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof ResonatorItem)) {
                    hand = InteractionHand.OFF_HAND;
                    stack = player.getOffhandItem();
                }
                if (!(stack.getItem() instanceof ResonatorItem)) return;
                WheelLifecycleEventListener.resonatorWheelCache = Optional.ofNullable(
                    WheelLifecycleEventListener.getResonatorWheel(hand, stack)
                );
            }
            if (WheelLifecycleEventListener.resonatorWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.resonatorWheelCache.get());
            WheelLifecycleEventListener.resonatorKeyWasDown = true;
        }
    }

    private static void openMultitoolWheel(long gameTime) {
        if (
            WheelLifecycleEventListener.multitoolKeyTime > 0
            && gameTime - WheelLifecycleEventListener.multitoolKeyTime > 4
        ) {
            if (WheelLifecycleEventListener.multitoolWheelCache == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                InteractionHand hand = InteractionHand.MAIN_HAND;
                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof MultitoolItem)) {
                    hand = InteractionHand.OFF_HAND;
                    stack = player.getOffhandItem();
                }
                if (!(stack.getItem() instanceof MultitoolItem)) return;
                WheelLifecycleEventListener.multitoolWheelCache = Optional.ofNullable(
                    WheelLifecycleEventListener.getMultitoolWheel(hand, stack)
                );
            }
            if (WheelLifecycleEventListener.multitoolWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.multitoolWheelCache.get());
            WheelLifecycleEventListener.multitoolKeyWasDown = true;
        }
    }

    private static @Nullable WheelMenuModel getMultiphaseWheel(InteractionHand hand, ItemStack holding, Multiphase multiphase) {
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(multiphase.phases().size());
        multiphase.phases().stream()
            .sorted(Comparator.comparingInt(Multiphase.Phase::index))
            .forEachOrdered(phase -> builder.action(
                "" + Multiphase.DEFAULT_SUFFIXES.charAt(phase.index()),
                phase.phaseName(),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    phase.applyToStack(copied);
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new MultiphasePackets.ChangePhase(hand, ctx.slotIndex())
                )
            ));
        return builder.build();
    }

    private static @Nullable WheelMenuModel getResonatorWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(5)
            .action(
                "auto",
                Component.translatable("screen.anvilcraft.resonator.auto"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ResonatorItem.AUTO_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "axe",
                Component.translatable("screen.anvilcraft.resonator.axe"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ResonatorItem.AXE_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "shovel",
                Component.translatable("screen.anvilcraft.resonator.shovel"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ResonatorItem.SHOVEL_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "hoe",
                Component.translatable("screen.anvilcraft.resonator.hoe"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ResonatorItem.HOE_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "pickaxe",
                Component.translatable("screen.anvilcraft.resonator.pickaxe"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ResonatorItem.PICKAXE_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .build();
    }

    private static @Nullable WheelMenuModel getMultitoolWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(9)
            .action(
                "all",
                Component.translatable("screen.anvilcraft.multitool.all"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.ALL_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "shears",
                Component.translatable("item.minecraft.shears"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.SHEARS_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "flint_and_steel",
                Component.translatable("item.minecraft.flint_and_steel"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.FLINT_AND_STEEL_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "brush",
                Component.translatable("item.minecraft.brush"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.BRUSH_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "spyglass",
                Component.translatable("item.minecraft.spyglass"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.SPYGLASS_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "magnet",
                Component.translatable("item.anvilcraft.magnet"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.MAGNET_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "fishing_rod",
                Component.translatable("item.minecraft.fishing_rod"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.FISHING_ROD_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "carrot_on_a_stick",
                Component.translatable("item.minecraft.carrot_on_a_stick"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.CARROT_ON_A_STICK_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "warped_fungus_on_a_stick",
                Component.translatable("item.minecraft.warped_fungus_on_a_stick"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.WARPED_FUNGUS_ON_A_STICK_MODE));
                    graphics.renderItem(copied, 2, 2, 9910597);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .build();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (ModKeyMappings.SWITCH_PHASE.get().matches(event.getKey(), event.getScanCode())) {
            WheelLifecycleEventListener.processMultiphasePress(client, event.getAction());
        }
        if (ModKeyMappings.SWITCH_TOOL_MODE.get().matches(event.getKey(), event.getScanCode())) {
            WheelLifecycleEventListener.processResonatorPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.MouseButton.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (ModKeyMappings.SWITCH_PHASE.get().matchesMouse(event.getButton())) {
            WheelLifecycleEventListener.processMultiphasePress(client, event.getAction());
        }
        if (ModKeyMappings.SWITCH_TOOL_MODE.get().matchesMouse(event.getButton())) {
            WheelLifecycleEventListener.processResonatorPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
        }
    }

    private static void processMultiphasePress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.multiphaseKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            } else {
                PacketDistributor.sendToServer(new MultiphasePackets.SwitchPhase());
            }
            WheelLifecycleEventListener.multiphaseKeyWasDown = false;
            WheelLifecycleEventListener.multiphaseKeyTime = -1L;
            WheelLifecycleEventListener.multiphaseWheelCache = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.multiphaseKeyWasDown) {
                WheelLifecycleEventListener.multiphaseKeyTime = client.level.getGameTime();
            }
        }
    }

    private static void processResonatorPress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.resonatorKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            }
            WheelLifecycleEventListener.resonatorKeyWasDown = false;
            WheelLifecycleEventListener.resonatorKeyTime = -1L;
            WheelLifecycleEventListener.resonatorWheelCache = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.resonatorKeyWasDown) {
                WheelLifecycleEventListener.resonatorKeyTime = client.level.getGameTime();
            }
        }
    }

    private static void processMultitoolPress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.multitoolKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            }
            WheelLifecycleEventListener.multitoolKeyWasDown = false;
            WheelLifecycleEventListener.multitoolKeyTime = -1L;
            WheelLifecycleEventListener.multitoolWheelCache = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.multitoolKeyWasDown) {
                WheelLifecycleEventListener.multitoolKeyTime = client.level.getGameTime();
            }
        }
    }
}
