package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder.ModelRenderTarget;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import dev.dubhe.anvilcraft.network.HammerChangeBlockPacket;
import dev.dubhe.anvilcraft.network.HammerChangeFlexibleMultiPartBlockPacket;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.network.SwitchHeavyHalberdModePacket;
import dev.dubhe.anvilcraft.network.SwitchMultitoolModePacket;
import dev.dubhe.anvilcraft.network.SwitchResonateModePacket;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class WheelLifecycleEventListener {
    private static final WheelScreenController CONTROLLER = new WheelScreenController();

    private static long hammerKeyTime = -1L;
    private static boolean hammerKeyWasDown = false;
    private static @Nullable Optional<WheelMenuModel> hammerWheelCache = null;
    // TODO: These three fields should be refactored when the AnvilLib
    //       adds an "on close without action" handler to the wheel menu model
    private static @Nullable BlockPos hammerWheelTargetPos = null;
    private static @Nullable BlockState hammerWheelNextBlockState = null;
    private static @Nullable Supplier<Boolean> hammerInteraction = null;

    private static long multiphaseKeyTime = -1L;
    private static boolean multiphaseKeyWasDown = false;
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

    /** 判断当前是否正在通过长按铁砧锤打开方块状态选择轮。 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isHammerWheelOpen() {
        return WheelLifecycleEventListener.hammerKeyWasDown && WheelLifecycleEventListener.hammerWheelCache != null
               && WheelLifecycleEventListener.hammerWheelCache.isPresent();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isHammerWheelModel(WheelMenuModel model) {
        return WheelLifecycleEventListener.hammerWheelCache != null && WheelLifecycleEventListener.hammerWheelCache.orElse(null) == model;
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
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        WheelLifecycleEventListener.hammerInteraction = () -> {
            boolean interacted = player != null && AnvilHammerItem.interactWithBlock(
                player,
                targetPos,
                level,
                player.getItemInHand(hand),
                hand,
                hitVec
            );
            ClientPacketDistributor.sendToServer(new HammerUsePacket(targetPos, hand, hitVec));
            return interacted;
        };
        if (property == null) {
            return WheelLifecycleEventListener.hammerInteraction.get();
        }
        if (player == null) return false;
        if (WheelLifecycleEventListener.hammerWheelCache == null) {
            if (player.isShiftKeyDown()) {
                ClientPacketDistributor.sendToServer(new HammerUsePacket(targetPos, hand, hitVec));
                return true;
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
        WheelLifecycleEventListener.CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.hammerWheelCache.get());
        WheelLifecycleEventListener.hammerKeyWasDown = true;
        WheelLifecycleEventListener.hammerWheelTargetPos = targetPos;
        WheelLifecycleEventListener.hammerWheelNextBlockState = level.getBlockState(targetPos).cycle(property);
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
                Multiphase multiphase = stack.get(ModComponents.MULTIPHASE);
                if (multiphase == null) return;
                multiphase = multiphase.forDisplay(stack);
                WheelLifecycleEventListener.multiphaseWheelCache = Optional.of(
                    WheelLifecycleEventListener.getMultiphaseWheel(hand, stack, multiphase)
                );
            }
            if (WheelLifecycleEventListener.multiphaseWheelCache.isEmpty()) return;
            WheelLifecycleEventListener.CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.multiphaseWheelCache.get());
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
            WheelLifecycleEventListener.CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.resonatorWheelCache.get());
            WheelLifecycleEventListener.resonatorKeyWasDown = true;
        } else {
            if (WheelLifecycleEventListener.resonatorWheelCache.isEmpty()) return;
            WheelLifecycleEventListener.CONTROLLER.openTap(WheelLifecycleEventListener.resonatorWheelCache.get());
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
            WheelLifecycleEventListener.CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.multitoolWheelCache.get());
            WheelLifecycleEventListener.multitoolKeyWasDown = true;
        } else {
            if (WheelLifecycleEventListener.multitoolWheelCache.isEmpty()) return;
            WheelLifecycleEventListener.CONTROLLER.openTap(WheelLifecycleEventListener.multitoolWheelCache.get());
        }
    }

    private static WheelMenuModel getHammerWheel(
        BlockPos targetPos,
        Property<?> property,
        List<BlockState> possibleStates,
        Vec2 camera
    ) {
        Level level = Objects.requireNonNull(Minecraft.getInstance().level);
        BlockState initialState = level.getBlockState(targetPos);
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(possibleStates.size());
        possibleStates
            .forEach(state -> {
                ModelRenderTarget modelTarget = state.getBlock() instanceof IMultiPartBlockModelHolder holder
                                                ? holder.getModelRenderTarget(level, targetPos, initialState, state)
                                                : new ModelRenderTarget(targetPos, state);
                String name = property.getName(Util.cast(state.getValue(property)));
                builder.action(
                    name,
                    Component.literal(name),
                    (graphics, _, _, _) -> {
                        PoseStack pose = new PoseStack();
                        pose.translate(0, 0, 0);
                        pose.mulPose(Axis.XP.rotationDegrees(camera.x));
                        pose.mulPose(Axis.YP.rotationDegrees(camera.y + 180F));
                        GuiRenderExtras.tessellateBlock(graphics, modelTarget.state(), -15f, -5, pose);
                    },
                    _ -> WheelLifecycleEventListener.sendHammerChangeBlockPacketToServer(state, targetPos)
                );
            });
        return builder.build();
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
            WheelLifecycleEventListener.CONTROLLER.onHoldKeyPressed(WheelLifecycleEventListener.heavyHalberdWheelCache.get());
            WheelLifecycleEventListener.heavyHalberdKeyWasDown = true;
        }
    }

    private static WheelMenuModel getMultiphaseWheel(InteractionHand hand, ItemStack holding, Multiphase multiphase) {
        int phaseCount = multiphase.phases().size();
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(phaseCount);
        for (int i = 0; i < phaseCount; i++) {
            WheelLifecycleEventListener.addMultiphaseWheelEntry(builder, hand, holding, multiphase, i);
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
            (graphics, _, _, _) -> {
                ItemStack copied = holding.copy();
                multiphase.applySelectionPreview(copied, phaseIndex);
                graphics.item(copied, 2, 2, 9910597);
            },
            _ -> ClientPacketDistributor.sendToServer(
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

    private static WheelMenuModel getHeavyHalberdWheel(InteractionHand hand, ItemStack holding) {
        return WheelMenuBuilder.create()
            .slotsPerPage(4)
            .action(
                "trident",
                Component.translatable("screen.anvilcraft.heavy_halberd.trident"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.HEAVY_HALBERD_MODE, HeavyHalberdMode.TRIDENT);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, HeavyHalberdMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "spear",
                Component.translatable("screen.anvilcraft.heavy_halberd.spear"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.HEAVY_HALBERD_MODE, HeavyHalberdMode.SPEAR);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, HeavyHalberdMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "sword",
                Component.translatable("screen.anvilcraft.heavy_halberd.sword"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.HEAVY_HALBERD_MODE, HeavyHalberdMode.SWORD);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, HeavyHalberdMode.values()[ctx.slotIndex()])
                )
            )
            .action(
                "mace",
                Component.translatable("screen.anvilcraft.heavy_halberd.mace"),
                (graphics, _, _, _) -> {
                    ItemStack copied = holding.copy();
                    copied.set(ModComponents.HEAVY_HALBERD_MODE, HeavyHalberdMode.MACE);
                    graphics.item(copied, 2, 2, 9910597);
                },
                ctx -> ClientPacketDistributor.sendToServer(
                    new SwitchHeavyHalberdModePacket(hand, HeavyHalberdMode.values()[ctx.slotIndex()])
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
            WheelLifecycleEventListener.processHeavyHalberdPress(client, event.getAction());
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
            WheelLifecycleEventListener.processHeavyHalberdPress(client, event.getAction());
            WheelLifecycleEventListener.processMultitoolPress(client, event.getAction());
        }
    }

    private static void processHammerPress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.hammerKeyWasDown) {
                WheelLifecycleEventListener.CONTROLLER.onHoldKeyReleased();
            }

            // TODO: This three fields should be refactored when the AnvilLib
            //       adds an "on close without action" handler to the wheel menu model
            BlockPos targetPos = WheelLifecycleEventListener.hammerWheelTargetPos;
            BlockState state = WheelLifecycleEventListener.hammerWheelNextBlockState;
            Supplier<Boolean> hammerInteraction = WheelLifecycleEventListener.hammerInteraction;
            if (
                client.level.getGameTime() - WheelLifecycleEventListener.hammerKeyTime <= 4 &&
                targetPos != null && state != null && hammerInteraction != null
            ) {
                // On single right-click
                if (!hammerInteraction.get()) {
                    WheelLifecycleEventListener.sendHammerChangeBlockPacketToServer(state, targetPos);
                }
            }

            WheelLifecycleEventListener.hammerKeyWasDown = false;
            WheelLifecycleEventListener.hammerKeyTime = -1L;
            WheelLifecycleEventListener.hammerWheelCache = null;

            // TODO: These three fields should be refactored when the AnvilLib
            //       adds an "on close without action" handler to the wheel menu model
            WheelLifecycleEventListener.hammerWheelTargetPos = null;
            WheelLifecycleEventListener.hammerWheelNextBlockState = null;
            WheelLifecycleEventListener.hammerInteraction = null;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (!WheelLifecycleEventListener.hammerKeyWasDown) {
                WheelLifecycleEventListener.hammerKeyTime = client.level.getGameTime();
            }
        }
    }

    private static void sendHammerChangeBlockPacketToServer(BlockState state, BlockPos targetPos) {
        if (state.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            if (state.hasProperty(BlockStateProperties.FACING)) {
                ClientPacketDistributor.sendToServer(new HammerChangeFlexibleMultiPartBlockPacket(
                    targetPos,
                    state,
                    state.getValue(BlockStateProperties.FACING)
                ));
            } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                ClientPacketDistributor.sendToServer(new HammerChangeFlexibleMultiPartBlockPacket(
                    targetPos,
                    state,
                    state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                ));
            }
        } else {
            ClientPacketDistributor.sendToServer(new HammerChangeBlockPacket(
                targetPos,
                state
            ));
        }
    }

    private static void processMultiphasePress(Minecraft client, int action) {
        if (client.level == null) return;
        if (action == GLFW.GLFW_RELEASE) {
            if (WheelLifecycleEventListener.multiphaseKeyWasDown) {
                WheelLifecycleEventListener.CONTROLLER.onHoldKeyReleased();
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
                WheelLifecycleEventListener.CONTROLLER.onHoldKeyReleased();
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
                WheelLifecycleEventListener.CONTROLLER.onHoldKeyReleased();
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
                WheelLifecycleEventListener.CONTROLLER.onHoldKeyReleased();
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
}
