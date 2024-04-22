package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.HammerUsePack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class BlockEvent {
    /**
     * 侦听左键方块事件
     *
     * @param event 左键方块事件
     */
    @SubscribeEvent
    public static void anvilHammerAttack(@NotNull PlayerInteractEvent.LeftClickBlock event) {
        InteractionHand hand = event.getHand();
        if (event.getEntity().getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            AnvilHammerItem.attackBlock(event.getEntity(), event.getPos(), event.getLevel());
        }
    }

    /**
     * 侦听右键方块事件
     *
     * @param event 右键方块事件
     */
    @SubscribeEvent
    public static void anvilHammerUse(@NotNull PlayerInteractEvent.RightClickBlock event) {
        InteractionHand hand = event.getHand();
        if (event.getEntity().getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            if (event.getLevel().isClientSide()) {
                new HammerUsePack(event.getPos(), hand).send();
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
