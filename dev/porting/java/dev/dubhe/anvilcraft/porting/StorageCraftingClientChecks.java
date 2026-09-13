package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class StorageCraftingClientChecks {
    private static boolean requested;
    private static volatile boolean prepared;
    private static boolean checking;
    private static boolean done;
    private static Throwable failure;
    private static boolean inputsStarted;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (inputsStarted) {
            StorageCraftingInputClientChecks.frame(client, corePos);
            return;
        }
        if (!requested) {
            requested = true;
            client.getSingleplayerServer().execute(() -> {
                var core = (StorageBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(corePos);
                var storage = Storages.get().getOrCreate(core.getId(), HyperdimensionStorage.class);
                try (Transaction transaction = Transaction.openRoot()) {
                    storage.getItems().insert(ItemResource.of(Items.CRAFTING_TABLE), 1, transaction);
                    storage.getItems().insert(ItemResource.of(Items.STONECUTTER), 1, transaction);
                    transaction.commit();
                }
                var state = CraftingStorage.EMPTY.withCraftingSlot(4, new ItemStack(Items.DIAMOND, 7))
                    .withStonecutterInput(new ItemStack(Items.STONE, 12));
                storage.setCrafting(state);
                var item = new ItemStack(Items.STICK);
                item.set(ModComponents.CRAFTING, state);
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(12, item);
                player.inventoryMenu.broadcastChanges();
                prepared = true;
            });
        }
        if (failure != null) throw new IllegalStateException("合成状态客户端检查失败", failure);
        if (prepared && !checking) {
            checking = true;
            StorageClientStub.craftingAvailable(corePos).thenComposeAsync(available -> {
                if (available) throw new IllegalStateException("合成面板应尚未解锁");
                return StorageClientStub.craftingUnlock(corePos);
            }, client).thenComposeAsync(unlocked -> {
                if (!unlocked) throw new IllegalStateException("仓储材料未能解锁面板");
                StorageClientStub.craftingSelect(corePos, 3);
                StorageClientStub.craftingSetOptions(corePos, true, true);
                StorageClientStub.craftingSetLastOpened(corePos, true);
                return StorageClientStub.craftingGet(corePos);
            }, client).whenCompleteAsync((state, error) -> {
                failure = error;
                if (error == null) {
                    if (state.craftingInput().get(4).getCount() != 7 || state.stonecutterInput().getCount() != 12
                        || state.stonecutterSelected() != 3 || !state.autoFill() || !state.toStorage() || !state.lastOpened()) {
                        failure = new IllegalStateException("仓储合成状态同步不完整");
                    }
                    done = true;
                }
            }, client);
        }
        if (done && failure == null) {
            var component = client.player.getInventory().getItem(12).get(ModComponents.CRAFTING);
            if (component == null) return;
            if (component.craftingInput().get(4).getCount() != 7 || component.autoFill()) {
                throw new IllegalStateException("物品合成数据未独立同步");
            }
            AnvilCraft.LOGGER.info("PORT_CRAFTING_STATE_CLIENT_PASSED: unlock, options, grid and independent item component");
            if (Boolean.getBoolean("anvilcraft.portCraftingInputsScene")) inputsStarted = true;
            else client.stop();
        }
    }
}
