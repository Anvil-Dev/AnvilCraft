package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.DevourRange;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.lwjgl.glfw.GLFW;

public final class DragonRodScene {
    private static final BlockPos TARGET = new BlockPos(200, 101, 198);
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile boolean protectedContents;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 120000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
            throw new IllegalStateException("龙杖客户端验证失败，阶段 " + stage, failure);
        }
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.setNoGravity(true);
                    for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                    player.getInventory().setItem(0, ModItems.EMBER_DRAGON_ROD.asStack());
                    player.inventoryMenu.broadcastChanges();
                    server.overworld().setBlock(TARGET, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                    server.overworld().setBlock(TARGET.east(), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
                    ((ChestBlockEntity) server.overworld().getBlockEntity(TARGET)).setItem(0, new ItemStack(Items.DIAMOND, 3));
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 200.5 100 200.5 180 0");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    server.tickRateManager().setTickRate(20);
                    prepared = true;
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.EMBER_DRAGON_ROD)) return;
                client.player.getInventory().setSelectedSlot(0);
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(2);
            }
            case 2 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(3);
            }
            case 3 -> capture(client, "protect-wheel", 4);
            case 4 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(5);
            }
            case 5 -> {
                if (client.screen != null || !DragonRodItem.protectsContainers(client.player.getMainHandItem())) return;
                var lines = client.player.getMainHandItem().getTooltipLines(Item.TooltipContext.of(client.level),
                    client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
                require(lines.stream().anyMatch(line -> line.contains("Container Protection") && line.endsWith("On")),
                    "保护状态必须通过本地化提示显示");
                client.gameMode.startDestroyBlock(TARGET, Direction.SOUTH);
                client.gameMode.stopDestroyBlock();
                advance(6);
            }
            case 6 -> {
                if (!client.level.getBlockState(TARGET.east()).isAir()) return;
                require(client.level.getBlockState(TARGET).is(Blocks.CHEST), "实际左键吞噬必须保留开启保护的箱子");
                server(client, () -> {
                    var chest = (ChestBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(TARGET);
                    require(chest != null && chest.getItem(0).getCount() == 3, "保护后服务器容器内容必须完整");
                    protectedContents = true;
                });
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(7);
            }
            case 7 -> {
                if (!(client.screen instanceof WheelScreen) || !protectedContents) return;
                TerminalBalanceScene.point(client, 1);
                advance(8);
            }
            case 8 -> capture(client, "devour-wheel", 9);
            case 9 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(10);
            }
            case 10 -> {
                if (client.screen != null || DragonRodItem.protectsContainers(client.player.getMainHandItem())) return;
                client.gameMode.startDestroyBlock(TARGET, Direction.SOUTH);
                client.gameMode.stopDestroyBlock();
                advance(11);
            }
            case 11 -> {
                if (!client.level.getBlockState(TARGET).isAir()) return;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                server(client, () -> client.getSingleplayerServer().overworld()
                    .setBlock(TARGET, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL));
                advance(12);
            }
            case 12 -> {
                if (client.player.getMainHandItem().get(ModComponents.DEVOUR_RANGE) != DevourRange.FIVE
                    || !client.level.getBlockState(TARGET).is(Blocks.GLASS)) return;
                capture(client, "range-five", 13);
            }
            case 13 -> {
                AnvilCraft.LOGGER.info("PORT_DRAGON_ROD_PASSED: hold/release wheel, protected contents, actual devour, range switch");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown dragon rod stage " + stage);
        }
    }

    private static void server(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_DRAGON_ROD_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "dragon-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }
}
