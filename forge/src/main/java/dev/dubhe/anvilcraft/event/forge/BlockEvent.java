package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class BlockEvent {
    /**
     * 侦听左键方块事件
     *
     * @param leftClickBlock 左键方块事件
     */
    @SubscribeEvent
    public static void anvilHammer(@NotNull PlayerInteractEvent.LeftClickBlock leftClickBlock) {
        if (leftClickBlock.getEntity().getItemInHand(leftClickBlock.getEntity().getUsedItemHand()).getItem()
            instanceof AnvilHammerItem anvilHammer) {
            anvilHammer.useAttackBlock(leftClickBlock.getEntity(), leftClickBlock.getPos(), leftClickBlock.getLevel());
        }
    }
}
