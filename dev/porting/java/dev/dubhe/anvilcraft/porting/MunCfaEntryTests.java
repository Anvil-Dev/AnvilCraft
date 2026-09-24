package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSeedMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MunCfaEntryTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(AnvilCraft.of("port_mun_cfa_recipe"),
            MunCfaEntryTests::recipe));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_mun_cfa_recipe"));
        event.registerTest(AnvilCraft.of("port_mun_cfa_recipe"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_mun_cfa_recipe")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)));
    }

    private static void recipe(GameTestHelper helper) {
        var level = helper.getLevel();
        var seed = ModBlocks.LUNAR_ROCK.asItem();
        var result = CelestialSeedMatcher.match(level, 34, 4, 1, 14, seed);
        helper.assertTrue(result != null, "The source Moon recipe must match lunar rock and 34/4/1/14");
        if (result == null) return;
        var body = result.body();
        helper.assertTrue(body.name().equals("mun") && body.model().equals("planet_atmosphereless")
            && !body.canBeShattered() && body.liquidCoverage() == LiquidCoverage.NONE && body.atmosphereColor() == null,
            "Moon celestial properties differ from source");
        var landing = body.landing();
        helper.assertTrue(landing != null && landing.dimension().equals(AnvilCraft.of("mun"))
            && landing.coordinateRule().type() == CelestialTravelData.CoordinateRule.Type.FIXED_SURFACE
            && landing.returnRule().type() == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL,
            "Moon entry or linked return rules differ from source");
        helper.assertTrue(CelestialSeedMatcher.match(level, 33, 4, 1, 14, seed) == null
            && CelestialSeedMatcher.match(level, 34, 5, 1, 14, seed) == null
            && CelestialSeedMatcher.match(level, 34, 4, 2, 14, seed) == null
            && CelestialSeedMatcher.match(level, 34, 4, 1, 15, seed) == null
            && CelestialSeedMatcher.match(level, 34, 4, 1, 14, Items.STONE) == null,
            "Moon recipe accepted incorrect parameters or seed");
        AnvilCraft.LOGGER.info("PORT_MUN_CFA_RECIPE_PASSED");
        helper.succeed();
    }
}
