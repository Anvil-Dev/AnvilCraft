package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageTerminalServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TerminalReachabilityCache {
    private static final long TTL_MILLIS = 2000;
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
    private static final Map<UUID, CompletableFuture<Boolean>> PENDING = new HashMap<>();
    private static @Nullable ClientPacketListener connection;
    private static int epoch;

    private TerminalReachabilityCache() {
    }

    public static boolean isReachable(UUID terminalId) {
        Boolean reachable = getReachability(terminalId);
        return reachable == null || reachable;
    }

    public static @Nullable Boolean getReachability(UUID terminalId) {
        if (connection != Minecraft.getInstance().getConnection()) return null;
        Entry entry = ENTRIES.get(terminalId);
        return entry == null || System.currentTimeMillis() >= entry.expiresAt ? null : entry.reachable;
    }

    public static void ensure(UUID terminalId) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;
        if (connection != client.getConnection()) {
            clear();
            connection = client.getConnection();
        }
        if (getReachability(terminalId) != null || PENDING.containsKey(terminalId)) return;
        final int requestEpoch = epoch;
        var request = RPC.invoke(RpcTarget.server(), StorageTerminalServerStub::isTerminalReachable, client.player.getUUID(), terminalId);
        PENDING.put(terminalId, request);
        request.whenCompleteAsync((reachable, error) -> {
            if (requestEpoch != epoch) return;
            PENDING.remove(terminalId, request);
            ENTRIES.put(terminalId, new Entry(error == null && Boolean.TRUE.equals(reachable), System.currentTimeMillis() + TTL_MILLIS));
        }, client);
    }

    public static void clear() {
        epoch++;
        connection = null;
        ENTRIES.clear();
        PENDING.clear();
    }

    private record Entry(boolean reachable, long expiresAt) {
    }
}
