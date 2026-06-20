package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;

public class DysonSphereHandler extends BaseMegastructureHandler {

    private int cachedGridConsumption = 0;
    private final String name;

    public DysonSphereHandler(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        // Dyson Sphere is passive
    }

    @Override
    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        if (!be.isAmplifierPresent()) return 0;
        if (!(be.getCelestialBodyData() instanceof StarData star)) return 0;
        if (be.isAcceleratorActive() && be.getAcceleratorStage() == 1) {
            return Math.max(cachedGridConsumption * 2, cachedGridConsumption + 1);
        }
        int e = star.energy();
        int r = star.size();
        if (e > 0 && r > 0) {
            int powerMW = (e * r * r) / 800;
            return powerMW * 1000;
        }
        return 0;
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }

    @Override
    public void gridTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getGrid() != null
            && be.isAcceleratorActive()
            && be.getAcceleratorStage() == 1
            && be.getActiveMegastructureIndex() >= 0) {
            this.cachedGridConsumption = be.getGrid().getConsume();
        }
    }
}
