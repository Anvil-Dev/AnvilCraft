package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.UUID;

public final class TerminalScene {
    private static final BlockPos HYPER = new BlockPos(20, 81, 0);
    private static final BlockPos LOCAL = new BlockPos(25, 81, 0);
    private static final BlockPos SHULKER = new BlockPos(30, 81, 0);
    private static boolean started;
    private static boolean keyTesting;
    private static boolean overlayTesting;
    private static boolean overlayUiTesting;
    private static boolean presentationTesting;
    private static boolean creativeTesting;
    private static boolean restockTesting;
    private static boolean jeiTesting;
    private static boolean unfilteredTesting;
    private static boolean smithingTesting;
    private static boolean readonlyTesting;
    private static boolean frostTesting;
    private static boolean amuletTesting;
    private static boolean headgearTesting;
    private static boolean atmosphereTesting;
    private static volatile boolean prepared;
    private static volatile boolean clientLoaded;
    private static volatile Throwable failure;
    private static StorageScreen screen;
    private static int stage;
    private static long deadline;
    private static long nextAction;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (atmosphereTesting) {
            AtmosphereScene.frame(client);
            return;
        }
        if (headgearTesting) {
            HeadgearScene.frame(client);
            return;
        }
        if (amuletTesting) {
            AmuletRegistryScene.frame(client);
            return;
        }
        if (frostTesting) {
            FrostSmithingScene.frame(client);
            return;
        }
        if (readonlyTesting) {
            ReadOnlyStorageScene.frame(client);
            return;
        }
        if (smithingTesting) {
            SmithingJeiScene.frame(client);
            return;
        }
        if (unfilteredTesting) {
            StorageUnfilteredScene.frame(client);
            return;
        }
        if (jeiTesting) {
            TerminalJeiScene.frame(client);
            return;
        }
        if (restockTesting) {
            TerminalRestockScene.frame(client);
            return;
        }
        if (creativeTesting) {
            TerminalCreativeScene.frame(client);
            return;
        }
        if (presentationTesting) {
            TerminalPresentationScene.frame(client);
            return;
        }
        if (overlayUiTesting) {
            TerminalOverlayScene.frame(client);
            return;
        }
        if (overlayTesting) {
            TerminalOverlayRpcScene.frame(client);
            return;
        }
        if (keyTesting) {
            TerminalKeyScene.frame(client);
            return;
        }
        if (!started) {
            started = true;
            client.options.guiScale().set(2);
            client.resizeGui();
            deadline = System.currentTimeMillis() + 150000;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    var hyper = place(level, HYPER, ModBlocks.HYPERDIMENSION_STORAGE_STATION.get());
                    stock(hyper, Items.DIAMOND, 8);
                    stock(hyper, Items.CRAFTING_TABLE, 1);
                    stock(hyper, Items.STONECUTTER, 1);
                    stock(place(level, LOCAL, ModBlocks.LARGE_CRATE.get()), Items.IRON_INGOT, 5);
                    stock(place(level, SHULKER, ModBlocks.SHULKER_CONTAINER.get()), Items.EMERALD, 6);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.getInventory().setItem(0, new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get()));
                    player.getInventory().setItem(1, new ItemStack(ModItems.LOCAL_TERMINAL.get()));
                    player.getInventory().setItem(2, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
                    player.getInventory().setItem(3, new ItemStack(ModBlocks.SHULKER_CONTAINER.asItem()));
                    player.getInventory().setItem(10, new ItemStack(Items.OAK_LOG, 4));
                    player.getInventory().setSelectedSlot(0);
                    player.setNoGravity(true);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.inventoryMenu.broadcastChanges();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 20.5 82 2.5 180 0");
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("终端客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端客户端超时：" + stage);
        client.getToastManager().clear();
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                if (!clientLoaded) {
                    client.getSingleplayerServer().execute(() -> clientLoaded = client.getSingleplayerServer()
                        .getPlayerList().getPlayers().getFirst().connection.hasClientLoaded());
                    return;
                }
                if (!client.level.getBlockState(HYPER).is(ModBlocks.HYPERDIMENSION_STORAGE_STATION.get())) return;
                if (!client.player.getMainHandItem().is(ModItems.HYPERDIMENSION_TERMINAL)
                    || client.player.distanceToSqr(Vec3.atCenterOf(HYPER)) > 25
                    || client.level.getBlockEntity(HYPER) == null) return;
                var outcome = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(HYPER), Direction.SOUTH, HYPER, false));
                AnvilCraft.LOGGER.info("PORT_TERMINAL_BIND_CLICK: {} at {}", outcome, client.player.position());
                advance(1);
            }
            case 1 -> {
                if (client.player.getInventory().getItem(0).get(ModComponents.TERMINAL_BINDING).id().isEmpty()) return;
                use(client, 0);
                advance(2);
            }
            case 2 -> {
                if (!ready(client)) return;
                require(count(Items.DIAMOND) == 8, "超维终端未显示绑定库存");
                capture(client, "hyper", 3);
            }
            case 3 -> {
                click(287, 204);
                advance(4);
            }
            case 4 -> {
                click(140, 148);
                advance(5);
            }
            case 5 -> {
                click(51, 206);
                advance(6);
            }
            case 6 -> {
                var state = client.player.getInventory().getItem(0).get(ModComponents.CRAFTING);
                require(state.craftingInput().get(8).getCount() == 4, "终端未同步独立合成组件");
                capture(client, "crafting", 7);
            }
            case 7 -> {
                click(91, 206);
                advance(8);
            }
            case 8 -> {
                require(screen.getMenu().getCarried().is(Items.OAK_PLANKS) && screen.getMenu().getCarried().getCount() == 4,
                    "终端合成结果取出失败");
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    var core = (StorageBlockEntity) level.getBlockEntity(HYPER);
                    if (!Storages.get().get(core.getId()).orElseThrow().getCrafting().craftingInput().stream()
                        .allMatch(ItemStack::isEmpty)) {
                        failure = new IllegalStateException("终端合成错误写入世界主存储");
                    }
                });
                screen.onClose();
                advance(9);
            }
            case 9 -> {
                if (client.screen != null) return;
                use(client, 1);
                advance(10);
            }
            case 10 -> {
                if (!ready(client)) return;
                require(count(Items.IRON_INGOT) == 5, "本地终端未连接最近的大型板条箱");
                capture(client, "local", 11);
            }
            case 11 -> {
                teleport(client, "tp @a 100.5 82 2.5");
                advance(12);
            }
            case 12 -> {
                if (count(Items.IRON_INGOT) != 0) return;
                teleport(client, "tp @a 20.5 82 2.5");
                advance(13);
            }
            case 13 -> {
                if (count(Items.IRON_INGOT) != 5) return;
                screen.onClose();
                advance(14);
            }
            case 14 -> {
                use(client, 2);
                advance(15);
            }
            case 15 -> {
                if (!ready(client)) return;
                require(count(Items.EMERALD) == 0, "潜影终端必须优先使用随身集装箱");
                var id = client.player.getInventory().getItem(3).get(ModComponents.STORAGE).id().orElseThrow();
                client.getSingleplayerServer().execute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        Storages.get().get(id).orElseThrow().getItems().insert(ItemResource.of(Items.AMETHYST_SHARD), 7, transaction);
                        transaction.commit();
                    }
                });
                advance(16);
            }
            case 16 -> {
                if (count(Items.AMETHYST_SHARD) != 7) return;
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(3, ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                });
                advance(17);
            }
            case 17 -> {
                if (count(Items.EMERALD) != 6 || count(Items.AMETHYST_SHARD) != 0) return;
                capture(client, "shulker", 18);
            }
            case 18 -> {
                AnvilCraft.LOGGER.info("PORT_TERMINAL_SCENE_PASSED: binding, inventory, own crafting, local range, shulker priority");
                if (Boolean.getBoolean("anvilcraft.portAtmosphereScene")) {
                    atmosphereTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portHeadgearScene")) {
                    headgearTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portAmuletRegistryScene")) {
                    amuletTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portFrostSmithingScene")) {
                    frostTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portReadOnlyStorageScene")) {
                    readonlyTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portSmithingJeiScene")) {
                    smithingTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portStorageUnfilteredScene")) {
                    unfilteredTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalJeiScene")) {
                    jeiTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalRestockScene")) {
                    restockTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalCreativeScene")) {
                    creativeTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalPresentationScene")) {
                    presentationTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalOverlayScene")) {
                    overlayUiTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalOverlayRpcScene")) {
                    overlayTesting = true;
                } else if (Boolean.getBoolean("anvilcraft.portTerminalKeyScene")) {
                    keyTesting = true;
                } else {
                    client.stop();
                }
            }
            default -> throw new IllegalStateException("Unknown terminal stage " + stage);
        }
    }

    private static boolean ready(Minecraft client) {
        if (!(client.screen instanceof StorageScreen current) || !current.canTransferRecipe()) return false;
        screen = current;
        return true;
    }

    private static long count(net.minecraft.world.item.Item item) {
        return screen.getTransferMaterials().getOrDefault(ItemResource.of(item), 0L);
    }

    private static void use(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
    }

    private static void teleport(Minecraft client, String command) {
        var server = client.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static <P extends Enum<P>> StorageBlockEntity place(ServerLevel level, BlockPos pos, AbstractMultiPartBlock<P> block) {
        var state = block.defaultBlockState();
        for (P part : block.getParts()) {
            level.setBlock(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var entity = (StorageBlockEntity) level.getBlockEntity(pos);
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private static void stock(StorageBlockEntity entity, net.minecraft.world.item.Item item, int count) {
        var storage = Storages.get().getOrCreate(entity.getId(), entity.getStorageType().clazz());
        try (Transaction transaction = Transaction.openRoot()) {
            storage.getItems().insert(ItemResource.of(item), count, transaction);
            transaction.commit();
        }
    }

    private static void click(int x, int y) {
        var event = new MouseButtonEvent(screen.getLeftPos() + x, screen.getTopPos() + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
