package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.StructureDiskRequestPacket;
import dev.dubhe.anvilcraft.network.StructureDiskResponsePacket;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StructureDiskDataTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_structure_disk_codec", StructureDiskDataTests::codec,
        "port_structure_disk_cache", StructureDiskDataTests::cache,
        "port_structure_disk_file", StructureDiskDataTests::file,
        "port_disk_recorded_block", StructureDiskDataTests::recordedBlock
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_structure_disk_data"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static StructureDiskData disk(String name) {
        var id = UUID.randomUUID();
        return new StructureDiskData(name + "_" + id + ".nbt", name, id, Direction.NORTH, 17, 18, 17);
    }

    static CompoundTag structure() {
        final var tag = new CompoundTag();
        var size = new ListTag();
        size.add(IntTag.valueOf(17));
        size.add(IntTag.valueOf(18));
        size.add(IntTag.valueOf(17));
        tag.put("size", size);
        var palette = new ListTag();
        palette.add(NbtUtils.writeBlockState(Blocks.CHEST.defaultBlockState()));
        tag.put("palette", palette);
        var blocks = new ListTag();
        for (int i = 0; i < 5000; i++) {
            final var block = new CompoundTag();
            var position = new ListTag();
            position.add(IntTag.valueOf(i % 17));
            position.add(IntTag.valueOf(i / 289));
            position.add(IntTag.valueOf(i / 17 % 17));
            block.put("pos", position);
            block.putInt("state", 0);
            var nbt = new CompoundTag();
            nbt.putString("CustomName", "preview-" + i);
            block.put("nbt", nbt);
            blocks.add(block);
        }
        tag.put("blocks", blocks);
        return tag;
    }

    private static void codec(GameTestHelper helper) {
        var data = disk("codec");
        var tag = structure();
        var buffer = Unpooled.buffer();
        try {
            StructureDiskRequestPacket.STREAM_CODEC.encode(buffer, new StructureDiskRequestPacket(data.file()));
            helper.assertTrue(StructureDiskRequestPacket.STREAM_CODEC.decode(buffer).file().equals(data.file()), "请求文件名必须保留");
            StructureDiskResponsePacket.STREAM_CODEC.encode(buffer, new StructureDiskResponsePacket(data.file(), tag));
            var restored = StructureDiskResponsePacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(restored.file().equals(data.file()) && tag.equals(restored.tag()), "完整 NBT 同步必须无损");
            StructureDiskResponsePacket.STREAM_CODEC.encode(buffer, new StructureDiskResponsePacket(data.file(), null));
            helper.assertTrue(StructureDiskResponsePacket.STREAM_CODEC.decode(buffer).tag() == null, "缺失文件必须显式同步");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void cache(GameTestHelper helper) {
        StructureLoadUtil.clearClientStructureCache();
        try {
            var data = disk("cache");
            var tag = structure();
            StructureLoadUtil.cacheStructureNbt(data.file(), tag);
            tag.putString("mutated", "outside");
            var cached = StructureLoadUtil.getStructureNbtForPreview(helper.getLevel(), data).orElseThrow();
            helper.assertTrue(!cached.contains("mutated"), "缓存必须独立持有收到的数据");
            StructureLoadUtil.cacheStructureNbt(data.file(), null);
            helper.assertTrue(StructureLoadUtil.getStructureNbtForPreview(helper.getLevel(), data).isEmpty(), "缺失结果必须清除旧缓存");
            StructureLoadUtil.cacheStructureNbt(data.file(), cached);
            helper.assertTrue(StructureLoadUtil.getStructureNbtForPreview(helper.getLevel(), data).isPresent(), "新数据必须解除缺失状态");
            for (int i = 0; i < 100; i++) {
                StructureLoadUtil.cacheStructureNbt(disk("evict" + i).file(), new CompoundTag());
            }
            helper.assertTrue(StructureLoadUtil.getStructureNbtForPreview(helper.getLevel(), data).isEmpty(), "缓存必须有界并淘汰最旧文件");
            StructureLoadUtil.cacheStructureNbt(data.file(), cached);
            StructureLoadUtil.clearClientStructureCache();
            helper.assertTrue(StructureLoadUtil.getStructureNbtForPreview(helper.getLevel(), data).isEmpty(), "离开服务器必须清除缓存");
        } finally {
            StructureLoadUtil.clearClientStructureCache();
        }
        helper.succeed();
    }

    private static void file(GameTestHelper helper) {
        var data = disk("full");
        var directory = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures");
        var path = directory.resolve(data.file());
        try {
            Files.createDirectories(directory);
            var tag = structure();
            NbtIo.writeCompressed(tag, path);
            helper.assertTrue(tag.equals(StructureLoadUtil.readStructureFileOnServer(helper.getLevel(), data.file())),
                "服务端读取必须保留 4096 格以外的方块、结构尺寸及方块实体数据");
            helper.assertTrue(StructureLoadUtil.readStructureFileOnServer(helper.getLevel(), "../" + data.file()) == null,
                "拒绝越界文件名");
            helper.assertTrue(StructureLoadUtil.readStructureFileOnServer(helper.getLevel(), disk("missing").file()) == null,
                "缺失文件返回空结果");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
        helper.succeed();
    }

    private static void recordedBlock(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var pos = helper.absolutePos(new BlockPos(1, 2, 1));
            var state = ModBlocks.OVERFLOW_CHUTE.getDefaultState();
            helper.getLevel().setBlockAndUpdate(pos, state);
            var disk = ModItems.DISK.asStack();
            fixture.player().setItemInHand(InteractionHand.MAIN_HAND, disk);
            var context = new UseOnContext(fixture.player(), InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
            disk.getItem().useOn(context);
            var stored = disk.get(ModComponents.DISK_DATA);
            helper.assertTrue(stored != null, "磁盘必须保存设备数据");
            helper.assertTrue(stored.tag().getStringOr("StoredBlock", "")
                .equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()), "磁盘必须记录实际方块类型");
        }
        helper.succeed();
    }
}
