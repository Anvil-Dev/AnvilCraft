package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Accessors(fluent = true)
public enum Color implements StringRepresentable {
    BLACK("black", Items.BLACK_DYE),
    BLUE("blue", Items.BLUE_DYE),
    BROWN("brown", Items.BROWN_DYE),
    CYAN("cyan", Items.CYAN_DYE),
    GRAY("gray", Items.GRAY_DYE),
    GREEN("green", Items.GREEN_DYE),
    LIGHT_BLUE("light_blue", Items.LIGHT_BLUE_DYE),
    LIGHT_GRAY("light_gray", Items.LIGHT_GRAY_DYE),
    LIME("lime", Items.LIME_DYE),
    MAGENTA("magenta", Items.MAGENTA_DYE),
    ORANGE("orange", Items.ORANGE_DYE),
    PINK("pink", Items.PINK_DYE),
    PURPLE("purple", Items.PURPLE_DYE),
    RED("red", Items.RED_DYE),
    WHITE("white", Items.WHITE_DYE),
    YELLOW("yellow", Items.YELLOW_DYE);

    private final String name;

    @Getter
    private final Item dyeItem;

    Color(String name, Item dyeItem) {
        this.name = name;
        this.dyeItem = dyeItem;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    @Nullable
    public static Color getColorByDyeItem(Item dyeItem) {
        if (dyeItem == Items.BLACK_DYE) {
            return BLACK;
        } else if (dyeItem == Items.BLUE_DYE) {
            return BLUE;
        } else if (dyeItem == Items.BROWN_DYE) {
            return BROWN;
        } else if (dyeItem == Items.CYAN_DYE) {
            return CYAN;
        } else if (dyeItem == Items.GRAY_DYE) {
            return GRAY;
        } else if (dyeItem == Items.GREEN_DYE) {
            return GREEN;
        } else if (dyeItem == Items.LIGHT_BLUE_DYE) {
            return LIGHT_BLUE;
        } else if (dyeItem == Items.LIGHT_GRAY_DYE) {
            return LIGHT_GRAY;
        } else if (dyeItem == Items.LIME_DYE) {
            return LIME;
        } else if (dyeItem == Items.MAGENTA_DYE) {
            return MAGENTA;
        } else if (dyeItem == Items.ORANGE_DYE) {
            return ORANGE;
        } else if (dyeItem == Items.PINK_DYE) {
            return PINK;
        } else if (dyeItem == Items.PURPLE_DYE) {
            return PURPLE;
        } else if (dyeItem == Items.RED_DYE) {
            return RED;
        } else if (dyeItem == Items.WHITE_DYE) {
            return WHITE;
        } else if (dyeItem == Items.YELLOW_DYE) {
            return YELLOW;
        } else {
            return null;
        }
    }

    public static Color getColorByIndex(int index) {
        Color[] values = values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return values[0];
    }
}
