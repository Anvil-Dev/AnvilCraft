package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageClientStub {
    public static CompletableFuture<Double> load(BlockPos sourcePos) {
        return StorageClientStub.loadMetadata(sourcePos).thenApply(StorageServerStub.Metadata::fullness);
    }

    public static CompletableFuture<StorageServerStub.Metadata> loadMetadata(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::load,
            playerId(),
            sourcePos.asLong()
        );
    }

    public static CompletableFuture<IntList> reorder(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::reorder,
            playerId(),
            sourcePos.asLong()
        );
    }

    public static CompletableFuture<StorageServerStub.SyncResult> sync(
        BlockPos sourcePos,
        IntList slots
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::sync,
            playerId(),
            sourcePos.asLong(),
            slots
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
