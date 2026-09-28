package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StorageInteractionScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static int stage;
    private static int openingSounds;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (Boolean.getBoolean("anvilcraft.portStorageInteractionScene") && event.getName().equals("block.ender_chest.open")) {
            openingSounds++;
        }
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Storage interaction stage " + stage);
        if (client.getOverlay() != null || capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_INTERACTION_MODS: jade={}, jei={}",
                    net.neoforged.fml.ModList.get().isLoaded("jade"), net.neoforged.fml.ModList.get().isLoaded("jei"));
                if (Boolean.getBoolean("anvilcraft.portExpectNoJade")) {
                    if (net.neoforged.fml.ModList.get().isLoaded("jade")) throw new IllegalStateException("Jade was not excluded");
                    TooltipDedupProbe.verifyNoJade(client);
                    AnvilCraft.LOGGER.info("PORT_OPTIONAL_JADE_ABSENT");
                }
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    var block = ModBlocks.HYPERDIMENSION_STORAGE_STATION.get();
                    var state = block.defaultBlockState();
                    for (var part : block.getParts()) {
                        level.setBlock(POS.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                    UUID id = UUID.randomUUID();
                    ((HyperdimensionStorageStationBlockEntity) level.getBlockEntity(POS)).setId(id);
                    Storages.get().getOrCreate(id, HyperdimensionStorage.class);
                    level.setBlockAndUpdate(POS.south(5).below(), Blocks.STONE.defaultBlockState());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 5.5 180 0");
                    server.getPlayerList().getPlayers().forEach(player -> {
                        player.setNoGravity(true);
                        player.getInventory().clearContent();
                        player.containerMenu.broadcastChanges();
                    });
                });
                client.options.guiScale().set(2);
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!(client.level.getBlockEntity(POS) instanceof HyperdimensionStorageStationBlockEntity)
                    || client.player.distanceToSqr(0.5, 162, 5.5) > 1 || !client.player.getMainHandItem().isEmpty()) return;
                advance(2);
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(POS.above()), Direction.SOUTH, POS.above(), false));
            }
            case 2 -> {
                if (!(client.screen instanceof StorageScreen)) return;
                if (openingSounds != 1) throw new IllegalStateException("Station opening sound count " + openingSounds);
                reportTitle(client);
                capture(client, "open", 3);
            }
            case 3 -> {
                var button = (AbstractWidget) field(client.screen, "flipButton");
                var event = new MouseButtonEvent(button.getX() + 5, button.getY() + 4, new MouseButtonInfo(0, 0));
                advance(4);
                client.screen.mouseClicked(event, false);
                client.screen.mouseReleased(event);
            }
            case 4 -> {
                reportTitle(client);
                capture(client, "flipped", 5);
            }
            case 5 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_INTERACTION_PASSED: empty-hand block use, one opening sound and flipped screen");
                stage = 6;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void reportTitle(Minecraft client) {
        var screen = client.screen;
        var button = (AbstractWidget) field(screen, "flipButton");
        int width = client.font.width(screen.getTitle());
        int right = (int) field(screen, "leftPos") + (int) field(screen, "titleLabelX") + width;
        AnvilCraft.LOGGER.info("PORT_STORAGE_TITLE: width={}, right={}, buttonX={}, overlaps={}",
            width, right, button.getX(), right > button.getX());
    }

    private static Object field(Object instance, String name) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                continue;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalStateException("Missing field " + name);
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2500;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-interaction-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }
}
