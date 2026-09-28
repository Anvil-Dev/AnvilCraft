package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StorageBehaviorClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final Map<String, Integer> SOUNDS = new HashMap<>();
    private static int index;
    private static int stage;
    private static long next;
    private static long deadline;
    private static UUID id;
    private static boolean capturing;

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (Boolean.getBoolean("anvilcraft.portStorageBehaviorScene") && event.getName().endsWith(".open")) {
            SOUNDS.merge(event.getName(), 1, Integer::sum);
        }
    }

    private static Block block() {
        return switch (index) {
            case 0 -> ModBlocks.CRATE.get();
            case 1 -> ModBlocks.LARGE_CRATE.get();
            case 2 -> ModBlocks.SHULKER_CONTAINER.get();
            default -> ModBlocks.HYPERDIMENSION_STORAGE_STATION.get();
        };
    }

    private static <P extends Enum<P>> void place(net.minecraft.server.level.ServerLevel level, AbstractMultiPartBlock<P> block) {
        var state = block.defaultBlockState();
        for (P part : block.getParts()) {
            level.setBlock(POS.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Storage behavior " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        BlockPos target = index == 0 ? POS : POS.above().south();
        switch (stage) {
            case 0 -> {
                advance(1);
                client.setScreen(null);
                id = UUID.randomUUID();
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    if (block() instanceof AbstractMultiPartBlock<?> multipart) place(level, multipart);
                    else level.setBlockAndUpdate(POS, block().defaultBlockState());
                    var be = (StorageBlockEntity) level.getBlockEntity(POS);
                    be.setId(id);
                    Storages.get().getOrCreate(id, be.getStorageType().clazz());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 163 4.5 180 0");
                    server.getPlayerList().getPlayers().forEach(player -> {
                        player.setNoGravity(true);
                        player.getInventory().clearContent();
                        player.containerMenu.broadcastChanges();
                    });
                });
            }
            case 1 -> {
                if (!(client.level.getBlockEntity(POS) instanceof StorageBlockEntity be) || !id.equals(be.getId())
                    || !client.player.getMainHandItem().isEmpty()) return;
                advance(2);
                SOUNDS.clear();
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(target), Direction.SOUTH, target, false));
            }
            case 2 -> {
                if (!(client.screen instanceof StorageScreen)) return;
                String name = index < 2 ? "block.barrel.open" : index == 2 ? "block.shulker_box.open" : "block.ender_chest.open";
                if (SOUNDS.getOrDefault(name, 0) != 1) throw new IllegalStateException("Opening sound " + SOUNDS);
                capturing = true;
                Screenshot.grab(client.gameDirectory, "storage-behavior-26.1-" + index + ".png", client.getMainRenderTarget(), 1,
                    message -> client.execute(() -> {
                        capturing = false;
                        advance(3);
                    }));
            }
            case 3 -> {
                advance(4);
                client.setScreen(null);
                client.getConnection().send(new ServerboundPickItemFromBlockPacket(target, false));
            }
            case 4 -> {
                var stack = client.player.getMainHandItem();
                var ref = stack.get(ModComponents.STORAGE);
                if (!stack.is(block().asItem()) || ref == null || ref.id().isPresent()) {
                    throw new IllegalStateException("Plain pick must be unbound: " + stack);
                }
                advance(5);
                client.getConnection().send(new ServerboundPickItemFromBlockPacket(target, true));
            }
            case 5 -> {
                var ref = client.player.getMainHandItem().get(ModComponents.STORAGE);
                if (ref == null || !ref.id().orElseThrow().equals(id)) throw new IllegalStateException("Ctrl pick lost storage ID");
                AnvilCraft.LOGGER.info("PORT_STORAGE_BEHAVIOR_CLIENT: block={}, sound=1, plain=unbound, ctrl=main-id", block());
                if (++index == 4) {
                    stage = 6;
                    AnvilCraft.LOGGER.info("PORT_STORAGE_BEHAVIOR_CLIENT_PASSED");
                    client.stop();
                } else advance(0);
            }
            default -> {
            }
        }
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2000;
    }
}
