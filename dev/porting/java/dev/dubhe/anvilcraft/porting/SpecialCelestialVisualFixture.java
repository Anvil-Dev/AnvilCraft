package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ColorRGBA;

public final class SpecialCelestialVisualFixture {
    public static CompoundTag snapshot(String name) {
        boolean complex = name.contains("complex");
        var body = new SpecialCelestialBodyData("fixture:special", complex ? "flesh_planet" : "overworld_like",
            16, 0, 0, 2, name.startsWith("no-temperature") ? null : Temperature.MILD,
            name.startsWith("no-atmosphere") ? null : new ColorRGBA(name.contains("magenta") ? 0xFF20E0 : 0x20FFD0),
            LiquidCoverage.MEDIUM, false, complex, false, complex ? "planet_flesh" : "planet_overworld", null, null);
        if (name.startsWith("gateway")) body = gatewayBody();
        CompoundTag tag = new CompoundTag();
        tag.put("celestialBody", body.toTag());
        tag.putLong("bodySeed", 73);
        tag.putBoolean("amplified", false);
        tag.putBoolean("amplifierPresent", false);
        tag.putBoolean("locked", true);
        tag.putInt("stellarMass", 20);
        tag.putInt("ageAnvilCount", 32);
        return tag;
    }

    public static SpecialCelestialBodyData gatewayBody() {
        return new SpecialCelestialBodyData("fixture:gateway", "void_planet", 16, 0, 2, 0,
            null, false, LiquidCoverage.NONE, false, false, SpecialCelestialBodyData.END_GATEWAY_MODEL, null);
    }

}
