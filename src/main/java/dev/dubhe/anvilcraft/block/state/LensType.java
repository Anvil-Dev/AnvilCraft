package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;

public enum LensType implements StringRepresentable {
    NONE("none"),
    ROYAL("royal"),
    FROST("frost"),
    EMBER("ember");

    private final String name;

    LensType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
