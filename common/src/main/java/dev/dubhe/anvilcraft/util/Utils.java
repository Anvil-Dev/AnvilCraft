package dev.dubhe.anvilcraft.util;

import dev.architectury.injectables.annotations.ExpectPlatform;

public abstract class Utils {
    private Utils() {
    }

    /**
     * @return 模组是否加载
     */
    @ExpectPlatform
    public static boolean isLoaded(String modid) {
        throw new AssertionError();
    }
}
