package dev.dubhe.anvilcraft.fluid;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CementFluidTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_cement_colors", CementFluidTests::colors,
        "port_cement_transfer", CementFluidTests::transfer,
        "port_cement_blockers", CementFluidTests::blockers,
        "port_cement_dispatch", CementFluidTests::dispatch,
        "port_cement_search", CementFluidTests::search
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_cement"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static CementFluid source(Color color) {
        return (CementFluid) ModFluids.SOURCE_CEMENTS.get(color).get();
    }

    private static void source(ServerLevel level, BlockPos pos, Color color) {
        level.setBlock(pos, source(color).defaultFluidState().createLegacyBlock(), 2);
    }

    private static CementFluid flowing(Color color) {
        return (CementFluid) ModFluids.FLOWING_CEMENTS.get(color).get();
    }

    private static void flow(ServerLevel level, BlockPos pos, Color color, int amount) {
        level.setBlock(pos, flowing(color).getFlowing(amount, false).createLegacyBlock(), 2);
    }

    private static void tick(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        state.getFluidState().tick(level, pos, state);
    }

    private static void colors(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(4, 4, 4));
        for (Color color : Color.values()) {
            source(level, center, color);
            level.setBlock(center.below(), Blocks.STONE.defaultBlockState(), 2);
            flow(level, center.east(), color, 7);
            flow(level, center.east(2), color, 6);
            flow(level, center.east(3), color, 5);
            Color other = color == Color.RED ? Color.BLUE : Color.RED;
            flow(level, center.north(), other, 7);
            source(level, center.west(), color);
            source(color).randomTick(level, center, level.getFluidState(center), new FixedRandom(0));
            var concrete = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + "_concrete"));
            helper.assertTrue(level.getBlockState(center).is(concrete) && level.getBlockState(center.east()).is(concrete)
                && level.getBlockState(center.east(2)).is(concrete), "Matching source and two rings did not solidify: " + color);
            helper.assertTrue(level.getFluidState(center.east(3)).getAmount() == 5
                && level.getFluidState(center.north()).getType() == flowing(other)
                && level.getFluidState(center.west()).isSource(), "Solidification consumed unrelated flow/source");
            helper.assertTrue(source(color).defaultFluidState().isRandomlyTicking()
                && !flowing(color).defaultFluidState().isRandomlyTicking(), "Random ticking was enabled on flowing cement");
        }
        helper.succeed();
    }

    private static void transfer(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 6, 4));
        source(level, pos, Color.RED);
        tick(level, pos);
        helper.assertTrue(level.getBlockState(pos).isAir() && level.getFluidState(pos.below()).isSource(), "Source did not descend intact");
        tick(level, pos.below());
        helper.assertTrue(level.getBlockState(pos.below()).isAir() && level.getFluidState(pos.below(2)).isSource(),
            "Source duplicated or stopped on its next scheduled step");
        level.setBlock(pos.below(2), Blocks.AIR.defaultBlockState(), 2);
        source(level, pos.west(), Color.RED);
        level.setBlock(pos.west().below(), Blocks.STONE.defaultBlockState(), 2);
        flow(level, pos, Color.RED, 7);
        tick(level, pos);
        helper.assertTrue(level.getBlockState(pos.west()).isAir() && level.getFluidState(pos.below()).isSource(),
            "Flowing edge did not relocate the adjacent source");
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
        source(level, pos.above(), Color.BLUE);
        flow(level, pos, Color.BLUE, 7);
        tick(level, pos);
        helper.assertTrue(level.getBlockState(pos.above()).isAir() && level.getFluidState(pos.below()).isSource(),
            "Vertical flow failed to find its source above");
        helper.succeed();
    }

    private static void blockers(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 5, 4));
        source(level, pos, Color.YELLOW);
        level.setBlock(pos.east(), Blocks.HONEY_BLOCK.defaultBlockState(), 2);
        tick(level, pos);
        source(Color.YELLOW).randomTick(level, pos, level.getFluidState(pos), new FixedRandom(0));
        helper.assertTrue(level.getFluidState(pos).isSource(), "Honey did not prevent movement and solidification");
        level.setBlock(pos.east(), Blocks.SLIME_BLOCK.defaultBlockState(), 2);
        source(level, pos, Color.BLUE);
        tick(level, pos);
        helper.assertTrue(level.getFluidState(pos).isSource(), "Slime did not retain the source");
        source(Color.BLUE).randomTick(level, pos, level.getFluidState(pos), new FixedRandom(0));
        helper.assertTrue(level.getBlockState(pos).is(Blocks.BLUE_CONCRETE), "Slime wrongly prevented solidification");
        level.setBlock(pos.east(), ModBlocks.SUGAR_BLOCK.getDefaultState(), 2);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        source(level, pos, Color.GREEN);
        source(Color.GREEN).randomTick(level, pos, level.getFluidState(pos), new FixedRandom(0));
        helper.assertTrue(level.getFluidState(pos).isSource(), "Sugar did not prevent solidification");
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
        tick(level, pos);
        helper.assertTrue(level.getBlockState(pos).isAir() && level.getFluidState(pos.below()).isSource(), "Sugar blocked descent");
        helper.succeed();
    }

    private static void dispatch(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 5, 4));
        source(level, pos, Color.BLACK);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        var snapshot = level.getFluidState(pos);
        var random = new FixedRandom(1);
        source(Color.BLACK).randomTick(level, pos, snapshot, random);
        source(Color.BLACK).randomTick(level, pos, snapshot, random);
        helper.assertTrue(random.calls == 1 && level.getFluidState(pos).isSource(), "Duplicate dispatch rolled twice");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        source(Color.BLACK).randomTick(level, pos, snapshot, random);
        helper.assertTrue(random.calls == 1 && level.getBlockState(pos).isAir(), "Stale snapshot recreated cement");
        helper.succeed();
    }

    private static void search(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 50, 4));
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        try {
            for (int x = -2; x <= 70; x++) {
                previous.put(pos.east(x), level.getBlockState(pos.east(x)));
            }
            previous.put(pos.below(), level.getBlockState(pos.below()));
            flow(level, pos, Color.RED, 7);
            source(level, pos.below(), Color.RED);
            helper.assertTrue(findSource(level, pos) == null, "Source search descended below the flow");
            source(level, pos.east(), Color.RED);
            flow(level, pos.west(), Color.RED, 7);
            source(level, pos.west(2), Color.RED);
            helper.assertTrue(pos.east().equals(findSource(level, pos)), "Source search did not choose the nearest source");
            for (int x = -2; x <= 0; x++) {
                level.setBlock(pos.east(x), Blocks.AIR.defaultBlockState(), 2);
            }
            for (int x = 0; x < 70; x++) {
                flow(level, pos.east(x), Color.RED, 7);
            }
            source(level, pos.east(70), Color.RED);
            helper.assertTrue(findSource(level, pos) == null, "Source search exceeded its bounded frontier");
        } finally {
            previous.forEach((block, state) -> level.setBlock(block, state, 2));
        }
        helper.succeed();
    }

    @Nullable
    private static BlockPos findSource(ServerLevel level, BlockPos pos) {
        try {
            var method = CementFluid.class.getDeclaredMethod("findNearestSource", Level.class, BlockPos.class);
            method.setAccessible(true);
            return (BlockPos) method.invoke(flowing(Color.RED), level, pos);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FixedRandom extends LegacyRandomSource {
        private final float value;
        private int calls;

        private FixedRandom(float value) {
            super(0);
            this.value = value;
        }

        @Override
        public float nextFloat() {
            this.calls++;
            return this.value;
        }
    }
}
