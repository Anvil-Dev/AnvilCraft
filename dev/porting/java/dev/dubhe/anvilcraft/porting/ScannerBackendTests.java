package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.StructureScannerBlock;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket;
import dev.dubhe.anvilcraft.util.StructureBlueprintFiles;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import dev.dubhe.anvilcraft.util.StructureSaveUtil;
import dev.dubhe.anvilcraft.util.StructureScannerFiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ScannerBackendTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_scanner_file_chunks", helper -> checked(helper, () -> chunks(helper)),
        "port_scanner_file_names", helper -> checked(helper, () -> files(helper)),
        "port_scanner_import_save", helper -> checked(helper, () -> imports(helper)),
        "port_scanner_raw_import", helper -> checked(helper, () -> raw(helper)),
        "port_scanner_capture_nbt", helper -> checked(helper, () -> capture(helper)),
        "port_scanner_live_save", helper -> checked(helper, () -> save(helper)),
        "port_scanner_entity_coordinates", helper -> checked(helper, () -> coordinates(helper)),
        "port_scanner_menu_staging", helper -> checked(helper, () -> staging(helper))
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_scanner_backend"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private interface Checked {
        void run() throws Exception;
    }

    private static void checked(GameTestHelper helper, Checked action) {
        try {
            action.run();
            helper.succeed();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static void rejects(Checked action) throws Exception {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new IllegalStateException("Expected file operation rejection");
    }

    private static CompoundTag sample() {
        return StructureSnapshotCodec.write(new StructureSnapshot(new Vec3i(1, 1, 1),
            List.of(Blocks.STONE.defaultBlockState()),
            List.of(new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty())), List.of()));
    }

    private static void chunks(GameTestHelper helper) throws Exception {
        var id = UUID.randomUUID();
        byte[] bytes = new byte[StructureFileTransfer.CHUNK_BYTES + 37];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        var transfer = new StructureFileTransfer(id, "a.nbt", bytes.length);
        helper.assertTrue(!transfer.append(id, "a.nbt", bytes.length, 0,
            Arrays.copyOf(bytes, StructureFileTransfer.CHUNK_BYTES)), "首块不应提前结束");
        rejects(transfer::finish);
        rejects(() -> transfer.append(id, "a.nbt", bytes.length, 0, new byte[StructureFileTransfer.CHUNK_BYTES]));
        helper.assertTrue(transfer.append(id, "a.nbt", bytes.length, StructureFileTransfer.CHUNK_BYTES,
            Arrays.copyOfRange(bytes, StructureFileTransfer.CHUNK_BYTES, bytes.length)), "尾块完成");
        helper.assertTrue(Arrays.equals(bytes, transfer.finish()), "分块数据应无损重组");
        rejects(() -> new StructureFileTransfer(id, "", 0));
        rejects(() -> new StructureFileTransfer(id, "", StructureFileTransfer.MAX_BYTES + 1));
        var wrong = new StructureFileTransfer(id, "a.nbt", 1);
        rejects(() -> wrong.append(UUID.randomUUID(), "a.nbt", 1, 0, new byte[1]));
        rejects(() -> wrong.append(id, "b.nbt", 1, 0, new byte[1]));
        var started = StructureFileTransfer.class.getDeclaredField("started");
        started.setAccessible(true);
        started.setLong(wrong, System.nanoTime() - 61_000_000_000L);
        rejects(() -> wrong.append(id, "a.nbt", 1, 0, new byte[1]));
    }

    private static void files(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        Path root = StructureBlueprintFiles.directory(server);
        String base = "port_" + UUID.randomUUID();
        var paths = new ArrayList<Path>();
        try {
            for (int i = 0; i < 3; i++) {
                String name = StructureBlueprintFiles.write(server, base + ".nbt", new byte[]{(byte) i});
                paths.add(root.resolve(name));
                helper.assertTrue(name.equals(base + (i == 0 ? "" : "_" + i) + ".nbt"), "同名导出使用连续后缀");
            }
            for (int i = 0; i < 3; i++) helper.assertTrue(Files.readAllBytes(paths.get(i))[0] == i, "保留之前每个文件内容");
            String unicode = base + "_蓝图.LITEMATIC";
            Path imported = root.resolve(unicode);
            paths.add(imported);
            Files.write(imported, new byte[]{5});
            helper.assertTrue(StructureBlueprintFiles.list(server).contains(unicode), "列表支持 Unicode 与大小写扩展名");
            helper.assertTrue(StructureBlueprintFiles.read(server, unicode)[0] == 5, "读取合法文件");
            for (String invalid : List.of("../escape.nbt", "a/b.nbt", "C:\\escape.nbt", "x:stream.nbt", "a.nbt ")) {
                helper.assertTrue(!StructureFileTransfer.isSafeName(invalid), "拒绝路径和非法名称");
                rejects(() -> StructureBlueprintFiles.read(server, invalid));
            }
            rejects(() -> StructureBlueprintFiles.write(server, base + ".litematic", new byte[]{1}));
            rejects(() -> StructureBlueprintFiles.write(server, base + ".nbt", new byte[StructureFileTransfer.MAX_BYTES + 1]));
        } finally {
            for (Path path : paths) Files.deleteIfExists(path);
        }
    }

    private static Fixture fixture(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 16, 4));
        for (var cursor : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 4))) {
            helper.getLevel().setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.STRUCTURE_SCANNER.getDefaultState()
            .setValue(StructureScannerBlock.FACING, Direction.NORTH));
        var scanner = (StructureScannerBlockEntity) helper.getLevel().getBlockEntity(pos);
        scanner.getRangeX().fromIndex(0);
        scanner.getRangeY().fromIndex(0);
        scanner.getRangeZ().fromIndex(0);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortScanner"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos.north()));
        var menu = new StructureScannerMenu(ModMenuTypes.STRUCTURE_SCANNER.get(), 73, player.getInventory(), scanner);
        player.containerMenu = menu;
        return new Fixture(scanner, player, menu);
    }

    private record Fixture(StructureScannerBlockEntity scanner, ServerPlayer player, StructureScannerMenu menu) {
    }

    private static Path diskPath(Fixture fixture, ItemStack disk) {
        return fixture.player.level().getServer().getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures")
            .resolve(disk.get(ModComponents.STRUCTURE_DISK_DATA).file());
    }

    private static void imports(GameTestHelper helper) throws Exception {
        var f = fixture(helper);
        f.scanner.setDiskStack(ModItems.STRUCTURE_DISK.asStack(2));
        StructureScannerFiles.stageImport(f.player, f.menu, sample(), "shape.nbt");
        helper.assertTrue(f.menu.getImportedStructure() != null && f.menu.getSlot(0).getItem().getCount() == 2,
            "导入只暂存且不消耗磁盘");
        f.menu.getSlot(1).set(ModItems.STRUCTURE_DISK.asStack());
        helper.assertTrue(!StructureScannerFiles.saveImportedStructure(f.player, f.menu, "Shape", false, ItemStack.EMPTY)
            && f.menu.getImportedStructure() != null, "输出占用时保留暂存以供重试");
        f.menu.getSlot(1).set(ItemStack.EMPTY);
        var marker = new ItemStack(Items.DIAMOND, 9);
        marker.set(DataComponents.CUSTOM_NAME, Component.literal("Marker"));
        helper.assertTrue(StructureScannerFiles.saveImportedStructure(f.player, f.menu, "Shape", false, marker), "保存暂存结构");
        var disk = f.menu.getSlot(1).getItem();
        try {
            var data = disk.get(ModComponents.STRUCTURE_DISK_DATA);
            helper.assertTrue(!data.autoRotate() && data.direction() == Direction.NORTH && disk.getCount() == 1,
                "规范化磁盘保留用户设置");
            helper.assertTrue(f.menu.getSlot(0).getItem().getCount() == 1 && f.menu.getImportedStructure() == null,
                "只消耗一张盘并清除暂存");
            helper.assertTrue(disk.get(ModComponents.DISPLAY_ITEM).stored().getCount() == 1
                && marker.getCount() == 9, "图标复制为一份但不消耗样品");
            CompoundTag exported = StructureScannerFiles.exportStructure(f.player, f.menu);
            helper.assertTrue(!exported.getCompoundOrEmpty("anvilcraft:blueprint").getBooleanOr("auto_rotate", true), "导出保留自动旋转设置");
            helper.assertTrue(StructureSnapshotCodec.parse(exported, helper.getLevel().registryAccess()).snapshot().blocks().size() == 1,
                "已保存磁盘可重新导出规范结构");
        } finally {
            Files.deleteIfExists(diskPath(f, disk));
        }
    }

    private static void raw(GameTestHelper helper) throws Exception {
        var f = fixture(helper);
        Path root = StructureBlueprintFiles.directory(helper.getLevel().getServer());
        String prefix = "port_raw_" + UUID.randomUUID();
        Path plain = root.resolve(prefix + ".nbt");
        Path compressed = root.resolve(prefix + "_compressed.nbt");
        Path litematic = root.resolve(prefix + ".litematic");
        try {
            try (var stream = new DataOutputStream(Files.newOutputStream(plain))) {
                NbtIo.write(sample(), stream);
            }
            NbtIo.writeCompressed(sample(), compressed);
            var region = BlueprintDataTests.region(BlockPos.ZERO, new Vec3i(1, 1, 1),
                List.of(Blocks.STONE.defaultBlockState()), new int[]{0});
            NbtIo.writeCompressed(BlueprintDataTests.litematic(Map.of("one", region)), litematic);
            for (Path path : List.of(plain, compressed, litematic)) {
                byte[] preview = StructureScannerFiles.importBlueprint(f.player, f.menu, path.getFileName().toString());
                helper.assertTrue(preview.length > 0 && f.menu.getImportedStructure().snapshot().blocks().size() == 1,
                    "压缩、未压缩 NBT 和 Litematica 均能从实际文件导入");
            }
        } finally {
            Files.deleteIfExists(plain);
            Files.deleteIfExists(compressed);
            Files.deleteIfExists(litematic);
        }
    }

    private static void scan(StructureScannerBlockEntity scanner) throws Exception {
        var method = StructureScannerBlockEntity.class.getDeclaredMethod("scanNextLayer");
        method.setAccessible(true);
        method.invoke(scanner);
    }

    private static void capture(GameTestHelper helper) throws Exception {
        var f = fixture(helper);
        var pos = f.scanner.getBlockPos().south(2);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) helper.getLevel().getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        f.scanner.getRangeY().fromIndex(1);
        f.scanner.startScanning();
        scan(f.scanner);
        f.scanner.stopScanning();
        helper.assertTrue(!f.scanner.isScanComplete(), "中途停止不能被当作完整扫描");
        var data = f.scanner.getScannedBlocks().getFirst();
        helper.assertTrue(data.nbt() != null, "逐层缓存保存完整方块实体数据");
        var clone = (StructureScannerBlockEntity) BlockEntity.loadStatic(f.scanner.getBlockPos(), f.scanner.getBlockState(),
            f.scanner.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(clone.getScannedBlocks().getFirst().nbt().equals(data.nbt()), "缓存 NBT 存档往返");
        f.scanner.clearScan();
        helper.assertTrue(f.scanner.getScannedBlocks().isEmpty() && f.scanner.getCurrentScanLayer() == 0, "清除扫描重置所有层");
    }

    private static void save(GameTestHelper helper) throws Exception {
        var f = fixture(helper);
        var pos = f.scanner.getBlockPos().south(2);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        f.scanner.setDiskStack(ModItems.STRUCTURE_DISK.asStack(2));
        f.scanner.startScanning();
        scan(f.scanner);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        StructureSaveUtil.saveStructureToDisk(helper.getLevel(), f.scanner, "port_scan", false, new ItemStack(Items.STICK));
        var disk = f.scanner.getOutputStack();
        helper.assertTrue(!disk.isEmpty(), "完成扫描可保存");
        try {
            var metadata = disk.get(ModComponents.STRUCTURE_DISK_DATA);
            var tag = StructureLoadUtil.readStructureFileOnServer(helper.getLevel(), metadata.file());
            var snapshot = StructureSnapshotCodec.parse(tag, helper.getLevel().registryAccess()).snapshot();
            var entry = snapshot.blocks().getFirst();
            var restored = (ChestBlockEntity) BlockEntity.loadStatic(BlockPos.ZERO, snapshot.stateOf(entry),
                entry.nbt().orElseThrow(), helper.getLevel().registryAccess());
            helper.assertTrue(restored.getItem(0).getCount() == 5, "按保存时刻捕获真实内容而非过期缓存");
            helper.assertTrue(f.scanner.getDiskStack().getCount() == 1 && !metadata.autoRotate()
                && f.scanner.getScannedBlocks().isEmpty(), "保存消费单盘并保留设置、清除扫描");
        } finally {
            Files.deleteIfExists(diskPath(f, disk));
        }
    }

    private static void coordinates(GameTestHelper helper) throws Exception {
        var f = fixture(helper);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean upside : new boolean[]{false, true}) {
                var state = f.scanner.getBlockState().setValue(StructureScannerBlock.FACING, facing)
                    .setValue(StructureScannerBlock.UPSIDE_DOWN, upside);
                helper.getLevel().setBlock(f.scanner.getBlockPos(), state, Block.UPDATE_CLIENTS);
                f.scanner.getRangeY().fromIndex(1);
                var bounds = f.scanner.getScanBounds();
                var stand = EntityType.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.LOAD);
                stand.setPos(bounds.getCenter());
                stand.setNoGravity(true);
                helper.getLevel().addFreshEntity(stand);
                try {
                    var captured = f.scanner.captureEntities();
                    helper.assertTrue(captured.size() == 1 && captured.getFirst().pos().distanceTo(new Vec3(0.5, 1, 0.5)) < 0.0001,
                        "四方向和倒置均采用同一预览坐标系");
                } finally {
                    stand.discard();
                }
            }
        }
    }

    private static void staging(GameTestHelper helper) throws Exception {
        var f = fixture(helper);
        StructureScannerFiles.stageImport(f.player, f.menu, sample(), "a.nbt");
        int range = f.scanner.getRangeX().index();
        new StructureScannerActionPacket(StructureScannerActionPacket.Action.RANGE_CHANGE, 3,
            StructureScannerActionPacket.RangeAxis.X).handleOnServer(f.player);
        helper.assertTrue(f.scanner.getRangeX().index() == range, "导入暂存时不改变扫描尺寸");
        f.menu.setData(0, 0);
        helper.assertTrue(f.menu.getImportedStructure() == null, "菜单同步可以清除暂存");
        StructureScannerFiles.stageImport(f.player, f.menu, sample(), "a.nbt");
        new StructureScannerActionPacket(StructureScannerActionPacket.Action.START).handleOnServer(f.player);
        helper.assertTrue(f.menu.getImportedStructure() == null && f.scanner.isScanning(), "开始新扫描清理导入会话");
        f.scanner.stopScanning();
        StructureScannerFiles.stageImport(f.player, f.menu, sample(), "a.nbt");
        f.menu.removed(f.player);
        helper.assertTrue(f.menu.getImportedStructure() == null, "关闭菜单不遗留导入会话");
    }
}
