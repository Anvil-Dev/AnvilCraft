package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageClientStub {
    public static CompletableFuture<Double> load(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::getFullness,
            playerId(),
            sourcePos.asLong()
        );
    }

    public static void reorder(BlockPos sourcePos) {
        RPC.call(
            RpcTarget.server(),
            StorageServerStub::reorder,
            playerId(),
            sourcePos.asLong()
        );
    }

    private static UUID playerId() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Cannot call storage RPC without a client player");
        }
        return player.getGameProfile().id();
    }

    private StorageClientStub() {
    }
}
