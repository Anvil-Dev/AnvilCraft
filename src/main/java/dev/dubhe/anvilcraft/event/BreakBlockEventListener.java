package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.util.ModEnchantmentHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class BreakBlockEventListener {
    @SubscribeEvent
    public static void onBlockRemoved(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        ModEnchantmentHelper.onPostBreakBlock(
            (ServerLevel) player.level(),
            stack,
            player,
            EquipmentSlot.MAINHAND,
            event.getPos().getCenter(),
            event.getState()
        );
    }
}
