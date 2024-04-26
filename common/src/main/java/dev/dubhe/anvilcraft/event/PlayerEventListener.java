package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.PlayerEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.item.ResinBlockItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerEventListener {
    /**
     * @param event 玩家右键实体事件
     */
    @SubscribeEvent
    public void useEntity(@NotNull PlayerEvent.UseEntity event) {
        InteractionHand hand = event.getHand();
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(hand);
        Entity target = event.getTarget();
        if (item.is(ModBlocks.RESIN_BLOCK.asItem())) {
            event.setResult(ResinBlockItem.useEntity(player, target, item));
        }
    }
}
