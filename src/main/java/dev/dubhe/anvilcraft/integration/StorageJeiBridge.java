package dev.dubhe.anvilcraft.integration;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** JEI 在运行时注册入口，使仓储屏幕不直接加载可选依赖。 */
public final class StorageJeiBridge {
    private static @Nullable Consumer<Boolean> recipeOpener;

    private StorageJeiBridge() {
    }

    public static void setRecipeOpener(@Nullable Consumer<Boolean> opener) {
        recipeOpener = opener;
    }

    public static boolean openRecipes(boolean stonecutter) {
        if (recipeOpener == null) return false;
        recipeOpener.accept(stonecutter);
        return true;
    }
}
