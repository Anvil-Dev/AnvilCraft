package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.client.gui.tooltip.ClientFilterTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.FilterTooltip;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

public class ModTooltipComponents {
    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FilterTooltip.class, ClientFilterTooltip::new);
    }
}
