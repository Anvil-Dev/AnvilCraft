package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerSyncFixture;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.network.PowerGridSyncChunkPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PowerSyncTests {
    private static final int ID = -734510;
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_power_sync_codec", PowerSyncTests::codec,
        "port_power_sync_chunks", PowerSyncTests::chunks,
        "port_power_sync_atomic", PowerSyncTests::atomic,
        "port_power_sync_remove", PowerSyncTests::remove,
        "port_power_sync_invalid", PowerSyncTests::invalid
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_power_sync"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    public static PowerComponentInfo info(int x, PowerComponentType type) {
        return new PowerComponentInfo(new BlockPos(x, 160, 0), 11, 22, 33, 44, 9,
            new AABB(x + 0.125, 159.75, -0.375, x + 1.875, 161.25, 1.625), type, type == PowerComponentType.PRODUCER);
    }

    private static void codec(GameTestHelper h) {
        var original = info(29999990, PowerComponentType.PRODUCER);
        var buffer = Unpooled.buffer();
        try {
            PowerComponentInfo.STREAM_CODEC.encode(buffer, original);
            h.assertTrue(PowerComponentInfo.STREAM_CODEC.decode(buffer).equals(original),
                "Every scalar and double-precision bound survives");
        } finally {
            buffer.release();
        }
        var tag = (CompoundTag) PowerComponentInfo.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        h.assertTrue(PowerComponentInfo.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().equals(original), "NBT retains infinite power");
        tag.remove("infinitePower");
        h.assertTrue(!PowerComponentInfo.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().infinitePower(),
                "Legacy native NBT defaults finite");
        h.succeed();
    }

    private static void chunks(GameTestHelper h) {
        for (int count : new int[]{0, 1, 255, 256, 257, 2049}) {
            var grid = PowerSyncFixture.create(h.getLevel(), count);
            var packets = PowerSyncFixture.chunks(grid);
            int total = Math.max(1, (count + 255) / 256);
            h.assertTrue(packets.length == total, "Source chunk boundary " + count);
            var received = new ArrayList<PowerComponentInfo>();
            for (int index = 0; index < total; index++) {
                var packet = packets[index];
                h.assertTrue(packet.totalChunks() == total && packet.chunkIndex() == index && packet.components().size() <= 256,
                    "Packet numbering and per-packet limit");
                var buffer = Unpooled.buffer();
                try {
                    PowerGridSyncChunkPacket.STREAM_CODEC.encode(buffer, packet);
                    h.assertTrue(buffer.readableBytes() < 65536, "Each packet remains well below the custom payload limit");
                    var decoded = PowerGridSyncChunkPacket.STREAM_CODEC.decode(buffer);
                    h.assertTrue(decoded.equals(packet), "Chunk wire roundtrip");
                    received.addAll(decoded.components());
                } finally {
                    buffer.release();
                }
            }
            h.assertTrue(received.size() == count && received.stream().map(PowerComponentInfo::pos).distinct().count() == count,
                "No component lost or duplicated");
            for (var component : received) {
                int index = component.pos().getX() - 100000;
                h.assertTrue(component.produces() == 100 + index && component.infinitePower() == (index == 0),
                    "Producer-specific power and infinity survive the server snapshot");
            }
        }
        h.succeed();
    }

    private static SimplePowerGrid grid(List<PowerComponentInfo> components) {
        return new SimplePowerGrid(ID, "minecraft:overworld", BlockPos.ZERO, components, 999, 123, true);
    }

    public static PowerGridSyncChunkPacket packet(int total, int index, List<PowerComponentInfo> components) {
        return new PowerGridSyncChunkPacket(ID, "minecraft:overworld", BlockPos.ZERO, total, index, 999, 123, true, components);
    }

    private static void atomic(GameTestHelper h) {
        try {
            var transmitters = List.of(info(0, PowerComponentType.TRANSMITTER), info(5, PowerComponentType.TRANSMITTER));
            var old = grid(transmitters);
            PowerGridSupport.acceptGrid(old);
            var lines = old.getPowerTransmitterLines();
            h.assertTrue(lines.size() == 1, "Fixture has one transmitter link");
            var producer = info(2, PowerComponentType.PRODUCER);
            PowerGridSupport.mergeSyncChunk(packet(2, 1, List.of(producer)));
            PowerGridSupport.mergeSyncChunk(packet(2, 1, List.of(producer)));
            h.assertTrue(PowerGridSupport.getGridMap().get(ID) == old, "Partial and duplicate chunks must not replace the visible grid");
            PowerGridSupport.mergeSyncChunk(packet(2, 0, transmitters));
            var current = PowerGridSupport.getGridMap().get(ID);
            h.assertTrue(current != old && current.getPowerComponentInfoList()
                .equals(List.of(transmitters.get(0), transmitters.get(1), producer)),
                "Out-of-order chunks assemble in source order");
            h.assertTrue(current.getPowerTransmitterLines() == lines, "Unchanged links are reused without flicker");
            h.assertTrue(current.isInfinitePower() && current.getGenerate() == 999 && current.getConsume() == 123,
                "Grid summary is preserved");
        } finally {
            PowerGridSupport.clearAllGrid();
        }
        h.succeed();
    }

    private static void remove(GameTestHelper h) {
        var item = info(0, PowerComponentType.PRODUCER);
        try {
            PowerGridSupport.mergeSyncChunk(packet(2, 0, List.of(item)));
            PowerGridSupport.removeGrid(ID);
            PowerGridSupport.mergeSyncChunk(packet(2, 1, List.of(item)));
            h.assertTrue(!PowerGridSupport.getGridMap().containsKey(ID), "Remove clears incomplete assembly");
            PowerGridSupport.clearAllGrid();
            PowerGridSupport.mergeSyncChunk(packet(2, 0, List.of(item)));
            var complete = grid(List.of(item));
            PowerGridSupport.acceptGrid(complete);
            PowerGridSupport.mergeSyncChunk(packet(2, 1, List.of(item)));
            h.assertTrue(PowerGridSupport.getGridMap().get(ID) == complete, "Legacy full updates discard pending chunks");
            PowerGridSupport.clearAllGrid();
            PowerGridSupport.mergeSyncChunk(packet(2, 0, List.of(item)));
            h.assertTrue(!PowerGridSupport.getGridMap().containsKey(ID), "World cleanup clears incomplete assembly");
        } finally {
            PowerGridSupport.clearAllGrid();
        }
        h.succeed();
    }

    private static void invalid(GameTestHelper h) {
        try {
            for (var packet : List.of(packet(0, 0, List.of()), packet(1, -1, List.of()), packet(1, 1, List.of()))) {
                PowerGridSupport.mergeSyncChunk(packet);
            }
            h.assertTrue(!PowerGridSupport.getGridMap().containsKey(ID), "Invalid indices are ignored");
            PowerGridSupport.mergeSyncChunk(packet(Integer.MAX_VALUE, 0, List.of()));
            h.assertTrue(!PowerGridSupport.getGridMap().containsKey(ID), "Advertised chunk count does not allocate an enormous array");
            var buffer = Unpooled.buffer();
            boolean rejected = false;
            try {
                PowerGridSyncChunkPacket.STREAM_CODEC.encode(buffer, packet(1, 0, java.util.Collections.nCopies(257,
                    info(0, PowerComponentType.PRODUCER))));
            } catch (RuntimeException expected) {
                rejected = true;
            } finally {
                buffer.release();
            }
            h.assertTrue(rejected, "Oversized component lists are rejected by the codec");
        } finally {
            PowerGridSupport.clearAllGrid();
        }
        h.succeed();
    }
}
