package dev.dubhe.anvilcraft.api.power;

import java.util.Optional;

/// 发电
public interface IPowerProducer extends IPowerComponent {
    default int getOutputPower() {
        return 0;
    }

    default int getTime() {
        return 0;
    }

    /** 是否正在提供无限电力。 */
    default boolean isInfinitePower() {
        return false;
    }

    @Override
    default PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }

    @Override
    default PowerComponentInfo toPowerComponentInfo() {
        return new PowerComponentInfo(
            this.getPos(),
            0,
            this.getOutputPower(),
            0,
            0,
            this.getRange(),
            this.getShape(),
            PowerComponentType.PRODUCER
        );
    }

    /// 实际电量
    // @OnlyIn(Dist.CLIENT)
    default int getServerPower() {
        Optional<SimplePowerGrid> s = SimplePowerGrid.findPowerGrid(this.getPos());
        if (s.isPresent()) {
            if (s.get().getConsume() > s.get().getGenerate()) {
                return 0;
            }
            Optional<PowerComponentInfo> info = s.get().getInfoForPos(this.getPos());
            return info.map(powerComponentInfo -> powerComponentInfo.type() == PowerComponentType.PRODUCER
                    ? powerComponentInfo.produces()
                    : powerComponentInfo.consumes())
                .orElse(1);
        } else {
            return Math.abs(this.getOutputPower());
        }
    }
}
