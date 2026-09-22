package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EquipmentClientEvents {
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
}
