package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CreativeModeTabEventListener {
    @SubscribeEvent
    public static void onTab(BuildCreativeModeTabContentsEvent event) {
        if (ModItemGroups.ANVILCRAFT_FUNCTION_BLOCK.is(event.getTabKey())) {
            event.insertBefore(ModBlocks.PUMP.asStack(), ModItems.PIPE.asStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
