package dev.dubhe.anvilcraft.integration;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** JEI 在运行时注册入口，使仓储屏幕不直接加载可选依赖。 */
public final class StorageJeiBridge {
    private static @Nullable Consumer<Boolean> recipeOpener;
    private static @Nullable Predicate<Screen> recipeParent;

    private StorageJeiBridge() {
    }

    public static void setRecipeOpener(@Nullable Consumer<Boolean> opener) {
        recipeOpener = opener;
    }

    public static void setRecipeParent(@Nullable Predicate<Screen> parent) {
        recipeParent = parent;
    }

    public static boolean isRecipeParent(Screen screen) {
        return recipeParent != null && recipeParent.test(screen);
    }

    public static boolean openRecipes(boolean stonecutter) {
        if (recipeOpener == null) return false;
        recipeOpener.accept(stonecutter);
        return true;
    }
}
