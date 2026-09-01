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

import java.util.List;
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

    public static CompletableFuture<Boolean> quickMoveFromStorage(BlockPos sourcePos, IntList slots) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::quickMoveFromStorage,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slots
        );
    }

    public static void quickMoveUndo(BlockPos sourcePos, int slot, int count) {
        RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::quickMoveUndo,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot,
            count
        );
    }

    public static CompletableFuture<Boolean> quickMoveToStorage(BlockPos sourcePos, IntList slots) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::quickMoveToStorage,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slots
        );
    }

    public static CompletableFuture<Boolean> moveSameToStorage(BlockPos sourcePos, int slot) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::moveSameToStorage,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot
        );
    }

    public static CompletableFuture<StorageServerStub.StorageUsage> loadUsage(UUID storageId) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::getStorageUsage,
            StorageClientStub.playerId(),
            storageId
        );
    }

    public static CompletableFuture<StorageServerStub.DepositResult> undo(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::undo,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    public static void beginUndoGroup(BlockPos sourcePos) {
        RPC.call(
            RpcTarget.server(),
            StorageServerStub::beginUndoGroup,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    public static void endUndoGroup(BlockPos sourcePos) {
        RPC.call(
            RpcTarget.server(),
            StorageServerStub::endUndoGroup,
            StorageClientStub.playerId(),
            sourcePos.asLong()
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

    /** 仓储合成模式是否可用（主存储中同时存在工作台与切石机）。 */
    public static CompletableFuture<Boolean> craftingAvailable(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingAvailable,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    /** 读取仓储合成面板数据（① 切石机输入、② 合成 9 宫格、切石机选中配方）。 */
    public static CompletableFuture<CraftingStorage> craftingGet(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingGet,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    /** ① 切石机输入：按玩家物品栏点击语义交换指针物品并返回最新指针。button=0 左键 / 1 右键。 */
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

    /** ② 合成 9 宫格：按玩家物品栏点击语义与指定槽交换物品并返回最新指针。 */
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

    /** ① 当前输入对应的切石机候选配方结果列表。 */
    public static CompletableFuture<List<ItemStack>> craftingStonecutterRecipes(BlockPos sourcePos) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingStonecutterRecipes,
            StorageClientStub.playerId(),
            sourcePos.asLong()
        );
    }

    /** 设置① 的切石机选中配方索引。 */
    public static void craftingSelect(BlockPos sourcePos, int index) {
        RPC.call(
            RpcTarget.server(),
            StorageServerStub::craftingSelect,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            index
        );
    }

    /** 记录上次关闭界面时是否为合成模式（关闭界面时调用）。 */
    public static void craftingSetLastOpened(BlockPos sourcePos, boolean opened) {
        RPC.call(
            RpcTarget.server(),
            StorageServerStub::craftingSetLastOpened,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            opened
        );
    }

    /**
     * 拖拽分配到 ①/② 输入槽与玩家背包槽（统一均分）。
     * {@code craftingSlots} 为 ①/② 槽（0 为①，1~9 为②），{@code inventorySlots} 为背包 menu 槽位号。
     */
    public static CompletableFuture<StorageServerStub.InteractionResult> craftingQuickCraft(
        BlockPos sourcePos,
        int button,
        IntList craftingSlots,
        IntList inventorySlots,
        ItemStack clientCarried
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingQuickCraft,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            button,
            craftingSlots,
            inventorySlots,
            clientCarried
        );
    }

    /** 双击 ①/② 输入槽：拿起槽内物品并从背包收集同种到指针。 */
    public static CompletableFuture<StorageServerStub.InteractionResult> craftingPickupAll(
        BlockPos sourcePos,
        int slot,
        ItemStack clientCarried
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingPickupAll,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot,
            clientCarried
        );
    }

    /** 背包槽双击补充：把 ①/② 输入槽中与指针同种的物品收集到指针。 */
    public static CompletableFuture<StorageServerStub.InteractionResult> craftingPickupIntoCarried(
        BlockPos sourcePos,
        ItemStack clientCarried
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingPickupIntoCarried,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            clientCarried
        );
    }

    /** 输入槽 Shift 点击：把槽内物品移出到背包 → 仓储（放不下留在槽内，不拿指针）。 */
    public static CompletableFuture<Boolean> craftingQuickMoveOut(BlockPos sourcePos, int slot) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingQuickMoveOut,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            slot
        );
    }

    /** 取③/④ 配方结果：消耗输入并放到指针。 */
    public static CompletableFuture<StorageServerStub.InteractionResult> craftingTakeResult(
        BlockPos sourcePos, boolean stonecutter, boolean shift
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingTakeResult,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            stonecutter,
            shift
        );
    }

    /**
     * 按住 Shift 取③/④ 配方结果：连续合成直到材料不足或产物无处可放。
     * 单次调用最多合成 {@code CRAFTING_TAKE_ALL_CHUNK} 次；返回 {@code done=false}
     * 时调用方应继续调用直至 {@code done=true}（分块，避免一次性阻塞服务端线程）。
     */
    public static CompletableFuture<StorageServerStub.TakeAllResult> craftingTakeAll(
        BlockPos sourcePos,
        boolean stonecutter
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingTakeAll,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            stonecutter
        );
    }

    /** JEI 转移：把配方输入放入 ①/② 输入槽（材料从背包/存储扣取）。
     *  {@code stonecutterResult}：切石机场景传 JEI 当前配方产物，服务端据此选中配方；否则为 EMPTY。 */
    public static CompletableFuture<Boolean> craftingTransfer(
        BlockPos sourcePos,
        boolean stonecutter,
        List<ItemStack> inputs,
        ItemStack stonecutterResult
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            StorageServerStub::craftingTransfer,
            StorageClientStub.playerId(),
            sourcePos.asLong(),
            stonecutter,
            inputs,
            stonecutterResult
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
