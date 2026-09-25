package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TerminalRestockScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean requested;
    private static boolean done;
    private static boolean captured;
    private static UUID target;
    private static long readyAt;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            readyAt = System.currentTimeMillis() + 1000;
            deadline = readyAt + 90000;
        }
        if (failure != null) throw new IllegalStateException("终端配方补库 RPC 验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端配方补库 RPC 超时");
        if (System.currentTimeMillis() < readyAt) return;
        if (!prepared && !requested) {
            requested = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var terminal = player.getInventory().getItem(0).copy();
                    target = terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
                    player.setGameMode(GameType.SURVIVAL);
                    player.getAbilities().invulnerable = true;
                    player.onUpdateAbilities();
                    for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                    player.getInventory().setItem(0, terminal);
                    player.getInventory().setItem(10, new ItemStack(Items.STONE, 2));
                    var items = Storages.get().get(target).orElseThrow().getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        for (int slot = 0; slot < items.size(); slot++) {
                            if (!items.getResource(slot).isEmpty()) {
                                items.extract(slot, items.getResource(slot), Integer.MAX_VALUE, transaction);
                            }
                        }
                        items.insert(ItemResource.of(Items.STONE), 8, transaction);
                        items.insert(ItemResource.of(Items.BUCKET), 2, transaction);
                        transaction.commit();
                    }
                    var pos = new BlockPos(18, 81, 0);
                    server.overworld().setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.getDefaultState(), Block.UPDATE_ALL);
                    var port = (StorageFluidPortBlockEntity) server.overworld().getBlockEntity(pos);
                    port.getTank().set(0, FluidResource.of(Fluids.WATER), 1000);
                    port.tickServer();
                    PlayerSettings.getSetting(player.registryAccess(), player.getUUID()).storage().setSearchContent("@missing");
                    player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                        ChestMenu.threeRows(id, inventory, new SimpleContainer(27)), Component.literal("Terminal restock check")));
                    player.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 3));
                    player.containerMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        if (!prepared || client.player.containerMenu.containerId == 0 || client.player.containerMenu.getCarried().getCount() != 3) return;
        if (requested) {
            requested = false;
            final int menu = client.player.containerMenu.containerId;
            final List<UUID> targets = List.of(target);
            StorageTerminalClientStub.snapshot(targets).thenComposeAsync(snapshot -> {
                require(count(snapshot.items(), Items.STONE) == 8
                    && snapshot.fluids().stream().mapToInt(entry -> entry.amount()).sum() == 1000,
                    "独立库存快照不应被主页面搜索过滤，且应包含实际流体");
                return StorageTerminalClientStub.restock(menu, targets, List.of(new ItemStack(Items.STONE, 7)));
            }, client).thenComposeAsync(result -> {
                require(count(result.withdrawn(), Items.STONE) == 5 && count(result.inventoryBefore(), Items.STONE) == 2
                    && inventoryCount(client, Items.STONE) == 7, "实际补入数量或背包同步错误");
                var consumed = new CompletableFuture<Void>();
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    try (Transaction transaction = Transaction.openRoot()) {
                        require(PlayerInventoryWrapper.of(player).getMainSlots().extract(ItemResource.of(Items.STONE), 3, transaction) == 3,
                            "模拟填充消耗失败");
                        transaction.commit();
                    }
                    player.containerMenu.broadcastChanges();
                    consumed.complete(null);
                });
                return consumed.thenComposeAsync(ignored -> StorageTerminalClientStub.returnExcess(targets, result), client);
            }, client).thenComposeAsync(changed -> {
                require(changed && inventoryCount(client, Items.STONE) == 2, "退回多余材料必须保留原有两个石头");
                return StorageTerminalClientStub.restock(menu, targets, List.of(new ItemStack(Items.WATER_BUCKET)));
            }, client).thenComposeAsync(result -> {
                require(count(result.withdrawn(), Items.WATER_BUCKET) == 1 && inventoryCount(client, Items.WATER_BUCKET) == 1,
                    "真实 RPC 未完成流体容器补库");
                return StorageTerminalClientStub.returnExcess(targets, result);
            }, client).thenComposeAsync(changed -> {
                require(changed && inventoryCount(client, Items.WATER_BUCKET) == 0, "未使用的流体容器应以物品还库");
                return StorageTerminalClientStub.snapshot(targets);
            }, client).thenComposeAsync(snapshot -> {
                require(count(snapshot.items(), Items.STONE) == 5 && count(snapshot.items(), Items.BUCKET) == 1
                    && count(snapshot.items(), Items.WATER_BUCKET) == 1, "往返后物品数量不守恒");
                require(snapshot.fluids().stream().mapToInt(entry -> entry.amount()).sum() == 0, "流体未正确转移到满桶");
                return StorageTerminalClientStub.restock(menu + 1, targets, List.of(new ItemStack(Items.STONE, 9)))
                    .handleAsync((unexpected, error) -> {
                        require(error != null, "服务端应拒绝过期菜单请求");
                        return null;
                    }, client);
            }, client).whenCompleteAsync((ignored, error) -> {
                if (error != null) failure = error;
                else done = true;
            }, client);
        }
        if (done && !captured) {
            require(client.player.containerMenu.getCarried().is(Items.DIAMOND), "补库不能改变菜单指针");
            captured = true;
            Screenshot.grab(client.gameDirectory, "terminal-restock-26.1.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    AnvilCraft.LOGGER.info("PORT_TERMINAL_RESTOCK_PASSED: snapshot, totals, surplus floor, fluid transaction, menu guard");
                    client.stop();
                }));
        }
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int inventoryCount(Minecraft client, Item item) {
        int result = 0;
        for (int slot = 0; slot < 36; slot++) {
            var stack = client.player.getInventory().getItem(slot);
            if (stack.is(item)) result += stack.getCount();
        }
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
