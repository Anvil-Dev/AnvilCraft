package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.util.ModEnchantmentHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

@EventBusSubscriber
public class UseOnBlockEventListener {
    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent e) {
        UseOnContext context = e.getUseOnContext();
        if (!context.getItemInHand().is(ItemTags.HOES)) return;
        if (context.getLevel().isClientSide) return;
        ModEnchantmentHelper.onUseOnBlock(
            (ServerLevel) context.getLevel(),
            context.getItemInHand(),
            context.getPlayer(),
            LivingEntity.getSlotForHand(context.getHand()),
            context.getClickedPos().getCenter(),
            context.getLevel().getBlockState(context.getClickedPos())
        );
    }
}
