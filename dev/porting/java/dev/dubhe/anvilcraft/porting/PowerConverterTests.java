package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.block.power.converter.BasePowerConverterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PowerConverterTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);
    private static final int[] POWERS = {1, 16, 256, 4096, 65536};
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_converter_states", PowerConverterTests::states,
        "port_converter_generation", PowerConverterTests::generation,
        "port_converter_transactions", PowerConverterTests::transactions,
        "port_converter_recipes", PowerConverterTests::recipes
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_converter"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true))));
    }

    private static List<BasePowerConverterBlock> blocks() {
        return List.of(ModBlocks.POWER_CONVERTER_SMALL.get(), ModBlocks.POWER_CONVERTER_MIDDLE.get(), ModBlocks.POWER_CONVERTER_BIG.get(),
            ModBlocks.POWER_CONVERTER_SUPER_BIG.get(), ModBlocks.POWER_CONVERTER_EXTREMELY_BIG.get());
    }

    private static PowerConverterBlockEntity place(GameTestHelper helper, int tier) {
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS, blocks().get(tier));
        return helper.getBlockEntity(POS, PowerConverterBlockEntity.class);
    }

    private static void states(GameTestHelper helper) {
        for (int tier = 0; tier < 5; tier++) {
            var block = blocks().get(tier);
            var converter = place(helper, tier);
            helper.assertTrue(converter.getInputPower() == POWERS[tier] && converter.getMaxEnergyStored() == POWERS[tier] * 10000,
                "Every tier preserves its source demand and capacity");
            helper.assertTrue(block.defaultBlockState().is(ModBlockTags.POWER_CONVERTER)
                && block.asItem().getDefaultInstance().is(ModItemTags.POWER_CONVERTER)
                && block.asItem().getDescriptionId().equals(block.getDescriptionId()), "Converter tags and translation keys are complete");
            for (Direction facing : Direction.values()) {
                for (boolean powered : List.of(false, true)) {
                    for (boolean overload : List.of(false, true)) {
                        var state = block.defaultBlockState().setValue(BasePowerConverterBlock.FACING, facing)
                            .setValue(BasePowerConverterBlock.POWERED, powered).setValue(BasePowerConverterBlock.OVERLOAD, overload);
                        helper.assertTrue(state.getLightEmission() == (powered || overload ? 6 : 15),
                            "Redstone and overload both dim the converter");
                        int inset = 7 - tier;
                        int length = tier == 4 ? 16 : 8;
                        double[] min = {inset, inset, inset};
                        double[] max = {16 - inset, 16 - inset, 16 - inset};
                        int axis = facing.getAxis() == Direction.Axis.X ? 0 : facing.getAxis() == Direction.Axis.Y ? 1 : 2;
                        min[axis] = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 - length : 0;
                        max[axis] = min[axis] + length;
                        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(helper.getLevel(), helper.absolutePos(POS)),
                            Block.box(min[0], min[1], min[2], max[0], max[1], max[2]), BooleanOp.NOT_SAME),
                                "All facing shapes match source");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static void generation(GameTestHelper helper) {
        int efficiency = AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency;
        double loss = AnvilCraft.CONFIG.powerConverter.powerConverterLoss;
        try {
            AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency = 100;
            AnvilCraft.CONFIG.powerConverter.powerConverterLoss = 0.1;
            helper.setBlock(POS.west(), ModBlocks.CREATIVE_GENERATOR.get());
            var generator = helper.getBlockEntity(POS.west(), CreativeGeneratorBlockEntity.class);
            generator.setPower(65536);
            var dispatch = PowerGrid.class.getDeclaredMethod("gridTick");
            dispatch.setAccessible(true);
            for (int tier = 0; tier < 5; tier++) {
                var converter = place(helper, tier);
                var grid = new PowerGrid(helper.getLevel());
                grid.add(generator, converter);
                for (int tick = 0; tick < 40; tick++) converter.tick();
                helper.assertTrue(converter.getEnergyStored() == 0, "Block ticks do not duplicate grid generation");
                dispatch.invoke(grid);
                int amount = (int) (POWERS[tier] * 100 * 0.9) * PowerGrid.GRID_TICK;
                helper.assertTrue(converter.getEnergyStored() == amount, "Actual grid component dispatch generates exactly one interval");
                for (int tick = 0; tick < 20; tick++) dispatch.invoke(grid);
                helper.assertTrue(converter.getEnergyStored() == converter.getMaxEnergyStored(),
                    "Large capacities saturate without wrapping");
                if (tier == 4) {
                    AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency = 1600;
                    dispatch.invoke(grid);
                    helper.assertTrue(converter.getEnergyStored() == converter.getMaxEnergyStored(),
                        "Energy plus one interval can exceed signed-int range without wrapping");
                    AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency = 100;
                }
                var saved = converter.saveWithFullMetadata(helper.getLevel().registryAccess());
                var loaded = ModBlockEntities.POWER_CONVERTER.get().create(converter.getBlockPos(), converter.getBlockState());
                loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
                helper.assertTrue(loaded.getEnergyStored() == converter.getEnergyStored() && loaded.getInputPower() == POWERS[tier],
                    "Saving preserves energy and tier demand");
                helper.setBlock(POS, converter.getBlockState().setValue(BasePowerConverterBlock.POWERED, true));
                int before = converter.getEnergyStored();
                converter.gridTick();
                converter.tick();
                helper.assertTrue(converter.getInputPower() == 0 && converter.getEnergyStored() == before,
                    "Redstone disables generation and push");
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        } finally {
            AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency = efficiency;
            AnvilCraft.CONFIG.powerConverter.powerConverterLoss = loss;
        }
        helper.succeed();
    }

    private static void transactions(GameTestHelper helper) {
        var converter = place(helper, 4);
        converter.setGrid(new PowerGrid(helper.getLevel()));
        converter.gridTick();
        int before = converter.getEnergyStored();
        var face = converter.getBlockState().getValue(BasePowerConverterBlock.FACING);
        helper.assertTrue(converter.getEnergyHandler(face.getOpposite()) == null, "Only the output face exposes energy");
        var first = converter.getEnergyHandler(face);
        var second = converter.getEnergyHandler(null);
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertTrue(first.extract(100, tx) == 100 && second.extract(200, tx) == 200,
                "Separate capabilities share one energy journal");
        }
        helper.assertTrue(converter.getEnergyStored() == before, "Aborted multi-handler extraction restores the entire balance");
        try (Transaction tx = Transaction.openRoot()) {
            first.extract(100, tx);
            try (Transaction nested = Transaction.open(tx)) {
                second.extract(200, nested);
            }
            helper.assertTrue(converter.getEnergyStored() == before - 100, "Nested rollback preserves the parent extraction");
            tx.commit();
        }
        helper.assertTrue(converter.getEnergyStored() == before - 100, "Committed extraction settles once");
        helper.setBlock(POS, converter.getBlockState().setValue(BasePowerConverterBlock.FACING, Direction.EAST));
        helper.setBlock(POS.east(), ModBlocks.FE_COLLECTOR.getDefaultState()
            .setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X));
        var receiver = helper.getBlockEntity(POS.east(), FeCollectorBlockEntity.class).getEnergyHandler(Direction.WEST);
        int available = converter.getEnergyStored();
        converter.tick();
        helper.assertTrue(receiver.getAmountAsLong() == FeCollectorBlockEntity.MAX_ENERGY
            && converter.getEnergyStored() == available - FeCollectorBlockEntity.MAX_ENERGY, "Automatic output conserves accepted FE");
        available = converter.getEnergyStored();
        converter.tick();
        helper.assertTrue(converter.getEnergyStored() == available, "Full receivers do not consume converter energy");
        helper.succeed();
    }

    private static void recipes(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        String[] tiers = {"small", "middle", "big", "super_big", "extremely_big"};
        for (int index = 1; index < tiers.length; index++) {
            var upgrade = (ShapelessRecipe) manager.byKey(ResourceKey.create(Registries.RECIPE,
                AnvilCraft.of("power_converter_" + tiers[index] + "_from_" + tiers[index - 1]))).orElseThrow().value();
            helper.assertTrue(upgrade.placementInfo().ingredients().size() == 8 && upgrade.result.count() == 1,
                "Eight lower tiers upgrade once");
            var downgrade = (ShapelessRecipe) manager.byKey(ResourceKey.create(Registries.RECIPE,
                AnvilCraft.of("power_converter_" + tiers[index - 1] + "_from_" + tiers[index]))).orElseThrow().value();
            helper.assertTrue(downgrade.result.count() == 8, "Downgrade yields eight converters");
        }
        var cut = (StonecutterRecipe) manager.byKey(ResourceKey.create(Registries.RECIPE,
            AnvilCraft.of("stonecutting/power_converter_big_from_extremely_big"))).orElseThrow().value();
        helper.assertTrue(cut.result.count() == 64, "Two-tier stonecutting yields sixty-four converters");
        helper.succeed();
    }
}
