package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.HandbookStructureExportHandler;
import dev.dubhe.anvilcraft.network.HandbookStructureExportPacket;
import dev.dubhe.anvilcraft.util.HandbookStructureExporter;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class HandbookExportTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_handbook_export_files", HandbookExportTests::files,
        "port_handbook_export_chunks", HandbookExportTests::chunks,
        "port_handbook_export_rejection", HandbookExportTests::rejection,
        "port_handbook_export_cleanup", HandbookExportTests::cleanup
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_handbook_export"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    public static CompoundTag structure(boolean large) {
        var root = new CompoundTag();
        var size = new ListTag();
        for (int value : new int[]{3, 2, 1}) size.add(IntTag.valueOf(value));
        root.put("size", size);
        root.put("blocks", new ListTag());
        root.put("palette", new ListTag());
        root.put("entities", new ListTag());
        root.putString("test_extra", "Preserve full exported data");
        if (large) {
            byte[] bytes = new byte[80000];
            new Random(121261).nextBytes(bytes);
            root.putByteArray("test_blob", bytes);
        }
        return root;
    }

    public static byte[] compressed(CompoundTag root) {
        try {
            var output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, output);
            return output.toByteArray();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void files(GameTestHelper helper) {
        try {
            var root = structure(false);
            helper.assertTrue(HandbookStructureExporter.read(compressed(root)).equals(root), "Full NBT is preserved");
            var directory = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("port_export_" + UUID.randomUUID());
            var first = HandbookStructureExporter.write(directory, Identifier.parse("anvilcraft:folder/../con.nbt"), root);
            var second = HandbookStructureExporter.write(directory, Identifier.parse("anvilcraft:folder/../con.nbt"), root);
            helper.assertTrue(first.getFileName().toString().equals("anvilcraft_con.nbt")
                && second.getFileName().toString().equals("anvilcraft_con_1.nbt")
                && first.getParent().equals(directory.resolve("data/ageratum")), "Basename isolation and collision-safe suffixes");
            helper.assertTrue(HandbookStructureExporter.read(Files.readAllBytes(first)).equals(root),
                "Existing export was not overwritten");
            boolean rejected = false;
            try {
                HandbookStructureExporter.read(compressed(new CompoundTag()));
            } catch (java.io.IOException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "Non-structure NBT is rejected");
            var bomb = new ByteArrayOutputStream();
            try (var data = new DataOutputStream(new GZIPOutputStream(bomb))) {
                data.writeByte(10);
                data.writeUTF("");
                data.writeByte(7);
                data.writeUTF("payload");
                data.writeInt(64 * 1024 * 1024 + 1);
            }
            boolean quota = false;
            try {
                HandbookStructureExporter.read(bomb.toByteArray());
            } catch (net.minecraft.nbt.NbtAccounterException expected) {
                quota = true;
            }
            helper.assertTrue(quota, "NBT allocation is rejected at the source 64 MiB quota before reading the body");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        helper.succeed();
    }

    private static void chunks(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var root = structure(true);
            byte[] data = compressed(root);
            helper.assertTrue(data.length > HandbookStructureExportPacket.CHUNK_BYTES, "Fixture spans multiple packets");
            var location = AnvilCraft.of("port_export_" + UUID.randomUUID());
            var output = player.level().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data/ageratum/anvilcraft_" + location.getPath() + ".nbt");
            for (int offset = 0; offset < data.length; offset += HandbookStructureExportPacket.CHUNK_BYTES) {
                byte[] chunk = Arrays.copyOfRange(data, offset, Math.min(data.length, offset + HandbookStructureExportPacket.CHUNK_BYTES));
                new HandbookStructureExportPacket(location, data.length, offset, chunk).handleOnServer(player);
                if (offset + chunk.length < data.length) helper.assertTrue(!Files.exists(output), "Partial exports do not create files");
            }
            try {
                helper.assertTrue(HandbookStructureExporter.read(Files.readAllBytes(output)).equals(root),
                    "All chunks are written to the actual server save with extra data intact");
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            new HandbookStructureExportPacket(location, data.length, 0, Arrays.copyOf(data, 8)).handleOnServer(player);
            helper.assertTrue(!state("TRANSFERS").containsKey(player.getUUID()), "Two-second cooldown rejects an immediate new transfer");
            HandbookStructureExportHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        }
        helper.succeed();
    }

    private static void rejection(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        boolean bounded = false;
        try {
            HandbookStructureExportPacket.STREAM_CODEC.encode(buffer, new HandbookStructureExportPacket(AnvilCraft.of("too_large"),
                30000, 0, new byte[HandbookStructureExportPacket.CHUNK_BYTES + 1]));
            HandbookStructureExportPacket.STREAM_CODEC.decode(buffer);
        } catch (RuntimeException expected) {
            bounded = true;
        } finally {
            buffer.release();
        }
        helper.assertTrue(bounded, "Wire decoder enforces the 24 KiB chunk limit");
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var location = AnvilCraft.of("port_rejected");
            for (int total : new int[]{0, -1, HandbookStructureExportPacket.MAX_BYTES + 1}) {
                new HandbookStructureExportPacket(location, total, 0, new byte[]{1}).handleOnServer(player);
                helper.assertTrue(!state("TRANSFERS").containsKey(player.getUUID()), "Invalid declared size is rejected");
                HandbookStructureExportHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            }
            new HandbookStructureExportPacket(location, 20, 0, new byte[]{1, 2}).handleOnServer(player);
            new HandbookStructureExportPacket(location, 20, 3, new byte[]{3}).handleOnServer(player);
            helper.assertTrue(!state("TRANSFERS").containsKey(player.getUUID()), "Out-of-order data discards the transfer");
            HandbookStructureExportHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            new HandbookStructureExportPacket(location, 20, 0, new byte[]{1, 2}).handleOnServer(player);
            new HandbookStructureExportPacket(AnvilCraft.of("different"), 20, 2, new byte[]{3}).handleOnServer(player);
            helper.assertTrue(!state("TRANSFERS").containsKey(player.getUUID()), "Chunks cannot change the structure ID");
            HandbookStructureExportHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        }
        helper.succeed();
    }

    private static void cleanup(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var id = player.getUUID();
            new HandbookStructureExportPacket(AnvilCraft.of("port_expired"), 20, 0, new byte[]{1}).handleOnServer(player);
            try {
                Object current = state("TRANSFERS").get(id);
                var constructor = current.getClass().getDeclaredConstructor(Identifier.class, int.class, long.class);
                constructor.setAccessible(true);
                state("TRANSFERS").put(id, constructor.newInstance(AnvilCraft.of("port_expired"), 20,
                    System.nanoTime() - 31000000000L));
                state("LAST_REQUEST").put(id, System.nanoTime() - 31000000000L);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
            HandbookStructureExportHandler.onServerTick(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
            helper.assertTrue(!state("TRANSFERS").containsKey(id) && !state("LAST_REQUEST").containsKey(id), "Expired state is cleaned");
            new HandbookStructureExportPacket(AnvilCraft.of("port_logout"), 20, 0, new byte[]{1}).handleOnServer(player);
            HandbookStructureExportHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            helper.assertTrue(!state("TRANSFERS").containsKey(id) && !state("LAST_REQUEST").containsKey(id), "Logout clears both maps");
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> state(String name) {
        try {
            var field = HandbookStructureExportHandler.class.getDeclaredField(name);
            field.setAccessible(true);
            return (Map<UUID, Object>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
