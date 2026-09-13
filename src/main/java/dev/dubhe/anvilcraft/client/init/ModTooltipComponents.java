package dev.dubhe.anvilcraft.client.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientCreativeContainerTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientFilterTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientFluidTankTooltip;
import dev.dubhe.anvilcraft.client.gui.tooltip.ClientStoragePortTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.CreativeContainerTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.FilterTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.StoragePortTooltip;
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
        event.register(StoragePortTooltip.class, ClientStoragePortTooltip::new);
        event.register(FluidTankTooltip.class, ClientFluidTankTooltip::new);
    }
}
