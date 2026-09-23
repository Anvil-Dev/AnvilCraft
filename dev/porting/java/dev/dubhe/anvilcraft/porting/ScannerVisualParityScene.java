package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ScannerVisualParityScene {
    private static final String VERSION = "26.1";
    private static final String[] NAMES = {"cube", "flat", "x-line", "y-line", "z-line", "entities", "effect"};
    private static boolean creating;
    private static boolean preparing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean capturing;
    private static int stage;
    private static int phase;
    private static long next;
    private static long deadline;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portScannerVisualParityScene")) return;
        var client = Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        client.options.guiScale().set(2);
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "scanner-parity-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(121261L, false, false), WorldPresets::createFlatWorldDimensions, client.screen);
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (!preparing) {
            preparing = true;
            deadline = System.currentTimeMillis() + 150000;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8 81 10 180 0");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Scanner parity stage " + stage, failure);
        }
        if (!prepared || capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        if (stage == NAMES.length * 2) {
            AnvilCraft.LOGGER.info("PORT_SCANNER_PARITY_PASSED {}: {} scenes", VERSION, stage);
            client.stop();
            return;
        }
        if (phase == 0) {
            var scanner = (StructureScannerBlockEntity) ModBlocks.STRUCTURE_SCANNER.get()
                .newBlockEntity(new BlockPos(8, 81, 8), ModBlocks.STRUCTURE_SCANNER.getDefaultState());
            scanner.setLevel(client.level);
            var menu = new StructureScannerMenu(null, 123, client.player.getInventory(), scanner);
            menu.setImportedStructure(NAMES[stage / 2], snapshot(stage / 2));
            var screen = new StructureScannerScreen(menu, client.player.getInventory(), Component.literal("Structure Scanner"));
            client.setScreen(screen);
            set(screen, "previewRotationX", stage % 2 == 0 ? -30.0F : -60.0F);
            set(screen, "previewRotationY", stage % 2 == 0 ? 45.0F : 105.0F);
            AnvilCraft.CLIENT_CONFIG.renderScanPreviewEffect = stage / 2 == 6;
            phase = 1;
            next = System.currentTimeMillis() + 1200;
            return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "scanner-parity-" + VERSION + "-" + NAMES[stage / 2] + "-" + stage % 2 + ".png",
            client.getMainRenderTarget(), 1, message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_SCANNER_PARITY {} {}", VERSION, NAMES[stage / 2] + "-" + stage % 2);
                capturing = false;
                stage++;
                phase = 0;
            }));
    }

    private static StructureSnapshot snapshot(int shape) {
        Vec3i size = switch (shape) {
            case 1 -> new Vec3i(13, 1, 7);
            case 2 -> new Vec3i(16, 1, 1);
            case 3 -> new Vec3i(1, 16, 1);
            case 4 -> new Vec3i(1, 1, 16);
            case 5, 6 -> new Vec3i(3, 2, 1);
            default -> new Vec3i(3, 3, 3);
        };
        List<StructureSnapshot.BlockEntry> blocks = new ArrayList<>();
        List<StructureSnapshot.EntityEntry> entities = new ArrayList<>();
        if (shape >= 5) {
            var chest = new CompoundTag();
            chest.putString("id", "minecraft:chest");
            blocks.add(new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty()));
            blocks.add(new StructureSnapshot.BlockEntry(new BlockPos(2, 0, 0), 2, Optional.of(chest)));
            var tag = new CompoundTag();
            tag.putString("id", "minecraft:armor_stand");
            Vec3 pos = new Vec3(1.5, 0, 0.5);
            tag = BuildingEntityTransform.withWorldPos(tag, pos);
            entities.add(new StructureSnapshot.EntityEntry(pos, BlockPos.containing(pos), tag));
        } else {
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(size).offset(-1, -1, -1))) {
                int material = (pos.getX() + pos.getY() + pos.getZ()) % 2;
                blocks.add(new StructureSnapshot.BlockEntry(pos.immutable(), material, Optional.empty()));
            }
        }
        return new StructureSnapshot(size, List.of(Blocks.STONE.defaultBlockState(),
            Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.CHEST.defaultBlockState()), blocks, entities);
    }

    private static void set(Object owner, String name, Object value) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(owner, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
