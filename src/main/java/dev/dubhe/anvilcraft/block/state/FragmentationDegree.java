package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@Getter
public enum FragmentationDegree implements StringRepresentable {
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3");

    private final String value;

    FragmentationDegree(String value) {
        this.value = value;
    }

    @Override
    public String getSerializedName() {
        return this.value;
    }

    public FragmentationDegree next() {
        return switch (this) {
            case ZERO -> ONE;
            case ONE -> TWO;
            case TWO, THREE -> THREE;
        };
    }
}
