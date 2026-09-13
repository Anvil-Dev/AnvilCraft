package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.IEmptyCauldron;
import dev.dubhe.anvilcraft.api.plasma.PlasmaJetBehavior;
import dev.dubhe.anvilcraft.api.plasma.PlasmaJetFuelHandler;
import dev.dubhe.anvilcraft.api.plasma.PlasmaJetHooks;
import dev.dubhe.anvilcraft.block.cauldron.FireCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.special.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PlasmaJetPortTests {
    private static final BlockPos BASE = new BlockPos(5, 2, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_plasma_empty_base", PlasmaJetPortTests::emptyBase,
        "port_plasma_fuel_simulation", PlasmaJetPortTests::fuelSimulation,
        "port_plasma_fish_fuel", PlasmaJetPortTests::fishFuel,
        "port_plasma_fuel_hook", PlasmaJetPortTests::fuelHook,
        "port_plasma_addon_save", PlasmaJetPortTests::addonSave,
        "port_plasma_structure", PlasmaJetPortTests::structure,
        "port_plasma_duration_cap", PlasmaJetPortTests::durationCap,
        "port_plasma_pass_through", PlasmaJetPortTests::passThrough
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_plasma"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void tube(GameTestHelper helper) {
        helper.setBlock(BASE.below(), ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false));
        for (Direction direction : Direction.Plane.HORIZONTAL) helper.setBlock(BASE.above().relative(direction), Blocks.STONE);
    }

    private static PlasmaJetsBlockEntity spawn(GameTestHelper helper) {
        tube(helper);
        helper.setBlock(BASE, ModBlocks.FIRE_CAULDRON.getDefaultState());
        helper.assertTrue(PlasmaJetsBlock.trySpawn(helper.absolutePos(BASE.above()), helper.getLevel()), "燃烧锅应能生成喷流");
        var initial = helper.getBlockEntity(BASE.above(), PlasmaJetsBlockEntity.class);
        PlasmaJetsBlockEntity.tick(helper.getLevel(), initial.getBlockPos(), initial.getBlockState(), initial);
        return helper.getBlockEntity(BASE.above(2), PlasmaJetsBlockEntity.class);
    }

    private static void tick(GameTestHelper helper, PlasmaJetsBlockEntity jet) {
        PlasmaJetsBlockEntity.tick(helper.getLevel(), jet.getBlockPos(), jet.getBlockState(), jet);
    }

    private static void emptyBase(GameTestHelper helper) {
        helper.assertTrue(Blocks.CAULDRON instanceof IEmptyCauldron, "原版空炼药锅必须具有空锅接口");
        tube(helper);
        helper.setBlock(BASE, Blocks.CAULDRON);
        helper.assertTrue(PlasmaJetsBlock.isValidBaseCauldron(helper.getLevel(), helper.absolutePos(BASE)), "空锅应能作为已有喷流底座");
        helper.assertTrue(!PlasmaJetsBlock.trySpawn(helper.absolutePos(BASE.above()), helper.getLevel()), "空锅不能启动喷流");
        var jet = spawn(helper);
        jet.setDuration(100);
        tick(helper, jet);
        helper.assertTrue(helper.getBlockState(BASE).is(Blocks.CAULDRON), "消耗最后一层后应变为空锅");
        int remaining = jet.getDuration();
        tick(helper, jet);
        helper.assertTrue(helper.getBlockState(BASE.above(2)).is(ModBlocks.PLASMA_JETS) && jet.getDuration() == remaining - 1,
            "底座变空后喷流必须继续运行并正常倒计时");
        jet.setDuration(0);
        tick(helper, jet);
        helper.assertTrue(helper.getBlockState(BASE.above(2)).isAir(), "剩余时长耗尽后才应停止");
        helper.succeed();
    }

    private static void fuelSimulation(GameTestHelper helper) {
        int old = AnvilCraft.CONFIG.plasmaJetsCauldronConsumeAmount;
        try {
            AnvilCraft.CONFIG.plasmaJetsCauldronConsumeAmount = 2;
            helper.setBlock(BASE, ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(FireCauldronBlock.LEVEL, 3));
            BlockPos pos = helper.absolutePos(BASE);
            helper.assertTrue(PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), pos, true).isPresent()
                && helper.getBlockState(BASE).getValue(FireCauldronBlock.LEVEL) == 3, "模拟消费不能改变液位");
            helper.assertTrue(PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), pos, false).orElseThrow()
                == AnvilCraft.CONFIG.plasmaJetsCauldronExtraDuration, "实际消费应返回配置的延长时长");
            helper.assertTrue(helper.getBlockState(BASE).getValue(FireCauldronBlock.LEVEL) == 1
                && PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), pos, false).isEmpty(), "不足一轮用量时不能部分扣除");
        } finally {
            AnvilCraft.CONFIG.plasmaJetsCauldronConsumeAmount = old;
        }
        helper.succeed();
    }

    private static void fishFuel(GameTestHelper helper) {
        helper.setBlock(BASE, ModBlocks.FISH_TANK.get());
        var tank = helper.getBlockEntity(BASE, FishTankBlockEntity.class);
        try (Transaction transaction = Transaction.openRoot()) {
            tank.getFluidHandler().insert(FluidResource.of(ModFluids.OIL.get()), 250, transaction);
            transaction.commit();
        }
        BlockPos pos = helper.absolutePos(BASE);
        helper.assertTrue(AnvilCraft.CONFIG.plasmaJetsFishTankExtraDuration == 24, "默认鱼缸续燃必须保持 24 gt");
        helper.assertTrue(PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), pos, true).orElseThrow() == 24
            && tank.getFluidHandler().getAmountAsInt(0) == 250, "鱼缸模拟应保留存量");
        helper.assertTrue(PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), pos, false).orElseThrow() == 24
            && tank.getFluidHandler().getAmountAsInt(0) == 249, "鱼缸每毫桶燃料应按最新规则延长时间");
        helper.succeed();
    }

    private static void fuelHook(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(BASE);
        helper.setBlock(BASE, Blocks.STONE);
        int[] consumed = {0};
        PlasmaJetHooks.registerFuel(new PlasmaJetFuelHandler() {
            @Override
            public TriState isValidBase(Level level, BlockPos pos) {
                return level == helper.getLevel() && pos.equals(target) ? TriState.TRUE : TriState.DEFAULT;
            }

            @Override
            public OptionalInt tryConsumeOnce(Level level, BlockPos pos, boolean simulate) {
                if (level != helper.getLevel() || !pos.equals(target)) return OptionalInt.empty();
                if (!simulate) consumed[0]++;
                return OptionalInt.of(7);
            }
        });
        helper.assertTrue(PlasmaJetsBlock.isValidBaseCauldron(helper.getLevel(), target), "扩展应能接管底座判断");
        helper.assertTrue(PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), target, true).orElseThrow() == 7 && consumed[0] == 0,
            "燃料扩展必须收到模拟标记");
        PlasmaJetsBlock.tryConsumeOnce(helper.getLevel(), target, false);
        helper.assertTrue(consumed[0] == 1, "实际消费应只执行一次扩展");
        helper.succeed();
    }

    private static void addonSave(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        PlasmaJetHooks.registerBehavior(new PlasmaJetBehavior() {
            @Override
            public void save(PlasmaJetsBlockEntity jet, CompoundTag tag, HolderLookup.Provider provider) {
                if (!tag.getBooleanOr("port_test", false)) return;
                helper.assertTrue(provider == registries, "保存回调必须收到输出器实际注册表，即使实体尚未附着世界");
                tag.putInt("saved", 42);
            }
        });
        var jet = new PlasmaJetsBlockEntity(ModBlockEntities.PLASMA_JETS.get(), helper.absolutePos(BASE.above(2)),
            ModBlocks.PLASMA_JETS.getDefaultState());
        jet.setDuration(123);
        jet.getAddonData().putBoolean("port_test", true);
        var wall = PlasmaJetsBlockEntity.TubeWallLayer.of(helper.absolutePos(BASE.above()));
        jet.getTubeWalls().add(wall);
        var tag = jet.saveCustomOnly(registries);
        var restored = new PlasmaJetsBlockEntity(ModBlockEntities.PLASMA_JETS.get(), jet.getBlockPos(), jet.getBlockState());
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        helper.assertTrue(restored.getDuration() == 123 && restored.getTubeWalls().equals(Set.of(wall))
            && restored.getAddonData().getIntOr("saved", 0) == 42, "时长、管壁与扩展数据必须往返保存");
        helper.succeed();
    }

    private static void structure(GameTestHelper helper) {
        var jet = spawn(helper);
        helper.assertTrue(jet.getTubeWalls().size() == 1 && jet.getCauldronPos().equals(helper.absolutePos(BASE)),
            "上升后必须保留管壁并定位原底座");
        jet.setDuration(100);
        helper.setBlock(BASE.above().north(), Blocks.AIR);
        tick(helper, jet);
        helper.assertTrue(helper.getBlockState(BASE.above(2)).isAir(), "管壁破损仍须立即停止喷流");
        helper.succeed();
    }

    private static void durationCap(GameTestHelper helper) {
        var jet = spawn(helper);
        int maximum = AnvilCraft.CONFIG.plasmaJetsMaxDuration;
        final int extra = AnvilCraft.CONFIG.plasmaJetsCauldronExtraDuration;
        jet.setDuration(maximum);
        tick(helper, jet);
        helper.assertTrue(jet.getDuration() == maximum - 1 && helper.getBlockState(BASE).is(ModBlocks.FIRE_CAULDRON),
            "剩余时长已满时不能浪费燃料");
        jet.setDuration(maximum - extra);
        tick(helper, jet);
        helper.assertTrue(jet.getDuration() == maximum - 1 && helper.getBlockState(BASE).is(Blocks.CAULDRON),
            "存在足够余量后才应实际消费并补充时长");
        helper.succeed();
    }

    private static void passThrough(GameTestHelper helper) {
        PlasmaJetHooks.registerPassThrough(state -> state.is(Blocks.GLASS));
        tube(helper);
        helper.setBlock(BASE, ModBlocks.FIRE_CAULDRON.getDefaultState());
        helper.setBlock(BASE.above(2), Blocks.GLASS);
        helper.assertTrue(PlasmaJetsBlock.trySpawn(helper.absolutePos(BASE.above()), helper.getLevel()), "扩展透过方块应允许生成喷流");
        var jet = helper.getBlockEntity(BASE.above(), PlasmaJetsBlockEntity.class);
        jet.setDuration(100);
        tick(helper, jet);
        helper.assertTrue(jet.getTubeWalls().isEmpty() && helper.getBlockState(BASE.above()).is(ModBlocks.PLASMA_JETS),
            "顶部为扩展透过方块时，应保留初始喷流而不继续上升");
        helper.succeed();
    }
}
