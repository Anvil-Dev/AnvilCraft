package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageClientStub {
    public static CompletableFuture<StorageServerStub.InteractionResult> craftingPutStonecutterInput(
        BlockPos sourcePos,
        int button,
        ItemStack clientCarried
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingPutStonecutterInput,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            button,
            clientCarried
        );
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> craftingPutCraftingSlot(
        BlockPos sourcePos,
        int slot,
        int button,
        ItemStack clientCarried
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingPutCraftingSlot,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot,
            button,
            clientCarried
        );
    }

    public static CompletableFuture<List<ItemStack>> craftingStonecutterRecipes(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingStonecutterRecipes,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    public static CompletableFuture<Boolean> craftingClearToStorage(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingClearToStorage,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    public static CompletableFuture<Boolean> craftingAvailable(BlockPos sourcePos) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::craftingAvailable, StorageClientStub.playerId(), sourcePos.asLong());
    }

    public static CompletableFuture<Boolean> craftingUnlock(BlockPos sourcePos) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::craftingUnlock, StorageClientStub.playerId(), sourcePos.asLong());
    }

    public static CompletableFuture<CraftingStorage> craftingGet(BlockPos sourcePos) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::craftingGet, StorageClientStub.playerId(), sourcePos.asLong());
    }

    public static void craftingSelect(BlockPos sourcePos, int index) {
        RPC.call(RpcTarget.server(), StorageServerStub::craftingSelect, StorageClientStub.playerId(), sourcePos.asLong(), index);
    }

    public static void craftingSetOptions(BlockPos sourcePos, boolean autoFill, boolean toStorage) {
        RPC.call(RpcTarget.server(), StorageServerStub::craftingSetOptions,
            StorageClientStub.playerId(), sourcePos.asLong(), autoFill, toStorage);
    }

    public static void craftingSetLastOpened(BlockPos sourcePos, boolean opened) {
        RPC.call(RpcTarget.server(), StorageServerStub::craftingSetLastOpened, StorageClientStub.playerId(), sourcePos.asLong(), opened);
    }

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
        return StorageClientStub.interact(sourcePos, slot, button, action, FluidStack.EMPTY);
    }

    public static CompletableFuture<StorageServerStub.InteractionResult> interact(
        BlockPos sourcePos,
        int slot,
        int button,
        StorageInput action,
        FluidStack fluid
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::interact,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot,
            button,
            action,
            fluid
        );
    }

    public static CompletableFuture<StorageServerStub.DepositResult> deposit(BlockPos sourcePos, boolean all, boolean pour) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::deposit,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            all,
            pour
        );
    }

    public static CompletableFuture<StorageServerStub.DepositResult> undo(BlockPos sourcePos) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::undo, StorageClientStub.playerId(), sourcePos.asLong());
    }

    public static CompletableFuture<Boolean> quickMoveToStorage(BlockPos sourcePos, IntList slots) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::quickMoveToStorage,
            StorageClientStub.playerId(), sourcePos.asLong(), slots);
    }

    public static CompletableFuture<Boolean> quickMoveFromStorage(BlockPos sourcePos, IntList slots) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::quickMoveFromStorage,
            StorageClientStub.playerId(), sourcePos.asLong(), slots);
    }

    public static void beginUndoGroup(BlockPos sourcePos) {
        RPC.call(RpcTarget.server(), StorageServerStub::beginUndoGroup, StorageClientStub.playerId(), sourcePos.asLong());
    }

    public static CompletableFuture<Boolean> moveSameToStorage(BlockPos sourcePos, int slot, boolean pour) {
        return RPC.invoke(RpcTarget.server(), StorageServerStub::moveSameToStorage,
            StorageClientStub.playerId(), sourcePos.asLong(), slot, pour);
    }

    public static void endUndoGroup(BlockPos sourcePos) {
        RPC.call(RpcTarget.server(), StorageServerStub::endUndoGroup, StorageClientStub.playerId(), sourcePos.asLong());
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
        return player.getGameProfile().id();
    }

    private StorageClientStub() {
    }
}
