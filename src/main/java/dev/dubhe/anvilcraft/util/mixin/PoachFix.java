package dev.dubhe.anvilcraft.util.mixin;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PoachFix {
    private static final String NO_POACH_METHOD_NAME = "shouldNotPoach_PreVeNTDUpLICatioN";

    public static boolean shouldItPoach() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : elements) {
            if (element.getMethodName().contains(NO_POACH_METHOD_NAME)) return false;
        }
        return true;
    }
}
