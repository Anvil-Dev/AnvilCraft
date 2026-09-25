package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MunTerrainTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(AnvilCraft.of("port_mun_terrain"),
            MunTerrainTests::terrain));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_mun_terrain"));
        event.registerTest(AnvilCraft.of("port_mun_terrain"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_mun_terrain")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 1200, 0, true)));
    }

    static ServerLevel level(MinecraftServer server) {
        var existing = server.getLevel(CelestialTravelManager.MUN_LEVEL);
        if (existing != null) return existing;
        var biome = server.registryAccess().lookupOrThrow(Registries.BIOME)
            .getOrThrow(ResourceKey.create(Registries.BIOME, AnvilCraft.of("mun")));
        var settings = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
            .getOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS, AnvilCraft.of("mun")));
        var type = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE)
            .getOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, AnvilCraft.of("mun")));
        var stem = new LevelStem(type,
            new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), settings), OptionalLong.of(121261));
        var accessor = (MinecraftServerAccessor) server;
        var created = new ServerLevel(server, net.minecraft.util.Util.backgroundExecutor(), accessor.anvilcraft$getStorageSource(),
            new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
            CelestialTravelManager.MUN_LEVEL, stem, false, BiomeManager.obfuscateSeed(121261), List.of(), false);
        accessor.anvilcraft$getLevels().put(CelestialTravelManager.MUN_LEVEL, created);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(created));
        return created;
    }

    private static void terrain(GameTestHelper helper) {
        var level = level(helper.getLevel().getServer());
        helper.assertTrue(GravityManager.getDimensionGravity(level) == 1.0 / 6.0
            && AtmosphereManager.getDimensionAirResistance(level) == 0.1, "Mun retains one-sixth gravity and minimum vacuum drag");
        List<String> expected;
        try (var input = Objects.requireNonNull(MunTerrainTests.class.getResourceAsStream("/mun-terrain-source.csv"))) {
            expected = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        List<String> actual = MunTerrainSamples.sample(level);
        helper.assertTrue(actual.size() == expected.size(), "Source/native sample counts agree");
        for (int i = 0; i < actual.size(); i++) {
            helper.assertTrue(actual.get(i).equals(expected.get(i)), "Mun sample " + i + ": " + actual.get(i) + " != " + expected.get(i));
        }
        AnvilCraft.LOGGER.info("PORT_MUN_TERRAIN_MATCHED: {} samples", actual.size());
        helper.succeed();
    }
}
