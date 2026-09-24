package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MunWorldLightTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(AnvilCraft.of("port_mun_world_light"), MunWorldLightTests::lighting);
            registry.register(AnvilCraft.of("port_mun_detector_boundary"), MunWorldLightTests::detectorBoundary);
        });
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_mun_world_light"));
        event.registerTest(AnvilCraft.of("port_mun_world_light"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_mun_world_light")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 1200, 0, true)));
        event.registerTest(AnvilCraft.of("port_mun_detector_boundary"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_mun_detector_boundary")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 1200, 0, true)));
    }

    private static void lighting(GameTestHelper helper) {
        var level = MunTerrainTests.level(helper.getLevel().getServer());
        var open = new BlockPos(8, 300, 0);
        var far = new BlockPos(4096, 300, 0);
        var covered = new BlockPos(-24, 299, 0);
        var lit = new BlockPos(0, 299, 0);
        var detector = new BlockPos(12, 300, 0);
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        Set<ChunkPos> forced = new LinkedHashSet<>();
        for (var pos : List.of(open, far, covered, lit, detector)) {
            var chunk = ChunkPos.containing(pos);
            for (int x = chunk.x() - 1; x <= chunk.x() + 1; x++) {
                for (int z = chunk.z() - 1; z <= chunk.z() + 1; z++) {
                    if (level.setChunkForced(x, z, true)) forced.add(new ChunkPos(x, z));
                }
            }
            level.getChunkAt(pos);
        }
        put(level, previous, covered.above(), Blocks.STONE.defaultBlockState());
        put(level, previous, lit.above(), Blocks.STONE.defaultBlockState());
        put(level, previous, lit.west(4), Blocks.GLOWSTONE.defaultBlockState());
        put(level, previous, detector, Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
        helper.runAtTickTime(40, () -> {
            var clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
            long originalTime = level.clockManager().getTotalTicks(clock);
            try {
                helper.assertTrue(level.getBrightness(LightLayer.SKY, open) == 15
                    && level.getBrightness(LightLayer.SKY, far) == 15, "Open near/far-side samples have full sky light");
                helper.assertTrue(level.getBrightness(LightLayer.SKY, covered) < 15
                    && level.getBrightness(LightLayer.BLOCK, covered) == 0, "Covered sample is shaded and unlit");
                helper.assertTrue(level.getBrightness(LightLayer.SKY, lit) < 15
                    && level.getBrightness(LightLayer.BLOCK, lit) > 0, "Lamp sample combines indirect sky and block light");
                for (long time = 0; time < 192000; time++) {
                    float angle = sourceTurns(time) * (float) (Math.PI * 2);
                    angle += ((angle < (float) Math.PI ? 0 : (float) (Math.PI * 2)) - angle) * 0.2F;
                    int index = (int) (angle * 10430.378F + 16384.0F) & 65535;
                    float expected = (float) Math.sin(index * Math.PI * 2 / 65536.0);
                    helper.assertTrue(MunSkyMath.daylightDetectorCosine(time) == expected,
                        "Detector cosine differs from source at " + time);
                }
                int checks = 0;
                for (long time = -192000; time <= 384000; time += 137) {
                    level.clockManager().setTotalTicks(clock, time);
                    level.environmentAttributes().invalidateTickCache();
                    int darkness = MunSkyMath.skyDarken(0, 0, time);
                    helper.assertTrue(level.getSkyDarken() == darkness, "Dimension darkness follows the source origin at " + time);
                    boolean day = MunSkyMath.sunlight(0, 0, time, 0) > 0;
                    helper.assertTrue(level.isBrightOutside() == day && level.isDarkOutside() == !day,
                        "Day/night follows the source solar disc at " + time);
                    for (BlockPos pos : List.of(open, far, covered, lit)) {
                        int sky = level.getBrightness(LightLayer.SKY, pos);
                        int block = level.getBrightness(LightLayer.BLOCK, pos);
                        int expected = Math.max(block, sky == 15 ? 15 - MunSkyMath.skyDarken(pos.getX(), pos.getZ(), time) : 0);
                        helper.assertTrue(level.getMaxLocalRawBrightness(pos) == expected, "Local light mismatch at " + time + ": " + pos);
                    }
                    float turns = sourceTurns(time);
                    float degrees = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, detector);
                    helper.assertTrue(Math.abs(degrees - turns * 360) < 0.0001, "Sun angle did not use the eight-day cycle");
                    for (boolean inverted : new boolean[]{false, true}) {
                        BlockState state = Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(DaylightDetectorBlock.INVERTED, inverted);
                        level.setBlock(detector, state, 2);
                        updateDetector(level, detector);
                        int sky = level.getBrightness(LightLayer.SKY, detector) - darkness;
                        int expected = sky;
                        if (inverted) expected = 15 - expected;
                        else if (expected > 0) {
                            float angle = turns * ((float) Math.PI * 2);
                            float offset = angle < Math.PI ? 0 : (float) Math.PI * 2;
                            angle += (offset - angle) * 0.2F;
                            int index = (int) (angle * 10430.378F + 16384.0F) & 65535;
                            float cosine = (float) Math.sin(index * Math.PI * 2 / 65536.0);
                            expected = Math.round(expected * cosine);
                        }
                        helper.assertTrue(level.getBlockState(detector).getValue(DaylightDetectorBlock.POWER)
                            == Mth.clamp(expected, 0, 15), "Daylight detector diverged from source at " + time);
                    }
                    checks++;
                }
                level.clockManager().setTotalTicks(clock, 0);
                helper.assertTrue(level.getMaxLocalRawBrightness(open) > level.getMaxLocalRawBrightness(far),
                    "Near and far sides must not share uniform daylight");
                var overworld = helper.getLevel().getServer().overworld();
                var control = helper.absolutePos(new BlockPos(0, 2, 0));
                helper.assertTrue(overworld.getMaxLocalRawBrightness(control)
                    == overworld.getMaxLocalRawBrightness(control, overworld.getSkyDarken()), "Non-Mun brightness changed");
                AnvilCraft.LOGGER.info("PORT_MUN_WORLD_LIGHT_PASSED: {} times, local light, daylight detectors and non-Mun guard", checks);
            } finally {
                level.clockManager().setTotalTicks(clock, originalTime);
                level.environmentAttributes().invalidateTickCache();
                previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
                forced.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), false));
            }
            helper.succeed();
        });
    }

    private static void detectorBoundary(GameTestHelper helper) {
        var level = MunTerrainTests.level(helper.getLevel().getServer());
        var pos = new BlockPos(128, 300, 0);
        var center = ChunkPos.containing(pos);
        Set<ChunkPos> forced = new LinkedHashSet<>();
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        for (int x = center.x() - 1; x <= center.x() + 1; x++) {
            for (int z = center.z() - 1; z <= center.z() + 1; z++) {
                if (level.setChunkForced(x, z, true)) forced.add(new ChunkPos(x, z));
            }
        }
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) put(level, previous, pos.offset(x, 1, z), Blocks.STONE.defaultBlockState());
        }
        put(level, previous, pos, Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
        helper.runAtTickTime(40, () -> {
            var clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
            long originalTime = level.clockManager().getTotalTicks(clock);
            try {
                level.clockManager().setTotalTicks(clock, 165260);
                level.environmentAttributes().invalidateTickCache();
                helper.assertTrue(level.getEffectiveSkyBrightness(pos) == 9, "Boundary fixture must supply nine sky-light levels");
                updateDetector(level, pos);
                helper.assertTrue(level.getBlockState(pos).getValue(DaylightDetectorBlock.POWER) == 4,
                    "Source float lookup yields four, while the native double path yields three at this boundary");
                AnvilCraft.LOGGER.info("PORT_MUN_DETECTOR_BOUNDARY_PASSED: time=165260, sky=9, power=4");
            } finally {
                level.clockManager().setTotalTicks(clock, originalTime);
                level.environmentAttributes().invalidateTickCache();
                previous.forEach((block, state) -> level.setBlock(block, state, 2));
                forced.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), false));
            }
            helper.succeed();
        });
    }

    private static float sourceTurns(long time) {
        double turns = MunSkyMath.solarAngle(time, 0) / (Math.PI * 2);
        return (float) (turns - Math.floor(turns));
    }

    private static void put(Level level, Map<BlockPos, BlockState> previous, BlockPos pos, BlockState state) {
        previous.put(pos, level.getBlockState(pos));
        level.setBlock(pos, state, 2);
    }

    private static void updateDetector(Level level, BlockPos pos) {
        try {
            var method = DaylightDetectorBlock.class.getDeclaredMethod("updateSignalStrength",
                BlockState.class, Level.class, BlockPos.class);
            method.setAccessible(true);
            method.invoke(null, level.getBlockState(pos), level, pos);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
