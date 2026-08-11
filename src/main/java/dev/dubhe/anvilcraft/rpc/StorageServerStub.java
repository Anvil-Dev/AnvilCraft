package dev.dubhe.anvilcraft.rpc;

import java.util.UUID;

/**
 * 服务端仓储 RPC 桩。完整 RPC 逻辑在 P4 移植。
 */
public final class StorageServerStub {
    private StorageServerStub() {
    }

    public static void onContentsChanged(UUID storageId) {
    }

    public static void remove(UUID playerId) {
    }

    public static void clear() {
    }
}
