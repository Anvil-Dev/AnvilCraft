package dev.dubhe.anvilcraft.api.power;

/// 用电
public interface IPowerConsumer extends IPowerComponent {
    default int getInputPower() {
        return 0;
    }

    @Override
    default PowerComponentType getComponentType() {
        return PowerComponentType.CONSUMER;
    }

    @Override
    default PowerComponentInfo toPowerComponentInfo() {
        return new PowerComponentInfo(
            this.getPos(),
            this.getInputPower(),
            0,
            0,
            0,
            this.getRange(),
            this.getShape(),
            PowerComponentType.CONSUMER
        );
    }
}
