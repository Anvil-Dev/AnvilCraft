package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.laser.LaserComponentMap;
import dev.dubhe.anvilcraft.api.laser.LaserComponentTypes;
import dev.dubhe.anvilcraft.api.laser.LaserHitBehavior;
import dev.dubhe.anvilcraft.api.laser.LaserMiningComponent;
import dev.dubhe.anvilcraft.api.laser.LaserStrengthComponent;
import dev.dubhe.anvilcraft.api.laser.LaserTypeComponent;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LensBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.laser.LensBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.LaserHitRecipe;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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
public final class LaserComponentTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_laser_components_merge", LaserComponentTests::merge,
        "port_laser_components_recipes", LaserComponentTests::recipes,
        "port_laser_components_continuous", LaserComponentTests::continuous,
        "port_laser_components_lens", LaserComponentTests::lens,
        "port_laser_components_heating", LaserComponentTests::heating,
        "port_laser_components_gamma", LaserComponentTests::gamma,
        "port_laser_components_interface", LaserComponentTests::interfaces,
        "port_laser_components_delivery", LaserComponentTests::delivery
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_laser_components"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static final class Beam extends BaseLaserBlockEntity {
        int strength;
        boolean gamma;

        final List<ItemStack> drops = new ArrayList<>();

        Beam(GameTestHelper helper, int strength, boolean gamma) {
            super(ModBlockEntities.RUBY_LASER.get(), helper.absolutePos(new BlockPos(3, 5, 3)), ModBlocks.RUBY_LASER.getDefaultState());
            setLevel(helper.getLevel());
            this.strength = strength;
            this.gamma = gamma;
        }

        @Override
        public Direction getFacing() {
            return Direction.EAST;
        }

        @Override
        protected int getBaseLaserLevel() {
            return this.strength;
        }

        @Override
        protected boolean isGammaLaserConfigured() {
            return this.gamma;
        }

        @Override
        public void deliverItem(List<ItemStack> stacks, Direction direction, BlockPos source) {
            this.drops.addAll(stacks);
        }

        void deliverToInventory(List<ItemStack> stacks) {
            super.deliverItem(stacks, Direction.EAST, getBlockPos().east(4));
        }

        void step() {
            this.tickCount++;
            emitLaser(Direction.EAST);
        }

        void steps(int count) {
            for (int i = 0; i < count; i++) this.step();
        }
    }

    private static void merge(GameTestHelper h) {
        var a = new LaserComponentMap();
        var b = new LaserComponentMap();
        a.put(LaserComponentTypes.STRENGTH, new LaserStrengthComponent(Integer.MAX_VALUE));
        b.put(LaserComponentTypes.STRENGTH, new LaserStrengthComponent(16));
        a.put(LaserComponentTypes.MINING, new LaserMiningComponent(BlockMiningEffect.SILK_TOUCH, true));
        b.put(LaserComponentTypes.MINING, new LaserMiningComponent(BlockMiningEffect.SMELTING, true));
        b.put(LaserComponentTypes.LASER_TYPE, new LaserTypeComponent(true));
        var merged = LaserComponentMap.mergeIncoming(List.of(a, b));
        h.assertTrue(merged.get(LaserComponentTypes.STRENGTH).strength() == Integer.MAX_VALUE, "合束强度饱和而不溢出");
        h.assertTrue(merged.get(LaserComponentTypes.LASER_TYPE).gamma(), "混合束保留伽马");
        h.assertTrue(merged.get(LaserComponentTypes.MINING).equals(new LaserMiningComponent(BlockMiningEffect.NORMAL, false)),
            "不兼容镜片恢复普通开采");
        b.put(LaserComponentTypes.MINING, new LaserMiningComponent(BlockMiningEffect.SILK_TOUCH, false));
        merged = LaserComponentMap.mergeIncoming(List.of(a, b));
        h.assertTrue(merged.get(LaserComponentTypes.MINING).specialTargets(), "兼容镜片合并特殊目标能力");
        var local = new LaserHitBehavior();
        a.put(LaserComponentTypes.HIT_BEHAVIOR, local);
        b.put(LaserComponentTypes.HIT_BEHAVIOR, new LaserHitBehavior());
        a.replaceWith(b);
        h.assertTrue(a.get(LaserComponentTypes.HIT_BEHAVIOR) == local, "组件更新保留本地进度实例");
        var beam = new Beam(h, 1, false);
        h.assertTrue(beam.setOrCreateComponent(LaserComponentTypes.STRENGTH, null, null) == null, "无环境不创建组件");
        var explicit = new LaserStrengthComponent(7);
        h.assertTrue(beam.setOrCreateComponent(LaserComponentTypes.STRENGTH, explicit, 8) == explicit, "显式实例优先");
        h.assertTrue(beam.setOrCreateComponent(LaserComponentTypes.STRENGTH, null, 9) == explicit, "已有实例优先于工厂");
        h.succeed();
    }

    private static void recipes(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(8, 5, 3));
        h.assertTrue(level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.LASER_HIT.get()).size() == 36, "36 个原分支激光配方");
        for (Block ore : List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS)) {
            level.setBlock(pos, ore.defaultBlockState(), Block.UPDATE_ALL);
            h.assertTrue(LaserHitRecipe.find(level, new LaserHitRecipe.Input(pos, ore.defaultBlockState(), 3, false)).isEmpty(), "三级不可开采");
            for (int tier = 1; tier <= 4; tier++) {
                var recipe = LaserHitRecipe.find(level, new LaserHitRecipe.Input(pos, ore.defaultBlockState(), tier * 4,
                    false)).orElseThrow().value();
                h.assertTrue(recipe.getLaserStrength() == tier * 4 && recipe.getHitTime() == new int[]{480, 120, 40, 20}[tier - 1],
                    "最高可用强度优先");
                Block expected = ore == Blocks.DIAMOND_ORE ? Blocks.STONE
                    : ore == Blocks.DEEPSLATE_DIAMOND_ORE ? Blocks.DEEPSLATE : Blocks.NETHERRACK;
                h.assertTrue(recipe.getResultBlock().is(expected), "矿种替换优先级正确");
            }
            h.assertTrue(LaserHitRecipe.find(level, new LaserHitRecipe.Input(pos, ore.defaultBlockState(), 16, true, true)).isEmpty(),
                "普通配方不吞掉伽马行为");
        }
        h.succeed();
    }

    private static void continuous(GameTestHelper h) {
        var beam = new Beam(h, 16, false);
        var target = beam.getBlockPos().east(4);
        h.getLevel().setBlock(target, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
        beam.steps(19);
        beam.emitLaser(Direction.EAST);
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.DIAMOND_ORE), "同刻重复发射不累计开采");
        beam.updateIrradiateBlockPos(null);
        beam.steps(19);
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.DIAMOND_ORE), "断光必须重置计时");
        beam.step();
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.STONE) && beam.drops.stream().anyMatch(s -> s.is(Items.DIAMOND)),
            "连续二十刻替换并交付矿物");
        h.getLevel().setBlock(target, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
        beam.steps(19);
        beam.setOrCreateComponent(LaserComponentTypes.MINING, new LaserMiningComponent(BlockMiningEffect.SILK_TOUCH, true), null);
        beam.step();
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.DIAMOND_ORE), "更换开采效果重置进度");
        beam.steps(19);
        h.assertTrue(beam.drops.stream().anyMatch(s -> s.is(Items.DIAMOND_ORE)), "精准采集使用方块战利品");
        h.succeed();
    }

    private static void lens(GameTestHelper h) {
        var beam = new Beam(h, 16, false);
        var lensPos = beam.getBlockPos().east(2);
        var target = lensPos.east(2);
        h.getLevel().setBlock(lensPos, ModBlocks.LENS.getDefaultState().setValue(LensBlock.AXIS,
            Direction.Axis.X).setValue(LensBlock.TYPE, LensType.ROYAL), Block.UPDATE_ALL);
        var lens = (LensBlockEntity) h.getLevel().getBlockEntity(lensPos);
        for (Block block : List.of(ModBlocks.VOID_STONE.get(), ModBlocks.EARTH_CORE_SHARD_ORE.get())) {
            h.getLevel().setBlock(target, block.defaultBlockState(), Block.UPDATE_ALL);
            for (int tick = 0; tick < 20; tick++) {
                beam.step();
                lens.tick(h.getLevel());
            }
            h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.DEEPSLATE), "两种特殊目标均留下深板岩");
            h.assertTrue(beam.drops.stream().anyMatch(stack -> stack.is(block.asItem())), "真实透镜向上游交付精准采集掉落");
        }
        beam.updateLaserLevel(0);
        lens.onCancelingIrradiation(beam);
        h.assertTrue(lens.getIrradiateBlockPos() == null, "撤去输入清除下游光束");
        h.succeed();
    }

    private static void heating(GameTestHelper h) {
        var beam = new Beam(h, 1, false);
        var target = beam.getBlockPos().east(4);
        var results = List.of(ModBlocks.HEATED_NETHERITE_BLOCK.get(), ModBlocks.REDHOT_NETHERITE_BLOCK.get(),
            ModBlocks.GLOWING_NETHERITE_BLOCK.get(), ModBlocks.INCANDESCENT_NETHERITE_BLOCK.get());
        int[] strengths = {1, 4, 16, 64};
        for (int tier = 0; tier < 4; tier++) {
            beam.updateIrradiateBlockPos(null);
            beam.strength = strengths[tier];
            h.getLevel().setBlock(target, Blocks.NETHERITE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            beam.steps(19);
            h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.NETHERITE_BLOCK), "加热需要连续二十刻");
            beam.step();
            h.assertTrue(h.getLevel().getBlockState(target).is(results.get(tier)), "激光等级决定加热温度");
            var heated = (HeatableBlockEntity) h.getLevel().getBlockEntity(target);
            h.assertTrue(heated.getDuration() == 40, "首次加热增加四十刻");
            beam.steps(20);
            h.assertTrue(heated.getDuration() == 80, "持续照射续热且不重复替换实体");
        }
        h.succeed();
    }

    private static void gamma(GameTestHelper h) {
        var beam = new Beam(h, 4, true);
        var target = beam.getBlockPos().east(4);
        for (int tier = 0; tier < 4; tier++) {
            beam.updateIrradiateBlockPos(null);
            beam.strength = (tier + 1) * 4;
            h.getLevel().setBlock(target, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
            int required = new int[]{60, 20, 5, 1}[tier];
            beam.steps(required - 1);
            h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.GLASS), "伽马破坏前维持连续曝光阈值");
            beam.step();
            h.assertTrue(h.getLevel().getBlockState(target).isAir(), "伽马阻于玻璃并按等级破坏");
            h.assertTrue(beam.getLaserMaxLength() == 16, "伽马射程十六格");
        }
        h.getLevel().setBlock(target, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
        beam.steps(5);
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.BEDROCK), "不可破坏方块保持不变");
        beam.gamma = false;
        beam.step();
        h.assertTrue(beam.getLaserMaxLength() == 128 && !beam.isEmittingGamma(), "切回普通恢复射程与类型");
        h.succeed();
    }

    private static void interfaces(GameTestHelper h) {
        var beam = new Beam(h, 4, true);
        var target = beam.getBlockPos().east(4);
        var state = ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.getDefaultState()
            .setValue(CelestialForgingAnvilInterfaceBlock.FACING, Direction.WEST);
        h.getLevel().setBlock(target, state, Block.UPDATE_ALL);
        var receiver = (CelestialForgingAnvilLaserInterfaceBlockEntity) h.getLevel().getBlockEntity(target);
        beam.step();
        h.assertTrue(receiver.getReceivedLaserLevel() == 4 && receiver.isReceivedGamma(), "普通组件源的伽马类型传入锻星接口");
        receiver.onCancelingIrradiation(beam);
        receiver.emitGammaLaser(4);
        receiver.serverTick();
        h.assertTrue(receiver.isEmittingGamma() && receiver.getLaserLevel() == 4, "彭罗斯请求发射一刻");
        receiver.serverTick();
        h.assertTrue(!receiver.isEmittingGamma() && receiver.getLaserLevel() == 0 && receiver.getIrradiateBlockPos() == null, "停止请求清除伽马光束");
        h.succeed();
    }

    private static void delivery(GameTestHelper h) {
        var beam = new Beam(h, 16, false);
        var chestPos = beam.getBlockPos().west();
        h.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(chestPos);
        for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 63));
        beam.deliverToInventory(List.of(new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.IRON_INGOT, 3)));
        h.assertTrue(chest.getItem(0).getCount() == 64, "只填入库存可容纳部分");
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(chestPos).inflate(1));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 1
            && drops.stream().filter(e -> e.getItem().is(Items.IRON_INGOT)).mapToInt(e -> e.getItem().getCount()).sum() == 3,
                "仅剩余物品落地且数量守恒");
        h.succeed();
    }
}
