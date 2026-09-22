package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.rpc.StorageTerminalServerStub;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageTerminalClientStub {
    private static final Map<UUID, CompletableFuture<Long>> VIRTUAL_POSITIONS = new HashMap<>();
    private static @Nullable ClientPacketListener cacheConnection;
    private static int cacheEpoch;
    private static @Nullable CompletableFuture<Long> pendingOpen;
    private static @Nullable ClientPacketListener pendingConnection;

    private StorageTerminalClientStub() {
    }

    public static CompletableFuture<Long> openRemote(UUID target) {
        return RPC.invoke(RpcTarget.server(), StorageTerminalServerStub::openRemote, Minecraft.getInstance().player.getUUID(), target);
    }

    public static CompletableFuture<Long> ensureVirtualPos(UUID target) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No connected client player"));
        }
        if (cacheConnection != client.getConnection()) {
            cacheEpoch++;
            VIRTUAL_POSITIONS.clear();
            cacheConnection = client.getConnection();
        }
        CompletableFuture<Long> existing = VIRTUAL_POSITIONS.get(target);
        if (existing != null) return existing;
        final int epoch = cacheEpoch;
        CompletableFuture<Long> request = openRemote(target);
        VIRTUAL_POSITIONS.put(target, request);
        request.whenCompleteAsync((token, error) -> {
            if (epoch == cacheEpoch && (error != null || token == null || token == -1L)) VIRTUAL_POSITIONS.remove(target, request);
        }, client);
        return request;
    }

    public static CompletableFuture<IntList> reorder(UUID target, String search) {
        return invoke(target, token -> RPC.invoke(RpcTarget.server(), StorageServerStub::terminalReorder,
            Minecraft.getInstance().player.getUUID(), token, search));
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> take(UUID target, int slot, int button, ItemStack carried) {
        return invoke(target, token -> RPC.invoke(RpcTarget.server(), StorageServerStub::terminalTake,
            Minecraft.getInstance().player.getUUID(), token, slot, button, carried));
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> takeToInventory(UUID target, int slot, int button) {
        return invoke(target, token -> RPC.invoke(RpcTarget.server(), StorageServerStub::terminalTakeToInventory,
            Minecraft.getInstance().player.getUUID(), token, slot, button));
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> insert(UUID target, ItemStack carried) {
        return invoke(target, token -> RPC.invoke(RpcTarget.server(), StorageServerStub::terminalInsert,
            Minecraft.getInstance().player.getUUID(), token, carried));
    }

    private static <T> CompletableFuture<T> invoke(UUID target, java.util.function.Function<Long, CompletableFuture<T>> operation) {
        CompletableFuture<Long> position = ensureVirtualPos(target);
        final int epoch = cacheEpoch;
        return position.thenComposeAsync(token -> {
            if (epoch != cacheEpoch) return CompletableFuture.failedFuture(new IllegalStateException("Terminal session changed"));
            if (token == -1L) return CompletableFuture.failedFuture(new IllegalStateException("Terminal is unreachable"));
            return operation.apply(token);
        }, Minecraft.getInstance()).whenCompleteAsync((result, error) -> {
            if (epoch == cacheEpoch && error != null) VIRTUAL_POSITIONS.remove(target, position);
        }, Minecraft.getInstance());
    }

    public static void clear() {
        cacheEpoch++;
        cacheConnection = null;
        VIRTUAL_POSITIONS.clear();
        pendingOpen = null;
        pendingConnection = null;
    }

    public static void open(Player player, ItemStack stack, TerminalItem.Kind kind) {
        if (!(stack.getItem() instanceof TerminalItem terminal)) return;
        var client = Minecraft.getInstance();
        if (pendingConnection == client.getConnection() && pendingOpen != null) return;
        final var origin = client.screen;
        UUID target = terminal.targetId(player, stack);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("message.anvilcraft.hyperdimension_terminal.not_bound"));
            return;
        }
        String name = switch (kind) {
            case LOCAL -> "local_terminal";
            case SHULKER -> "shulker_terminal";
            case HYPERDIMENSION -> "hyperdimension_terminal";
        };
        String title = switch (kind) {
            case LOCAL -> "large_crate";
            case SHULKER -> "shulker_container";
            case HYPERDIMENSION -> "hyperdimension_storage_station";
        };
        CompletableFuture<Long> request = openRemote(target);
        pendingOpen = request;
        pendingConnection = client.getConnection();
        request.whenCompleteAsync((token, error) -> {
            if (pendingOpen == request) {
                pendingOpen = null;
                pendingConnection = null;
            }
            if (client.player != player || client.screen != origin) return;
            if (error != null || token == null || token == -1L) {
                player.sendOverlayMessage(Component.translatable("message.anvilcraft." + name + ".not_found"));
                return;
            }
            StorageScreen.openScreen(BlockPos.of(token), Component.translatable("block.anvilcraft." + title));
        }, Minecraft.getInstance());
    }
}
