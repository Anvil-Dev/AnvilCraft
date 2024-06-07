package dev.dubhe.anvilcraft.block.state.giantanvil;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum GiantAnvilStructure implements StringRepresentable {
    BASE("base"),
    BASE_ANGLE("base_angle"),
    MID_EDGE("mid_edge"),
    MID_ANGLE("mid_angle"),
    TOP("top")
        ;

    private final String name;

    GiantAnvilStructure(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
