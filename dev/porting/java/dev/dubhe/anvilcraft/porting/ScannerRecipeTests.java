package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintNormalizer;
import dev.dubhe.anvilcraft.building.ScannerDiskNormalizer;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.util.StructureScannerRecipes;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ScannerRecipeTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_scanner_recipe_inputs", ScannerRecipeTests::inputs,
        "port_scanner_recipe_bounds", ScannerRecipeTests::bounds,
        "port_scanner_recipe_catalog", ScannerRecipeTests::catalog,
        "port_scanner_recipe_coordinates", ScannerRecipeTests::coordinates
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_scanner_recipe"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void inputs(GameTestHelper helper) {
        var nbt = new CompoundTag();
        nbt.putString("id", "minecraft:furnace");
        nbt.putInt("port_value", 42);
        var pattern = MultiblockDefinition.seriaBuilder().layer("F S")
            .map('F', BlockStatePredicate.builder().of(Blocks.FURNACE)
                .with(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).nbt(nbt))
            .map('S', Blocks.STONE).build();
        var recipe = new MultiblockRecipe(pattern, new ItemStackTemplate(Items.DIAMOND, 1));
        var snapshot = StructureScannerRecipes.snapshot(recipe);
        helper.assertTrue(snapshot.size().getX() == 3 && snapshot.nonAirBlockCount() == 2, "Input bounds/materials changed");
        var furnace = snapshot.blocks().stream().filter(entry -> snapshot.stateOf(entry).is(Blocks.FURNACE)).findFirst().orElseThrow();
        helper.assertTrue(snapshot.stateOf(furnace).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
            "Predicate state was replaced by the default state");
        helper.assertTrue(furnace.nbt().orElseThrow().getIntOr("port_value", 0) == 42, "Predicate NBT was lost");
        furnace.nbt().orElseThrow().putInt("port_value", 99);
        helper.assertTrue(nbt.getIntOr("port_value", 0) == 42, "Snapshot mutated the recipe NBT");
        var output = MultiblockDefinition.seriaBuilder().layer("G").map('G', Blocks.GOLD_BLOCK).build();
        var conversion = StructureScannerRecipes.snapshot(new MultiblockConversionRecipe(pattern, output));
        helper.assertTrue(conversion.nonAirBlockCount() == 2
            && conversion.palette().stream().noneMatch(state -> state.is(Blocks.GOLD_BLOCK)),
            "Conversion imported its output instead of its input");
        helper.succeed();
    }

    private static void bounds(GameTestHelper helper) {
        var allowed = MultiblockDefinition.seriaBuilder().layer("S".repeat(16)).map('S', Blocks.STONE).build();
        var snapshot = StructureScannerRecipes.snapshot(new MultiblockRecipe(allowed, new ItemStackTemplate(Items.DIAMOND, 1)));
        helper.assertTrue(snapshot.size().getX() == 16, "Sixteen-block input was rejected");
        var oversized = MultiblockDefinition.seriaBuilder().layer("S".repeat(17)).map('S', Blocks.STONE).build();
        boolean rejected = false;
        try {
            StructureScannerRecipes.snapshot(new MultiblockRecipe(oversized, new ItemStackTemplate(Items.DIAMOND, 1)));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Oversized input was accepted");
        helper.assertTrue(StructureScannerRecipes.fileName(AnvilCraft.of("folder/test")).equals("folder_test.nbt"),
            "Unsafe recipe filename");
        helper.assertTrue(StructureScannerRecipes.fileName(AnvilCraft.of("a".repeat(150))).length() == 124,
            "Recipe filename exceeds limit");
        helper.succeed();
    }

    private static void coordinates(GameTestHelper helper) {
        var pattern = MultiblockDefinition.seriaBuilder().layer("S G").layer("S  ")
            .map('S', Blocks.STONE).map('G', Blocks.GOLD_BLOCK).build();
        var snapshot = StructureScannerRecipes.snapshot(new MultiblockRecipe(pattern, new ItemStackTemplate(Items.DIAMOND, 1)));
        var tag = StructureSnapshotCodec.write(snapshot);
        helper.assertTrue(tag.getBooleanOr("anvilcraft:world_coordinates", false), "Normalized coordinate marker is missing");
        try {
            for (var facing : Direction.Plane.HORIZONTAL) {
                for (boolean upsideDown : new boolean[]{false, true}) {
                    var loaded = BlueprintNormalizer.load(tag, helper.getLevel().registryAccess(), facing, upsideDown);
                    helper.assertTrue(tag.equals(StructureSnapshotCodec.write(loaded)), "Normalized coordinates transformed twice");
                    var legacy = tag.copy();
                    legacy.remove("anvilcraft:world_coordinates");
                    var oldLoaded = BlueprintNormalizer.load(legacy, helper.getLevel().registryAccess(), facing, upsideDown);
                    var expected = BlueprintNormalizer.normalize(ScannerDiskNormalizer.normalize(snapshot, facing, upsideDown)).snapshot();
                    helper.assertTrue(StructureSnapshotCodec.write(expected).equals(StructureSnapshotCodec.write(oldLoaded)),
                        "Legacy scanner orientation was not preserved");
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        helper.succeed();
    }

    private static void catalog(GameTestHelper helper) {
        int crafting = 0;
        int conversion = 0;
        for (var holder : helper.getLevel().getServer().getRecipeManager().recipeMap().values()) {
            var recipe = holder.value();
            if (!(recipe instanceof MultiblockRecipe) && !(recipe instanceof MultiblockConversionRecipe)) continue;
            var snapshot = StructureScannerRecipes.snapshot(recipe);
            helper.assertTrue(snapshot.nonAirBlockCount() > 0, "Empty recipe import: " + holder.id());
            helper.assertTrue(snapshot.size().getX() <= 16 && snapshot.size().getY() <= 16 && snapshot.size().getZ() <= 16,
                "Recipe import exceeded scanner size: " + holder.id());
            if (recipe instanceof MultiblockRecipe) crafting++;
            else conversion++;
        }
        helper.assertTrue(crafting > 0 && conversion > 0, "Both recipe categories must be exercised");
        AnvilCraft.LOGGER.info("PORT_SCANNER_RECIPE_CATALOG_PASSED: crafting={}, conversion={}", crafting, conversion);
        helper.succeed();
    }
}
