package dev.dubhe.anvilcraft.integration.jei;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 仓储界面与配方查看器的桥接入口：优先调用直接注册的回调，未注册时回退到 JEI 反射入口，
 * 未安装配方查看器时各方法安全地不做任何事。
 */
public final class StorageJeiBridge {
    private static final String SUPPORT = "dev.dubhe.anvilcraft.integration.jei.StorageJeiSupport";

    /** 已解析的方法缓存：类加载后首次调用时填充，未安装 JEI 时为 null。 */
    private static volatile @Nullable Method openStonecutterRecipesMethod;
    private static volatile @Nullable Method openCraftingRecipesMethod;
    private static volatile @Nullable Method isAvailableMethod;
    private static Predicate<Screen> recipeParent = screen -> false;
    private static @Nullable Consumer<Boolean> emiRecipeOpener;
    private static @Nullable Predicate<Screen> emiRecipeParent;

    private StorageJeiBridge() {
    }

    public static void setRecipeParent(Predicate<Screen> parent) {
        StorageJeiBridge.recipeParent = parent;
    }

    public static void setEmiRecipeOpener(@Nullable Consumer<Boolean> opener) {
        StorageJeiBridge.emiRecipeOpener = opener;
    }

    public static void setEmiRecipeParent(@Nullable Predicate<Screen> parent) {
        StorageJeiBridge.emiRecipeParent = parent;
    }

    public static boolean isRecipeParent(Screen screen) {
        return StorageJeiBridge.emiRecipeParent != null && StorageJeiBridge.emiRecipeParent.test(screen)
            || StorageJeiBridge.recipeParent.test(screen);
    }

    /** 打开切石机配方查看界面（点击③ 结果槽区域时调用）。 */
    public static void openStonecutterRecipes() {
        if (StorageJeiBridge.emiRecipeOpener != null) {
            StorageJeiBridge.emiRecipeOpener.accept(true);
            return;
        }
        StorageJeiBridge.call(StorageJeiBridge.openStonecutterRecipesMethod, "openStonecutterRecipes");
    }

    /** 打开合成配方查看界面（点击④ 结果槽区域时调用）。 */
    public static void openCraftingRecipes() {
        if (StorageJeiBridge.emiRecipeOpener != null) {
            StorageJeiBridge.emiRecipeOpener.accept(false);
            return;
        }
        StorageJeiBridge.call(StorageJeiBridge.openCraftingRecipesMethod, "openCraftingRecipes");
    }

    /** 配方查看器是否已安装且运行时可用。 */
    public static boolean isAvailable() {
        if (StorageJeiBridge.emiRecipeOpener != null) {
            return true;
        }
        try {
            Object result = Objects.requireNonNull(StorageJeiBridge.resolve(StorageJeiBridge.isAvailableMethod, "isAvailable"))
                .invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void call(@Nullable Method cached, String name) {
        try {
            Objects.requireNonNull(StorageJeiBridge.resolve(cached, name)).invoke(null);
        } catch (Throwable ignored) {
            // 未安装 JEI 时忽略
        }
    }

    /** 返回缓存的方法；未解析过（或未安装 JEI）时查找并缓存。 */
    private static @Nullable Method resolve(@Nullable Method cached, String name) {
        if (cached != null) {
            return cached;
        }
        try {
            Method method = Class.forName(StorageJeiBridge.SUPPORT).getMethod(name);
            StorageJeiBridge.cache(name, method);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void cache(String name, Method method) {
        if (name.equals("openStonecutterRecipes")) {
            StorageJeiBridge.openStonecutterRecipesMethod = method;
        } else if (name.equals("openCraftingRecipes")) {
            StorageJeiBridge.openCraftingRecipesMethod = method;
        } else {
            StorageJeiBridge.isAvailableMethod = method;
        }
    }
}
