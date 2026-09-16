package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class StorageCraftingTransferClientChecks {
    private static boolean started;
    private static boolean menuTesting;
    private static volatile boolean done;
    private static volatile Throwable failure;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (menuTesting) {
            StorageMenuScene.frame(client, corePos);
            return;
        }
        if (failure != null) throw new IllegalStateException("仓储配方填料网络检查失败", failure);
        if (!started) {
            started = true;
            client.setScreen(null);
            var prepared = new CompletableFuture<Void>();
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    var storage = Storages.get().get(core.getId()).orElseThrow();
                    storage.setCrafting(CraftingStorage.EMPTY);
                    var items = storage.getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        for (int i = 0; i < items.size(); i++) {
                            if (!items.getResource(i).isEmpty()) items.extract(i, items.getResource(i), Integer.MAX_VALUE, transaction);
                        }
                        items.insert(ItemResource.of(Items.STONE), 70, transaction);
                        transaction.commit();
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.BIRCH_LOG, 7));
                    player.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 7));
                    player.inventoryMenu.broadcastChanges();
                    prepared.complete(null);
                } catch (Throwable error) {
                    prepared.completeExceptionally(error);
                }
            });
            prepared.thenComposeAsync(ignored -> StorageClientStub.craftingTransfer(corePos, false, true,
                List.of(new ItemStack(Items.BIRCH_LOG), ItemStack.EMPTY, new ItemStack(Items.BIRCH_LOG)), ItemStack.EMPTY,
                new IntArrayList(new int[]{2, 0, 1})), client)
                .thenComposeAsync(changed -> {
                    require(changed, "合成填料 RPC 未报告变化");
                    return StorageClientStub.craftingGet(corePos);
                }, client).thenComposeAsync(state -> {
                    require(state.craftingInput().get(0).getCount() == 4 && state.craftingInput().get(1).isEmpty()
                        && state.craftingInput().get(2).getCount() == 2, "份数或空格经过网络传输后丢失");
                    return StorageClientStub.craftingTransfer(corePos, true, true, List.of(new ItemStack(Items.STONE)),
                        new ItemStack(Items.STONE_SLAB), new IntArrayList());
                }, client).thenComposeAsync(changed -> {
                    require(changed, "切石填料 RPC 未报告变化");
                    return StorageClientStub.craftingGet(corePos);
                }, client).whenCompleteAsync((state, error) -> {
                    if (error != null) {
                        failure = error;
                        return;
                    }
                    try {
                        require(state.stonecutterInput().getCount() == 64
                            && state.craftingInput().stream().allMatch(ItemStack::isEmpty), "切石填满后旧合成输入未清空");
                        require(client.player.inventoryMenu.getCarried().is(Items.DIAMOND)
                            && client.player.inventoryMenu.getCarried().getCount() == 7, "转移错误修改指针");
                    } catch (Throwable problem) {
                        failure = problem;
                        return;
                    }
                    client.getSingleplayerServer().execute(() -> {
                        try {
                            var server = client.getSingleplayerServer();
                            var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                            var items = Storages.get().get(core.getId()).orElseThrow().getItems();
                            long logs = 0;
                            long stone = 0;
                            for (int i = 0; i < items.size(); i++) {
                                if (items.getResource(i).equals(ItemResource.of(Items.BIRCH_LOG))) logs += items.getAmountAsLong(i);
                                if (items.getResource(i).equals(ItemResource.of(Items.STONE))) stone += items.getAmountAsLong(i);
                            }
                            require(logs == 6 && stone == 6, "服务端扣料和旧输入归还数量错误");
                            done = true;
                        } catch (Throwable problem) {
                            failure = problem;
                        }
                    });
                }, client);
        }
        if (done) {
            AnvilCraft.LOGGER.info("PORT_CRAFTING_TRANSFER_CLIENT_PASSED: grid counts, empty cells, stonecutter, returns, cursor");
            if (Boolean.getBoolean("anvilcraft.portStorageMenuScene")) menuTesting = true;
            else client.stop();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
