package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.client.building.BlueprintClientFiles;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.StructureScannerStatusPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureBlueprintFiles;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class ScannerFilesClientScene {
    private static final BlockPos SCANNER = new BlockPos(8, 81, 8);
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile Throwable failure;
    private static volatile boolean prepared;
    private static volatile Path inputFile;
    private static volatile String exportBase;
    private static Path recordedFile;
    private static Path liveFile;
    private static volatile int liveEntity = -1;
    private static boolean capturing;
    private static boolean opened;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException(
            "Scanner scene " + stage + ", prepared=" + prepared + ", opened=" + opened + ", screen=" + client.screen
                + ", position=" + client.player.position() + ", menu=" + client.player.containerMenu, failure);
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        client.options.guiScale().set(2);
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.setNoGravity(true);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 81 10.5");
                    player.level().setBlockAndUpdate(SCANNER, ModBlocks.STRUCTURE_SCANNER.getDefaultState());
                    var scanner = (StructureScannerBlockEntity) player.level().getBlockEntity(SCANNER);
                    scanner.setDiskStack(ModItems.STRUCTURE_DISK.asStack());
                    var nbt = new CompoundTag();
                    nbt.putString("id", "minecraft:chest");
                    byte[] payload = new byte[70000];
                    new Random(42).nextBytes(payload);
                    nbt.putByteArray("port_payload", payload);
                    var entityTag = new CompoundTag();
                    entityTag.putString("id", "minecraft:armor_stand");
                    entityTag = BuildingEntityTransform.withWorldPos(entityTag, new Vec3(1.5, 0, 0.5));
                    var snapshot = new StructureSnapshot(new Vec3i(3, 2, 1),
                        List.of(Blocks.STONE.defaultBlockState(), Blocks.CHEST.defaultBlockState()),
                        List.of(new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty()),
                            new StructureSnapshot.BlockEntry(new BlockPos(2, 0, 0), 1, Optional.of(nbt))),
                        List.of(new StructureSnapshot.EntityEntry(new Vec3(1.5, 0, 0.5), new BlockPos(1, 0, 0), entityTag)));
                    String id = UUID.randomUUID().toString();
                    exportBase = "export_" + id;
                    inputFile = StructureBlueprintFiles.directory(server).resolve("ui_" + id + ".nbt");
                    NbtIo.writeCompressed(StructureSnapshotCodec.write(snapshot), inputFile);
                    check(Files.size(inputFile) > StructureFileTransfer.CHUNK_BYTES, "Fixture requires multiple network chunks");
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            advance(1);
            return;
        }
        if (stage == 1 && !opened) {
            client.player.setPos(8.5, 81, 10.5);
            client.player.setNoGravity(true);
            if (!prepared || !(client.level.getBlockEntity(SCANNER) instanceof StructureScannerBlockEntity)) return;
            opened = true;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.openMenu((StructureScannerBlockEntity) player.level().getBlockEntity(SCANNER), SCANNER);
            });
            next = System.currentTimeMillis() + 900;
            return;
        }
        if (!prepared || !(client.screen instanceof StructureScannerScreen screen)) return;
        switch (stage) {
            case 1 -> {
                if (!((List<?>) field(screen, "importFiles")).contains(inputFile.getFileName().toString())) return;
                check(screen.getMenu().getSlot(0).hasItem(), "Input disk synchronized");
                capture(client, "initial", 2);
            }
            case 2 -> {
                var input = (EditBox) field(screen, "importInput");
                input.setValue(inputFile.getFileName().toString());
                click(screen, input.getX() + 3, input.getY() + 3, false);
                screen.mouseReleased(mouse(input.getX() + 3, input.getY() + 3));
                check((Boolean) field(screen, "importDropdown"), "Input opens file dropdown");
                capture(client, "dropdown", 3);
            }
            case 3 -> {
                click(screen, screen.getLeftPos() + 10, screen.getTopPos() + 40, true);
                var button = (AbstractWidget) field(screen, "importButton");
                click(screen, button.getX() + 4, button.getY() + 4, false);
                check(!BlueprintClientFiles.isBusy(), "Button down must not begin import");
                screen.mouseReleased(mouse(button.getX() - 3, button.getY() - 3));
                check(!BlueprintClientFiles.isBusy(), "Release outside cancels import");
                click(screen, button.getX() + 4, button.getY() + 4, true);
                check(BlueprintClientFiles.isBusy(), "Release inside starts import");
                advance(4);
            }
            case 4 -> {
                if (BlueprintClientFiles.isBusy() || screen.getMenu().getImportedStructure() == null) return;
                check(screen.getMenu().getSlot(0).hasItem() && !screen.getMenu().getSlot(1).hasItem(), "Import is preview only");
                var preview = (LevelLike) field(screen, "cachedImportedPreview");
                check(preview != null && preview.getEntities().size() == 1
                    && preview.getBlockEntity(new BlockPos(2, 0, 0)) != null, "Preview includes chest and entity");
                Object title = field(screen, "statusTitle");
                new StructureScannerStatusPacket(screen.getMenu().containerId + 1,
                    Component.literal("Wrong menu")).handleOnClient(client.player);
                check(field(screen, "statusTitle") == title, "Ignore status for another menu");
                capture(client, "imported", 5);
            }
            case 5 -> {
                screen.acceptGhost(null, new ItemStack(Items.DIAMOND, 8));
                ((EditBox) field(screen, "nameInput")).setValue("Imported blueprint");
                click(screen, screen.getLeftPos() + 30, screen.getTopPos() + 90, true);
                var confirm = (AbstractWidget) field(screen, "confirmButton");
                click(screen, confirm.getX() + 4, confirm.getY() + 4, true);
                advance(6);
            }
            case 6 -> {
                var disk = screen.getMenu().getSlot(1).getItem();
                if (disk.isEmpty()) return;
                var data = disk.get(ModComponents.STRUCTURE_DISK_DATA);
                check(!data.autoRotate() && data.name().equals("Imported blueprint") && !screen.getMenu().getSlot(0).hasItem(),
                    "Save preserves settings and consumes input disk");
                check(disk.get(ModComponents.DISPLAY_ITEM).stored().is(Items.DIAMOND), "Ghost marker saved on disk");
                check(screen.getMenu().getImportedStructure() == null, "Save clears staging through menu data");
                recordedFile = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .resolve("anvilcraft/structures").resolve(data.file());
                ((EditBox) field(screen, "exportInput")).setValue(exportBase);
                clickButton(screen, "exportButton");
                advance(7);
            }
            case 7 -> {
                if (BlueprintClientFiles.isBusy() || !Files.exists(inputFile.getParent().resolve(exportBase + ".nbt"))) return;
                clickButton(screen, "exportButton");
                advance(8);
            }
            case 8 -> {
                if (BlueprintClientFiles.isBusy() || !Files.exists(inputFile.getParent().resolve(exportBase + "_1.nbt"))) return;
                check(fileSize(inputFile.getParent().resolve(exportBase + ".nbt")) > StructureFileTransfer.CHUNK_BYTES,
                    "Export retains complete imported NBT");
                capture(client, "exported", 10);
            }
            case 9 -> {
                ((EditBox) field(screen, "importInput")).setValue(inputFile.getFileName().toString());
                clickButton(screen, "importButton");
                screen.onClose();
                check(!BlueprintClientFiles.isBusy(), "Closing screen cancels pending import");
                try {
                    Files.deleteIfExists(inputFile);
                    Files.deleteIfExists(inputFile.getParent().resolve(exportBase + ".nbt"));
                    Files.deleteIfExists(inputFile.getParent().resolve(exportBase + "_1.nbt"));
                    Files.deleteIfExists(recordedFile);
                    Files.deleteIfExists(liveFile);
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
                AnvilCraft.LOGGER.info("PORT_SCANNER_FILES_PASSED: real list/chunked import/preview/save/marker/export/suffix/cancel");
                client.stop();
            }
            case 10 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        var menu = player.containerMenu;
                        player.getInventory().placeItemBackInInventory(menu.getSlot(1).remove(1));
                        menu.getSlot(0).set(ModItems.STRUCTURE_DISK.asStack());
                        var level = player.level();
                        var chestPos = SCANNER.south(2).west();
                        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                        ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 2));
                        level.setBlockAndUpdate(SCANNER.south(3), Blocks.STONE.defaultBlockState());
                        var stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
                        stand.setPos(Vec3.atBottomCenterOf(SCANNER.south(3).east()));
                        stand.setNoGravity(true);
                        level.addFreshEntity(stand);
                        liveEntity = stand.getId();
                        menu.broadcastChanges();
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(11);
            }
            case 11 -> {
                if (screen.getMenu().getSlot(1).hasItem() || !screen.getMenu().getSlot(0).hasItem()) return;
                clickButton(screen, "modeToggleButton");
                advance(12);
            }
            case 12 -> {
                if (!screen.getMenu().getBlockEntity().isScanComplete()) return;
                var preview = (LevelLike) field(screen, "cachedPreviewLevelLike");
                check(preview != null && !preview.getEntities().isEmpty(), "Live scanner preview includes captured entity");
                double x = screen.getLeftPos() + 190;
                double y = screen.getTopPos() + 60;
                final float before = (Float) field(screen, "previewRotationY");
                click(screen, x, y, false);
                screen.mouseDragged(mouse(x + 20, y + 10), 20, 10);
                screen.mouseReleased(mouse(x + 20, y + 10));
                check((Float) field(screen, "previewRotationY") != before, "Native drag rotates complete preview");
                capture(client, "live-scan", 13);
            }
            case 13 -> {
                ((EditBox) field(screen, "nameInput")).setValue("Live capture");
                clickButton(screen, "confirmButton");
                advance(14);
            }
            case 14 -> {
                var disk = screen.getMenu().getSlot(1).getItem();
                if (disk.isEmpty()) return;
                var data = disk.get(ModComponents.STRUCTURE_DISK_DATA);
                check(data.name().equals("Live capture") && data.autoRotate(), "Live capture saved with default rotation");
                liveFile = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .resolve("anvilcraft/structures").resolve(data.file());
                client.getSingleplayerServer().execute(() -> {
                    var entity = client.getSingleplayerServer().overworld().getEntity(liveEntity);
                    if (entity != null) entity.discard();
                });
                advance(9);
            }
            default -> throw new IllegalStateException("Unknown scanner stage");
        }
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private static MouseButtonEvent mouse(double x, double y) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
    }

    private static void click(StructureScannerScreen screen, double x, double y, boolean release) {
        screen.mouseClicked(mouse(x, y), false);
        if (release) screen.mouseReleased(mouse(x, y));
    }

    private static void clickButton(StructureScannerScreen screen, String name) {
        var button = (AbstractWidget) field(screen, name);
        click(screen, button.getX() + 4, button.getY() + 4, true);
    }

    private static Object field(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        stage = value;
        AnvilCraft.LOGGER.info("PORT_SCANNER_STAGE {}", value);
        next = System.currentTimeMillis() + 900;
    }

    private static void capture(Minecraft client, String label, int nextStage) {
        try {
            var x = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            var y = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            x.setAccessible(true);
            y.setAccessible(true);
            x.setDouble(client.mouseHandler, 10);
            y.setDouble(client.mouseHandler, 10);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "scanner-files-26.1-" + label + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }
}
