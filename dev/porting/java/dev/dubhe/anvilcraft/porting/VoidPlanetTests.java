package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialBackGateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSeedMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class VoidPlanetTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_void_dimension", VoidPlanetTests::dimension,
        "port_void_physics", VoidPlanetTests::physics,
        "port_void_recipe_travel", VoidPlanetTests::travel
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_void"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static ServerLevel level(MinecraftServer server) {
        var existing = server.getLevel(CelestialTravelManager.VOID_PLANET_LEVEL);
        if (existing != null) return existing;
        try (var input = Objects.requireNonNull(VoidPlanetTests.class.getResourceAsStream(
            "/data/anvilcraft/dimension/void_planet.json"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            var stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()),
                JsonParser.parseReader(reader)).getOrThrow();
            var accessor = (MinecraftServerAccessor) server;
            var created = new ServerLevel(server, net.minecraft.util.Util.backgroundExecutor(),
                accessor.anvilcraft$getStorageSource(),
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
                CelestialTravelManager.VOID_PLANET_LEVEL, stem, false, 42, List.of(), false);
            accessor.anvilcraft$getLevels().put(CelestialTravelManager.VOID_PLANET_LEVEL, created);
            server.markWorldsDirty();
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(created));
            return created;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void dimension(GameTestHelper helper) {
        var level = level(helper.getLevel().getServer());
        var type = level.dimensionType();
        helper.assertTrue(type.minY() == -64 && type.height() == 384 && type.logicalHeight() == 384
            && type.hasFixedTime() && type.hasSkyLight() && !type.hasCeiling()
            && !type.hasEnderDragonFight() && type.skybox() == DimensionType.Skybox.END
            && type.ambientLight() == 0 && type.coordinateScale() == 1, "Source dimension bounds and rules survive native codec");
        var attributes = level.environmentAttributes();
        helper.assertTrue(!attributes.getValue(EnvironmentAttributes.CAN_START_RAID, BlockPos.ZERO)
            && !attributes.getValue(EnvironmentAttributes.MONSTERS_BURN, BlockPos.ZERO)
            && !attributes.getValue(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, BlockPos.ZERO)
            && !attributes.getValue(EnvironmentAttributes.WATER_EVAPORATES, BlockPos.ZERO)
            && !attributes.getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, BlockPos.ZERO)
            && attributes.getValue(EnvironmentAttributes.BED_RULE, BlockPos.ZERO).explodes(),
            "Source gameplay dimension flags use native environment attributes");
        var chunk = level.getChunk(64, 64);
        for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
            helper.assertTrue(chunk.getBlockState(new BlockPos(1024, y, 1024)).isAir(), "Void remains empty away from start platform");
        }
        helper.assertTrue(level.getBiome(new BlockPos(1024, 64, 1024)).is(net.minecraft.world.level.biome.Biomes.THE_VOID),
            "Void biome remains native");
        helper.succeed();
    }

    private static void physics(GameTestHelper helper) {
        var level = level(helper.getLevel().getServer());
        level.getChunk(32, 32);
        helper.assertTrue(GravityManager.getDimensionGravity(level) == 0
            && AtmosphereManager.getDimensionAirResistance(level) == 0, "Builtin void has no gravity or drag overrides missing");
        var item = new ItemEntity(level, 520, 200, 520, new ItemStack(Items.STONE));
        var pig = EntityType.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        try {
            item.setDeltaMovement(0.2, 0.1, 0.3);
            for (int i = 0; i < 10; i++) item.tick();
            helper.assertTrue(!item.isNoGravity() && item.getDeltaMovement().distanceTo(new Vec3(0.2, 0.1, 0.3)) < 0.000001,
                "Real item ticks retain three-dimensional velocity in the builtin void");
            pig.setPos(525, 200, 525);
            pig.setAirSupply(100);
            pig.baseTick();
            helper.assertTrue(pig.getGravity() == 0 && AtmosphereManager.isSuffocating(pig) && pig.getAirSupply() < 100,
                "Living entity has zero gravity but still consumes breath in vacuum");
        } finally {
            item.discard();
            pig.discard();
        }
        helper.succeed();
    }

    private static void travel(GameTestHelper helper) {
        var source = helper.getLevel();
        final var destination = level(source.getServer());
        var match = CelestialSeedMatcher.match(source, 0, 0, 0, 0, Items.BARRIER);
        helper.assertTrue(match != null && match.body().usesEndGatewayModel() && !match.body().canBeShattered(),
            "Barrier and zero counts discover the builtin gateway planet");
        var travel = match.body().landing();
        helper.assertTrue(travel.dimension().equals(CelestialTravelManager.VOID_PLANET_DIMENSION)
            && travel.coordinateRule().type() == CelestialTravelData.CoordinateRule.Type.SAME_3D
            && travel.returnRule().type() == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL, "Recipe selects source travel rules");
        var entity = helper.spawn(EntityType.PIG, new BlockPos(3, 22, 3));
        entity.setNoGravity(true);
        var origin = entity.blockPosition();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                source.setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        var uuid = entity.getUUID();
        helper.assertTrue(CelestialTravelManager.tryLand(entity, source, origin, Direction.NORTH, travel), "Builtin landing succeeds");
        var moved = destination.getEntity(uuid);
        helper.assertTrue(moved != null && moved.blockPosition().equals(origin.north())
            && destination.getBlockState(origin.below()).is(Blocks.OBSIDIAN), "Same-3D landing creates source-height emergency platform");
        var gate = (CelestialBackGateBlockEntity) destination.getBlockEntity(origin.above());
        helper.assertTrue(gate != null && gate.getReturnPortalPos().equals(origin), "Return gate retains exact entry connection");
        moved.setPortalCooldown(0);
        helper.assertTrue(CelestialTravelManager.tryReturn(moved, gate), "Builtin return succeeds");
        var returned = source.getEntity(uuid);
        helper.assertTrue(returned != null && returned.blockPosition().equals(origin.north()), "Return uses the source portal");
        returned.discard();
        helper.succeed();
    }
}
