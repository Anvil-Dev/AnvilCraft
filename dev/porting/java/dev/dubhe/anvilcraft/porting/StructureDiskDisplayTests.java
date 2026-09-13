package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StructureDiskDisplayTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_disk_display_rotations", StructureDiskDisplayTests::rotations,
        "port_disk_display_invalid", StructureDiskDisplayTests::invalid,
        "port_disk_display_nbt", StructureDiskDisplayTests::nbt,
        "port_disk_display_conversion", StructureDiskDisplayTests::conversion,
        "port_disk_display_metadata", StructureDiskDisplayTests::metadata
    );

    @net.neoforged.bus.api.SubscribeEvent
    public static void functions(net.neoforged.neoforge.registries.RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void register(net.neoforged.neoforge.event.RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_structure_disk_display"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static StructureDiskData data(Direction facing, boolean flipped) {
        return new StructureDiskData("test_00000000-0000-0000-0000-000000000000.nbt", "Test", new UUID(0, 0), facing, 3, 3, 3, flipped);
    }

    private static CompoundTag requirement() {
        var tag = new CompoundTag();
        tag.putString("id", "minecraft:furnace");
        tag.putString("marker", "accepted");
        return tag;
    }

    static ListTag ints(int x, int y, int z) {
        var result = new ListTag();
        result.add(IntTag.valueOf(x));
        result.add(IntTag.valueOf(y));
        result.add(IntTag.valueOf(z));
        return result;
    }

    static CompoundTag structure(MultiblockDefinition definition, Direction facing, Rotation rotation, boolean flipped) {
        final var result = new CompoundTag();
        var grid = DefinitionSerialization.fromDefinition(definition);
        int size = grid.grid().length;
        result.put("size", ints(size, size, size));
        var palette = new ListTag();
        var blocks = new ListTag();
        List<BlockState> states = new ArrayList<>();
        Rotation storedFacing = switch (facing) {
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            default -> Rotation.NONE;
        };
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    char symbol = grid.grid()[y][z].charAt(x);
                    if (symbol == ' ') continue;
                    var state = MultiblockUtil.getDefaultState(grid.mapping().get(symbol)).rotate(rotation).rotate(storedFacing);
                    int index = states.indexOf(state);
                    if (index < 0) {
                        index = states.size();
                        states.add(state);
                        palette.add(NbtUtils.writeBlockState(state));
                    }
                    int px = switch (rotation) {
                        case CLOCKWISE_90 -> size - 1 - z;
                        case CLOCKWISE_180 -> size - 1 - x;
                        case COUNTERCLOCKWISE_90 -> z;
                        default -> x;
                    };
                    int pz = switch (rotation) {
                        case CLOCKWISE_90 -> x;
                        case CLOCKWISE_180 -> size - 1 - z;
                        case COUNTERCLOCKWISE_90 -> size - 1 - x;
                        default -> z;
                    };
                    var entry = new CompoundTag();
                    entry.put("pos", ints(px, flipped ? size - 1 - y : y, pz));
                    entry.putInt("state", index);
                    if (state.is(Blocks.FURNACE)) entry.put("nbt", requirement());
                    blocks.add(entry);
                }
            }
        }
        result.put("palette", palette);
        result.put("blocks", blocks);
        return result;
    }

    private static void rotations(GameTestHelper helper) {
        var definition = MultiblockDefinitionTests.pattern(requirement());
        final var choices = List.of(DiskDisplaySupport.pattern(definition, Items.EMERALD.getDefaultInstance()).orElseThrow());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Rotation rotation : Rotation.values()) {
                for (boolean flipped : new boolean[]{false, true}) {
                    var tag = structure(definition, facing, rotation, flipped);
                    helper.assertTrue(DiskDisplaySupport.matchStructure(
                        data(facing, flipped), tag, helper.getLevel().registryAccess(), choices)
                        .is(Items.EMERALD), "结构图标必须兼容扫描朝向、四向旋转和上下翻转");
                }
            }
        }
        helper.succeed();
    }

    private static void invalid(GameTestHelper helper) {
        var definition = MultiblockDefinitionTests.pattern(requirement());
        final var choices = List.of(DiskDisplaySupport.pattern(definition, Items.EMERALD.getDefaultInstance()).orElseThrow());
        var original = structure(definition, Direction.NORTH, Rotation.NONE, false);
        List<CompoundTag> invalid = new ArrayList<>();
        var duplicate = original.copy();
        duplicate.getListOrEmpty("blocks").add(duplicate.getListOrEmpty("blocks").getFirst().copy());
        invalid.add(duplicate);
        var outside = original.copy();
        outside.getListOrEmpty("blocks").getCompoundOrEmpty(0).put("pos", ints(3, 0, 0));
        invalid.add(outside);
        var state = original.copy();
        state.getListOrEmpty("blocks").getCompoundOrEmpty(0).putInt("state", 999);
        invalid.add(state);
        var unknown = original.copy();
        unknown.getListOrEmpty("palette").getCompoundOrEmpty(0).putString("Name", "anvilcraft:missing_block");
        invalid.add(unknown);
        var dimensions = original.copy();
        dimensions.getListOrEmpty("size").set(0, DoubleTag.valueOf(3));
        invalid.add(dimensions);
        var properties = original.copy();
        properties.getListOrEmpty("palette").getCompoundOrEmpty(0)
            .getCompoundOrEmpty("Properties").putString("facing", "invalid");
        invalid.add(properties);
        for (var tag : invalid) {
            helper.assertTrue(DiskDisplaySupport.matchStructure(
                data(Direction.NORTH, false), tag, helper.getLevel().registryAccess(), choices)
                .isEmpty(), "畸形结构不能显示配方结果");
        }
        helper.succeed();
    }

    private static void nbt(GameTestHelper helper) {
        var definition = MultiblockDefinitionTests.pattern(requirement());
        final var choices = List.of(DiskDisplaySupport.pattern(definition, Items.EMERALD.getDefaultInstance()).orElseThrow());
        var tag = structure(definition, Direction.NORTH, Rotation.NONE, false);
        tag.getListOrEmpty("blocks").getCompoundOrEmpty(0).getCompoundOrEmpty("nbt").putString("marker", "rejected");
        helper.assertTrue(DiskDisplaySupport.matchStructure(
                data(Direction.NORTH, false), tag, helper.getLevel().registryAccess(), choices)
            .isEmpty(), "NBT 不符时不能只凭外观识别配方");
        var badShape = new StructureDiskData(data(Direction.NORTH, false).file(), "Shape", new UUID(0, 0), Direction.NORTH, 3, 2, 3);
        helper.assertTrue(DiskDisplaySupport.matchStructure(badShape, tag, helper.getLevel().registryAccess(), choices).isEmpty(),
            "非立方体磁盘不能显示多方块结果");
        helper.succeed();
    }

    private static void conversion(GameTestHelper helper) {
        var input = MultiblockDefinitionTests.pattern(requirement());
        var mixed = MultiblockDefinition.seriaBuilder().layer("S  ", "   ", "   ").layer("   ", " C ", "   ")
            .layer("   ", "   ", "  G").map('S', Blocks.STONE).map('C', Blocks.DIAMOND_BLOCK).map('G', Blocks.GOLD_BLOCK).build();
        helper.assertTrue(DiskDisplaySupport.conversionResult(new MultiblockConversionRecipe(input, mixed)).is(Items.DIAMOND_BLOCK),
            "转换图标优先使用中心产物");
        var unique = MultiblockDefinition.seriaBuilder().layer("S  ", "   ", "   ").layer("   ", "   ", "   ")
            .layer("   ", "   ", "  S").map('S', Blocks.STONE).build();
        helper.assertTrue(DiskDisplaySupport.conversionResult(new MultiblockConversionRecipe(input, unique)).is(Items.STONE),
            "无中心产物时可识别唯一输出类型");
        helper.assertTrue(DiskDisplaySupport.conversionResult(new MultiblockConversionRecipe(input, input)).isEmpty(),
            "无中心且混合输出时不能猜测图标");
        helper.succeed();
    }

    private static void metadata(GameTestHelper helper) {
        var data = data(Direction.EAST, true);
        var json = StructureDiskData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        helper.assertTrue(StructureDiskData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(data), "翻转元数据必须持久化");
        json.getAsJsonObject().remove("upsideDown");
        helper.assertTrue(!StructureDiskData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().upsideDown(), "旧磁盘默认正向");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            StructureDiskData.STREAM_CODEC.encode(buffer, data);
            helper.assertTrue(StructureDiskData.STREAM_CODEC.decode(buffer).equals(data), "翻转元数据必须完整同步");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
