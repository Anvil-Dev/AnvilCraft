package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;

public class MagnetarCoilHandler extends BaseMegastructureHandler {

    @Override
    public String name() {
        return "magnetar_coil";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        // Magnetar Coil is passive
    }

    @SuppressWarnings("checkstyle:LocalVariableName")
    @Override
    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAmplifierPresent()) return 0;
        if (!(be.getCelestialBodyData() instanceof StarData star)) return 0;
        int b = star.magneticFieldStrength();
        int n = star.rotationSpeed();
        int bMinus2 = b - 2;
        int bTerm = bMinus2 * bMinus2 * bMinus2 * bMinus2;
        int nTerm = n * n;
        int powerMW = (bTerm * nTerm) / 16;
        return powerMW * 1000;
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }
}
