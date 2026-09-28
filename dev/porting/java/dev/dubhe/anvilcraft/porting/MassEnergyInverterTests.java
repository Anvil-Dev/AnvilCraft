package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.anvil.MassInjectBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.MassEnergyInverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.block.workstation.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStepManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MassEnergyInverterTests {
    private static final BlockPos POS = new BlockPos(8, 6, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_mass_energy_power", MassEnergyInverterTests::power,
        "port_mass_energy_anvil", MassEnergyInverterTests::anvil,
        "port_mass_energy_process", MassEnergyInverterTests::process
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_mass_energy"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    private static void power(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.MASS_ENERGY_INVERTER.get());
        var inverter = h.getBlockEntity(POS, MassEnergyInverterBlockEntity.class);
        for (var direction : Direction.values()) h.setBlock(POS.relative(direction), ModBlocks.SPACE_OVERCOMPRESSOR.get());
        h.assertTrue(inverter.getInputPower() == 1024, "Fixed 1024 kW demand");
        MassEnergyInverterBlockEntity.tick(h.getLevel(), h.absolutePos(POS), inverter.getBlockState(), inverter);
        h.assertTrue(h.getBlockEntity(POS.east(), SpaceOvercompressorBlockEntity.class).getStoredMass() == 0, "No grid gives no mass");
        boolean[] working = {false};
        inverter.setGrid(new PowerGrid(h.getLevel()) {
            @Override
            public boolean isWorking() {
                return working[0];
            }
        });
        MassEnergyInverterBlockEntity.tick(h.getLevel(), h.absolutePos(POS), inverter.getBlockState(), inverter);
        h.assertTrue(h.getBlockEntity(POS.east(), SpaceOvercompressorBlockEntity.class).getStoredMass() == 0,
            "Overloaded grid gives no mass");
        working[0] = true;
        for (int i = 0; i < 20; i++) {
            MassEnergyInverterBlockEntity.tick(h.getLevel(), h.absolutePos(POS), inverter.getBlockState(), inverter);
        }
        for (var direction : Direction.values()) {
            var compressor = h.getBlockEntity(POS.relative(direction), SpaceOvercompressorBlockEntity.class);
            h.assertTrue(compressor.getStoredMass() == (direction.getAxis().isHorizontal() ? 100 : 0),
                "Five mass per tick goes to each horizontal neighbor only");
        }
        inverter.setGrid(null);
        h.succeed();
    }

    private static void anvil(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        h.setBlock(POS, ModBlocks.SPACE_OVERCOMPRESSOR.get());
        var compressor = h.getBlockEntity(POS, SpaceOvercompressorBlockEntity.class);
        var input = new SingleRecipeInput(new ItemStack(Items.IRON_INGOT));
        long unit = level.getServer().getRecipeManager().getRecipeFor(ModRecipeTypes.MASS_INJECT.get(), input, level)
            .orElseThrow().value().getMass();
        for (int mode = 0; mode < 4; mode++) {
            for (var direction : Direction.values()) h.setBlock(POS.relative(direction), Blocks.AIR);
            if (mode == 1) h.setBlock(POS.above(), ModBlocks.MASS_ENERGY_INVERTER.get());
            if (mode >= 2) h.setBlock(POS.east(), ModBlocks.MASS_ENERGY_INVERTER.get());
            if (mode == 3) h.setBlock(POS.west(), ModBlocks.MASS_ENERGY_INVERTER.get());
            long before = compressor.getStoredMass();
            var point = pos.above().getCenter();
            var drop = new ItemEntity(level, point.x, point.y, point.z, new ItemStack(Items.IRON_INGOT, 2));
            level.addFreshEntity(drop);
            var falling = new FallingBlockEntity(level, point.x, point.y, point.z, Blocks.ANVIL.defaultBlockState());
            var event = new AnvilEvent.OnLand(level, pos.above(), falling, 1);
            h.assertTrue(new MassInjectBehavior().handle(level, pos, compressor.getBlockState(), 1, event),
                "Anvil behavior handles compressor");
            long expected = unit * 2 * (mode >= 2 ? 2 : 1);
            h.assertTrue(compressor.getStoredMass() - before == expected, "Horizontal presence doubles once, including unpowered blocks");
            h.assertTrue(drop.isRemoved(), "Inputs consumed exactly once");
        }
        h.succeed();
    }

    private static void process(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        for (boolean massFirst : new boolean[]{true, false}) {
            var id = AnvilCraft.of("procedural_process/mass_energy_inverter_" + (massFirst ? "mass_first" : "energy_first"));
            var recipe = (ProceduralProcessRecipe) level.getServer().getRecipeManager().recipeMap()
                .byKey(ResourceKey.create(Registries.RECIPE, id)).value();
            h.assertTrue(recipe.steps().size() == 3 && recipe.loop() == 3 && recipe.multiLoopFirstStep().isPresent(),
                "Three steps and three loops");
            h.setBlock(POS, ModBlocks.LASER_RECEIVER.get());
            var point = pos.above().getCenter();
            var falling = new FallingBlockEntity(level, point.x, point.y, point.z, Blocks.ANVIL.defaultBlockState());
            var event = new AnvilEvent.OnLand(level, pos.above(), falling, 1);
            for (int step = 0; step < 9; step++) {
                int phase = step % 3;
                if (phase < 2) {
                    var type = (phase == 0) == massFirst ? IrradiatorType.MASS : IrradiatorType.ENERGY;
                    h.setBlock(POS.below(), ModBlocks.NEUTRON_IRRADIATOR.getDefaultState().setValue(NeutronIrradiatorBlock.TYPE, type));
                } else {
                    level.addFreshEntity(new ItemEntity(level, point.x, point.y - 0.25, point.z, ModItems.TRANSCENDIUM_NUGGET.asStack()));
                }
                h.assertTrue(ProceduralProcessStepManager.checkAnyMatches(event),
                    "Real procedural step " + step + " massFirst=" + massFirst);
                if (step < 8) {
                    var wip = h.getBlockEntity(POS, WipBlockEntity.class);
                    h.assertTrue(wip.getStepCount() == step + 1 && id.equals(wip.getRecipeId()), "Persistent recipe progress");
                    int loop = step / 3;
                    var model = AnvilCraft.of("block/wip_display/mass_energy_inverter_wip" + (loop == 0 ? "" : "_" + (loop + 1)));
                    h.assertTrue(recipe.getDisplayedModelForStep(step + 1).orElseThrow().equals(model), "Per-loop WIP model selection");
                    if (step == 3) {
                        var tag = wip.saveWithFullMetadata(level.registryAccess());
                        wip.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                        h.assertTrue(wip.getStepCount() == 4, "Mid-process progress survives save/reload");
                    }
                }
            }
            h.assertTrue(h.getBlockState(POS).is(ModBlocks.MASS_ENERGY_INVERTER), "Nine real steps produce the inverter");
        }
        h.succeed();
    }
}
