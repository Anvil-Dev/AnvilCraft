package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModConsumables;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModFoods;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ItemDefaultComponentsEventListener {
    @SubscribeEvent
    public static void cannedFood(ModifyDefaultComponentsEvent event) {
        event.modify(ModFoodItems.CANNED_FOOD, (components, context, item) -> {
            components.set(DataComponents.FOOD, ModFoods.BEEF_MUSHROOM_STEW);
            components.set(DataComponents.CONSUMABLE, ModConsumables.FAST_FOOD);
            components.set(ModComponents.DISPLAY_ITEM,
                new StoredItem(new net.minecraft.world.item.ItemStackTemplate(ModFoodItems.BEEF_MUSHROOM_STEW.get())));
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void multiphaseNames(ModifyDefaultComponentsEvent event) {
        event.modifyMatching((item, components) -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(AnvilCraft.MOD_ID)
            && components.has(ModComponents.MULTIPHASE), (components, context, item) -> {
                Multiphase phases = components.get(ModComponents.MULTIPHASE);
                if (phases != null) components.set(DataComponents.ITEM_NAME, Multiphase.itemName(item, phases.activePhase()));
            });
    }
}
