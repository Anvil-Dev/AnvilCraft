package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.client.gui.tooltip.ClientFluidTankTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

public class ModTooltipComponents {
    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FluidTankTooltip.class, ClientFluidTankTooltip::new);
    }
}
