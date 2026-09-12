package dev.dubhe.anvilcraft.api.laser;

public record LaserStrengthComponent(int strength) implements ILaserComponent {
    public LaserStrengthComponent {
        if (strength < 0) throw new IllegalArgumentException("Laser strength cannot be negative");
    }

    public static int getStrength(ILaserComponentOwner owner) {
        LaserStrengthComponent component = owner.getComponent(LaserComponentTypes.STRENGTH);
        return component == null ? 0 : component.strength;
    }
}
