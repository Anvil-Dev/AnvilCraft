package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TerminalOverlayRpcScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean operationsStarted;
    private static volatile boolean operationsDone;
    private static UUID target;
    private static UUID local;
    private static int stage;
    private static long nextAction;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            target = client.player.getInventory().getItem(0).get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
            local = TerminalSessions.localTerminalId(client.player.getUUID());
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 120000;
        }
        if (failure != null) throw new IllegalStateException("终端浮窗 RPC 验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端浮窗 RPC 超时：" + stage);
        if (System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        var items = Storages.get().get(target).orElseThrow().getItems();
                        try (Transaction transaction = Transaction.openRoot()) {
                            for (int i = 0; i < items.size(); i++) {
                                if (!items.getResource(i).isEmpty()) items.extract(i, items.getResource(i), Integer.MAX_VALUE, transaction);
                            }
                            items.insert(ItemResource.of(Items.STONE), 70, transaction);
                            transaction.commit();
                        }
                        player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                            ChestMenu.threeRows(id, inventory, new SimpleContainer(27)), Component.literal("Terminal transfer check")));
                        player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 3));
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || client.player.containerMenu.containerId == 0 || client.player.hasInfiniteMaterials()) return;
                if (!operationsStarted) {
                    operationsStarted = true;
                    var first = StorageTerminalClientStub.ensureVirtualPos(target);
                    require(first == StorageTerminalClientStub.ensureVirtualPos(target), "同目标并发请求必须复用在途会话");
                    first.thenComposeAsync(token -> StorageTerminalClientStub.reorder(target, "@minecraft"), client)
                        .thenComposeAsync(order -> {
                            require(order.contains(0), "独立排序未收到物品槽");
                            return StorageTerminalClientStub.take(target, 0, 1, ItemStack.EMPTY);
                        }, client).thenComposeAsync(result -> {
                            require(result.changed() && result.carried().is(Items.STONE) && result.carried().getCount() == 1,
                                "右键取一件 RPC 失败");
                            return StorageTerminalClientStub.insert(target, new ItemStack(Items.DIAMOND, 64));
                        }, client).thenComposeAsync(result -> {
                            require(result.changed() && result.carried().isEmpty(), "存入没有使用当前菜单的实际指针");
                            return StorageTerminalClientStub.takeToInventory(target, 0, 0);
                        }, client).thenComposeAsync(result -> {
                            require(result.changed() && result.carried().isEmpty(), "Shift 取出应保持指针为空");
                            return StorageTerminalClientStub.take(target, 0, 0, ItemStack.EMPTY);
                        }, client).whenCompleteAsync((result, error) -> {
                            if (error != null) failure = error;
                            else {
                                try {
                                    require(result.carried().getCount() == 6, "最后只能取剩余六个");
                                    operationsDone = true;
                                } catch (Throwable problem) {
                                    failure = problem;
                                }
                            }
                        }, client);
                }
                if (operationsDone) {
                    require(client.player.containerMenu.getCarried().is(Items.STONE)
                        && client.player.containerMenu.getCarried().getCount() == 6, "真实菜单指针广播未同步");
                    TerminalReachabilityCache.ensure(local);
                    TerminalReachabilityCache.clear();
                    advance(2);
                }
            }
            case 2 -> {
                require(TerminalReachabilityCache.getReachability(local) == null, "清理后的旧响应不能恢复缓存");
                TerminalReachabilityCache.ensure(local);
                advance(3);
            }
            case 3 -> {
                if (TerminalReachabilityCache.getReachability(local) == null) return;
                require(TerminalReachabilityCache.isReachable(local), "附近本地仓储应可达");
                var server = client.getSingleplayerServer();
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 100.5 82 2.5"));
                advance(4);
                nextAction += 1500;
            }
            case 4 -> {
                TerminalReachabilityCache.ensure(local);
                advance(5);
            }
            case 5 -> {
                if (TerminalReachabilityCache.getReachability(local) == null) return;
                require(!TerminalReachabilityCache.isReachable(local), "过期刷新后应识别脱离范围");
                var cleared = new CompletableFuture<Void>();
                client.getSingleplayerServer().execute(() -> {
                    TerminalSessions.clear(client.player.getUUID());
                    cleared.complete(null);
                });
                operationsDone = false;
                cleared.thenComposeAsync(ignored -> StorageTerminalClientStub.reorder(target, ""), client)
                    .handleAsync((order, error) -> {
                        require(error != null, "过期会话应被服务端拒绝");
                        return null;
                    }, client).thenComposeAsync(ignored -> StorageTerminalClientStub.reorder(target, ""), client)
                    .whenCompleteAsync((order, error) -> {
                        if (error != null) failure = error;
                        else {
                            if (!order.isEmpty()) failure = new IllegalStateException("失效后恢复到了错误库存");
                            operationsDone = true;
                        }
                    }, client);
                advance(6);
            }
            case 6 -> {
                if (!operationsDone) return;
                StorageTerminalClientStub.clear();
                TerminalReachabilityCache.clear();
                AnvilCraft.LOGGER.info("PORT_TERMINAL_OVERLAY_RPC_PASSED: active menu, take, shift, insert, cache expiry and recovery");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown overlay RPC stage " + stage);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_OVERLAY_RPC_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 1000;
    }
}
