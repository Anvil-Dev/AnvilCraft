package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class RegisterColorHandlersEventListener {
    @SubscribeEvent
    public static void registerItemColorHandlersEvent(RegisterColorHandlersEvent.Item event) {
        event.register((itemStack, index) -> {
            PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (index > 0 || potionContents.potion().isEmpty()) {
                return -1;
            } else {
                return FastColor.ARGB32.opaque(potionContents.getColor());
            }
        }, ModFoodItems.PILL);
    }
}
