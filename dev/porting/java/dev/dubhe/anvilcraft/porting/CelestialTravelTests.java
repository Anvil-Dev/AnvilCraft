package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CelestialBackGateBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialBackGateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionGate331PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CelestialTravelTests {
    private static final ResourceKey<Level> VOID = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("port_travel_void"));
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_travel_round_trip", CelestialTravelTests::roundTrip,
        "port_travel_rules", CelestialTravelTests::rules,
        "port_travel_gate_state", CelestialTravelTests::gateState,
        "port_travel_platform", CelestialTravelTests::platform,
        "port_travel_cfa_gate", CelestialTravelTests::cfaGate,
        "port_travel_independent_return", CelestialTravelTests::independentReturn
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_travel"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static ServerLevel destination(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(VOID);
        if (level == null) level = createTestDimension(helper, VOID);
        return level;
    }

    @SuppressWarnings("unchecked")
    private static ServerLevel createTestDimension(GameTestHelper helper, ResourceKey<Level> dimension) {
        var server = helper.getLevel().getServer();
        try (var input = java.util.Objects.requireNonNull(CelestialTravelTests.class.getResourceAsStream(
            "/data/anvilcraft/dimension/port_travel_void.json"));
             var reader = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
            final var stem = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(
                net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess()),
                com.google.gson.JsonParser.parseReader(reader)).getOrThrow();
            var executor = net.minecraft.server.MinecraftServer.class.getDeclaredField("executor");
            var storage = net.minecraft.server.MinecraftServer.class.getDeclaredField("storageSource");
            var levels = net.minecraft.server.MinecraftServer.class.getDeclaredField("levels");
            executor.setAccessible(true);
            storage.setAccessible(true);
            levels.setAccessible(true);
            var data = new net.minecraft.world.level.storage.DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
            var created = new ServerLevel(server, (java.util.concurrent.Executor) executor.get(server),
                (net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess) storage.get(server),
                data, dimension, stem, false, 42, java.util.List.of(), false);
            ((Map<ResourceKey<Level>, ServerLevel>) levels.get(server)).put(dimension, created);
            server.markWorldsDirty();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Load(created));
            return created;
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static CelestialTravelData travel(ResourceKey<Level> dimension, BlockPos feet) {
        return new CelestialTravelData(dimension.identifier(),
            new CelestialTravelData.CoordinateRule(CelestialTravelData.CoordinateRule.Type.FIXED, 1,
                feet.getX(), feet.getY(), feet.getZ(), 8), CelestialTravelData.ReturnRule.DEFAULT);
    }

    private static void floor(ServerLevel level, BlockPos feet) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(feet.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 0; y < 4; y++) level.setBlock(feet.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void roundTrip(GameTestHelper helper) {
        var source = helper.getLevel();
        final var target = destination(helper);
        var origin = helper.absolutePos(new BlockPos(3, 22, 3));
        floor(source, origin);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 22, 3));
        pig.setNoGravity(true);
        pig.setCustomName(Component.literal("Travel identity"));
        pig.setYRot(30);
        Entity sameLevel = CelestialTravelManager.teleportEntity(pig, source, source,
            Vec3.atBottomCenterOf(origin.south()), new Vec3(0.2, 0.3, 0.4));
        helper.assertTrue(sameLevel == pig && pig.position().equals(Vec3.atBottomCenterOf(origin.south()))
            && Math.abs(net.minecraft.util.Mth.wrapDegrees(pig.getYRot() - 210)) < 0.001F,
            "Same-dimension transfer retains the entity and reverses facing");
        pig.setYRot(30);
        pig.setDeltaMovement(0.2, 0.3, -0.4);
        var uuid = pig.getUUID();
        var feet = new BlockPos(160, 100, 160);
        var data = travel(VOID, feet);
        helper.assertTrue(CelestialTravelManager.tryLand(pig, source, origin, Direction.NORTH, data), "Cross-dimension landing succeeds");
        var moved = target.getEntity(uuid);
        helper.assertTrue(moved != null && moved != pig && moved.getCustomName().getString().equals("Travel identity"),
            "Entity identity and data survive the native transition");
        helper.assertTrue(moved.position().equals(Vec3.atBottomCenterOf(feet.north()))
            && moved.getDeltaMovement().distanceTo(new Vec3(0.2, 0.3, 0.4)) < 0.00001, "Gate exit and momentum match source");
        helper.assertTrue(target.getBlockState(feet.below()).is(Blocks.OBSIDIAN), "Void landing creates an emergency platform");
        var gate = (CelestialBackGateBlockEntity) target.getBlockEntity(feet.above());
        helper.assertTrue(gate != null && gate.getReturnPortalPos().equals(origin) && moved.isOnPortalCooldown(),
            "Generated gate stores the exact entry and starts cooldown");
        helper.assertTrue(!CelestialTravelManager.tryReturn(moved, gate), "Cooldown prevents immediate return");
        moved.setPortalCooldown(0);
        helper.assertTrue(CelestialTravelManager.tryReturn(moved, gate), "Return traverses back to the source dimension");
        Entity returned = source.getEntity(uuid);
        helper.assertTrue(returned != null && returned.position().equals(Vec3.atBottomCenterOf(origin.north()))
            && returned.getDeltaMovement().distanceTo(new Vec3(0.2, 0.3, -0.4)) < 0.00001, "Round trip preserves outward movement");
        returned.setPortalCooldown(0);
        helper.assertTrue(CelestialTravelManager.tryLand(returned, source, origin, Direction.NORTH, data),
            "Existing return gate is reused");
        helper.assertTrue(target.getBlockEntity(feet.above()) == gate, "Reuse keeps the original gate instance");
        target.getEntity(uuid).discard();
        helper.succeed();
    }

    private static void independentReturn(GameTestHelper helper) {
        var dimension = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("port_travel_independent"));
        var target = createTestDimension(helper, dimension);
        int index = 0;
        for (var type : new CelestialTravelData.ReturnRule.Type[]{
            CelestialTravelData.ReturnRule.Type.FIXED_PORTAL, CelestialTravelData.ReturnRule.Type.RANDOM_PORTAL
        }) {
            BlockPos landing = new BlockPos(-160 - index * 64, 100, 160);
            BlockPos returned = type == CelestialTravelData.ReturnRule.Type.FIXED_PORTAL ? new BlockPos(-128, 120, 160)
                : target.getWorldBorderAdjustedRespawnData(target.getRespawnData()).pos().atY(90);
            floor(target, landing);
            floor(target, returned);
            var coordinates = travel(VOID, landing).coordinateRule();
            var rule = new CelestialTravelData.ReturnRule(type, returned.getX(), returned.getY(), returned.getZ(), 0);
            var data = new CelestialTravelData(dimension.identifier(), coordinates, rule);
            var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
            var uuid = pig.getUUID();
            helper.assertTrue(CelestialTravelManager.tryLand(pig, helper.getLevel(), pig.blockPosition(), Direction.SOUTH, data),
                "Independent return rule lands successfully: " + type);
            var moved = target.getEntity(uuid);
            helper.assertTrue(moved != null && moved.position().equals(Vec3.atBottomCenterOf(landing)),
                "Independent gate placement preserves requested entity landing: " + type);
            helper.assertTrue(target.getBlockEntity(returned.above()) instanceof CelestialBackGateBlockEntity,
                "Return gate is placed independently: " + type);
            moved.discard();
            index++;
        }
        helper.succeed();
    }

    private static void rules(GameTestHelper helper) {
        var level = destination(helper);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
        pig.setPos(-12.6, 150.4, 37.9);
        for (var type : CelestialTravelData.CoordinateRule.Type.values()) {
            var rule = new CelestialTravelData.CoordinateRule(type, 0.125, 512, 90, 512, 0);
            if (type == CelestialTravelData.CoordinateRule.Type.FIXED_SURFACE) floor(level, new BlockPos(512, 90, 512));
            var result = (BlockPos) invoke("landingOrigin", new Class<?>[]{Entity.class, ServerLevel.class,
                CelestialTravelData.CoordinateRule.class}, pig, level, rule);
            switch (type) {
                case SAME -> helper.assertTrue(result.getX() == -13 && result.getZ() == 37, "Same coordinates use floor for negatives");
                case SAME_3D -> helper.assertTrue(result.equals(new BlockPos(-13, 150, 37)), "Same 3D keeps height");
                case SCALED -> helper.assertTrue(result.getX() == -2 && result.getZ() == 4, "Scaled coordinates preserve floor semantics");
                case FIXED, FIXED_SURFACE -> helper.assertTrue(result.equals(new BlockPos(512, 90, 512)),
                    "Fixed surface uses generated terrain");
                case RANDOM_SPAWN -> helper.assertTrue(result.equals(level.getWorldBorderAdjustedRespawnData(level.getRespawnData()).pos()),
                    "Zero random radius uses spawn");
                default -> throw new IllegalStateException("Unhandled coordinate rule " + type);
            }
        }
        var rule = new CelestialTravelData.CoordinateRule(CelestialTravelData.CoordinateRule.Type.RANDOM_SPAWN, 1, 0, 64, 0, 2);
        var first = invoke("landingOrigin", new Class<?>[]{Entity.class, ServerLevel.class, CelestialTravelData.CoordinateRule.class},
            pig, level, rule);
        helper.assertTrue(first.equals(invoke("landingOrigin", new Class<?>[]{Entity.class, ServerLevel.class,
            CelestialTravelData.CoordinateRule.class}, pig, level, rule)), "Random landing is stable within one tick");
        var fixed = new CelestialTravelData.ReturnRule(CelestialTravelData.ReturnRule.Type.FIXED_PORTAL, 4, 90, -8, 0);
        helper.assertTrue(invoke("returnOrigin", new Class<?>[]{ServerLevel.class, Entity.class, CelestialTravelData.ReturnRule.class,
            BlockPos.class}, level, pig, fixed, BlockPos.ZERO).equals(new BlockPos(4, 90, -8)), "Independent fixed return origin");
        pig.discard();
        helper.succeed();
    }

    private static void gateState(GameTestHelper helper) {
        var level = helper.getLevel();
        var portal = helper.absolutePos(new BlockPos(3, 42, 3));
        level.setBlock(portal, ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.getDefaultState(), Block.UPDATE_CLIENTS);
        var pos = portal.offset(4, 1, 0);
        level.setBlock(pos, ModBlocks.CELESTIAL_BACK_GATE.getDefaultState().setValue(CelestialBackGateBlock.WATERLOGGED, true),
            Block.UPDATE_CLIENTS);
        var gate = (CelestialBackGateBlockEntity) level.getBlockEntity(pos);
        gate.configure(level.dimension(), portal, Direction.NORTH);
        helper.assertTrue(level.getBlockState(portal).getValue(CelestialForgingAnvilPortalBlock.WATERLOGGED),
            "First sync preserves either water source");
        level.setBlock(pos, level.getBlockState(pos).setValue(CelestialBackGateBlock.WATERLOGGED, false), Block.UPDATE_CLIENTS);
        gate.tick();
        helper.assertTrue(!level.getBlockState(portal).getValue(CelestialForgingAnvilPortalBlock.WATERLOGGED),
            "Gate water removal propagates");
        gate.configure(level.dimension(), portal, Direction.EAST);
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        gate.saveCustomOnly(output);
        var saved = output.buildResult();
        var restored = dev.dubhe.anvilcraft.init.block.ModBlockEntities.CELESTIAL_BACK_GATE.create(pos, gate.getBlockState());
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        helper.assertTrue(restored.getReturnDimension().equals(level.dimension()) && restored.getReturnPortalPos().equals(portal)
            && restored.getReturnFacing() == Direction.EAST, "Return state survives reload into a new block entity");
        gate.configure(level.dimension(), portal, Direction.NORTH);
        var duplicate = pos.east();
        level.setBlock(duplicate, ModBlocks.CELESTIAL_BACK_GATE.getDefaultState(), Block.UPDATE_CLIENTS);
        ((CelestialBackGateBlockEntity) level.getBlockEntity(duplicate)).configure(level.dimension(), portal, Direction.NORTH);
        var other = pos.west();
        level.setBlock(other, ModBlocks.CELESTIAL_BACK_GATE.getDefaultState(), Block.UPDATE_CLIENTS);
        ((CelestialBackGateBlockEntity) level.getBlockEntity(other)).configure(level.dimension(), portal.east(32), Direction.NORTH);
        CelestialTravelManager.cleanupDuplicateGates(level, pos, level.dimension(), portal, Direction.NORTH);
        helper.assertTrue(level.isEmptyBlock(duplicate) && level.getBlockEntity(other) != null,
            "Cleanup only removes the same source connection");
        saved.putString("returnDimension", "bad id!");
        gate.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        helper.assertTrue(gate.getReturnDimension() == null, "Malformed saved dimension does not crash loading");
        helper.succeed();
    }

    private static void platform(GameTestHelper helper) {
        var level = destination(helper);
        BlockPos feet = new BlockPos(-512, 120, -512);
        level.setBlock(feet, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        Object rejected = invoke("createEmergencyLandingPlatform", new Class<?>[]{ServerLevel.class, BlockPos.class, boolean.class},
            level, feet, true);
        helper.assertTrue(rejected == null && level.getBlockState(feet).is(Blocks.BEDROCK)
            && level.isEmptyBlock(feet.below()), "Emergency platform does not partially destroy protected terrain");
        var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
        var missing = travel(ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("missing_travel_fixture")), feet);
        helper.assertTrue(!CelestialTravelManager.tryLand(pig, helper.getLevel(), pig.blockPosition(), Direction.NORTH, missing)
            && !pig.isOnPortalCooldown(), "Missing destination leaves travel retryable");
        pig.discard();
        helper.succeed();
    }

    public static CelestialForgingAnvilPortalBlockEntity machine(ServerLevel level, BlockPos center, CelestialTravelData travel) {
        var cfa = ModBlocks.CELESTIAL_FORGING_ANVIL.get();
        for (var part : Cube323PartHalf.values()) {
            level.setBlock(center.offset(part.getOffset()), cfa.placedState(part, cfa.defaultBlockState()), Block.UPDATE_CLIENTS);
        }
        var be = (CelestialForgingAnvilBlockEntity) level.getBlockEntity(center);
        be.setAmplifierPresent(true);
        be.setCelestialBodyData(new SpecialCelestialBodyData("fixture:travel", "overworld_like", 16, 0, 0, 0,
            Temperature.MILD, false, LiquidCoverage.NONE, false, false, "planet_overworld", null, travel));
        be.setChanged();
        be.syncToClient();
        BlockPos anchor = center.north(2);
        var block = ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.get();
        for (var part : DirectionGate331PartHalf.values()) {
            level.setBlock(anchor.offset(part.getOffset(Direction.NORTH)),
                block.placedState(part, block.defaultBlockState().setValue(CelestialForgingAnvilPortalBlock.FACING, Direction.NORTH)),
                Block.UPDATE_CLIENTS);
        }
        return (CelestialForgingAnvilPortalBlockEntity) level.getBlockEntity(anchor);
    }

    private static void cfaGate(GameTestHelper helper) {
        var level = helper.getLevel();
        var portal = machine(level, helper.absolutePos(new BlockPos(3, 62, 3)), travel(VOID, new BlockPos(1024, 140, 1024)));
        var old = new CompoundTag();
        old.putBoolean("emittingGamma", true);
        old.putInt("gammaLevel", 16);
        portal.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), old));
        portal.setWormholeLaser(16, true);
        portal.tick();
        helper.assertTrue(portal.getBlockState().getValue(CelestialForgingAnvilPortalBlock.OPEN)
            && portal.getLaserLevel() == 0 && !portal.isEmittingGamma(), "Standalone landing opens and clears old wormhole output");
        portal.findParentCfa().setAmplifierPresent(false);
        portal.tick();
        helper.assertTrue(!portal.getBlockState().getValue(CelestialForgingAnvilPortalBlock.OPEN), "Missing amplifier closes landing gate");
        helper.succeed();
    }

    private static Object invoke(String name, Class<?>[] types, Object... args) {
        try {
            var method = CelestialTravelManager.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
