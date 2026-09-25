package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import dev.dubhe.anvilcraft.porting.CelestialTravelTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MonolithGenerationTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(AnvilCraft.of("port_monolith_generation"),
            MonolithGenerationTests::generation));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_monolith_generation"));
        event.registerTest(AnvilCraft.of("port_monolith_generation"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_monolith_generation")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 600, 0, true)));
    }

    private static void generation(GameTestHelper helper) {
        var level = helper.getLevel();
        var template = level.getServer().getStructureManager().getOrCreate(TheMonolith.SMALL_TEMPLATE);
        var surface = helper.absolutePos(new BlockPos(3, 6, 3));
        var placement = TheMonolith.placement(template, surface);
        helper.assertTrue(placement.rotation() == Rotation.CLOCKWISE_90 && placement.boundingBox().minY() == surface.getY() + 1,
            "Template rotation or above-ground origin changed");
        BoundingBox bounds = placement.boundingBox();
        for (int x = bounds.minX() - 1; x <= bounds.maxX() + 1; x++) {
            for (int z = bounds.minZ() - 1; z <= bounds.maxZ() + 1; z++) {
                for (int y = surface.getY() - 3; y <= bounds.maxY() + 2; y++) {
                    level.setBlock(new BlockPos(x, y, z), y <= surface.getY() ? Blocks.DIRT.defaultBlockState()
                        : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int[] heights = TheMonolith.sampleSmallMonolithGround(level, bounds);
        helper.assertTrue(heights != null && heights.length == 15 && Arrays.stream(heights).allMatch(y -> y == surface.getY()),
            "The rotated footprint and margin were not fully sampled");
        var low = new BlockPos(bounds.minX() - 1, surface.getY(), bounds.minZ() - 1);
        level.setBlock(low, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(low.below(), Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(TheMonolith.prepareSmallMonolithGround(level, bounds, surface.getY()), "Two-block slope was rejected");
        helper.assertTrue(level.getBlockState(low).is(Blocks.DIRT), "Slope fill did not use local ground material");
        helper.assertTrue(surface.equals(TheMonolith.findSmallMonolithGround(level, template,
            new BlockPos(surface.getX(), 0, surface.getZ()))),
            "Flat natural site was not selected at the requested origin");
        level.setBlock(low.above(), Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(TheMonolith.sampleSmallMonolithGround(level, bounds) == null, "Wet site was accepted");
        helper.assertTrue(!TheMonolith.prepareSmallMonolithGround(level, bounds, surface.getY())
            && level.getBlockState(low.above()).is(Blocks.WATER), "Rejected site was partially modified");
        level.setBlock(low.above(), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        helper.assertTrue(TheMonolith.sampleSmallMonolithGround(level, bounds) == null, "Tree canopy was accepted");
        level.setBlock(low.above(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(low, Blocks.STONE_BRICKS.defaultBlockState(), 2);
        helper.assertTrue(TheMonolith.sampleSmallMonolithGround(level, bounds) == null, "Artificial ground was accepted");
        legacy(helper);
        AnvilCraft.LOGGER.info("PORT_MONOLITH_GENERATION_PASSED: rotated footprint, terrain validation and legacy state");
        helper.succeed();
    }

    private static void legacy(GameTestHelper helper) {
        var key = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("port_monolith_legacy_" + UUID.randomUUID()));
        var level = CelestialTravelTests.createTestDimension(helper, key);
        var folder = ((MinecraftServerAccessor) level.getServer()).anvilcraft$getStorageSource()
            .getDimensionPath(key).resolve("data");
        int[] box = {-3, 40, 5, 7, 80, 9};
        CompoundTag data = new CompoundTag();
        data.putIntArray("BoundingBox", box);
        CompoundTag root = new CompoundTag();
        root.put("data", data);
        try {
            Files.createDirectories(folder);
            NbtIo.writeCompressed(root, folder.resolve("anvilcraft_the_monolith.dat"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        var loaded = TheMonolith.State.get(level);
        var saved = (CompoundTag) TheMonolith.State.CODEC.encodeStart(NbtOps.INSTANCE, loaded).getOrThrow();
        helper.assertTrue(Arrays.equals(box, saved.getIntArray("BoundingBox").orElseThrow()), "Legacy monolith bounds were lost");
        var roundTrip = TheMonolith.State.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(TheMonolith.State.CODEC.encodeStart(NbtOps.INSTANCE, roundTrip).getOrThrow().equals(saved),
            "Monolith state codec changed its bounds");
        helper.assertTrue(Files.exists(folder.resolve("anvilcraft_the_monolith.dat")), "Legacy state was destructively removed");
    }
}
