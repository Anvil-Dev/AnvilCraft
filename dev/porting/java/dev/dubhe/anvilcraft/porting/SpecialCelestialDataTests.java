package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class SpecialCelestialDataTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_special_recipe_schema", SpecialCelestialDataTests::recipeSchema,
        "port_special_legacy_data", SpecialCelestialDataTests::legacyData,
        "port_special_travel_rules", SpecialCelestialDataTests::travelRules,
        "port_special_builtin_resources", SpecialCelestialDataTests::builtins
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_special_data"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static JsonObject builtin(String name) {
        String path = "/data/anvilcraft/recipe/special_celestial_body/" + name + ".json";
        try (var input = Objects.requireNonNull(SpecialCelestialDataTests.class.getResourceAsStream(path));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static SpecialCelestialBodyRecipe decode(JsonObject json) {
        return SpecialCelestialBodyRecipe.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static void recipeSchema(GameTestHelper helper) {
        var json = builtin("overworld_like");
        json.addProperty("model", "testpack:block/custom_planet");
        json.addProperty("needs_custom_model", true);
        json.addProperty("atmosphere", "#42A5F5");
        json.addProperty("can_be_shattered", false);
        json.addProperty("exclude_from_monolith", true);
        var recipe = decode(json);
        helper.assertTrue(recipe.atmosphere().orElseThrow().rgba() == 0x42A5F5, "Six-digit color keeps source integer value");
        helper.assertTrue(!recipe.canBeShattered() && recipe.excludeFromMonolith(), "Explicit behavior flags survive decoding");
        var encoded = SpecialCelestialBodyRecipe.CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe).getOrThrow();
        helper.assertTrue(decode(encoded.getAsJsonObject()).equals(recipe), "JSON round trip preserves all recipe fields");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            SpecialCelestialBodyRecipe.STREAM_CODEC.encode(buffer, recipe);
            helper.assertTrue(SpecialCelestialBodyRecipe.STREAM_CODEC.decode(buffer).equals(recipe) && !buffer.isReadable(),
                "Recipe network round trip has no missing or trailing fields");
        } finally {
            buffer.release();
        }
        var body = SpecialCelestialBodyData.fromRecipe(recipe, "testpack:custom");
        helper.assertTrue(body.getModelLocation().equals(Identifier.parse("testpack:block/custom_planet"))
            && body.atmosphereColor().rgba() == 0x42A5F5 && body.isLandable() && !body.canBeShattered(),
            "Discovered body snapshots model, color, landing and explicit shatterability");
        helper.assertTrue(SpecialCelestialBodyData.fromTag(body.toTag()).equals(body), "Body NBT round trip is lossless");
        json.remove("atmosphere");
        json.addProperty("has_atmosphere", true);
        var legacy = SpecialCelestialBodyRecipe.CODEC.codec().parse(JsonOps.INSTANCE, json);
        AnvilCraft.LOGGER.info("PORT_SPECIAL_LEGACY_ATMOSPHERE: color={}, lifecycle={}",
            legacy.getOrThrow().atmosphere().orElseThrow().rgba(), legacy.lifecycle());
        helper.assertTrue(legacy.getOrThrow().atmosphere().orElseThrow().rgba() == 0x99CCFF,
            "Old atmosphere boolean migrates to the source temperature color");
        json.addProperty("atmosphere", "#00123456");
        helper.assertTrue(decode(json).atmosphere().orElseThrow().rgba() == 0x123456, "Explicit color overrides the legacy boolean");
        json.addProperty("atmosphere", "invalid");
        helper.assertTrue(SpecialCelestialBodyRecipe.CODEC.codec().parse(JsonOps.INSTANCE, json).error().isPresent(),
            "Malformed color fails recipe decoding");
        helper.succeed();
    }

    private static void legacyData(GameTestHelper helper) {
        var body = SpecialCelestialBodyData.fromRecipe(decode(builtin("overworld_like")), "anvilcraft:overworld_like");
        var old = body.toTag();
        old.remove("model");
        old.putString("textureName", "planet_overworld");
        old.remove("atmosphereColor");
        old.putBoolean("hasAtmosphere", true);
        old.remove("temperature");
        old.put("travel", old.getCompoundOrEmpty("landing"));
        old.remove("landing");
        old.remove("canBeShattered");
        var migrated = SpecialCelestialBodyData.fromTag(old);
        helper.assertTrue(migrated.model().equals("planet_overworld") && migrated.atmosphereColor().rgba() == 0xFFFFFF
            && migrated.canBeShattered() && migrated.landing().equals(body.landing()), "Legacy NBT aliases and defaults migrate");
        old.putString("model", "end_gateway");
        old.putBoolean("canBeShattered", false);
        old.putInt("atmosphereColor", 0x40FF80);
        migrated = SpecialCelestialBodyData.fromTag(old);
        helper.assertTrue(migrated.usesEndGatewayModel() && !migrated.canBeShattered()
            && migrated.atmosphereColor().rgba() == 0x40FF80, "Explicit new NBT keys take precedence");
        var json = builtin("overworld_like");
        json.add("travel", json.remove("landing"));
        json.remove("can_be_shattered");
        helper.assertTrue(decode(json).canBeShattered() && decode(json).isLandable(),
            "Legacy travel recipe alias drives default shatterability");
        helper.succeed();
    }

    private static void travelRules(GameTestHelper helper) {
        for (var type : CelestialTravelData.CoordinateRule.Type.values()) {
            for (var returning : CelestialTravelData.ReturnRule.Type.values()) {
                var travel = new CelestialTravelData(Identifier.withDefaultNamespace("overworld"),
                    new CelestialTravelData.CoordinateRule(type, 0.125, -12, 90, 27, 16),
                    new CelestialTravelData.ReturnRule(returning, -50, 128, 6, 4));
                helper.assertTrue(travel.equals(CelestialTravelData.fromTag(travel.toTag())), "All coordinate/return rules round trip");
                var json = CelestialTravelData.CODEC.encodeStart(JsonOps.INSTANCE, travel).getOrThrow();
                helper.assertTrue(travel.equals(CelestialTravelData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow()),
                    "Travel JSON round trip " + type + "/" + returning);
            }
        }
        var defaults = JsonParser.parseString("{\"dimension\":\"minecraft:overworld\","
            + "\"coordinate_rule\":\"same_height\",\"return_rule\":\"entry\"}");
        var travel = CelestialTravelData.CODEC.parse(JsonOps.INSTANCE, defaults).getOrThrow();
        helper.assertTrue(travel.coordinateRule().type() == CelestialTravelData.CoordinateRule.Type.SAME_3D
            && travel.returnRule().type() == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL, "Concise aliases use source rules");
        var clamped = new CelestialTravelData.CoordinateRule(CelestialTravelData.CoordinateRule.Type.SCALED,
            Double.NaN, 0, 64, 0, -1);
        helper.assertTrue(clamped.scale() == 1 && clamped.radius() == 0, "Invalid numeric travel values normalize");
        var malformed = new CompoundTag();
        malformed.putString("dimension", "bad id!");
        helper.assertTrue(CelestialTravelData.fromTag(malformed) == null, "Malformed saved travel data is ignored");
        helper.succeed();
    }

    private static void builtins(GameTestHelper helper) {
        for (String name : List.of("overworld_like", "flesh_planet", "intelligent_planet", "hollow_planet", "error_planet")) {
            var recipe = decode(builtin(name));
            var body = SpecialCelestialBodyData.fromRecipe(recipe, "anvilcraft:" + name);
            helper.assertTrue(body.name().equals(name) && body.toTag().contains("model"), "Generated builtin uses new schema " + name);
        }
        var earth = decode(builtin("overworld_like"));
        var resources = earth.generateResources();
        helper.assertTrue(resources.getBiologicalFluids().size() == 1
            && resources.getBiologicalFluids().getFirst().fluidId().equals(Identifier.withDefaultNamespace("milk"))
            && resources.getBiologicalFluids().getFirst().weight() == 50, "Source overworld biological milk resource is discoverable");
        helper.assertTrue(earth.canBeShattered() && earth.landing().orElseThrow().dimension()
            .equals(CelestialTravelData.OVERWORLD_LIKE_DIMENSION), "Source overworld behavior metadata survives generation");
        helper.succeed();
    }
}
