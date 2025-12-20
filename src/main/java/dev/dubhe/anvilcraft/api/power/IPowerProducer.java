package dev.dubhe.anvilcraft.api.power;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

/**
 * 发电
 */
public interface IPowerProducer extends IPowerComponent {
    default int getOutputPower() {
        return 0;
    }

    default int getTime() {
        return 0;
    }

    @Override
    default PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }

    /**
     * 实际电量
     */
    @OnlyIn(Dist.CLIENT)
    default int getServerPower() {
        Optional<SimplePowerGrid> s = SimplePowerGrid.findPowerGrid(getPos());
        if (s.isPresent()) {
            if (s.get().getConsume() > s.get().getGenerate()) {
                return 0;
            }
            Optional<PowerComponentInfo> info = s.get().getInfoForPos(getPos());
            return info.map(powerComponentInfo -> powerComponentInfo.type() == PowerComponentType.PRODUCER
                    ? powerComponentInfo.produces()
                    : powerComponentInfo.consumes())
                .orElse(1);
        } else {
            return Math.abs(getOutputPower());
        }
    }
}
