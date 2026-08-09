package dev.dubhe.anvilcraft.util.mixin;

public class ProvidenceRef {
    private static final ThreadLocal<Boolean> SHOULD_TRIGGER = ThreadLocal.withInitial(() -> false);

    public static void shouldTrigger() {
        ProvidenceRef.SHOULD_TRIGGER.set(true);
    }

    public static boolean shouldItTrigger() {
        return ProvidenceRef.SHOULD_TRIGGER.get();
    }

    public static void reset() {
        ProvidenceRef.SHOULD_TRIGGER.remove();
    }
}
