package dev.dubhe.anvilcraft.util.mixin;

public class ProvidenceRef {
    private static final ThreadLocal<Boolean> SHOULD_TRIGGER = new ThreadLocal<>();

    public static void shouldTrigger() {
        SHOULD_TRIGGER.set(true);
    }

    public static boolean shouldItTrigger() {
        return SHOULD_TRIGGER.get();
    }

    public static void reset() {
        SHOULD_TRIGGER.remove();
    }
}
