package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientCreativeContainerTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientFilterTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.CreativeContainerTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.FilterTooltip;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ModTooltipComponents {

    @SubscribeEvent
    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FilterTooltip.class, ClientFilterTooltip::new);
        event.register(CreativeContainerTooltip.class, ClientCreativeContainerTooltip::new);
    }
}
