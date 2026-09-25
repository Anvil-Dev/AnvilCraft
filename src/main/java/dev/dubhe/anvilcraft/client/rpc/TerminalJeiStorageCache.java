package dev.dubhe.anvilcraft.client.rpc;

import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class TerminalJeiStorageCache {
    private static final long TTL_MILLIS = 60000;
    private static final Map<List<UUID>, Entry> CACHE = new HashMap<>();
    private static final Map<List<UUID>, CompletableFuture<StorageServerStub.TerminalSnapshot>> PENDING = new HashMap<>();
    private static final Map<List<UUID>, CompletableFuture<StorageServerStub.TerminalSnapshot>> COMPLETE_PENDING = new HashMap<>();
    private static final Set<Function<Player, ItemStack>> PROVIDERS = new LinkedHashSet<>();
    private static @Nullable ClientPacketListener connection;
    private static long epoch;
    private static boolean busy;
    private static boolean retrying;

    private TerminalJeiStorageCache() {
    }

    public static void addStackProvider(Function<Player, ItemStack> provider) {
        PROVIDERS.add(provider);
    }

    public static List<UUID> boundStorages(Player player) {
        Set<UUID> targets = new LinkedHashSet<>();
        List<ItemStack> terminals = new ArrayList<>(TerminalItem.getAll(player));
        for (var provider : PROVIDERS) terminals.add(provider.apply(player));
        for (ItemStack stack : terminals) {
            if (stack != null && stack.getItem() instanceof TerminalItem terminal) {
                UUID target = terminal.targetId(player, stack);
                if (target != null) targets.add(target);
            }
        }
        return List.copyOf(targets);
    }

    private static void checkConnection() {
        var current = Minecraft.getInstance().getConnection();
        if (current != connection) {
            clear();
            connection = current;
        }
    }

    public static StorageServerStub.@Nullable TerminalSnapshot get(List<UUID> targets) {
        checkConnection();
        Entry entry = CACHE.get(targets);
        return entry == null || System.currentTimeMillis() - entry.loadedAt >= TTL_MILLIS ? null : entry.snapshot;
    }

    public static CompletableFuture<StorageServerStub.TerminalSnapshot> ensure(List<UUID> targets) {
        var snapshot = get(targets);
        if (snapshot != null) return CompletableFuture.completedFuture(snapshot);
        final List<UUID> key = List.copyOf(targets);
        var existing = PENDING.get(key);
        if (existing != null) return existing;
        if (Minecraft.getInstance().player == null || connection == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No connected client player"));
        }
        final long requestedEpoch = epoch;
        var request = StorageTerminalClientStub.snapshot(key);
        PENDING.put(key, request);
        request.whenCompleteAsync((result, error) -> {
            if (epoch != requestedEpoch || PENDING.get(key) != request) return;
            PENDING.remove(key);
            if (error == null) CACHE.put(key, new Entry(result, System.currentTimeMillis()));
        }, Minecraft.getInstance());
        return request;
    }

    public static CompletableFuture<StorageServerStub.TerminalSnapshot> ensureComplete(List<UUID> targets) {
        var snapshot = get(targets);
        if (snapshot != null && snapshot.complete()) return CompletableFuture.completedFuture(snapshot);
        final List<UUID> key = List.copyOf(targets);
        var existing = COMPLETE_PENDING.get(key);
        if (existing != null) return existing;
        if (Minecraft.getInstance().player == null || connection == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No connected client player"));
        }
        final var request = new CompletableFuture<StorageServerStub.TerminalSnapshot>();
        final long requestedEpoch = epoch;
        COMPLETE_PENDING.put(key, request);
        TerminalSnapshotLoader.load(key, (target, offset) -> {
            if (!isCurrent(requestedEpoch) || Minecraft.getInstance().player == null || COMPLETE_PENDING.get(key) != request) {
                return CompletableFuture.failedFuture(new IllegalStateException("Terminal contents request is no longer current"));
            }
            return StorageTerminalClientStub.contentsPage(target, offset);
        }, Minecraft.getInstance()).whenCompleteAsync((result, error) -> {
            if (epoch == requestedEpoch && COMPLETE_PENDING.get(key) == request) {
                COMPLETE_PENDING.remove(key);
                if (error == null) {
                    PENDING.remove(key);
                    CACHE.put(key, new Entry(result, System.currentTimeMillis()));
                }
            }
            if (error == null) request.complete(result);
            else request.completeExceptionally(error);
        }, Minecraft.getInstance());
        return request;
    }

    public static void invalidate(List<UUID> targets) {
        CACHE.remove(targets);
        PENDING.remove(targets);
        COMPLETE_PENDING.remove(targets);
    }

    public static boolean isBusy() {
        checkConnection();
        return busy;
    }

    public static long begin() {
        checkConnection();
        busy = true;
        return epoch;
    }

    public static boolean isCurrent(long requestedEpoch) {
        checkConnection();
        return epoch == requestedEpoch;
    }

    public static void finish(long requestedEpoch) {
        if (isCurrent(requestedEpoch)) busy = false;
    }

    public static boolean isRetrying() {
        return retrying;
    }

    public static void setRetrying(boolean value) {
        retrying = value;
    }

    public static void clear() {
        epoch++;
        connection = null;
        CACHE.clear();
        PENDING.clear();
        COMPLETE_PENDING.clear();
        busy = false;
        retrying = false;
    }

    private record Entry(StorageServerStub.TerminalSnapshot snapshot, long loadedAt) {
    }
}
