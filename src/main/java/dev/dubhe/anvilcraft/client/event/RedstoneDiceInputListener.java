package dev.dubhe.anvilcraft.client.event;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.RedstoneDiceModePacket;
import dev.dubhe.anvilcraft.network.RedstoneDiceRollPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class RedstoneDiceInputListener {
    private static final int OPEN_WHEEL_TICKS = 4;
    private static final WheelScreenController CONTROLLER = new WheelScreenController();
    private static @Nullable BlockPos heldPos;
    private static @Nullable ClientLevel heldLevel;
    private static @Nullable Screen wheelScreen;
    private static long pressTick;
    private static InteractionHand heldHand = InteractionHand.MAIN_HAND;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft client = Minecraft.getInstance();
        BlockPos pos = targetedDice(client);
        if (pos == null || client.level == null) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static void press() {
        Minecraft client = Minecraft.getInstance();
        BlockPos pos = targetedDice(client);
        if (heldPos != null || pos == null || client.level == null || client.player == null) return;
        heldPos = pos;
        heldLevel = client.level;
        heldHand = client.player.getMainHandItem().getItem() instanceof AnvilHammerItem
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        pressTick = client.level.getGameTime();
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (!Minecraft.getInstance().options.keyUse.matchesMouse(event.getButton())) return;
        if (event.getAction() == GLFW.GLFW_PRESS) press();
        if (event.getAction() == GLFW.GLFW_RELEASE) release();
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!Minecraft.getInstance().options.keyUse.matches(event.getKey(), event.getScanCode())) return;
        if (event.getAction() == GLFW.GLFW_PRESS) press();
        if (event.getAction() == GLFW.GLFW_RELEASE) release();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (heldPos == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.level != heldLevel || client.player == null || !client.isWindowActive()
            || !(client.player.getItemInHand(heldHand).getItem() instanceof AnvilHammerItem)) {
            if (wheelScreen != null && client.screen == wheelScreen) client.setScreen(null);
            reset();
            return;
        }
        if (wheelScreen != null) {
            if (client.screen != wheelScreen) reset();
            return;
        }
        if (!heldPos.equals(targetedDice(client))) {
            reset();
            return;
        }
        if (!client.options.keyUse.isDown()) {
            release();
            return;
        }
        if (client.level.getGameTime() - pressTick >= OPEN_WHEEL_TICKS) {
            CONTROLLER.onHoldKeyPressed(buildWheel(heldPos));
            wheelScreen = client.screen;
        }
    }

    private static WheelMenuModel buildWheel(BlockPos pos) {
        WheelMenuBuilder builder = WheelMenuBuilder.create()
            .selectionEffect(WheelSelectionEffect.ANNULAR_SECTOR).slotsPerPage(2);
        boolean currentUniform = Minecraft.getInstance().level != null
            && Minecraft.getInstance().level.getBlockEntity(pos) instanceof RedstoneDiceBlockEntity dice && dice.isUniform();
        for (boolean uniform : new boolean[]{true, false}) {
            String mode = uniform ? "uniform" : "realistic";
            Component label = Component.translatable("screen.anvilcraft.redstone_dice." + mode);
            if (uniform == currentUniform) label = Component.literal("✓ ").append(label);
            ItemStack icon = new ItemStack(uniform ? Items.REDSTONE : ModBlocks.REDSTONE_DICE.asItem());
            builder.action(mode, label, (graphics, pose, width, height) -> graphics.renderItem(icon, -8, -8),
                context -> PacketDistributor.sendToServer(new RedstoneDiceModePacket(pos, uniform)));
        }
        return builder.build();
    }

    @Nullable
    private static BlockPos targetedDice(Minecraft client) {
        if (client.level == null || client.player == null || client.screen != null || !client.isWindowActive()
            || !client.player.isAlive() || client.player.isSpectator() || client.player.isShiftKeyDown()) return null;
        if (!(client.player.getMainHandItem().getItem() instanceof AnvilHammerItem)
            && !(client.player.getOffhandItem().getItem() instanceof AnvilHammerItem)) return null;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return client.level.getBlockState(hit.getBlockPos()).is(ModBlocks.REDSTONE_DICE) ? hit.getBlockPos() : null;
    }

    private static void release() {
        Minecraft client = Minecraft.getInstance();
        if (wheelScreen != null) {
            CONTROLLER.onHoldKeyReleased();
        } else if (heldPos != null && client.level != null && client.level == heldLevel && client.player != null
            && client.player.getItemInHand(heldHand).getItem() instanceof AnvilHammerItem
            && client.level.getGameTime() - pressTick < OPEN_WHEEL_TICKS && heldPos.equals(targetedDice(client))) {
            PacketDistributor.sendToServer(new RedstoneDiceRollPacket(heldPos, heldHand));
        }
        reset();
    }

    private static void reset() {
        heldPos = null;
        heldLevel = null;
        wheelScreen = null;
    }
}
