package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.item.property.FlightTimePropertyFunction;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;

public class ModItemProperties {
    public static final ClampedItemPropertyFunction FLIGHT_TIME = new FlightTimePropertyFunction();
}
