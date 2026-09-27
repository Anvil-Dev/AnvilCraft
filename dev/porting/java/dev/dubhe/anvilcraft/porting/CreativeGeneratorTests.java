package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.network.SliderUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CreativeGeneratorTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_creative_generator_bounds", CreativeGeneratorTests::bounds,
        "port_creative_generator_grid", CreativeGeneratorTests::grid
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_creative_generator"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true))));
    }

    private static CreativeGeneratorBlockEntity generator(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.CREATIVE_GENERATOR.get());
        return helper.getBlockEntity(pos, CreativeGeneratorBlockEntity.class);
    }

    private static void bounds(GameTestHelper helper) {
        var generator = generator(helper);
        helper.assertTrue(generator.getPower() == 8192, "New generators use the source default output");
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.containerMenu = new SliderMenu(1, generator::setPower);
        for (int requested : new int[]{Integer.MIN_VALUE, -65537, -65536, -8192, -1, 0, 1, 8192, 65536, 65537, Integer.MAX_VALUE}) {
            new SliderUpdatePacket(requested).handleOnServer(player);
            int expected = Math.clamp(requested, -65536, 65536);
            helper.assertTrue(generator.getPower() == expected && generator.getOutputPower() == Math.max(0, expected)
                && generator.getInputPower() == Math.max(0, -expected), "Packet callbacks clamp before signed-power conversion");
            helper.assertTrue(generator.getComponentType() == (expected > 0 ? PowerComponentType.PRODUCER : PowerComponentType.CONSUMER),
                "Signed power selects the source component role");
            var saved = generator.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(saved.getIntOr("power", Integer.MIN_VALUE) == expected, "Saved power is already bounded");
            var legacy = new CompoundTag();
            legacy.putInt("power", requested);
            generator.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
            helper.assertTrue(generator.getPower() == expected, "Out-of-range saved values are clamped on load");
        }
        helper.succeed();
    }

    private static void grid(GameTestHelper helper) {
        var generator = generator(helper);
        int[] changes = {0};
        var grid = new PowerGrid(helper.getLevel()) {
            @Override
            public void markChanged() {
                changes[0]++;
                super.markChanged();
            }
        };
        generator.setGrid(null);
        generator.setPower(1000000);
        generator.setGrid(grid);
        generator.tick();
        helper.assertTrue(changes[0] == 1 && generator.getOutputPower() == 65536, "Deferred changes reach the newly attached grid once");
        generator.tick();
        helper.assertTrue(changes[0] == 1, "Deferred notification is cleared");
        generator.setPower(-1000000);
        helper.assertTrue(changes[0] == 2 && generator.getInputPower() == 65536, "Attached grid observes bounded consumer demand");
        helper.succeed();
    }
}
