package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.shaders.FogShape;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.hud.BufferBootsChargeHUD;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import dev.dubhe.anvilcraft.network.SwapPocketPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EquipmentClientEvents {
    private static final WheelScreenController POCKET_WHEEL = new WheelScreenController();
    private static boolean pocketHeld;
    private static boolean chargedMessage;

    private EquipmentClientEvents() {
    }

    private static void press(InputConstants.Key key, int action) {
        Minecraft client = Minecraft.getInstance();
        if (action == InputConstants.RELEASE && pocketHeld && (key.equals(ModKeyMappings.POCKETS.get().getKey())
            || ModKeyMappings.POCKETS.get().getKeyModifier().matches(key))) {
            pocketHeld = false;
            POCKET_WHEEL.onHoldKeyReleased();
            return;
        }
        if (action != InputConstants.PRESS || client.screen != null || client.player == null || pocketHeld
            || !ModKeyMappings.POCKETS.get().isActiveAndMatches(key)) return;
        int capacity = PocketInventory.capacity(client.player);
        if (capacity == 0) return;
        WheelMenuBuilder builder = WheelMenuBuilder.create().selectionEffect(WheelSelectionEffect.ANNULAR_SECTOR)
            .slotsPerPage(capacity);
        for (int slot = 0; slot < capacity; slot++) {
            int selected = slot;
            ItemStack stack = PocketInventory.get(client.player).getItem(slot).copy();
            builder.action("pocket_" + slot, stack.isEmpty()
                    ? Component.translatable("screen.anvilcraft.pockets.empty") : stack.getHoverName(),
                (graphics, pose, width, height) -> {
                    graphics.renderItem(stack, -8, -8);
                    graphics.renderItemDecorations(client.font, stack, -8, -8);
                }, ctx -> PacketDistributor.sendToServer(new SwapPocketPacket(selected)));
        }
        pocketHeld = true;
        client.options.keySwapOffhand.setDown(false);
        while (client.options.keySwapOffhand.consumeClick()) {
            // 组合键用于口袋轮盘，消费同一次按键留下的原版副手交换。
        }
        POCKET_WHEEL.onHoldKeyPressed(builder.build());
    }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        press(InputConstants.getKey(event.getKey(), event.getScanCode()), event.getAction());
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Post event) {
        press(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), event.getAction());
    }

    @SubscribeEvent
    public static void releaseKey(ScreenEvent.KeyReleased.Pre event) {
        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (pocketHeld && (ModKeyMappings.POCKETS.get().matches(event.getKeyCode(), event.getScanCode())
            || ModKeyMappings.POCKETS.get().getKeyModifier().matches(key))) {
            press(InputConstants.getKey(event.getKeyCode(), event.getScanCode()), InputConstants.RELEASE);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void releaseMouse(ScreenEvent.MouseButtonReleased.Pre event) {
        if (pocketHeld && ModKeyMappings.POCKETS.get().matchesMouse(event.getButton())) {
            press(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), InputConstants.RELEASE);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            pocketHeld = false;
            chargedMessage = false;
            return;
        }
        if (EquipmentAbilities.chargeTicks(client.player) > EquipmentAbilities.CHARGE_TICKS) {
            client.gui.setOverlayMessage(Component.translatable("message.anvilcraft.buffer_boots.charged",
                client.options.keyJump.getTranslatedKeyMessage(), client.options.keyShift.getTranslatedKeyMessage()), false);
            chargedMessage = true;
        } else if (chargedMessage) {
            client.gui.setOverlayMessage(Component.empty(), false);
            chargedMessage = false;
        }
    }

    @SubscribeEvent
    public static void chargeBar(RenderGuiLayerEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        if (!event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR) || client.player == null
            || client.options.hideGui || client.player.isSpectator()) return;
        int ticks = EquipmentAbilities.chargeTicks(client.player);
        if (ticks <= 0) return;
        event.setCanceled(true);
        BufferBootsChargeHUD.render(event.getGuiGraphics(), ticks / (float) (EquipmentAbilities.CHARGE_TICKS + 1));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearFluidFog(ViewportEvent.RenderFog event) {
        if (!(event.getCamera().getEntity() instanceof LivingEntity entity)
            || !entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return;
        BlockPos pos = BlockPos.containing(event.getCamera().getPosition());
        FluidState fluid = entity.level().getFluidState(pos);
        if (fluid.isEmpty() || event.getCamera().getPosition().y >= pos.getY() + fluid.getHeight(entity.level(), pos)) return;
        event.setNearPlaneDistance(Math.max(48, Minecraft.getInstance().options.getEffectiveRenderDistance() * 12));
        event.setFarPlaneDistance(Math.max(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16));
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clearWaterOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER
            && event.getPlayer().getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) {
            event.setCanceled(true);
        }
    }
}
