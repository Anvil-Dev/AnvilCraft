package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingPlannerTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_plan_single", BuildingPlannerTests::single,
        "port_plan_tile", BuildingPlannerTests::tile,
        "port_plan_limit", BuildingPlannerTests::limit,
        "port_pattern_coordinates", BuildingPlannerTests::coordinates,
        "port_pattern_groups", BuildingPlannerTests::pattern,
        "port_plan_obstructions", BuildingPlannerTests::obstructions
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_building_planner"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BlockPos origin(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 15, 4));
        for (var cursor : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(9, 6, 9))) {
            helper.getLevel().setBlock(cursor, cursor.getY() < pos.getY() ? Blocks.STONE.defaultBlockState()
                : Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        return pos;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortPlan"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 15, 0))));
        player.setYRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.BUILDING_ROD.asStack());
        return player;
    }

    private static List<BuildingPlan.Cell> single(ServerPlayer player, ItemStack material, BlockPos pos, Direction face) {
        player.setItemInHand(InteractionHand.OFF_HAND, material);
        return BuildingBlockPlanner.singlePlacement(new UseOnContext(player.level(), player, InteractionHand.OFF_HAND, material,
            new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false)));
    }

    private static void single(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper);
        var slab = Blocks.OAK_SLAB.defaultBlockState();
        helper.getLevel().setBlockAndUpdate(pos, slab);
        var cells = single(player, new ItemStack(Items.OAK_SLAB), pos, Direction.UP);
        helper.assertTrue(cells.size() == 1 && cells.getFirst().pos().equals(pos)
            && cells.getFirst().state().getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE,
            "单点使用应复用原版半砖合并落点而不是移动到上方");
        helper.assertTrue(helper.getLevel().getBlockState(pos) == slab, "规划不得提前修改世界");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(single(player, new ItemStack(Items.OAK_DOOR), pos, Direction.UP).size() == 2, "门应包含上下两格");
        helper.assertTrue(single(player, new ItemStack(Items.RED_BED), pos, Direction.UP).size() == 2, "床应包含头脚两格");
        helper.assertTrue(single(player, new ItemStack(ModBlocks.LARGE_CAKE.get()), pos, Direction.UP).size() == 27,
            "大蛋糕应与实际物品一致生成 27 格");
        player.setGameMode(GameType.ADVENTURE);
        helper.assertTrue(single(player, new ItemStack(Items.STONE), pos, Direction.UP).isEmpty(), "不可建造玩家不能获得可提交计划");
        helper.succeed();
    }

    private static void tile(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper);
        var material = new ItemStack(ModBlocks.LARGE_CAKE.get());
        var groups = BuildingBlockPlanner.planBlocks(player, material, pos, pos.east(3), Direction.UP, null);
        helper.assertTrue(groups != null && groups.size() == 2, "宽 4 格选择区应按 3 格蛋糕足迹平铺两组");
        var positions = new HashSet<BlockPos>();
        for (var group : groups) {
            helper.assertTrue(group.cells.size() == 27 && group.blockMaterials.size() == 1,
                "每座完整蛋糕只收一份真实物品");
            group.cells.forEach(cell -> helper.assertTrue(positions.add(cell.pos()), "平铺组不能重叠"));
        }
        var backward = BuildingBlockPlanner.planBlocks(player, material, pos.east(3), pos, Direction.UP, null);
        helper.assertTrue(backward != null && backward.stream().flatMap(group -> group.cells.stream())
            .map(BuildingPlan.Cell::pos).collect(java.util.stream.Collectors.toSet()).equals(positions), "反向拖拽必须覆盖相同区域");
        helper.getLevel().setBlockAndUpdate(pos.east(4).above(2), Blocks.STONE.defaultBlockState());
        helper.assertTrue(BuildingBlockPlanner.planBlocks(player, material, pos, pos.east(3), Direction.UP, null) == null,
            "模板伸出框选区的格也必须检查阻挡");
        helper.succeed();
    }

    private static void limit(GameTestHelper helper) {
        var player = player(helper);
        var pos = helper.absolutePos(new BlockPos(2, 30, 2));
        var material = new ItemStack(Items.STONE);
        var groups = BuildingBlockPlanner.planBlocks(player, material, pos, pos.offset(9, 19, 19), Direction.UP, null);
        helper.assertTrue(groups != null && groups.size() == 4000, "4000 格仍允许规划");
        helper.assertTrue(BuildingBlockPlanner.planBlocks(player, material, pos, pos.offset(15, 15, 15), Direction.UP, null) == null,
            "4096 格超过共享限制必须拒绝");
        helper.succeed();
    }

    private static FilterContent content(boolean tiled) {
        var slots = NonNullList.withSize(18, ItemStack.EMPTY);
        slots.set(7, new ItemStack(Items.RED_CONCRETE));
        slots.get(7).set(DataComponents.CUSTOM_NAME, Component.literal("Pattern component"));
        slots.set(8, new ItemStack(Items.BLUE_CONCRETE));
        slots.set(13, new ItemStack(Items.GREEN_CONCRETE));
        return new FilterContent(slots, tiled, false);
    }

    private static void coordinates(GameTestHelper helper) {
        var first = new BlockPos(5, 16, 4);
        var tiled = new BuildingRodPattern(content(true));
        helper.assertTrue(tiled.material(first, first, Direction.UP, 0).has(DataComponents.CUSTOM_NAME), "图案保留样品组件");
        helper.assertTrue(tiled.material(first, first.west(), Direction.UP, 0).is(Items.BLUE_CONCRETE), "负列索引周期回绕");
        helper.assertTrue(tiled.material(first, first.north(), Direction.UP, 0).is(Items.GREEN_CONCRETE), "顶面按 Z 映射行");
        helper.assertTrue(tiled.material(first, first.above(), Direction.NORTH, 0).is(Items.GREEN_CONCRETE), "侧面高度反向映射行");
        helper.assertTrue(tiled.material(first, first.south(), Direction.EAST, 0).is(Items.BLUE_CONCRETE), "东西面按 Z 映射列");
        helper.assertTrue(tiled.material(first, first.east().south(), Direction.UP, 0).isEmpty(), "图案内部空槽保留为空洞");
        var random = new BuildingRodPattern(content(false));
        boolean changed = false;
        for (int x = -16; x <= 16; x++) {
            var cursor = first.east(x);
            var chosen = random.material(first, cursor, Direction.UP, 42);
            helper.assertTrue(ItemStack.matches(chosen, random.material(first, cursor, Direction.NORTH, 42)),
                "相同种子和世界格随机材质一致，不受遍历方向影响");
            changed |= !ItemStack.isSameItem(chosen, random.material(first, cursor, Direction.UP, 43));
        }
        helper.assertTrue(changed, "改变种子应改变随机分布");
        helper.succeed();
    }

    private static void pattern(GameTestHelper helper) {
        var pos = origin(helper);
        var player = player(helper);
        var filter = ModItems.FILTER.asStack();
        var content = content(true);
        filter.set(ModComponents.FILTER_CONTENT, content);
        var groups = BuildingRodPattern.plan(player, filter, pos, pos.offset(1, 0, 1), Direction.UP, 1);
        helper.assertTrue(groups != null && groups.size() == 3, "2x2 图案空槽不参与放置");
        var allocated = new BuildingMaterials(player, filter).reserve(groups.getFirst(), false);
        helper.assertTrue(allocated == null, "过滤器里的样品不是免费材料");
        player.getInventory().setItem(1, content.list().get(7).copy());
        var materials = new BuildingMaterials(player, filter);
        allocated = materials.reserve(groups.getFirst(), false);
        helper.assertTrue(allocated != null && materials.consume(), "通过统一分配器消费真实材料");
        helper.assertTrue(!content.list().get(7).isEmpty() && player.getInventory().getItem(1).isEmpty(), "扣背包真实材料，样品保持不变");
        var beds = NonNullList.withSize(18, ItemStack.EMPTY);
        beds.set(0, new ItemStack(Items.RED_BED));
        filter.set(ModComponents.FILTER_CONTENT, new FilterContent(beds, true, false));
        var bedGroups = BuildingRodPattern.plan(player, filter, pos, pos.south(2), Direction.UP, 1);
        helper.assertTrue(bedGroups != null && bedGroups.size() == 2
            && bedGroups.stream().flatMap(group -> group.cells.stream()).map(BuildingPlan.Cell::pos).distinct().count() == 4,
            "图案中的床按完整两格组去重，避免相邻锚点重叠放置");
        helper.succeed();
    }

    private static BuildingPlan.Cell cell(BlockPos pos, BlockState state) {
        return new BuildingPlan.Cell(pos, state, new CompoundTag(), List.of());
    }

    private static void obstructions(GameTestHelper helper) {
        var pos = origin(helper);
        var chicken = EntityType.CHICKEN.create(helper.getLevel(), EntitySpawnReason.LOAD);
        chicken.setBaby(true);
        chicken.setNoAi(true);
        chicken.setNoGravity(true);
        chicken.setPos(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(chicken);
        try {
            var context = CollisionContext.empty();
            var full = List.of(cell(pos, Blocks.STONE.defaultBlockState()), cell(pos, Blocks.STONE.defaultBlockState()));
            helper.assertTrue(BuildingRodObstructions.find(helper.getLevel(), full, context).size() == 1,
                "同一阻挡实体只返回一次");
            var top = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
            helper.assertTrue(BuildingRodObstructions.find(helper.getLevel(), List.of(cell(pos, top)), context).isEmpty(),
                "实体处于上半砖空隙时不得按整格误报");
            helper.assertTrue(BuildingRodObstructions.find(helper.getLevel(), List.of(cell(pos, Blocks.TORCH.defaultBlockState())), context)
                .isEmpty(), "无碰撞方块不阻挡");
            chicken.discard();
            helper.assertTrue(BuildingRodObstructions.find(helper.getLevel(), full, context).isEmpty(), "已移除实体不能阻挡");
        } finally {
            chicken.discard();
        }
        helper.succeed();
    }
}
