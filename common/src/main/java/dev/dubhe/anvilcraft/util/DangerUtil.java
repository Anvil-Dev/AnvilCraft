package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import org.jetbrains.annotations.NotNull;

public final class DangerUtil {
    private DangerUtil() {
    }

    public static ConfiguredModel @NotNull [] genConfiguredModel(String path) {
        return new ConfiguredModel[]{new ConfiguredModel(new ModelFile.UncheckedModelFile(AnvilCraft.of(path)))};
    }

    public static ModelFile.@NotNull UncheckedModelFile genUncheckedModelFile(String path) {
        return new ModelFile.UncheckedModelFile(AnvilCraft.of(path));
    }
}
