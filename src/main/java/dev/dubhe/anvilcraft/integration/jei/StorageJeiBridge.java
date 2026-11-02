package dev.dubhe.anvilcraft.integration.jei;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 仓储界面与 JEI 的桥接入口：通过反射打开配方，通过 JEI 注册的回调识别父屏幕，
 * 未安装 JEI 时各方法安全地不做任何事。
 * 反射查找（{@link Class#forName} / {@link Class#getMethod}）结果缓存，
 * 避免每次调用重复执行开销。
 */
public final class StorageJeiBridge {
    private static final String SUPPORT = "dev.dubhe.anvilcraft.integration.jei.StorageJeiSupport";

    /** 已解析的方法缓存：类加载后首次调用时填充，未安装 JEI 时为 null。 */
    private static volatile @Nullable Method openStonecutterRecipesMethod;
    private static volatile @Nullable Method openCraftingRecipesMethod;
    private static volatile @Nullable Method isAvailableMethod;
    private static Predicate<Screen> recipeParent = screen -> false;

    private StorageJeiBridge() {
    }

    public static void setRecipeParent(Predicate<Screen> parent) {
        StorageJeiBridge.recipeParent = parent;
    }

    public static boolean isRecipeParent(Screen screen) {
        return StorageJeiBridge.recipeParent.test(screen);
    }

    /** 打开切石机配方 JEI 界面（点击③ 结果槽区域时调用）。 */
    public static void openStonecutterRecipes() {
        StorageJeiBridge.call(StorageJeiBridge.openStonecutterRecipesMethod, "openStonecutterRecipes");
    }

    /** 打开合成配方 JEI 界面（点击④ 结果槽区域时调用）。 */
    public static void openCraftingRecipes() {
        StorageJeiBridge.call(StorageJeiBridge.openCraftingRecipesMethod, "openCraftingRecipes");
    }

    /** JEI 是否可用（已安装且运行时可用）。 */
    public static boolean isAvailable() {
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
