package dev.dubhe.anvilcraft.data.recipe.transform;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum TransformOptions implements StringRepresentable {


    KEEP_INVENTORY("keepInventory");

    public static final Codec<TransformOptions> CODEC = StringRepresentable.fromEnum(TransformOptions::values);
    private final String name;

    TransformOptions(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
