package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageTerminalClientStub {
    public static CompletableFuture<Long> openRemote(UUID storageId) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::openRemote,
            StorageTerminalClientStub.playerId(),
            storageId
        );
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
