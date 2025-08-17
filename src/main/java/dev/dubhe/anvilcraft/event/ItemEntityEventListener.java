package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.util.IDiscardableItemEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ItemEntityEventListener {
    @SubscribeEvent
    public static void onItemEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (item.getItem().has(ModComponents.ETERNAL)) {
            item.setUnlimitedLifetime();
        }
        if (IDiscardableItemEntity.castFromItemEntity(item).anvilcraft$getDiscarded()) {
            event.setCanceled(true);
        }
    }
}
