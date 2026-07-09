package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.wheel.api.WheelEntryAction;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import dev.dubhe.anvilcraft.network.HammerChangeBlockPacket;
import dev.dubhe.anvilcraft.network.HammerChangeFlexibleMultiPartBlockPacket;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.network.SwitchMultitoolModePacket;
import dev.dubhe.anvilcraft.network.SwitchResonateModePacket;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class WheelLifecycleEventListener {
    private static final WheelScreenController CONTROLLER = new WheelScreenController();

    private static long hammerKeyTime = -1L;
    private static boolean hammerKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> hammerWheelCache = null;

    private static long multiphaseKeyTime = -1L;
    private static boolean multiphaseKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> multiphaseWheelCache = null;

    private static long resonatorKeyTime = -1L;
    private static boolean resonatorKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> resonatorWheelCache = null;

    private static long multitoolKeyTime = -1L;
    private static boolean multitoolKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> multitoolWheelCache = null;

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

    public static boolean openHammerWheel(
        long gameTime,
        Level level,
        BlockPos targetPos,
        InteractionHand hand,
        @Nullable Property<?> property,
        Supplier<List<BlockState>> possibleStatesFac,
        BlockHitResult hitVec
    ) {
        if (WheelLifecycleEventListener.hammerKeyTime <= 0) return false;
        if (property == null) {
            ClientPacketDistributor.sendToServer(new HammerUsePacket(targetPos, hand, hitVec));
            return false;
        }
        if (gameTime - WheelLifecycleEventListener.hammerKeyTime <= 4) {
            ClientPacketDistributor.sendToServer(new HammerUsePacket(targetPos, hand, hitVec));
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return false;
        if (WheelLifecycleEventListener.hammerWheelCache == null) {
            if (player.isShiftKeyDown()) {
                ClientPacketDistributor.sendToServer(new HammerUsePacket(targetPos, hand, hitVec));
                return false;
            }
            if (!player.getAbilities().mayBuild) return false;
            if (!AnvilHammerItem.ableToUseAnvilHammer(level, targetPos, player)) return false;
            List<BlockState> possibleStates = possibleStatesFac.get();
            if (possibleStates.isEmpty()) return true;
            if (client.getCameraEntity() == null) return false;
            WheelLifecycleEventListener.hammerWheelCache = Optional.of(WheelLifecycleEventListener.getHammerWheel(
                targetPos,
                property,
                possibleStates,
                client.getCameraEntity().getRotationVector()
            ));
        }
        if (WheelLifecycleEventListener.hammerWheelCache.isEmpty()) return false;
        CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.hammerWheelCache.get());
        WheelLifecycleEventListener.hammerKeyWasDown = true;
        return true;
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
                if (ref == null || ref.isEmpty()) return;
                if (stack.get(ModComponents.MULTIPHASE) == null) return;
                var component = stack.get(ModComponents.MULTIPHASE);
                if (component == null) return;
                ClientPacketDistributor.sendToServer(new MultiphasePackets.SingleSync(component.id().get()));
                var multiphase = ref.toMultiphase();
                if (multiphase == null) return;
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
        if (WheelLifecycleEventListener.resonatorKeyTime <= 0) return;
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
        if (gameTime - WheelLifecycleEventListener.resonatorKeyTime > 4) {
            if (WheelLifecycleEventListener.resonatorWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.resonatorWheelCache.get());
            WheelLifecycleEventListener.resonatorKeyWasDown = true;
        } else {
            if (WheelLifecycleEventListener.resonatorWheelCache.isEmpty()) return;
            CONTROLLER.openTap(WheelLifecycleEventListener.resonatorWheelCache.get());
        }
    }

    private static void openMultitoolWheel(long gameTime) {
        if (WheelLifecycleEventListener.multitoolKeyTime <= 0) return;
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
        if (gameTime - WheelLifecycleEventListener.multitoolKeyTime > 4) {
            if (WheelLifecycleEventListener.multitoolWheelCache.isEmpty()) return;
            CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.multitoolWheelCache.get());
            WheelLifecycleEventListener.multitoolKeyWasDown = true;
        } else {
            if (WheelLifecycleEventListener.multitoolWheelCache.isEmpty()) return;
            CONTROLLER.openTap(WheelLifecycleEventListener.multitoolWheelCache.get());
        }
    }

    private static WheelMenuModel getHammerWheel(
        BlockPos targetPos,
        Property<?> property,
        List<BlockState> possibleStates,
        Vec2 camera
    ) {
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(possibleStates.size());
        possibleStates
            .forEach(state -> {
                String name = property.getName(Util.cast(state.getValue(property)));
                WheelEntryAction action;
                if (state.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
                    if (state.hasProperty(BlockStateProperties.FACING)) {
                        action = _ -> ClientPacketDistributor.sendToServer(new HammerChangeFlexibleMultiPartBlockPacket(
                            targetPos,
                            state,
                            state.getValue(BlockStateProperties.FACING)
                        ));
                    } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        action = _ -> ClientPacketDistributor.sendToServer(new HammerChangeFlexibleMultiPartBlockPacket(
                            targetPos,
                            state,
                            state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                        ));
                    } else {
                        action = _ -> {
                        };
                    }
                } else {
                    action = _ -> ClientPacketDistributor.sendToServer(new HammerChangeBlockPacket(
                        targetPos,
                        state
                    ));
                }
                builder.action(
                    name,
                    Component.literal(name),
                    (graphics, _, _, _) -> {
                        PoseStack pose = new PoseStack();
                        pose.translate(0, 0, 0);
                        pose.mulPose(Axis.XP.rotationDegrees(camera.x));
                        pose.mulPose(Axis.YP.rotationDegrees(camera.y + 180F));
                        GuiRenderExtras.tessellateBlock(graphics, state, -15f, -5, pose);
                    },
                    action
                );
            });
        return builder.build();
    }

    private static WheelMenuModel getMultiphaseWheel(InteractionHand hand, ItemStack holding, Multiphase multiphase) {
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(multiphase.phases().size());
        multiphase.phases().stream()
            .sorted(Comparator.comparingInt(Multiphase.Phase::index))
            .forEachOrdered(phase -> builder.action(
                "" + Multiphase.DEFAULT_SUFFIXES.charAt(phase.index()),
                phase.phaseName(),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    phase.applyToStack(copied);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new MultiphasePackets.ChangePhase(hand, ctx.slotIndex())
                )
            ));
        return builder.build();
    }

    private static WheelMenuModel getResonatorWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(5)
            .action(
                "auto",
                Component.translatable("screen.anvilcraft.resonator.auto"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.RESONATE_MODE, ResonateMode.AUTO);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "axe",
                Component.translatable("screen.anvilcraft.resonator.axe"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.RESONATE_MODE, ResonateMode.AXE);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "shovel",
                Component.translatable("screen.anvilcraft.resonator.shovel"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.RESONATE_MODE, ResonateMode.SHOVEL);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "hoe",
                Component.translatable("screen.anvilcraft.resonator.hoe"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.RESONATE_MODE, ResonateMode.HOE);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
                )
            )
            .action(
                "pickaxe",
                Component.translatable("screen.anvilcraft.resonator.pickaxe"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.RESONATE_MODE, ResonateMode.PICKAXE);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchResonateModePacket(hand, ctx.slotIndex())
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
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.ALL);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "shears",
                Component.translatable("item.minecraft.shears"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.SHEARS);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "flint_and_steel",
                Component.translatable("item.minecraft.flint_and_steel"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.FLINT_AND_STEEL);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "brush",
                Component.translatable("item.minecraft.brush"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.BRUSH);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "spyglass",
                Component.translatable("item.minecraft.spyglass"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.SPYGLASS);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "magnet",
                Component.translatable("item.anvilcraft.magnet"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.MAGNET);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "fishing_rod",
                Component.translatable("item.minecraft.fishing_rod"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.FISHING_ROD);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "carrot_on_a_stick",
                Component.translatable("item.minecraft.carrot_on_a_stick"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.CARROT_ON_A_STICK);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "warped_fungus_on_a_stick",
                Component.translatable("item.minecraft.warped_fungus_on_a_stick"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.MULTITOOL_MODE, MultitoolMode.WARPED_FUNGUS_ON_A_STICK);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchMultitoolModePacket(hand, MultitoolMode.values()[ctx.slotIndex()])
                )
            )
            .build();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (ModKeyMappings.SWITCH_PHASE.get().matches(event.getKeyEvent())) {
            WheelLifecycleEventListener.processMultiphasePress(client, event.getAction());
        }
        if (ModKeyMappings.SWITCH_TOOL_MODE.get().matches(event.getKeyEvent())) {
            WheelLifecycleEventListener.processResonatorPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.MouseButton.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (client.options.keyUse.matchesMouse(new MouseButtonEvent(0, 0, event.getMouseButtonInfo()))) {
            WheelLifecycleEventListener.processHammerPress(client, event.getAction());
        }
        if (ModKeyMappings.SWITCH_PHASE.get().matchesMouse(new MouseButtonEvent(0, 0, event.getMouseButtonInfo()))) {
            WheelLifecycleEventListener.processMultiphasePress(client, event.getAction());
        }
        if (ModKeyMappings.SWITCH_TOOL_MODE.get().matchesMouse(new MouseButtonEvent(0, 0, event.getMouseButtonInfo()))) {
            WheelLifecycleEventListener.processResonatorPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
        }
    }

    private static void processHammerPress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.hammerKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            }
            WheelLifecycleEventListener.hammerKeyWasDown = false;
            WheelLifecycleEventListener.hammerKeyTime = -1L;
            WheelLifecycleEventListener.hammerWheelCache = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.hammerKeyWasDown) {
                WheelLifecycleEventListener.hammerKeyTime = client.level.getGameTime();
            }
        }
    }

    private static void processMultiphasePress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.multiphaseKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
            } else {
                ClientPacketDistributor.sendToServer(new MultiphasePackets.SwitchPhase());
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
