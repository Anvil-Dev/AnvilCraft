package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ResonatorScene {
    private static final BlockPos TARGET = new BlockPos(200, 101, 198);
    private static boolean handReady;
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (!Boolean.getBoolean("anvilcraft.portResonatorScene") || event.getHand() != InteractionHand.MAIN_HAND) return;
        handReady = event.getItemStack().is(Minecraft.getInstance().player.getMainHandItem().getItem())
            && event.getEquipProgress() < 0.01F;
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 150000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            client.options.keyUse.setDown(false);
            throw new IllegalStateException("共振器客户端验证失败，阶段 " + stage, failure);
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
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    player.getInventory().setItem(0, ModItems.FROST_METAL_RESONATOR.asStack());
                    player.getInventory().setSelectedSlot(0);
                    player.inventoryMenu.broadcastChanges();
                    server.overworld().setBlock(TARGET, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 200.5 100 200.5 180 0");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    server.tickRateManager().setTickRate(10);
                    prepared = true;
                });
                advance(1, 1000);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.FROST_METAL_RESONATOR) || !aimed(client)) return;
                client.player.getInventory().setSelectedSlot(0);
                advance(12, 1000);
            }
            case 12 -> {
                if (!handReady) return;
                capture(client, "idle", 11);
            }
            case 11 -> {
                use(client);
                advance(2, 500);
            }
            case 2 -> {
                require(client.player.isUsingItem(), "普通共振器应持续采掘");
                capture(client, "ordinary-charge", 3);
            }
            case 3 -> {
                if (!client.level.getBlockState(TARGET).isAir()) return;
                client.options.keyUse.setDown(false);
                if (client.player.getMainHandItem().getDamageValue() != 128) return;
                server(client, () -> client.getSingleplayerServer().overworld()
                    .setBlock(TARGET, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL));
                advance(4, 500);
            }
            case 4 -> {
                if (!client.level.getBlockState(TARGET).is(Blocks.CHEST) || !aimed(client)) return;
                use(client);
                advance(5, 200);
            }
            case 5 -> {
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                advance(6, 500);
            }
            case 6 -> {
                if (client.screen == null) return;
                require(client.level.getBlockState(TARGET).is(Blocks.CHEST) && client.player.getMainHandItem().getDamageValue() == 128,
                    "短按松开应打开箱子且不破坏或额外耗损");
                capture(client, "short-interaction", 7);
            }
            case 7 -> {
                client.screen.onClose();
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var stack = ModItems.TRANSCENDENCE_RESONATOR.asStack();
                    stack.set(ModComponents.RESONATE_MODE, ResonateMode.AUTO);
                    player.getInventory().setItem(0, stack);
                    player.inventoryMenu.broadcastChanges();
                    server.overworld().setBlock(TARGET, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                });
                advance(8, 500);
            }
            case 8 -> {
                if (!client.player.getMainHandItem().is(ModItems.TRANSCENDENCE_RESONATOR)
                    || !aimed(client) || !handReady) return;
                use(client);
                advance(9, 300);
            }
            case 9 -> capture(client, "transcendence-charge", 10);
            case 10 -> {
                if (!client.level.getBlockState(TARGET).isAir()) return;
                client.options.keyUse.setDown(false);
                require(client.player.getMainHandItem().getDamageValue() == 0, "超限共振器应保留无损采掘");
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().tickRateManager().setTickRate(20));
                AnvilCraft.LOGGER.info("PORT_RESONATOR_PASSED: ordinary mining, exact damage, short interaction, transcendence");
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    int slot = 0;
                    for (var item : new net.minecraft.world.item.Item[]{ModItems.FROST_METAL_RESONATOR.get(),
                        ModItems.EMBER_METAL_RESONATOR.get(), ModItems.TRANSCENDENCE_RESONATOR.get()}) {
                        for (var mode : ResonateMode.values()) {
                            var stack = new ItemStack(item);
                            stack.set(ModComponents.RESONATE_MODE, mode);
                            player.getInventory().setItem(slot++, stack);
                        }
                    }
                    player.inventoryMenu.broadcastChanges();
                });
                advance(13, 1000);
            }
            case 13 -> {
                client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
                try {
                    for (String field : new String[]{"xpos", "ypos"}) {
                        var position = net.minecraft.client.MouseHandler.class.getDeclaredField(field);
                        position.setAccessible(true);
                        position.setDouble(client.mouseHandler, 0);
                    }
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
                advance(14, 1000);
            }
            case 14 -> capture(client, "mode-models", 15);
            case 15 -> client.stop();
            default -> throw new IllegalStateException("Unknown resonator stage " + stage);
        }
    }

    private static boolean aimed(Minecraft client) {
        return client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(TARGET);
    }

    private static void use(Minecraft client) {
        client.options.keyUse.setDown(true);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, (BlockHitResult) client.hitResult);
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

    private static void advance(int value, long delay) {
        AnvilCraft.LOGGER.info("PORT_RESONATOR_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "resonator-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage, 0);
            }));
    }
}
