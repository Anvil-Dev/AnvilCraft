package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.impl.WailaClientRegistration;

import java.util.UUID;

public final class CrateDisposalClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static int stage;
    private static long next;
    private static long deadline;
    private static UUID id;
    private static String jadeName = "";
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Crate disposal stage " + stage + " Jade=" + jadeName);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                advance(1);
                WailaClientRegistration.instance().addTooltipCollectedCallback((box, accessor) -> {
                    if (accessor instanceof BlockAccessor block && block.getPosition().equals(POS)) {
                        jadeName = box.getTooltip().getString(JadeIds.CORE_OBJECT_NAME);
                    }
                });
                WailaClientRegistration.instance().tooltipCollectedCallback.sort();
                client.options.guiScale().set(2);
                client.resizeGui();
                client.setScreen(null);
                id = UUID.randomUUID();
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    level.setBlockAndUpdate(POS, ModBlocks.CRATE.getDefaultState());
                    ((CrateBlockEntity) level.getBlockEntity(POS)).setId(id);
                    Storages.get().getOrCreate(id, CrateStorage.class);
                    level.setBlockAndUpdate(POS.south(4).below(), Blocks.STONE.defaultBlockState());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 4.5 180 16");
                    server.getPlayerList().getPlayers().forEach(player -> {
                        player.setNoGravity(true);
                        player.getInventory().clearContent();
                        player.containerMenu.broadcastChanges();
                    });
                });
            }
            case 1 -> {
                if (!jadeName.equals("Crate")) return;
                capture(client, "normal-world", 2);
            }
            case 2 -> open(client, 3);
            case 3 -> {
                if (!(client.screen instanceof StorageScreen)) return;
                checkTitle(client, "Crate");
                capture(client, "normal-ui", 4);
            }
            case 4 -> {
                advance(5);
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().overworld()
                    .setBlockAndUpdate(POS.north(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState()));
            }
            case 5 -> {
                if (!client.level.getBlockState(POS).getValue(CrateBlock.DISPOSE)) return;
                checkTitle(client, "Overflow Disposal Crate");
                capture(client, "disposal-ui", 6);
            }
            case 6 -> {
                advance(7);
                jadeName = "";
                client.setScreen(null);
            }
            case 7 -> {
                if (!jadeName.equals("Overflow Disposal Crate")) return;
                capture(client, "disposal-world", 8);
            }
            case 8 -> {
                advance(9);
                jadeName = "";
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().overworld()
                    .setBlockAndUpdate(POS.north(), Blocks.AIR.defaultBlockState()));
            }
            case 9 -> {
                if (!jadeName.equals("Crate")) return;
                open(client, 10);
            }
            case 10 -> {
                if (!(client.screen instanceof StorageScreen)) return;
                checkTitle(client, "Crate");
                capture(client, "restored-ui", 11);
            }
            case 11 -> {
                stage = 12;
                AnvilCraft.LOGGER.info("PORT_CRATE_DISPOSAL_CLIENT_PASSED: live screen title, centered text, Jade enable/disable");
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void open(Minecraft client, int target) {
        advance(target);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(POS), Direction.SOUTH, POS, false));
    }

    private static void checkTitle(Minecraft client, String expected) {
        if (!client.screen.getTitle().getString().equals(expected)) throw new IllegalStateException("Title " + client.screen.getTitle());
        try {
            var field = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("titleLabelX");
            field.setAccessible(true);
            int actual = field.getInt(client.screen);
            int width = client.font.width(client.screen.getTitle());
            if (actual != (300 - 106 - width) / 2 + 106) throw new IllegalStateException("Title centering " + actual + " width=" + width);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "crate-disposal-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2000;
    }
}
