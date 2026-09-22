package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.hud.BufferBootsChargeHUD;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EquipmentClientEvents {
    private static boolean chargedMessage;

    private EquipmentClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearFluidFog(ViewportEvent.RenderFog event) {
        if (!(event.getCamera().entity() instanceof LivingEntity entity)
            || !entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return;
        BlockPos pos = BlockPos.containing(event.getCamera().position());
        FluidState fluid = entity.level().getFluidState(pos);
        if (fluid.isEmpty() || event.getCamera().position().y >= pos.getY() + fluid.getHeight(entity.level(), pos)) return;
        event.setNearPlaneDistance(Math.max(48, Minecraft.getInstance().options.getEffectiveRenderDistance() * 12));
        event.setFarPlaneDistance(Math.max(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16));
    }

    @SubscribeEvent
    public static void clearWaterOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER
            && event.getPlayer().getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            chargedMessage = false;
            return;
        }
        // 蓄力条可见期间（蓄力中、蓄满、松开后的停留与衰减）统一给出跳跃提示
        if (EquipmentAbilities.chargeProgress(client.player) > 0) {
            client.gui.setOverlayMessage(
                Component.translatable(
                    "message.anvilcraft.buffer_boots.charged",
                    client.options.keyJump.getTranslatedKeyMessage()
                ),
                false
            );
            chargedMessage = true;
        } else if (chargedMessage) {
            client.gui.setOverlayMessage(Component.empty(), false);
            chargedMessage = false;
        }
    }

    @SubscribeEvent
    public static void chargeBar(RenderGuiLayerEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        if (!event.getName().equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR)
            && !event.getName().equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND) || client.player == null
            || client.options.hideGui || client.player.isSpectator()) return;
        float progress = EquipmentAbilities.chargeProgress(client.player);
        if (progress <= 0) return;
        event.setCanceled(true);
        if (event.getName().equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND)) return;
        BufferBootsChargeHUD.render(event.getGuiGraphics(), progress, EquipmentAbilities.isChargeHeld(client.player));
    }

}
