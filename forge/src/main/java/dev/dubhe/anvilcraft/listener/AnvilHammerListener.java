package dev.dubhe.anvilcraft.listener;

import dev.dubhe.anvilcraft.item.AnvilHammer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AnvilHammerListener {
    @SubscribeEvent
    public void anvilHammer(PlayerInteractEvent.LeftClickBlock leftClickBlock) {
        if (leftClickBlock.getEntity().getItemInHand(leftClickBlock.getEntity().getUsedItemHand()).getItem() instanceof AnvilHammer anvilHammer) {
            anvilHammer.useAttackBlock(leftClickBlock.getEntity(), leftClickBlock.getPos(), leftClickBlock.getLevel());
        }
    }
}
