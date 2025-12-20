package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DangerUtil {

    public static Supplier<ConfiguredModel[]> genConfiguredModel(String path) {
        return () -> new ConfiguredModel[]{new ConfiguredModel(new ModelFile.UncheckedModelFile(AnvilCraft.of(path)))};
    }

    public static Supplier<ModelFile> genModModelFile(String path) {
        return () -> new ModelFile.UncheckedModelFile(AnvilCraft.of(path));
    }

    public static Supplier<ModelFile.UncheckedModelFile> genUncheckedModelFile(String path) {
        return () -> new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace(path));
    }

    public static Supplier<ModelFile.UncheckedModelFile> genUncheckedModelFile(String namespace, String path) {
        return () -> new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
