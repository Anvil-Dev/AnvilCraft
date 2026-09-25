package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.concurrent.CompletableFuture;

public final class StorageCraftingExecutionClientChecks {
    private static boolean started;
    private static int craftedBefore;
    private static volatile boolean done;
    private static volatile Throwable failure;
    private static boolean batchesStarted;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (batchesStarted) {
            StorageCraftingBatchClientChecks.frame(client, corePos);
            return;
        }
        if (failure != null) throw new IllegalStateException("仓储合成执行客户端检查失败", failure);
        if (!started) {
            started = true;
            var prepared = new CompletableFuture<Void>();
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    var storage = Storages.get().get(core.getId()).orElseThrow();
                    storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)).withAutoFill(true));
                    var player = server.getPlayerList().getPlayers().getFirst();
                    craftedBefore = player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.SUGAR));
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.getInventory().setItem(15, new ItemStack(Items.HONEY_BOTTLE));
                    player.inventoryMenu.broadcastChanges();
                    prepared.complete(null);
                } catch (Throwable error) {
                    prepared.completeExceptionally(error);
                }
            });
            prepared.thenComposeAsync(ignored -> StorageClientStub.craftingTakeResult(corePos, false, false), client)
                .thenComposeAsync(result -> {
                    if (!result.changed() || !result.carried().is(Items.SUGAR) || result.carried().getCount() != 3
                        || result.refilledSlots() != 1 << 9) throw new IllegalStateException("普通取出或补料掩码错误");
                    return StorageClientStub.craftingGet(corePos);
                }, client).thenComposeAsync(state -> {
                    if (!state.craftingInput().get(8).is(Items.HONEY_BOTTLE)) throw new IllegalStateException("客户端未收到补料状态");
                    StorageClientStub.craftingSetOptions(corePos, false, true);
                    return StorageClientStub.craftingTakeResult(corePos, false, true);
                }, client).thenComposeAsync(result -> {
                    if (!result.changed() || result.carried().getCount() != 3) throw new IllegalStateException("Shift 取出错误修改指针");
                    return StorageClientStub.craftingGet(corePos);
                }, client).thenComposeAsync(state -> {
                    if (!state.craftingInput().get(8).is(Items.GLASS_BOTTLE)) throw new IllegalStateException("未同步剩余瓶");
                    return StorageClientStub.craftingTakeResult(corePos, false, true);
                }, client).whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        failure = error;
                        return;
                    }
                    if (result.changed()) {
                        failure = new IllegalStateException("剩余瓶不应再次生成糖");
                        return;
                    }
                    client.getSingleplayerServer().execute(() -> {
                        var core = (StorageBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(corePos);
                        var items = Storages.get().get(core.getId()).orElseThrow().getItems();
                        long count = 0;
                        for (int i = 0; i < items.size(); i++) {
                            if (items.getResource(i).equals(ItemResource.of(Items.SUGAR))) count += items.getAmountAsLong(i);
                        }
                        if (count != 3) failure = new IllegalStateException("仓储产物数量错误：" + count);
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        var recipe = player.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,
                            CraftingInput.of(1, 1, java.util.List.of(new ItemStack(Items.HONEY_BOTTLE))), player.level()).orElseThrow();
                        if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.SUGAR)) != craftedBefore + 6
                            || !player.getRecipeBook().contains(recipe.id())) {
                            failure = new IllegalStateException("Actual player crafting statistics or recipe unlock missing");
                        }
                        if (failure == null) {
                            AnvilCraft.LOGGER.info("PORT_STORAGE_CRAFT_AWARDS_CLIENT_PASSED: crafted sugar=6 and recipe unlocked");
                        }
                        done = true;
                    });
                }, client);
        }
        if (done && failure == null) {
            AnvilCraft.LOGGER.info("PORT_CRAFTING_EXECUTION_CLIENT_PASSED: result, remainder, autofill, shift and no duplicate output");
            if (Boolean.getBoolean("anvilcraft.portCraftingBatchScene")) batchesStarted = true;
            else client.stop();
        }
    }
}
