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
        // 被动发电巨构
    }

    @Override
    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAmplifierPresent()) return 0;
        if (!(be.getCelestialBodyData() instanceof StarData star)) return 0;
        int b = star.magneticFieldStrength();
        int n = star.rotationSpeed();
        int minus2 = b - 2;
        int termB = minus2 * minus2 * minus2 * minus2;
        int termN = n * n;
        int powerMW = (termB * termN) / 16;
        return powerMW * 1000;
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }
}
