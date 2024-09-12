package dev.dubhe.anvilcraft.integration.top;

import dev.dubhe.anvilcraft.integration.top.provider.PowerBlockProvider;

import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.ITheOneProbe;

public class AnvilCraftTopPlugin {
    public static void init() {
        ITheOneProbe probe = TheOneProbe.theOneProbeImp;

        probe.registerProvider(PowerBlockProvider.INSTANCE);
    }
}
