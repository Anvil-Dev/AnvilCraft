package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.concurrent.CompletableFuture;

public final class StorageCraftingBatchClientChecks {
    private static boolean started;
    private static volatile boolean done;
    private static volatile Throwable failure;
    private static boolean panelStarted;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (panelStarted) {
            StorageCraftingPanelScene.frame(client, corePos);
            return;
        }
        if (failure != null) throw new IllegalStateException("批量合成客户端检查失败", failure);
        if (!started) {
            started = true;
            var prepared = new CompletableFuture<Void>();
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    var storage = Storages.get().get(core.getId()).orElseThrow();
                    storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.OAK_LOG))
                        .withAutoFill(true).withToStorage(true));
                    try (Transaction transaction = Transaction.openRoot()) {
                        storage.getItems().insert(ItemResource.of(Items.OAK_LOG), 200, transaction);
                        transaction.commit();
                    }
                    server.getPlayerList().getPlayers().getFirst().containerMenu.setCarried(ItemStack.EMPTY);
                    prepared.complete(null);
                } catch (Throwable error) {
                    prepared.completeExceptionally(error);
                }
            });
            prepared.thenComposeAsync(ignored -> StorageClientStub.craftingTakeAll(corePos, false, 8), client)
                .thenComposeAsync(result -> {
                    if (!result.changed() || result.done() || result.refilledSlots() != 1 << 9) {
                        throw new IllegalStateException("首个分块必须返回待续状态与补料掩码");
                    }
                    return StorageClientStub.craftingTakeAll(corePos, false, 1);
                }, client).thenComposeAsync(result -> {
                    if (!result.changed() || !result.done()) throw new IllegalStateException("续块没有按原预算结束");
                    return StorageClientStub.craftingThrowResult(corePos, false, false);
                }, client).thenComposeAsync(result -> {
                    if (!result.changed() || !result.carried().isEmpty()) throw new IllegalStateException("Q 丢出结果错误");
                    return StorageClientStub.craftingThrowResult(corePos, false, true);
                }, client).whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        failure = error;
                        return;
                    }
                    if (!result.changed()) {
                        failure = new IllegalStateException("Ctrl+Q 未产生产物");
                        return;
                    }
                    client.getSingleplayerServer().execute(() -> {
                        try {
                            var server = client.getSingleplayerServer();
                            var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                            var items = Storages.get().get(core.getId()).orElseThrow().getItems();
                            long planks = 0;
                            long logs = 0;
                            for (int slot = 0; slot < items.size(); slot++) {
                                if (items.getResource(slot).equals(ItemResource.of(Items.OAK_PLANKS))) {
                                    planks += items.getAmountAsLong(slot);
                                }
                                if (items.getResource(slot).equals(ItemResource.of(Items.OAK_LOG))) logs += items.getAmountAsLong(slot);
                            }
                            var player = server.getPlayerList().getPlayers().getFirst();
                            int dropped = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).stream()
                                .map(ItemEntity::getItem).filter(stack -> stack.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum();
                            if (planks != 512 || logs != 55 || dropped != 68) {
                                throw new IllegalStateException("批量数量不守恒：" + planks + "/" + logs + "/" + dropped);
                            }
                            done = true;
                        } catch (Throwable checkError) {
                            failure = checkError;
                        }
                    });
                }, client);
        }
        if (done && failure == null) {
            AnvilCraft.LOGGER.info("PORT_CRAFTING_BATCH_CLIENT_PASSED: continuation, budget, refill mask, Q and Ctrl+Q conservation");
            if (Boolean.getBoolean("anvilcraft.portCraftingPanelScene")) panelStarted = true;
            else client.stop();
        }
    }
}
