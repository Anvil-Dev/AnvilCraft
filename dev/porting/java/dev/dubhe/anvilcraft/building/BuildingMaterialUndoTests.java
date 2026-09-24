package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.porting.StorageFluidRpcTests;
import dev.dubhe.anvilcraft.porting.TerminalAccessTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingMaterialUndoTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_material_reservation", BuildingMaterialUndoTests::reservation,
        "port_material_held_priority", BuildingMaterialUndoTests::priority,
        "port_material_components", BuildingMaterialUndoTests::components,
        "port_material_atomic_fluid", BuildingMaterialUndoTests::atomic,
        "port_undo_blocks_drops", BuildingMaterialUndoTests::blocks,
        "port_undo_entity_drops", BuildingMaterialUndoTests::entities,
        "port_undo_returned_container", BuildingMaterialUndoTests::returned,
        "port_undo_fluid_debt", BuildingMaterialUndoTests::fluidDebt,
        "port_undo_falling_chain", BuildingMaterialUndoTests::falling,
        "port_undo_primed_tnt", BuildingMaterialUndoTests::tnt
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_material_undo"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static BlockPos pos(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 16, 4));
    }

    static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortMaterialUndo"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    static BuildingPlan.Group block(BlockPos pos, Block block, ItemStack material) {
        var group = new BuildingPlan.Group();
        group.cells.add(new BuildingPlan.Cell(pos, block.defaultBlockState(), new CompoundTag(), List.of()));
        if (!material.isEmpty()) group.materials.add(material);
        return group;
    }

    static BuildingRodUndo paid(GameTestHelper helper, ServerPlayer player, BuildingPlan.Group group) {
        var materials = new BuildingMaterials(player, ItemStack.EMPTY, false);
        var allocated = materials.reserve(group, false);
        helper.assertTrue(allocated != null, "应能预留本次建造材料");
        var undo = new BuildingRodUndo(player, List.of(allocated));
        helper.assertTrue(materials.consume(), "预留后应统一扣除材料");
        BuildingCommit.quietly(helper.getLevel(), () -> allocated.cells.forEach(cell ->
            BuildingCommit.set(helper.getLevel(), cell.pos(), cell.state())));
        return undo;
    }

    private static void reservation(GameTestHelper helper) {
        var player = player(helper);
        var stack = new ItemStack(Items.STONE, 5);
        player.getInventory().setItem(0, stack);
        var materials = new BuildingMaterials(player, ItemStack.EMPTY, false);
        helper.assertTrue(!materials.reserve(List.of(new ItemStack(Items.STONE, 3), new ItemStack(Items.DIAMOND)))
            && stack.getCount() == 5, "失败预留必须回滚计数且不扣实物");
        helper.assertTrue(materials.reserve(List.of(new ItemStack(Items.STONE, 5))), "失败预留不能占住后续材料");
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        helper.assertTrue(!materials.consume() && player.getInventory().getItem(0).getCount() == 5,
            "替换后的槽位不能被旧 ItemStack 引用当成仍然持有的供料");
        helper.succeed();
    }

    private static void priority(GameTestHelper helper) {
        var player = player(helper);
        player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
        PocketInventory.get(player).setItem(0, new ItemStack(Items.STONE, 2));
        var held = new ItemStack(Items.STONE, 5);
        player.getInventory().setItem(0, held);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 3));
        var materials = new BuildingMaterials(player, held, false);
        helper.assertTrue(materials.reserve(List.of(new ItemStack(Items.STONE, 5))) && materials.consume(), "跨口袋与背包供料应成功");
        helper.assertTrue(PocketInventory.get(player).getItem(0).isEmpty() && player.getInventory().getItem(1).isEmpty()
            && held.getCount() == 5, "选中的材料栈应在其他随身来源之后消耗");
        player.getInventory().setItem(2, new ItemStack(Items.FLINT_AND_STEEL));
        var toolGroup = block(pos(helper), Blocks.STONE, new ItemStack(Items.FLINT_AND_STEEL));
        toolGroup.tools.add(Items.FLINT_AND_STEEL);
        helper.assertTrue(new BuildingMaterials(player, ItemStack.EMPTY, false).reserve(toolGroup, false) == null,
            "保留工具不能同时当作可消耗材料");
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        var player = player(helper);
        var stock = new ItemStack(Items.CHEST);
        stock.set(DataComponents.CUSTOM_NAME, Component.literal("actual"));
        stock.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.EMERALD, 3))));
        player.getInventory().setItem(0, stock);
        var expected = new ItemStack(Items.CHEST);
        expected.set(DataComponents.CUSTOM_NAME, Component.literal("blueprint"));
        var group = block(pos(helper), Blocks.CHEST, ItemStack.EMPTY);
        group.blockMaterials.put(pos(helper), expected);
        group.separateContents = true;
        var materials = new BuildingMaterials(player, ItemStack.EMPTY, false);
        helper.assertTrue(materials.reserve(group, false) == null && stock.getCount() == 1, "未经确认不能降级组件需求");
        var allocated = materials.reserve(group, true);
        helper.assertTrue(allocated != null && allocated.componentMismatch
            && Component.literal("actual").equals(allocated.blockMaterials.get(pos(helper)).get(DataComponents.CUSTOM_NAME)),
            "降级后必须使用实际物品组件，不能复制蓝图名称");
        helper.assertTrue(materials.consume() && player.getInventory().countItem(Items.CHEST) == 0
            && player.getInventory().countItem(Items.EMERALD) == 3, "实际供料容器中的内容应返还一次");
        helper.succeed();
    }

    private static void atomic(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            TerminalAccessTests.bound(fixture);
            fixture.stock(ItemResource.of(Items.DIAMOND), 1);
            var water = fixture.fluid(FluidResource.of(Fluids.WATER), 2000);
            var lava = fixture.fluid(FluidResource.of(Fluids.LAVA), 2000);
            var materials = new BuildingMaterials(player, ItemStack.EMPTY, false);
            helper.assertTrue(materials.reserve(List.of(new ItemStack(Items.DIAMOND)),
                List.of(new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.LAVA, 1000))), "预留应联合物品及两种流体");
            lava.getTank().set(0, FluidResource.EMPTY, 0);
            helper.assertTrue(!materials.consume() && water.getFluid().getAmount() == 2000
                && fixture.count(ItemResource.of(Items.DIAMOND)) == 1, "后续流体失效必须回滚前面的抽取且不扣物品");
            lava.getTank().set(0, FluidResource.of(Fluids.LAVA), 1000);
            helper.assertTrue(materials.consume() && water.getFluid().getAmount() == 1000 && lava.getFluid().isEmpty()
                && fixture.count(ItemResource.of(Items.DIAMOND)) == 0, "重新满足条件后应足量统一扣除");
        }
        helper.succeed();
    }

    private static void blocks(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_BLOCK));
        var undo = paid(helper, player, block(pos(helper), Blocks.DIAMOND_BLOCK, new ItemStack(Items.DIAMOND_BLOCK)));
        undo.finish(player);
        helper.getLevel().destroyBlock(pos(helper), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos(helper)).inflate(1));
        helper.assertTrue(!drops.isEmpty(), "测试必须产生真实方块掉落");
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE
            && helper.getLevel().getBlockState(pos(helper)).isAir() && drops.stream().allMatch(ItemEntity::isRemoved)
            && player.getInventory().countItem(Items.DIAMOND_BLOCK) == 1, "撤销应清理仍在世界中的掉落并返还原材料");
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.NOTHING
            && player.getInventory().countItem(Items.DIAMOND_BLOCK) == 1, "已完成账单不能重复退款");
        helper.succeed();
    }

    private static void entities(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
        var center = pos(helper).getCenter();
        var entity = new ItemEntity(helper.getLevel(), center.x, center.y, center.z, new ItemStack(Items.DIAMOND));
        var plan = new EntityBuildAdapter.Planned(new ItemStack(Items.DIAMOND), ItemStack.EMPTY,
            BlueprintCapture.saveEntity(entity), List.of(), List.of(), false);
        var group = new BuildingPlan.Group();
        group.entities.add(plan);
        group.materials.add(new ItemStack(Items.DIAMOND));
        var undo = paid(helper, player, group);
        helper.getLevel().addFreshEntity(entity);
        undo.recordEntity(plan, entity);
        undo.finish(player);
        var first = entity.spawnAtLocation(helper.getLevel(), new ItemStack(Items.EMERALD), Vec3.ZERO);
        var second = first.spawnAtLocation(helper.getLevel(), new ItemStack(Items.IRON_INGOT), Vec3.ZERO);
        entity.discard();
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE && first.isRemoved() && second.isRemoved()
            && player.getInventory().countItem(Items.DIAMOND) == 0
            && player.getInventory().countItem(Items.EMERALD) == 1 && player.getInventory().countItem(Items.IRON_INGOT) == 1,
            "撤销只结算仍存在的实际产物，不能返还已经消失的原实体材料");
        helper.succeed();
    }

    private static void returned(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        var group = block(pos(helper), Blocks.WATER, ItemStack.EMPTY);
        group.fluids.add(new FluidStack(Fluids.WATER, 1000));
        var undo = paid(helper, player, group);
        undo.finish(player);
        helper.assertTrue(player.getInventory().countItem(Items.BUCKET) == 1, "流体供料应产生实际空桶");
        player.getInventory().clearContent();
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.MISSING_CONTAINERS
            && helper.getLevel().getBlockState(pos(helper)).is(Blocks.WATER)
            && player.getInventory().countItem(Items.WATER_BUCKET) == 0, "缺少容器时不得修改区域，应保留水源等待补齐");
        player.getInventory().setItem(0, new ItemStack(Items.BUCKET));
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE
            && player.getInventory().countItem(Items.BUCKET) == 0 && player.getInventory().countItem(Items.WATER_BUCKET) == 1,
            "补回空桶后应完成一次退款");
        helper.succeed();
    }

    private static void fluidDebt(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            final var terminal = TerminalAccessTests.bound(fixture);
            var port = fixture.fluid(FluidResource.of(Fluids.WATER), 2000);
            var group = block(pos(helper), Blocks.WATER, ItemStack.EMPTY);
            group.fluids.add(new FluidStack(Fluids.WATER, 1000));
            var undo = paid(helper, player, group);
            undo.finish(player);
            int capacity = port.getFluidHandler().getCapacityAsInt(0, FluidResource.of(Fluids.WATER));
            port.getTank().set(0, FluidResource.of(Fluids.WATER), capacity);
            helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.MISSING_CONTAINERS
                && helper.getLevel().getBlockState(pos(helper)).is(Blocks.WATER),
                "原流体仓储装满时应在恢复区域之前拒绝撤销");
            try (Transaction transaction = Transaction.openRoot()) {
                port.getFluidHandler().extract(FluidResource.of(Fluids.WATER), 1000, transaction);
                transaction.commit();
            }
            helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE
                && port.getFluid().getAmount() == capacity && !terminal.isEmpty(), "释放容量后应向原仓储补回欠额");
        }
        helper.succeed();
    }

    private static void falling(GameTestHelper helper) {
        var player = player(helper);
        player.getInventory().setItem(0, new ItemStack(Items.SAND));
        var undo = paid(helper, player, block(pos(helper), Blocks.SAND, new ItemStack(Items.SAND)));
        undo.finish(player);
        var falling = FallingBlockEntity.fall(helper.getLevel(), pos(helper), Blocks.SAND.defaultBlockState());
        var drop = falling.spawnAtLocation(helper.getLevel(), new ItemStack(Items.SAND), Vec3.ZERO);
        falling.discard();
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE && falling.isRemoved() && drop.isRemoved()
            && player.getInventory().countItem(Items.SAND) == 1, "区块替换钩子应追踪下落方块及其后续产物");
        helper.succeed();
    }

    private static void tnt(GameTestHelper helper) {
        var player = player(helper);
        var pos = pos(helper);
        player.getInventory().setItem(0, new ItemStack(Items.TNT));
        var undo = paid(helper, player, block(pos, Blocks.TNT, new ItemStack(Items.TNT)));
        undo.finish(player);
        player.getInventory().setItem(0, new ItemStack(Items.FLINT_AND_STEEL));
        helper.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false));
        var primed = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
            new net.minecraft.world.phys.AABB(pos).inflate(1));
        helper.assertTrue(primed.size() == 1, "点火交互必须真实产生已激活 TNT");
        primed.getFirst().setFuse(1000000);
        var result = BuildingRodUndo.restore(player);
        boolean removed = primed.getFirst().isRemoved();
        primed.getFirst().discard();
        helper.assertTrue(result == BuildingRodUndo.Result.UNDONE && removed && player.getInventory().countItem(Items.TNT) == 1,
            "先生成实体再移除方块的点火顺序也必须被撤销追踪");
        helper.succeed();
    }
}
