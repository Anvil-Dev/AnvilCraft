package dev.dubhe.anvilcraft.block.state;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

@Accessors(fluent = true)
public enum Color implements StringRepresentable {
    WHITE("white", Items.WHITE_DYE, DyeColor.WHITE),
    LIGHT_GRAY("light_gray", Items.LIGHT_GRAY_DYE, DyeColor.LIGHT_GRAY),
    GRAY("gray", Items.GRAY_DYE, DyeColor.GRAY),
    BLACK("black", Items.BLACK_DYE, DyeColor.BLACK),
    BROWN("brown", Items.BROWN_DYE, DyeColor.BROWN),
    RED("red", Items.RED_DYE, DyeColor.RED),
    ORANGE("orange", Items.ORANGE_DYE, DyeColor.ORANGE),
    YELLOW("yellow", Items.YELLOW_DYE, DyeColor.YELLOW),
    LIME("lime", Items.LIME_DYE, DyeColor.LIME),
    GREEN("green", Items.GREEN_DYE, DyeColor.GREEN),
    CYAN("cyan", Items.CYAN_DYE, DyeColor.CYAN),
    LIGHT_BLUE("light_blue", Items.LIGHT_BLUE_DYE, DyeColor.LIGHT_BLUE),
    BLUE("blue", Items.BLUE_DYE, DyeColor.BLUE),
    PURPLE("purple", Items.PURPLE_DYE, DyeColor.PURPLE),
    MAGENTA("magenta", Items.MAGENTA_DYE, DyeColor.MAGENTA),
    PINK("pink", Items.PINK_DYE, DyeColor.PINK);

    private final String name;

    @Getter
    private final Item dyeItem;

    @Getter
    private final DyeColor color;

    public static final Codec<Color> CODEC = StringRepresentable.fromValues(Color::values);

    Color(String name, Item dyeItem, DyeColor color) {
        this.name = name;
        this.dyeItem = dyeItem;
        this.color = color;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Nullable
    public static Color getColorByDyeItem(Item dyeItem) {
        if (dyeItem == Items.BLACK_DYE) {
            return Color.BLACK;
        } else if (dyeItem == Items.BLUE_DYE) {
            return Color.BLUE;
        } else if (dyeItem == Items.BROWN_DYE) {
            return Color.BROWN;
        } else if (dyeItem == Items.CYAN_DYE) {
            return Color.CYAN;
        } else if (dyeItem == Items.GRAY_DYE) {
            return Color.GRAY;
        } else if (dyeItem == Items.GREEN_DYE) {
            return Color.GREEN;
        } else if (dyeItem == Items.LIGHT_BLUE_DYE) {
            return Color.LIGHT_BLUE;
        } else if (dyeItem == Items.LIGHT_GRAY_DYE) {
            return Color.LIGHT_GRAY;
        } else if (dyeItem == Items.LIME_DYE) {
            return Color.LIME;
        } else if (dyeItem == Items.MAGENTA_DYE) {
            return Color.MAGENTA;
        } else if (dyeItem == Items.ORANGE_DYE) {
            return Color.ORANGE;
        } else if (dyeItem == Items.PINK_DYE) {
            return Color.PINK;
        } else if (dyeItem == Items.PURPLE_DYE) {
            return Color.PURPLE;
        } else if (dyeItem == Items.RED_DYE) {
            return Color.RED;
        } else if (dyeItem == Items.WHITE_DYE) {
            return Color.WHITE;
        } else if (dyeItem == Items.YELLOW_DYE) {
            return Color.YELLOW;
        } else {
            return null;
        }
    }

    public static Color getColorByIndex(int index) {
        Color[] values = Color.values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return values[0];
    }
}
