package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum LightColor implements StringRepresentable {
    PRIMARY("primary",1),
    PINK("pink",16),
    DARK("dark",16),
    YELLOW("yellow",16)
        ;

    public final String name;
    public final int dissipation;
    LightColor(String name, int dissipation) {
        this.name=name;
        this.dissipation=dissipation;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
