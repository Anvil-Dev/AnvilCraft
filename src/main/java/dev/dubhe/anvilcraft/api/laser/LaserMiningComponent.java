package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.util.BlockMiningEffect;

public record LaserMiningComponent(BlockMiningEffect effect, boolean specialTargets) implements ILaserComponent {
    public static BlockMiningEffect getEffect(ILaserComponentOwner owner) {
        LaserMiningComponent component = owner.getComponent(LaserComponentTypes.MINING);
        return component == null ? BlockMiningEffect.NORMAL : component.effect;
    }
}
