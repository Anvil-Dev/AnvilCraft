package dev.dubhe.anvilcraft.integration;

/**
 * 仓储界面与 JEI 的桥接入口：本类不依赖任何 mezz 类（纯反射调用
 * {@code StorageJeiSupport}），未安装 JEI 时各方法安全地不做任何事。
 */
public final class StorageJeiBridge {
    private static final String SUPPORT = "dev.dubhe.anvilcraft.integration.jei.StorageJeiSupport";

    private StorageJeiBridge() {
    }

    /** 打开切石机配方 JEI 界面（点击③ 结果槽区域时调用）。 */
    public static void openStonecutterRecipes() {
        StorageJeiBridge.call("openStonecutterRecipes");
    }

    /** 打开合成配方 JEI 界面（点击④ 结果槽区域时调用）。 */
    public static void openCraftingRecipes() {
        StorageJeiBridge.call("openCraftingRecipes");
    }

    /** JEI 是否可用（已安装且运行时可用）。 */
    public static boolean isAvailable() {
        try {
            Object result = Class.forName(StorageJeiBridge.SUPPORT)
                .getMethod("isAvailable")
                .invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void call(String method) {
        try {
            Class.forName(StorageJeiBridge.SUPPORT)
                .getMethod(method)
                .invoke(null);
        } catch (Throwable ignored) {
            // 未安装 JEI 时忽略
        }
    }
}
