package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AmuletEventListener {
    @SubscribeEvent
    public static void on(AmuletEvent.Find event) {
        Player player = event.getPlayer();
        event.provide(player.getMainHandItem());
        event.provide(player.getOffhandItem());
    }

    @SubscribeEvent
    public static void on(AmuletEvent.ProcessFound event) {
        ItemStack found = event.getFound();
        if (found.is(ModItems.AMULET_BOX)) {
            BoxContents contents = found.get(ModComponents.BOX_CONTENTS);
            if (contents == null) return;
            for (ItemStack stack : contents.amulets()) {
                if (stack.has(ModComponents.AMULET)) {
                    event.provide(stack.copy());
                }
            }
        }
    }
}
