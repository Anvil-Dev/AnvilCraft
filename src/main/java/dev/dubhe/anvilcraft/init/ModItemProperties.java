package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.item.property.FlightTimePropertyFunction;
import net.minecraft.client.renderer.item.ItemPropertyFunction;

@SuppressWarnings("deprecation")
public class ModItemProperties {
    public static final ItemPropertyFunction FLIGHT_TIME = new FlightTimePropertyFunction();
}
