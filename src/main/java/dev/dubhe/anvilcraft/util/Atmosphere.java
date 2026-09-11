package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** 各气体含量以主世界气压为基准，不进行归一化，总和即为气压。 */
public record Atmosphere(Map<ResourceLocation, Double> gases) {
    public static final ResourceLocation NITROGEN = AnvilCraft.of("nitrogen");
    public static final ResourceLocation OXYGEN = AnvilCraft.of("oxygen");
    public static final Atmosphere VACUUM = new Atmosphere(Map.of());
    public static final Atmosphere OVERWORLD = new Atmosphere(Map.of(NITROGEN, 0.8, OXYGEN, 0.2));

    public Atmosphere {
        gases = Map.copyOf(gases);
        double pressure = 0.0;
        for (double amount : gases.values()) {
            if (!Double.isFinite(amount) || amount < 0.0) {
                throw new IllegalArgumentException("Gas amounts must be finite and non-negative");
            }
            pressure += amount;
        }
        if (!Double.isFinite(pressure)) {
            throw new IllegalArgumentException("Atmospheric pressure must be finite");
        }
    }

    public double gasAmount(ResourceLocation gas) {
        return this.gases.getOrDefault(gas, 0.0);
    }

    public double pressure() {
        return this.gases.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
