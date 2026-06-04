package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.display.slot.CannedFoodSlotDemo;
import dev.dubhe.anvilcraft.recipe.display.slot.PillSlotDemo;
import dev.dubhe.anvilcraft.recipe.display.slot.WithAnyPotionsExcept;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModSlotDisplays {
    @SubscribeEvent
    public static void on(RegisterEvent event) {
        ModSlotDisplays.register(event, "canned_food", CannedFoodSlotDemo.TYPE);
        ModSlotDisplays.register(event, "with_any_potion_except", WithAnyPotionsExcept.TYPE);
        ModSlotDisplays.register(event, "pill", PillSlotDemo.TYPE);
    }

    private static void register(RegisterEvent event, String name, SlotDisplay.Type<?> type) {
        event.register(Registries.SLOT_DISPLAY, AnvilCraft.of(name), () -> type);
    }
}
