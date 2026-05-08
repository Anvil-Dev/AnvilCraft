package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DangerUtil {

    public static MultiVariant genConfiguredModel(String path) {
        return new MultiVariant(
            WeightedList.<Variant>builder()
                .add(new Variant(AnvilCraft.of(path)))
                .build()
        );
    }

    public static Identifier genModModelFile(String path) {
        return AnvilCraft.of(path);
    }

    public static Identifier genUncheckedModelFile(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public static Identifier genUncheckedModelFile(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
