package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageInput;
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
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    public static void setOpen(BlockPos sourcePos, boolean opened) {
        RPC.call(
            RpcTarget.server(),
            StorageServerStub::setOpen,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            opened
        );
    }

    public static CompletableFuture<IntList> reorder(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::reorder,
            StorageClientStub.playerId(),
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
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slots
        );
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> interact(
        BlockPos sourcePos,
        int slot,
        int button,
        StorageInput action
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::interact,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot,
            button,
            action
        );
    }

    public static CompletableFuture<Boolean> clonePut(BlockPos sourcePos, IntList slots) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::clonePut,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slots
        );
    }

    public static CompletableFuture<StorageServerStub.DepositResult> deposit(BlockPos sourcePos, boolean all) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::deposit,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            all
        );
    }

    public static CompletableFuture<StorageServerStub.DepositResult> take(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::take,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    private static UUID playerId() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Cannot call storage RPC without a client player");
        }
        return player.getGameProfile().getId();
    }

    private StorageClientStub() {
    }
}
