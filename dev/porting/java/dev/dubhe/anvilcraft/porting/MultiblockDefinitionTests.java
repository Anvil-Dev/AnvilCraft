package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.event.giantanvil.GiantAnvilLandingEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
public final class MultiblockDefinitionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_multiblock_rotations_nbt", MultiblockDefinitionTests::rotations,
        "port_multiblock_full_cube", MultiblockDefinitionTests::fullCube,
        "port_multiblock_definition_codec", MultiblockDefinitionTests::codec,
        "port_multiblock_conversion", MultiblockDefinitionTests::conversion,
        "port_multiblock_consume", MultiblockDefinitionTests::consume,
        "port_multiblock_anvil_event", MultiblockDefinitionTests::anvilEvent,
        "port_multiblock_predicate_identity", MultiblockDefinitionTests::predicateIdentity
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_multiblock_definition"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static MultiblockDefinition pattern(CompoundTag nbt) {
        return MultiblockDefinition.seriaBuilder()
            .layer("F  ", "   ", "   ")
            .layer("   ", "  S", "   ")
            .layer("   ", "   ", "  G")
            .map('F', BlockStatePredicate.builder().of(Blocks.FURNACE)
                .with(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).nbt(nbt))
            .map('S', Blocks.STONE).map('G', Blocks.GOLD_BLOCK).build();
    }

    private static BlockPos corner(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 1, 2));
    }

    private static FurnaceBlockEntity place(GameTestHelper helper, Rotation rotation) {
        BlockPos corner = corner(helper);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) helper.getLevel().setBlockAndUpdate(corner.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        var furnacePos = corner.offset(MultiblockUtil.rotatePatternToWorld(0, 0, 0, rotation, 3));
        helper.getLevel().setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).rotate(rotation));
        helper.getLevel().setBlockAndUpdate(corner.offset(MultiblockUtil.rotatePatternToWorld(2, 1, 1, rotation, 3)),
            Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(corner.offset(MultiblockUtil.rotatePatternToWorld(2, 2, 2, rotation, 3)),
            Blocks.GOLD_BLOCK.defaultBlockState());
        var furnace = (FurnaceBlockEntity) helper.getLevel().getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.DIAMOND, 3));
        return furnace;
    }

    private static CompoundTag predicateNbt(FurnaceBlockEntity furnace, GameTestHelper helper) {
        var tag = furnace.saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");
        return tag;
    }

    private static MultiblockInput input(GameTestHelper helper) {
        BlockPos corner = corner(helper);
        List<List<List<BlockState>>> blocks = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            List<List<BlockState>> layer = new ArrayList<>();
            for (int z = 0; z < 3; z++) {
                List<BlockState> row = new ArrayList<>();
                for (int x = 0; x < 3; x++) row.add(helper.getLevel().getBlockState(corner.offset(x, y, z)));
                layer.add(row);
            }
            blocks.add(layer);
        }
        return new MultiblockInput(blocks, 3, corner.offset(1, 3, 1));
    }

    private static void rotations(GameTestHelper helper) {
        for (Rotation rotation : Rotation.values()) {
            var furnace = place(helper, rotation);
            var pattern = pattern(predicateNbt(furnace, helper));
            var input = input(helper);
            helper.assertTrue(MultiblockUtil.match(pattern, input, helper.getLevel()).orElseThrow() == rotation,
                "旋转必须同时匹配网格坐标、方块朝向与实体 NBT");
            furnace.setItem(0, new ItemStack(Items.DIAMOND, 2));
            helper.assertTrue(MultiblockUtil.match(pattern, input, helper.getLevel()).isEmpty(), "NBT 不符时必须拒绝相同外观结构");
            furnace.clearContent();
        }
        helper.succeed();
    }

    private static void fullCube(GameTestHelper helper) {
        var furnace = place(helper, Rotation.NONE);
        var pattern = pattern(predicateNbt(furnace, helper));
        helper.getLevel().setBlockAndUpdate(corner(helper).offset(1, 1, 1), Blocks.STONE.defaultBlockState());
        helper.assertTrue(MultiblockUtil.match(pattern, input(helper), helper.getLevel()).isEmpty(), "空格位置必须为空气");
        var smaller = MultiblockDefinition.seriaBuilder().layer("S").map('S', Blocks.STONE).build();
        helper.assertTrue(MultiblockUtil.match(smaller, input(helper), helper.getLevel()).isEmpty(), "不可将小结构居中视为完整匹配");
        var invalid = MultiblockDefinition.seriaBuilder().layer("SS", "SS").layer("SS", "SS").layer("SS", "SS")
            .map('S', Blocks.STONE).build();
        helper.assertTrue(MultiblockUtil.match(invalid, input(helper), helper.getLevel()).isEmpty(), "非立方体定义必须拒绝");
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        var furnace = place(helper, Rotation.CLOCKWISE_90);
        var recipe = new MultiblockRecipe(pattern(predicateNbt(furnace, helper)), new ItemStackTemplate(Items.EMERALD, 4));
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var json = MultiblockRecipe.SERIALIZER.codec().codec().encodeStart(ops, recipe).getOrThrow();
        var decoded = MultiblockRecipe.SERIALIZER.codec().codec().parse(ops, json).getOrThrow();
        helper.assertTrue(decoded.matches(input(helper), helper.getLevel()) && decoded.getResult().count() == 4,
            "新配方 JSON 必须保留完整条件与产出数量");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            MultiblockRecipe.SERIALIZER.streamCodec().encode(buffer, recipe);
            var synced = MultiblockRecipe.SERIALIZER.streamCodec().decode(buffer);
            helper.assertTrue(synced.matches(input(helper), helper.getLevel()), "网络同步必须保留 NBT 条件");
            furnace.setItem(0, ItemStack.EMPTY);
            helper.assertTrue(!synced.matches(input(helper), helper.getLevel()), "同步后不可丢失 NBT 谓词");
        } finally {
            buffer.release();
        }
        var legacy = BlockPattern.create().layer("F").symbol('F', BlockPredicateWithState.of(Blocks.FURNACE)
            .hasState(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        var oldJson = BlockPattern.CODEC.encodeStart(ops, legacy).getOrThrow();
        var converted = MultiblockUtil.DEFINITION_CODEC.parse(ops, oldJson).getOrThrow();
        var predicate = DefinitionSerialization.fromDefinition(converted).mapping().values().iterator().next();
        helper.assertTrue(predicate.testWithoutEntity(Blocks.FURNACE.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), "旧格式属性必须迁移保留");
        helper.assertTrue(!predicate.testWithoutEntity(Blocks.FURNACE.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), "旧格式不能退化为仅匹配方块类型");
        helper.succeed();
    }

    private static void conversion(GameTestHelper helper) {
        var builder = MultiblockConversionRecipe.builder().inputLayer("S").outputLayer("D")
            .inputSymbol('S', Blocks.STONE).outputSymbol('D', Blocks.DIAMOND_BLOCK);
        helper.assertTrue(builder.defaultId().identifier().getPath().equals("multiblock_conversion/diamond_block"),
            "默认配方命名不能构造非法空物品模板");
        helper.assertTrue(builder.getResult().item().value() == Items.DIAMOND_BLOCK && builder.buildRecipe().getResultItem().isEmpty(),
            "构建器结果标识不得改变结构转换的实际产出方式");
        var furnace = place(helper, Rotation.CLOCKWISE_90);
        var output = MultiblockDefinition.seriaBuilder().layer("D  ", "   ", "   ").layer("   ", " C ", "   ")
            .layer("   ", "   ", "  G").map('D', BlockStatePredicate.builder().of(Blocks.DISPENSER)
                .with(BlockStateProperties.FACING, Direction.EAST)).map('C', Blocks.DIAMOND_BLOCK).map('G', Blocks.GOLD_BLOCK).build();
        var recipe = new MultiblockConversionRecipe(pattern(predicateNbt(furnace, helper)), output);
        var input = input(helper);
        helper.assertTrue(recipe.centerOutput() == Blocks.DIAMOND_BLOCK, "转换中心产物必须可供图标识别");
        recipe.assemble(helper.getLevel(), input.centerPos(), corner(helper), input);
        var state = helper.getLevel().getBlockState(corner(helper).offset(2, 0, 0));
        helper.assertTrue(state.is(Blocks.DISPENSER) && state.getValue(BlockStateProperties.FACING) == Direction.SOUTH,
            "输出方块位置与朝向必须同步旋转");
        helper.assertTrue(helper.getLevel().getBlockState(corner(helper).offset(1, 1, 1)).is(Blocks.DIAMOND_BLOCK), "转换中心必须生成");
        helper.assertTrue(helper.getLevel().getBlockState(corner(helper).offset(1, 1, 2)).isAir(), "原输入对应输出空格时必须清除");
        helper.succeed();
    }

    private static void consume(GameTestHelper helper) {
        var furnace = place(helper, Rotation.NONE);
        var recipe = new MultiblockRecipe(pattern(predicateNbt(furnace, helper)), new ItemStackTemplate(Items.EMERALD, 4));
        var input = input(helper);
        recipe.assemble(helper.getLevel(), input.centerPos(), corner(helper), input);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    helper.assertTrue(helper.getLevel().getBlockState(corner(helper).offset(x, y, z)).isAir(), "成功合成必须消耗结构");
                }
            }
        }
        var bounds = new AABB(corner(helper).getBottomCenter(), corner(helper).offset(3, 5, 3).getBottomCenter());
        int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds)
            .stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(count == 4, "合成产物必须只生成一次且数量正确");
        helper.succeed();
    }

    private static void predicateIdentity(GameTestHelper helper) {
        var tag = TagKey.create(Registries.BLOCK, AnvilCraft.of("port_unbound_construction_tag"));
        var builder = BlockStatePredicate.builder().of(MultiblockUtil.BLOCK_LOOKUP, tag);
        var first = builder.build();
        var second = builder.build();
        helper.assertTrue(first.equals(second) && first.hashCode() == second.hashCode(), "谓词身份不得触发未绑定标签的渲染缓存");
        var loadedBuilder = BlockStatePredicate.builder().of(Blocks.FURNACE);
        var loaded = loadedBuilder.build();
        var equal = loadedBuilder.build();
        int before = loaded.hashCode();
        loaded.getStatesCache();
        helper.assertTrue(loaded.hashCode() == before && loaded.equals(equal), "生成渲染状态后谓词身份必须稳定");
        try {
            var field = BlockStatePredicate.class.getDeclaredField("statesCache");
            field.setAccessible(true);
            helper.assertTrue(field.get(equal) == null, "等价比较不得为另一个谓词构建渲染缓存");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        helper.succeed();
    }

    private static void anvilEvent(GameTestHelper helper) {
        var recipe = helper.getLevel().getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.MULTIBLOCK.get()).stream()
            .filter(holder -> holder.id().identifier().getPath().equals("multiblock/giant_anvil_1"))
            .findFirst().orElseThrow().value();
        var definition = DefinitionSerialization.fromDefinition(recipe.getPattern());
        int size = definition.grid().length;
        helper.assertTrue(size == 3, "事件验证使用实际三阶巨型铁砧配方");
        var corner = corner(helper);
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    var predicate = definition.mapping().get(definition.grid()[y][z].charAt(x));
                    var state = predicate == null ? Blocks.AIR.defaultBlockState() : MultiblockUtil.getDefaultState(predicate);
                    helper.getLevel().setBlock(corner.offset(x, y, z), state, 18);
                }
            }
        }
        var center = corner.offset(1, 3, 1);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.getLevel().setBlockAndUpdate(center.offset(x, 0, z), Blocks.CRAFTING_TABLE.defaultBlockState());
            }
        }
        helper.getLevel().setBlockAndUpdate(center, ModBlocks.SPACE_OVERCOMPRESSOR.getDefaultState());
        var entity = new FallingGiantAnvilEntity(ModEntities.FALLING_GIANT_ANVIL.get(), helper.getLevel());
        GiantAnvilLandingEventListener.handleMultiblock(new AnvilEvent.GiantOnLand(helper.getLevel(), center.above(2), entity, 1));
        var bounds = new AABB(corner.getBottomCenter(), corner.offset(3, 5, 3).getBottomCenter());
        int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
            .map(ItemEntity::getItem).filter(stack -> stack.is(ModBlocks.GIANT_ANVIL.asItem())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(count == recipe.getResult().count(), "落地事件必须路由到新定义并生成真实配方产物");
        helper.assertTrue(helper.getLevel().getBlockState(corner).isAir(), "落地事件必须消耗输入结构");
        helper.succeed();
    }
}
