package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ResonanceMiningClientEventListener {
    @SubscribeEvent
    public static void suppressAirUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.hitResult == null) return;
        if (minecraft.hitResult.getType() != HitResult.Type.MISS) return;

        var stack = minecraft.player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof ResonatorItem)) return;
        if (ResonatorItem.getMode(stack) != ResonatorItem.AUTO_MODE) return;

        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
