package dev.dubhe.anvilcraft.integration;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** 配方查看器在运行时注册入口，使仓储屏幕不直接加载可选依赖。 */
public final class StorageJeiBridge {
    private static @Nullable Consumer<Boolean> recipeOpener;
    private static @Nullable Predicate<Screen> recipeParent;
    private static @Nullable Consumer<Boolean> emiRecipeOpener;
    private static @Nullable Predicate<Screen> emiRecipeParent;

    private StorageJeiBridge() {
    }

    public static void setRecipeOpener(@Nullable Consumer<Boolean> opener) {
        recipeOpener = opener;
    }

    public static void setRecipeParent(@Nullable Predicate<Screen> parent) {
        recipeParent = parent;
    }

    public static void setEmiRecipeOpener(@Nullable Consumer<Boolean> opener) {
        emiRecipeOpener = opener;
    }

    public static void setEmiRecipeParent(@Nullable Predicate<Screen> parent) {
        emiRecipeParent = parent;
    }

    public static boolean isRecipeParent(Screen screen) {
        return emiRecipeParent != null && emiRecipeParent.test(screen)
            || recipeParent != null && recipeParent.test(screen);
    }

    public static boolean openRecipes(boolean stonecutter) {
        if (emiRecipeOpener != null) {
            emiRecipeOpener.accept(stonecutter);
            return true;
        }
        if (recipeOpener == null) {
            return false;
        }
        recipeOpener.accept(stonecutter);
        return true;
    }
}
