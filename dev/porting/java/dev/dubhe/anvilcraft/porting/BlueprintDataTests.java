package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintTicks;
import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.building.LitematicaImporter;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintDataTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_blueprint_canonical", BlueprintDataTests::canonical,
        "port_blueprint_invalid", BlueprintDataTests::invalid,
        "port_blueprint_warnings", BlueprintDataTests::warnings,
        "port_blueprint_passengers", BlueprintDataTests::passengers,
        "port_blueprint_ticks", BlueprintDataTests::ticks,
        "port_litematic_packed", BlueprintDataTests::packed,
        "port_litematic_regions", BlueprintDataTests::regions,
        "port_litematic_invalid", BlueprintDataTests::litematicInvalid,
        "port_blueprint_hash_keys", BlueprintDataTests::hashKeys
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_blueprint_data"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StructureSnapshot.BlockEntry block(int x, int state, @Nullable CompoundTag data) {
        return new StructureSnapshot.BlockEntry(new BlockPos(x, 0, 0), state, Optional.ofNullable(data));
    }

    private static CompoundTag template() {
        return StructureSnapshotCodec.write(new StructureSnapshot(new Vec3i(2, 1, 1),
            List.of(Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState()),
            List.of(block(0, 0, null), block(1, 1, null)), List.of()));
    }

    private static StructureSnapshotCodec.ParsedSnapshot parse(GameTestHelper helper, CompoundTag tag) {
        try {
            return StructureSnapshotCodec.parse(tag, helper.getLevel().registryAccess());
        } catch (ConstructionBlueprintException error) {
            throw new IllegalStateException(error.reason() + ": " + error.detail(), error);
        }
    }

    private static LitematicaImporter.ConvertedStructure convert(CompoundTag tag) {
        try {
            return LitematicaImporter.convert(tag);
        } catch (ConstructionBlueprintException error) {
            throw new IllegalStateException(error.reason() + ": " + error.detail(), error);
        }
    }

    private static void expect(GameTestHelper helper, String reason, Checked action) {
        try {
            action.run();
            helper.fail("必须拒绝格式错误：" + reason);
        } catch (ConstructionBlueprintException error) {
            helper.assertTrue(error.reason().equals(reason), "错误类型应为 " + reason + "，实际 " + error.reason());
        }
    }

    private static void canonical(GameTestHelper helper) {
        var first = parse(helper, template()).snapshot();
        var second = new StructureSnapshot(new Vec3i(2, 1, 1),
            List.of(Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState()),
            List.of(block(1, 0, null), block(0, 1, null)), List.of());
        var expected = StructureSnapshotCodec.write(StructureSnapshotCodec.canonicalize(first));
        var actual = StructureSnapshotCodec.write(StructureSnapshotCodec.canonicalize(second));
        helper.assertTrue(expected.equals(actual) && StructureSnapshotCodec.hash(expected).equals(StructureSnapshotCodec.hash(actual)),
            "调色板编号与输入条目顺序不能改变规范内容或哈希");
        helper.assertTrue(first.blocks().getFirst().pos().equals(BlockPos.ZERO) && first.nonAirBlockCount() == 2,
            "规范排序与非空气计数必须保留");
        helper.succeed();
    }

    private static void invalid(GameTestHelper helper) {
        var invalidSize = template();
        invalidSize.put("size", ints(17, 1, 1));
        expect(helper, "oversized", () -> StructureSnapshotCodec.parse(invalidSize, helper.getLevel().registryAccess()));
        var mixed = template();
        var mixedSize = ints(2, 1, 1);
        mixedSize.set(1, StringTag.valueOf("1"));
        mixed.put("size", mixedSize);
        expect(helper, "corrupt_size", () -> StructureSnapshotCodec.parse(mixed, helper.getLevel().registryAccess()));
        var unknown = template();
        unknown.getListOrEmpty("palette").getCompoundOrEmpty(0).putString("Name", "missing_mod:unknown_block");
        expect(helper, "unknown_block", () -> StructureSnapshotCodec.parse(unknown, helper.getLevel().registryAccess()));
        var badIndex = template();
        badIndex.getListOrEmpty("blocks").getCompoundOrEmpty(0).putInt("state", 8);
        expect(helper, "corrupt_palette", () -> StructureSnapshotCodec.parse(badIndex, helper.getLevel().registryAccess()));
        var badPos = template();
        badPos.getListOrEmpty("blocks").getCompoundOrEmpty(0).put("pos", ints(-1, 0, 0));
        expect(helper, "position_out_of_bounds", () -> StructureSnapshotCodec.parse(badPos, helper.getLevel().registryAccess()));
        var entityData = new CompoundTag();
        entityData.putString("id", "missing_mod:unknown_entity");
        var unknownEntity = StructureSnapshotCodec.write(new StructureSnapshot(new Vec3i(1, 1, 1), List.of(), List.of(),
            List.of(new StructureSnapshot.EntityEntry(Vec3.ZERO, BlockPos.ZERO, entityData))));
        expect(helper, "unknown_entity", () -> StructureSnapshotCodec.parse(unknownEntity, helper.getLevel().registryAccess()));
        helper.succeed();
    }

    private static void warnings(GameTestHelper helper) {
        var tag = template();
        tag.getListOrEmpty("blocks").getCompoundOrEmpty(1).put("pos", ints(0, 0, 0));
        var palettes = new ListTag();
        palettes.add(tag.getListOrEmpty("palette"));
        palettes.add(tag.getListOrEmpty("palette").copy());
        tag.remove("palette");
        tag.put("palettes", palettes);
        var parsed = parse(helper, tag);
        helper.assertTrue(parsed.snapshot().blocks().size() == 1
            && parsed.snapshot().stateOf(parsed.snapshot().blocks().getFirst()).is(Blocks.DIRT), "重复坐标应保留最后一条");
        var reasons = parsed.warnings().stream().map(StructureSnapshotCodec.BlueprintWarning::reason).toList();
        helper.assertTrue(reasons.contains("duplicate_block_position") && reasons.contains("multiple_palettes"),
            "重复坐标与多调色板降级必须显式给出警告");
        helper.succeed();
    }

    private static void passengers(GameTestHelper helper) {
        UUID vehicleId = UUID.randomUUID();
        final UUID riderId = UUID.randomUUID();
        var vehicle = new CompoundTag();
        vehicle.putString("id", "minecraft:minecart");
        vehicle.store("UUID", UUIDUtil.CODEC, vehicleId);
        vehicle.put("Pos", doubles(100.5, 64, 200.5));
        var rider = new CompoundTag();
        rider.putString("id", "minecraft:pig");
        rider.store("UUID", UUIDUtil.CODEC, riderId);
        rider.put("Pos", doubles(100.5, 65, 200.5));
        var passengers = new ListTag();
        passengers.add(rider);
        vehicle.put("Passengers", passengers);
        var snapshot = new StructureSnapshot(new Vec3i(2, 2, 2), List.of(), List.of(), List.of(
            new StructureSnapshot.EntityEntry(new Vec3(0.5, 0, 0.5), BlockPos.ZERO, vehicle),
            new StructureSnapshot.EntityEntry(new Vec3(0.5, 1, 0.5), new BlockPos(0, 1, 0), rider)));
        var parsed = parse(helper, StructureSnapshotCodec.write(snapshot)).snapshot();
        helper.assertTrue(parsed.entities().size() == 2 && vehicle.contains("Passengers"), "乘客应展开去重且不修改输入数据");
        var normalized = parsed.entities().stream()
            .filter(entry -> entry.nbt().read("UUID", UUIDUtil.CODEC).orElseThrow().equals(riderId)).findFirst().orElseThrow();
        helper.assertTrue(normalized.pos().equals(new Vec3(0.5, 1, 0.5))
            && normalized.nbt().read("anvilcraft:vehicle", UUIDUtil.CODEC).orElseThrow().equals(vehicleId),
            "展开乘客必须保持局部位置和载具关系");
        helper.succeed();
    }

    private static void ticks(GameTestHelper helper) {
        var level = helper.getLevel();
        var first = helper.absolutePos(new BlockPos(2, 10, 2));
        var second = first.east(2);
        level.setBlock(first, Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(second, Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.scheduleTick(first, Blocks.OAK_PLANKS, 40, TickPriority.HIGH);
        var captured = BlueprintTicks.capture(level, BoundingBox.fromCorners(first, first));
        var tick = captured.stream().filter(entry -> entry.pos().equals(first)).findFirst().orElseThrow();
        helper.assertTrue(tick.delay() == 40 && tick.priority() == TickPriority.HIGH.getValue(), "捕获必须保留相对延迟与优先级");
        var tag = new CompoundTag();
        BlueprintTicks.write(tag, List.of(tick,
            new BlueprintTicks.Entry(first.above(), Identifier.parse("minecraft:water"), 12, 0, true, 7)));
        helper.assertTrue(BlueprintTicks.read(tag).size() == 2 && BlueprintTicks.read(tag).get(1).fluid(),
            "方块与流体 tick 必须往返保存");
        BlueprintTicks.restore(level, List.of(tick.at(second)), Set.of(second));
        var restored = BlueprintTicks.capture(level, BoundingBox.fromCorners(second, second)).getFirst();
        helper.assertTrue(restored.delay() == 40 && restored.priority() == tick.priority(), "实际调度恢复必须保持延迟和优先级");
        var skipped = second.above();
        level.setBlock(skipped, Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlueprintTicks.restore(level, List.of(tick.at(skipped)), Set.of());
        helper.assertTrue(BlueprintTicks.capture(level, BoundingBox.fromCorners(skipped, skipped)).isEmpty(), "不能恢复未提交位置的 tick");
        helper.succeed();
    }

    private static void packed(GameTestHelper helper) {
        List<BlockState> palette = List.of(Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
            Blocks.OAK_PLANKS.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState());
        int[] states = new int[32];
        for (int index = 0; index < states.length; index++) states[index] = index % palette.size();
        var root = litematic(Map.of("main", region(BlockPos.ZERO, new Vec3i(4, 2, 4), palette, states)));
        var parsed = parse(helper, convert(root).structureTag()).snapshot();
        helper.assertTrue(parsed.blocks().size() == states.length, "Litematica 必须输出完整稠密方块列表");
        for (var entry : parsed.blocks()) {
            int index = entry.pos().getX() + entry.pos().getZ() * 4 + entry.pos().getY() * 16;
            helper.assertTrue(parsed.stateOf(entry).equals(palette.get(states[index])), "跨 long 边界的调色板位数据不能串位：" + index);
        }
        helper.succeed();
    }

    private static void regions(GameTestHelper helper) {
        var negative = region(new BlockPos(-2, 0, 0), new Vec3i(-2, 1, 1), List.of(Blocks.STONE.defaultBlockState()), new int[]{0, 0});
        var positive = region(BlockPos.ZERO, new Vec3i(1, 1, 1), List.of(Blocks.DIRT.defaultBlockState()), new int[]{0});
        positive.putInt("CustomExtension", 1);
        var result = convert(litematic(Map.of("a", negative, "b", positive)));
        var parsed = parse(helper, result.structureTag()).snapshot();
        helper.assertTrue(parsed.size().equals(new Vec3i(4, 1, 1)) && parsed.blocks().size() == 4
            && parsed.stateOf(parsed.blocks().get(2)).isAir(), "负尺寸区域必须按最小角归一并保留中间空气");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.reason().equals("unmapped_fields")),
            "未映射扩展字段必须显式告知");
        var overlap = convert(litematic(Map.of("a", positive, "b", positive.copy())));
        helper.assertTrue(overlap.warnings().stream().anyMatch(warning -> warning.reason().equals("overlapping_regions")),
            "重叠区域必须报告警告");
        helper.succeed();
    }

    private static void litematicInvalid(GameTestHelper helper) {
        expect(helper, "corrupt_litematic", () -> LitematicaImporter.convert(new CompoundTag()));
        var truncated = region(BlockPos.ZERO, new Vec3i(2, 1, 1), List.of(Blocks.STONE.defaultBlockState()), new int[]{0, 0});
        truncated.putLongArray("BlockStates", new long[0]);
        expect(helper, "corrupt_litematic", () -> LitematicaImporter.convert(litematic(Map.of("bad", truncated))));
        var huge = region(BlockPos.ZERO, new Vec3i(1, 1, 1), List.of(Blocks.STONE.defaultBlockState()), new int[]{0});
        huge.getCompoundOrEmpty("Size").putInt("x", Integer.MIN_VALUE);
        expect(helper, "corrupt_litematic", () -> LitematicaImporter.convert(litematic(Map.of("bad", huge))));
        helper.succeed();
    }

    private static void hashKeys(GameTestHelper helper) {
        var first = new CompoundTag();
        first.putString("Aa", "one");
        first.putString("BB", "two");
        var second = new CompoundTag();
        second.putString("BB", "two");
        second.putString("Aa", "one");
        var a = new StructureSnapshot(new Vec3i(1, 1, 1), List.of(Blocks.CHEST.defaultBlockState()),
            List.of(block(0, 0, first)), List.of());
        var b = new StructureSnapshot(a.size(), a.palette(), List.of(block(0, 0, second)), List.of());
        var written = StructureSnapshotCodec.write(StructureSnapshotCodec.canonicalize(a));
        helper.assertTrue(StructureSnapshotCodec.hash(written)
            .equals(StructureSnapshotCodec.hash(StructureSnapshotCodec.write(StructureSnapshotCodec.canonicalize(b)))),
            "语义相同的方块实体数据不能因键的插入顺序产生不同哈希");
        first.putString("Aa", "changed");
        helper.assertTrue(written.getListOrEmpty("blocks").getCompoundOrEmpty(0).getCompoundOrEmpty("nbt")
            .getStringOr("Aa", "").equals("one"), "规范输出必须深拷贝方块实体数据");
        helper.succeed();
    }

    static CompoundTag litematic(Map<String, CompoundTag> entries) {
        var root = new CompoundTag();
        var regions = new CompoundTag();
        entries.forEach(regions::put);
        root.put("Regions", regions);
        root.putInt("MinecraftDataVersion", 3955);
        return root;
    }

    static CompoundTag region(BlockPos pos, Vec3i size, List<BlockState> palette, int[] states) {
        var region = new CompoundTag();
        region.put("Position", vector(pos));
        region.put("Size", vector(size));
        var paletteTag = new ListTag();
        palette.forEach(state -> paletteTag.add(NbtUtils.writeBlockState(state)));
        region.put("BlockStatePalette", paletteTag);
        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        long[] packed = new long[(states.length * bits + 63) / 64];
        for (int index = 0; index < states.length; index++) {
            int offset = index * bits;
            int word = offset / 64;
            int shift = offset % 64;
            packed[word] |= (long) states[index] << shift;
            if (shift + bits > 64) packed[word + 1] |= (long) states[index] >>> (64 - shift);
        }
        region.putLongArray("BlockStates", packed);
        return region;
    }

    private static CompoundTag vector(Vec3i pos) {
        var result = new CompoundTag();
        result.putInt("x", pos.getX());
        result.putInt("y", pos.getY());
        result.putInt("z", pos.getZ());
        return result;
    }

    private static ListTag ints(int... values) {
        var result = new ListTag();
        for (int value : values) result.add(IntTag.valueOf(value));
        return result;
    }

    private static ListTag doubles(double... values) {
        var result = new ListTag();
        for (double value : values) result.add(DoubleTag.valueOf(value));
        return result;
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws ConstructionBlueprintException;
    }
}
