package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.client.gui.tooltip.ClientConfinementChamberTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientCreativeCrateTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientFluidTankTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientStoragePortTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientStorageTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.ConfinementChamberTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.CreativeCrateTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.StoragePortTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.StorageTooltip;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

public class ModTooltipComponents {
    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FluidTankTooltip.class, ClientFluidTankTooltip::new);
        event.register(StorageTooltip.class, ClientStorageTooltip::new);
        event.register(ConfinementChamberTooltip.class, ClientConfinementChamberTooltip::new);
        event.register(CreativeCrateTooltip.class, ClientCreativeCrateTooltip::new);
        event.register(StoragePortTooltip.class, ClientStoragePortTooltip::new);
    }
}
