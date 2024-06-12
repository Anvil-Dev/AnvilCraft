package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.resources.ResourceLocation;

import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class DangerUtil {
    private DangerUtil() {}

    public static @NotNull Supplier<ConfiguredModel[]> genConfiguredModel(String path) {
        return () -> new ConfiguredModel[] {
            new ConfiguredModel(new ModelFile.UncheckedModelFile(AnvilCraft.of(path)))
        };
    }

    public static @NotNull Supplier<ModelFile.UncheckedModelFile> genUncheckedModelFile(String path) {
        return () -> new ModelFile.UncheckedModelFile(new ResourceLocation(path));
    }

    public static @NotNull Supplier<ModelFile.UncheckedModelFile> genUncheckedModelFile(
            String namespace, String path) {
        return () -> new ModelFile.UncheckedModelFile(new ResourceLocation(namespace, path));
    }
}
