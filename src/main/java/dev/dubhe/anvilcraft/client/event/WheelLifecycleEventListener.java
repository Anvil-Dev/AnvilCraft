package dev.dubhe.anvilcraft.client.event;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.renderer.item.ItemSlotClipping;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.DragonRodItem;
import dev.dubhe.anvilcraft.item.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.network.SwitchDragonRodProtectContainersPacket;
import dev.dubhe.anvilcraft.network.SwitchHeavyHalberdModePacket;
import dev.dubhe.anvilcraft.network.SwitchMultitoolModePacket;
import dev.dubhe.anvilcraft.network.SwitchResonateModePacket;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.setting.mode.BalanceMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import javax.annotation.Nullable;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class WheelLifecycleEventListener {
    private static final WheelScreenController CONTROLLER = new WheelScreenController();

    private static long multiphaseKeyTime = -1L;
    private static boolean multiphaseKeyWasDown = false;
    private static boolean multiphaseKeyPressAccepted = false;
    private static @Nullable Optional<WheelMenuModel> multiphaseWheelCache = null;

    private static long resonatorKeyTime = -1L;
    private static boolean resonatorKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> resonatorWheelCache = null;

    private static long heavyHalberdKeyTime = -1L;
    private static boolean heavyHalberdKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> heavyHalberdWheelCache = null;

    private static long multitoolKeyTime = -1L;
    private static boolean multitoolKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> multitoolWheelCache = null;

    private static long dragonRodKeyTime = -1L;
    private static boolean dragonRodKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> dragonRodWheelCache = null;

    private static long balanceKeyTime = -1L;
    private static boolean balanceKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> balanceWheelCache = null;

    private static void renderWheelItem(GuiGraphics graphics, ItemStack stack) {
        ItemSlotClipping.runWithoutClip(() -> graphics.renderItem(stack, -8, -8));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        long gameTime = level.getGameTime();
        WheelLifecycleEventListener.openMultiphaseWheel(gameTime);
        WheelLifecycleEventListener.openResonatorWheel(gameTime);
        WheelLifecycleEventListener.openHeavyHalberdWheel(gameTime);
        WheelLifecycleEventListener.openMultitoolWheel(gameTime);
        WheelLifecycleEventListener.openDragonRodWheel(gameTime);
        WheelLifecycleEventListener.openBalanceWheel(gameTime);
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
                Multiphase multiphase = stack.get(ModComponents.MULTIPHASE);
                if (multiphase == null) return;
                multiphase = multiphase.forDisplay(stack);
                WheelLifecycleEventListener.multiphaseWheelCache = Optional.of(
                    WheelLifecycleEventListener.getMultiphaseWheel(hand, stack, multiphase)
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
                WheelLifecycleEventListener.resonatorWheelCache = Optional.of(
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
                WheelLifecycleEventListener.multitoolWheelCache = Optional.of(
                    WheelLifecycleEventListener.getMultitoolWheel(hand, stack)
                );
            }
            if (WheelLifecycleEventListener.multitoolWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.multitoolWheelCache.get());
            WheelLifecycleEventListener.multitoolKeyWasDown = true;
        }
    }

    private static void openHeavyHalberdWheel(long gameTime) {
        if (
            WheelLifecycleEventListener.heavyHalberdKeyTime > 0
            && gameTime - WheelLifecycleEventListener.heavyHalberdKeyTime > 4
        ) {
            if (WheelLifecycleEventListener.heavyHalberdWheelCache == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                InteractionHand hand = InteractionHand.MAIN_HAND;
                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof HeavyHalberdItem)) {
                    hand = InteractionHand.OFF_HAND;
                    stack = player.getOffhandItem();
                }
                if (!(stack.getItem() instanceof HeavyHalberdItem)) return;
                WheelLifecycleEventListener.heavyHalberdWheelCache = Optional.of(
                    WheelLifecycleEventListener.getHeavyHalberdWheel(hand, stack)
                );
            }
            if (WheelLifecycleEventListener.heavyHalberdWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.heavyHalberdWheelCache.get());
            WheelLifecycleEventListener.heavyHalberdKeyWasDown = true;
        }
    }

    private static void openDragonRodWheel(long gameTime) {
        if (
            WheelLifecycleEventListener.dragonRodKeyTime > 0
            && gameTime - WheelLifecycleEventListener.dragonRodKeyTime > 4
        ) {
            if (WheelLifecycleEventListener.dragonRodWheelCache == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                InteractionHand hand = InteractionHand.MAIN_HAND;
                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof DragonRodItem)) {
                    hand = InteractionHand.OFF_HAND;
                    stack = player.getOffhandItem();
                }
                if (!(stack.getItem() instanceof DragonRodItem)) return;
                WheelLifecycleEventListener.dragonRodWheelCache = Optional.of(
                    WheelLifecycleEventListener.getDragonRodWheel(hand, stack)
                );
            }
            if (WheelLifecycleEventListener.dragonRodWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.dragonRodWheelCache.get());
            WheelLifecycleEventListener.dragonRodKeyWasDown = true;
        }
    }

    private static void openBalanceWheel(long gameTime) {
        if (
            WheelLifecycleEventListener.balanceKeyTime > 0
            && gameTime - WheelLifecycleEventListener.balanceKeyTime > 4
        ) {
            if (WheelLifecycleEventListener.balanceWheelCache == null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return;
                if (!WheelLifecycleEventListener.holdsBoundTerminal(player)) return;
                WheelLifecycleEventListener.balanceWheelCache = Optional.of(
                    WheelLifecycleEventListener.getBalanceWheel()
                );
            }
            if (WheelLifecycleEventListener.balanceWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.balanceWheelCache.get());
            WheelLifecycleEventListener.balanceKeyWasDown = true;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean holdsBoundTerminal(LocalPlayer player) {
        return WheelLifecycleEventListener.isBoundTerminal(player.getMainHandItem())
               || WheelLifecycleEventListener.isBoundTerminal(player.getOffhandItem());
    }

    private static boolean isBoundTerminal(ItemStack stack) {
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            return false;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        return binding != null && binding.id().isPresent();
    }

    private static WheelMenuModel getMultiphaseWheel(InteractionHand hand, ItemStack holding, Multiphase multiphase) {
        int phaseCount = multiphase.phases().size();
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(phaseCount);
        for (int i = 0; i < phaseCount; i++) {
            addMultiphaseWheelEntry(builder, hand, holding, multiphase, i);
        }
        return builder.build();
    }

    private static void addMultiphaseWheelEntry(
        WheelMenuBuilder builder,
        InteractionHand hand,
        ItemStack holding,
        Multiphase multiphase,
        int phaseIndex
    ) {
        builder.action(
            "phase_" + phaseIndex,
            multiphase.phaseDisplayName(phaseIndex),
            (graphics, pose, width, height) -> {
                ItemStack copied = holding.copy();
                multiphase.applySelectionPreview(copied, phaseIndex);
                renderWheelItem(graphics, copied);
            },
            ctx -> PacketDistributor.sendToServer(
                new MultiphasePackets.ChangePhase(hand, phaseIndex)
            )
        );
    }

    private static WheelMenuModel getResonatorWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(5)
            .action(
                "auto",
                Component.translatable("screen.anvilcraft.resonator.auto"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(ResonatorItem.AUTO_MODE));
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .build();
    }

    private static WheelMenuModel getHeavyHalberdWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(4)
            .action(
                "trident",
                Component.translatable("screen.anvilcraft.heavy_halberd.trident"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HeavyHalberdItem.TRIDENT_MODE));
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "spear",
                Component.translatable("screen.anvilcraft.heavy_halberd.spear"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HeavyHalberdItem.SPEAR_MODE));
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "sword",
                Component.translatable("screen.anvilcraft.heavy_halberd.sword"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HeavyHalberdItem.SWORD_MODE));
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "mace",
                Component.translatable("screen.anvilcraft.heavy_halberd.mace"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HeavyHalberdItem.MACE_MODE));
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, ctx.slotIndex())
                )
            )
            .build();
    }

    private static WheelMenuModel getMultitoolWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(9)
            .action(
                "all",
                Component.translatable("screen.anvilcraft.multitool.all"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    copied.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MultitoolItem.ALL_MODE));
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
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
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, ctx.slotIndex())
                )
            )
            .build();
    }

    private static WheelMenuModel getDragonRodWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(2)
            .action(
                "protect",
                Component.translatable("screen.anvilcraft.dragon_rod.protect_containers"),
                (graphics, pose, width, height) -> renderWheelItem(graphics, new ItemStack(Blocks.CHEST)),
                ctx -> PacketDistributor.sendToServer(
                    new SwitchDragonRodProtectContainersPacket(hand, true)
                )
            )
            .action(
                "devour",
                Component.translatable("screen.anvilcraft.dragon_rod.devour_containers"),
                (graphics, pose, width, height) -> {
                    ItemStack copied = holding.copy();
                    renderWheelItem(graphics, copied);
                },
                ctx -> PacketDistributor.sendToServer(
                    new SwitchDragonRodProtectContainersPacket(hand, false)
                )
            )
            .build();
    }

    private static WheelMenuModel getBalanceWheel() {
        return WheelMenuBuilder.create()
            .slotsPerPage(4)
            .action(
                "smart",
                Component.translatable("screen.anvilcraft.balance_mode.smart"),
                (graphics, pose, width, height) -> graphics.renderFakeItem(
                    ModItems.HYPERDIMENSION_TERMINAL.asStack(),
                    -8,
                    -8
                ),
                ctx -> SettingClientStub.update(BalanceMode.SMART)
            )
            .action(
                "restock",
                Component.translatable("screen.anvilcraft.balance_mode.restock"),
                (graphics, pose, width, height) -> graphics.renderFakeItem(
                    ModItems.HYPERDIMENSION_TERMINAL.asStack(),
                    -8,
                    -8
                ),
                ctx -> SettingClientStub.update(BalanceMode.RESTOCK)
            )
            .action(
                "off",
                Component.translatable("screen.anvilcraft.balance_mode.off"),
                (graphics, pose, width, height) -> graphics.renderFakeItem(
                    ModItems.HYPERDIMENSION_TERMINAL.asStack(),
                    -8,
                    -8
                ),
                ctx -> SettingClientStub.update(BalanceMode.OFF)
            )
            .action(
                "deposit",
                Component.translatable("screen.anvilcraft.balance_mode.deposit"),
                (graphics, pose, width, height) -> graphics.renderFakeItem(
                    ModItems.HYPERDIMENSION_TERMINAL.asStack(),
                    -8,
                    -8
                ),
                ctx -> SettingClientStub.update(BalanceMode.DEPOSIT)
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
            WheelLifecycleEventListener.processHeavyHalberdPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
            WheelLifecycleEventListener.processDragonRodPress(client, event.getAction());
            WheelLifecycleEventListener.processBalancePress(client, event.getAction());
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
            WheelLifecycleEventListener.processHeavyHalberdPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
            WheelLifecycleEventListener.processDragonRodPress(client, event.getAction());
            WheelLifecycleEventListener.processBalancePress(client, event.getAction());
        }
    }

    private static void processMultiphasePress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (!WheelLifecycleEventListener.multiphaseKeyPressAccepted) {
                return;
            }
            WheelLifecycleEventListener.multiphaseKeyPressAccepted = false;
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
            WheelLifecycleEventListener.multiphaseKeyPressAccepted = true;
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

    private static void processHeavyHalberdPress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.heavyHalberdKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            }
            WheelLifecycleEventListener.heavyHalberdKeyWasDown = false;
            WheelLifecycleEventListener.heavyHalberdKeyTime = -1L;
            WheelLifecycleEventListener.heavyHalberdWheelCache = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.heavyHalberdKeyWasDown) {
                WheelLifecycleEventListener.heavyHalberdKeyTime = client.level.getGameTime();
            }
        }
    }

    private static void processDragonRodPress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.dragonRodKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            }
            WheelLifecycleEventListener.dragonRodKeyWasDown = false;
            WheelLifecycleEventListener.dragonRodKeyTime = -1L;
            WheelLifecycleEventListener.dragonRodWheelCache = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.dragonRodKeyWasDown) {
                WheelLifecycleEventListener.dragonRodKeyTime = client.level.getGameTime();
            }
        }
    }

    private static void processBalancePress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.balanceKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            }
            WheelLifecycleEventListener.balanceKeyWasDown = false;
            WheelLifecycleEventListener.balanceKeyTime = -1L;
            WheelLifecycleEventListener.balanceWheelCache = null;
            return;
        }
        // 只有手持已绑定终端时才呼出物品均衡轮盘，不干扰工具的 Alt 轮盘
        if (client.player == null || !WheelLifecycleEventListener.holdsBoundTerminal(client.player)) {
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.balanceKeyWasDown) {
                WheelLifecycleEventListener.balanceKeyTime = client.level.getGameTime();
            }
        }
    }
}
