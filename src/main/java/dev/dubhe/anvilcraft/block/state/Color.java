package dev.dubhe.anvilcraft.block.state;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

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
}
