package dev.dubhe.anvilcraft.util;

import dev.architectury.injectables.annotations.ExpectPlatform;

public abstract class Utils {
    private Utils() {
    }

    /**
     * @return 是否只加载了 JEI
     */
    @ExpectPlatform
    public static boolean onlyJei() {
        throw new AssertionError();
    }
}
