package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public class BundleLikeServerStub {
    private static final Map<UUID, Boolean> INVERTED = new HashMap<>();

    @RemoteCallable
    public static void updateInverted(UUID id, boolean inverted) {
        BundleLikeServerStub.INVERTED.put(id, inverted);
    }

    public static boolean isInvertedAction(UUID id) {
        return Boolean.TRUE.equals(BundleLikeServerStub.INVERTED.get(id));
    }

    /** 玩家退出时清理其反色动作状态，避免静态表永久残留。 */
    public static void clear(UUID id) {
        BundleLikeServerStub.INVERTED.remove(id);
    }
}
