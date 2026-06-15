package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;

public enum IrradiatorType implements StringRepresentable {
    NEUTRON("neutron"),
    ENERGY("energy"),
    MASS("mass"),
    SPACE("space"),
    TIME("time");

    @Getter
    private final String name;

    IrradiatorType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
