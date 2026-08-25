package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageTerminalClientStub {
    private static final Map<UUID, Long> VIRTUAL_POS_CACHE = new HashMap<>();

    public static CompletableFuture<Long> openRemote(UUID storageId) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::openRemote,
            StorageTerminalClientStub.playerId(),
            storageId
        );
    }

    /** 本地终端的会话标识：按玩家 UUID 派生，与服务器端一致。 */
    public static UUID localTerminalId() {
        return UUID.nameUUIDFromBytes(
            ("anvilcraft:local_terminal:" + StorageTerminalClientStub.playerId()).getBytes(
                java.nio.charset.StandardCharsets.UTF_8
            )
        );
    }

    /** 潜影终端的会话标识：按玩家 UUID 派生，与服务器端一致。 */
    public static UUID shulkerTerminalId() {
        return UUID.nameUUIDFromBytes(
            ("anvilcraft:shulker_terminal:" + StorageTerminalClientStub.playerId()).getBytes(
                java.nio.charset.StandardCharsets.UTF_8
            )
        );
    }

    public static CompletableFuture<Long> ensureVirtualPos(UUID storageId) {
        Long cached = StorageTerminalClientStub.VIRTUAL_POS_CACHE.get(storageId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return StorageTerminalClientStub.openRemote(storageId).thenApply(virtualPos -> {
            if (virtualPos != -1L) {
                StorageTerminalClientStub.VIRTUAL_POS_CACHE.put(storageId, virtualPos);
            }
            return virtualPos;
        });
    }

    public static CompletableFuture<IntList> reorder(UUID storageId, String search) {
        return StorageTerminalClientStub.ensureVirtualPos(storageId).thenCompose(virtualPos ->
            RPC.invoke(
                RpcTarget.server(),
                StorageServerStub::terminalReorder,
                StorageTerminalClientStub.playerId(),
                virtualPos,
                search
            )
        );
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> take(
        UUID storageId,
        int slot,
        int button,
        ItemStack carried
    ) {
        return StorageTerminalClientStub.ensureVirtualPos(storageId).thenCompose(virtualPos ->
            RPC.invoke(
                RpcTarget.server(),
                StorageServerStub::terminalTake,
                StorageTerminalClientStub.playerId(),
                virtualPos,
                slot,
                button,
                carried
            )
        );
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> insert(UUID storageId, ItemStack carried) {
        return StorageTerminalClientStub.ensureVirtualPos(storageId).thenCompose(virtualPos ->
            RPC.invoke(
                RpcTarget.server(),
                StorageServerStub::terminalInsert,
                StorageTerminalClientStub.playerId(),
                virtualPos,
                carried
            )
        );
    }

    /** 按住 Shift 取出：直接移入背包；背包放得下多少就移多少，完全放不下则不移动也不取到鼠标。 */
    public static CompletableFuture<StorageServerStub.InteractionResult> takeToInventory(
        UUID storageId,
        int slot,
        int button
    ) {
        return StorageTerminalClientStub.ensureVirtualPos(storageId).thenCompose(virtualPos ->
            RPC.invoke(
                RpcTarget.server(),
                StorageServerStub::terminalTakeToInventory,
                StorageTerminalClientStub.playerId(),
                virtualPos,
                slot,
                button
            )
        );
    }

    /** JEI 快速合成补库：把合成缺少的物品从绑定存储站取出补入背包。 */
    public static CompletableFuture<Boolean> withdrawToInventory(UUID storageId, List<ItemStack> needs) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::terminalWithdrawToInventory,
            StorageTerminalClientStub.playerId(),
            storageId,
            needs
        );
    }

    public static void clear() {
        StorageTerminalClientStub.VIRTUAL_POS_CACHE.clear();
    }

    private static UUID playerId() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Cannot call storage terminal RPC without a client player");
        }
        return player.getGameProfile().getId();
    }

    private StorageTerminalClientStub() {
    }
}
