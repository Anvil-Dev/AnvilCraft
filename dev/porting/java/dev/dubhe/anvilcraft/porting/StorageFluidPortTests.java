package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageFluidPortTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_fluid_capacity", StorageFluidPortTests::capacity,
        "port_storage_fluid_bottles", StorageFluidPortTests::bottles,
        "port_storage_fluid_experience", StorageFluidPortTests::experience,
        "port_storage_fluid_bias", StorageFluidPortTests::bias,
        "port_storage_fluid_flow", StorageFluidPortTests::flow,
        "port_storage_fluid_save", StorageFluidPortTests::save,
        "port_storage_fluid_link", StorageFluidPortTests::link
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_fluid"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StorageFluidPortBlockEntity port(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.STORAGE_FLUID_PORT.get());
        return helper.getBlockEntity(POS, StorageFluidPortBlockEntity.class);
    }

    private static int fill(ResourceHandler<FluidResource> tank, FluidResource fluid, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = tank.insert(fluid, amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    private static void capacity(GameTestHelper helper) {
        var port = port(helper);
        var water = FluidResource.of(Fluids.WATER);
        helper.assertTrue(fill(port.getFluidHandler(), water, 200000) == 128000, "不连接核心也必须有 128 B 容量");
        helper.assertTrue(fill(port.getFluidHandler(), FluidResource.of(Fluids.LAVA), 1000) == 0, "不能混入第二种流体");
        try (Transaction transaction = Transaction.openRoot()) {
            port.getFluidHandler().extract(water, 1000, transaction);
        }
        helper.assertTrue(port.getFluid().getAmount() == 128000, "模拟/回滚不能丢失流体");
        port.clearFluid();
        helper.assertTrue(port.getFluid().isEmpty() && port.getRememberedFluid().getFluid() == Fluids.WATER,
            "取空后运行时仍需记住流体类型");
        helper.succeed();
    }

    private static void bottles(GameTestHelper helper) {
        var port = port(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && port.getFluid().getAmount() == 250
            && player.getMainHandItem().is(Items.GLASS_BOTTLE), "水瓶应注入 250 mB 并返还空瓶");
        helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && port.getFluid().isEmpty()
            && player.getMainHandItem().is(Items.POTION), "玻璃瓶应取出 250 mB 水");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BOTTLE));
        helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && port.getFluid().getAmount() == 250,
            "蜂蜜瓶应注入 250 mB");
        helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && player.getMainHandItem().is(Items.HONEY_BOTTLE),
            "玻璃瓶应恢复蜂蜜瓶");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && port.getFluid().getAmount() == 1000
            && player.getMainHandItem().is(Items.BUCKET), "原生桶交互应注入 1000 mB");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.MENGER_SPONGE.asItem()));
        port.onRightClick(player, InteractionHand.MAIN_HAND, List.of());
        helper.assertTrue(port.getFluid().isEmpty() && player.getMainHandItem().getCount() == 1, "门格海绵应清空流体且不消耗");
        helper.succeed();
    }

    private static void experience(GameTestHelper helper) {
        var port = port(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        int successes = 0;
        for (long seed : new long[]{0, 4096}) {
            var random = helper.getLevel().getRandom();
            random.setSeed(seed);
            final boolean expected = random.nextBoolean();
            random.setSeed(seed);
            port.clearFluid();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EXPERIENCE_BOTTLE));
            helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && player.getMainHandItem().is(Items.GLASS_BOTTLE),
                "附魔之瓶两种概率结果都应返还空瓶");
            helper.assertTrue(port.getFluid().getAmount() == (expected ? 250 : 0), "经验流体注入应使用源版随机判定");
            if (expected) successes++;
        }
        helper.assertTrue(successes == 1, "测试必须覆盖随机成功和失败两种结果");
        port.getTank().set(0, FluidResource.of(ModFluids.EXP_FLUID.get()), 250);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(port.onPlayerUse(player, InteractionHand.MAIN_HAND) && player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE),
            "250 mB 经验应取出为附魔之瓶");
        helper.succeed();
    }

    private static void adjust(StorageFluidPortBlockEntity port) {
        for (int tick = 0; tick < 11; tick++) port.tickServer();
    }

    private static void pipe(GameTestHelper helper) {
        helper.setBlock(POS.east(), ModBlocks.PIPE_STRAIGHT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
    }

    private static void bias(GameTestHelper helper) {
        var port = port(helper);
        port.getTank().set(0, FluidResource.of(Fluids.WATER), 32000);
        adjust(port);
        helper.assertTrue(port.getHeightBias() == 0, "没有相邻管道时不应空转调整偏置");
        pipe(helper);
        adjust(port);
        helper.assertTrue(port.getHeightBias() == -3, "低液位应向进液方向调低等效高度");
        for (int i = 0; i < 10; i++) adjust(port);
        helper.assertTrue(port.getHeightBias() == -20, "负偏置不能低于二十格");
        port.getTank().set(0, FluidResource.of(Fluids.WATER), 64000);
        adjust(port);
        helper.assertTrue(port.getHeightBias() == -20, "目标区间内应保持偏置，避免反复切换进出液");
        port.getTank().set(0, FluidResource.of(Fluids.WATER), 128000);
        adjust(port);
        helper.assertTrue(port.getHeightBias() == -17, "高液位应提高等效高度");
        port.clearFluid();
        adjust(port);
        helper.assertTrue(port.getHeightBias() == 0, "空罐应重置偏置");
        helper.succeed();
    }

    private static void flow(GameTestHelper helper) {
        var port = port(helper);
        port.getTank().set(0, FluidResource.of(Fluids.WATER), 16000);
        pipe(helper);
        helper.setBlock(POS.east(2), ModBlocks.FLUID_TANK.get());
        var source = helper.getBlockEntity(POS.east(2), FluidTankBlockEntity.class);
        fill(source.getFluidHandler(), FluidResource.of(Fluids.WATER), 16000);
        port.tickServer();
        var network = FluidNetworkScanner.scan(helper.getLevel(), helper.absolutePos(POS.east()));
        network.tick();
        helper.assertTrue(port.getFluid().getAmount() > 16000 && source.getFluidHandler().getAmountAsInt(0) < 16000,
            "等效高度偏置必须影响真实管网分配");
        helper.assertTrue(port.getFluid().getAmount() + source.getFluidHandler().getAmountAsInt(0) == 32000, "管网转移必须守恒");
        helper.succeed();
    }

    private static void save(GameTestHelper helper) {
        var port = port(helper);
        ItemStack packed = new ItemStack(ModBlocks.STORAGE_FLUID_PORT.asItem());
        port.saveToDrop(packed, helper.getLevel().registryAccess());
        helper.assertTrue(!packed.has(DataComponents.BLOCK_ENTITY_DATA), "空端口应保持可堆叠");
        fill(port.getFluidHandler(), FluidResource.of(Fluids.WATER), 4321);
        var tag = port.saveCustomOnly(helper.getLevel().registryAccess());
        helper.assertTrue(tag.getCompoundOrEmpty("Tank").contains("Fluid"), "必须保留源版 Tank.Fluid 格式");
        port.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(port.getFluid().getAmount() == 4321, "重载必须保留精确存量");
        helper.getLevel().destroyBlock(port.getBlockPos(), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(port.getBlockPos()).inflate(1),
            entity -> entity.getItem().is(ModBlocks.STORAGE_FLUID_PORT.asItem()));
        helper.assertTrue(drops.size() == 1, "实际破坏应掉落一个端口物品");
        ItemStack stack = drops.getFirst().getItem();
        drops.getFirst().discard();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos target = helper.absolutePos(POS.east(8));
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(target.getCenter(), Direction.UP, target, false)));
        var placed = (StorageFluidPortBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(placed != null && placed.getFluid().getAmount() == 4321, "重放置必须恢复携带流体");
        helper.succeed();
    }

    private static void link(GameTestHelper helper) {
        final var port = port(helper);
        helper.setBlock(POS.east(), ModBlocks.STORAGE_PORT.get());
        helper.setBlock(POS.east(2), ModBlocks.STORAGE_PORT.get());
        var block = ModBlocks.SHULKER_CONTAINER.get();
        var state = block.defaultBlockState();
        BlockPos center = helper.absolutePos(POS.east(4));
        for (var part : block.getParts()) {
            helper.getLevel().setBlock(center.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var core = (StorageBlockEntity) helper.getLevel().getBlockEntity(center);
        UUID id = UUID.randomUUID();
        core.setId(id);
        port.tickServer();
        helper.assertTrue(StoragePortManager.positions(id).contains(port.getBlockPos()), "物品端口应延伸流体端口的核心连接");
        helper.setBlock(POS.east(2), Blocks.AIR);
        for (int tick = 0; tick < 22; tick++) port.tickServer();
        helper.assertTrue(!StoragePortManager.positions(id).contains(port.getBlockPos()), "断开链路应撤销旧归属");
        helper.assertTrue(fill(port.getFluidHandler(), FluidResource.of(Fluids.WATER), 1000) == 1000, "断开核心后仍应能储存流体");
        helper.succeed();
    }
}
