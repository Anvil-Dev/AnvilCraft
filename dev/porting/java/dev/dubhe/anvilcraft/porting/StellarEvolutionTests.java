package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialMassTable;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEvolutionState;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrack;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StellarEvolutionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_stellar_coverage", StellarEvolutionTests::coverage,
        "port_stellar_round_trip", StellarEvolutionTests::roundTrip,
        "port_stellar_pause", StellarEvolutionTests::pause,
        "port_stellar_events", StellarEvolutionTests::events,
        "port_stellar_reload", StellarEvolutionTests::reload,
        "port_stellar_v2_migration", StellarEvolutionTests::migration,
        "port_stellar_invalid_snapshot", StellarEvolutionTests::invalidSnapshot,
        "port_stellar_phase_boundaries", StellarEvolutionTests::boundaries
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_stellar_evolution"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static StellarEvolutionState begin(StellarTrack track, long seed) {
        int mass = track.definition().massAnvils();
        return StellarEvolutionState.beginNew(track, mass == 41 ? CelestialBodyClass.BROWN_DWARF : CelestialBodyClass.G_MAIN,
            mass, 32, 32, seed, 1000, 12000);
    }

    private static void coverage(GameTestHelper helper) {
        helper.assertTrue(StellarTrackLibrary.tracks().size() == 24, "24 exact mass tracks");
        helper.assertTrue(StellarTrackLibrary.validateTracks().isEmpty(), "Valid entry/event/terminal references");
        for (int mass = 41; mass <= 64; mass++) {
            var track = StellarTrackLibrary.forMass(mass);
            var classes = mass == 41 ? List.of(CelestialBodyClass.BROWN_DWARF) : StellarTrackLibrary.EVOLVABLE_SURFACE_CLASSES;
            for (var bodyClass : classes) {
                var state = StellarEvolutionState.beginNew(track, bodyClass, mass, 32, 32, 73, 1000, 12000);
                helper.assertTrue(state.phaseIndex() == StellarTrackLibrary.startingPhaseIndex(track, bodyClass),
                    "Surface entry " + bodyClass);
                helper.assertTrue(state.phaseDurations(track).stream().allMatch(duration -> duration > 0), "Positive stage durations");
                helper.assertTrue(state.phaseDurations(track).stream().mapToInt(Integer::intValue).sum() == 12000, "Exact duration budget");
            }
        }
        helper.assertTrue(!StellarTrackLibrary.canStart(41, CelestialBodyClass.G_MAIN, false), "Brown dwarf mass entry guard");
        helper.assertTrue(!StellarTrackLibrary.canStart(49, CelestialBodyClass.G_MAIN, true), "Special red dwarf entry guard");
        helper.assertTrue(CelestialMassTable.at(49).solarMass() == 1.0, "49 anvils is one solar mass");
        helper.assertTrue(CelestialForgingAnvilMenu.formatMass(49).equals("1 M☉"), "Shared menu mass label");
        helper.succeed();
    }

    private static void roundTrip(GameTestHelper helper) {
        for (var track : StellarTrackLibrary.tracks()) {
            for (long seed : new long[]{0, 73, Long.MAX_VALUE}) {
                var state = begin(track, seed);
                state.update(6500, track);
                var saved = state.toTag();
                var loaded = StellarEvolutionState.fromTag(saved);
                helper.assertTrue(saved.equals(loaded.toTag()), "Complete NBT round trip " + track.trackId());
                helper.assertTrue(state.eventPlan().equals(loaded.eventPlan()), "Frozen event plan");
                for (long time : new long[]{1000, 6500, 12999, 13000}) {
                    helper.assertTrue(state.visualState(track, time, 0.5F).equals(loaded.visualState(track, time, 0.5F)),
                        "Reloaded visual interpolation " + track.trackId());
                }
            }
        }
        helper.succeed();
    }

    private static void pause(GameTestHelper helper) {
        var track = StellarTrackLibrary.forMass(56);
        var state = begin(track, 73);
        state.update(6500, track);
        var visual = state.visualState(track, 6500, 0.5F);
        final var events = state.eventPlan();
        float progress = state.totalProgress(6500, 0.5F);
        state.shiftTimeline(9000);
        helper.assertTrue(state.visualState(track, 15500, 0.5F).equals(visual), "Pause shifts the visual clock without a jump");
        helper.assertTrue(state.totalProgress(15500, 0.5F) == progress, "Pause preserves total progress");
        helper.assertTrue(state.eventPlan().equals(events), "Pause preserves event seeds and offsets");
        var loaded = StellarEvolutionState.fromTag(state.toTag());
        helper.assertTrue(loaded.visualState(track, 15500, 0.5F).equals(visual), "Paused clock survives NBT");
        helper.succeed();
    }

    private static void events(GameTestHelper helper) {
        int count = 0;
        for (var track : StellarTrackLibrary.tracks()) {
            var state = begin(track, 73);
            for (var event : state.eventPlan()) {
                long shock = state.totalStartGameTime() + event.shockOffset();
                helper.assertTrue(!state.dueEvents(shock - 1).contains(event), "Event cannot fire before its shock");
                helper.assertTrue(state.dueEvents(shock).contains(event), "Event fires at its shock");
                state.markEventApplied(event);
                state = StellarEvolutionState.fromTag(state.toTag());
                helper.assertTrue(!state.dueEvents(shock).contains(event), "Applied event never repeats after reload");
                count++;
            }
        }
        helper.assertTrue(count >= 10, "Coverage includes repeated and terminal events");
        helper.succeed();
    }

    private static void reload(GameTestHelper helper) {
        var original = StellarTrackLibrary.tracks();
        try {
            var track = StellarTrackLibrary.forMass(49);
            var state = begin(track, 73);
            final var frozen = state.toTag();
            JsonArray changed = StellarTrackLibrary.TRACKS_CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow().getAsJsonArray();
            var first = changed.get(8).getAsJsonObject().getAsJsonArray("phaseNodes").get(0).getAsJsonObject();
            first.addProperty("durationWeight", first.get("durationWeight").getAsFloat() * 2);
            helper.assertTrue(StellarTrackLibrary.replaceTracksFromJson(changed), "Valid resource set replaces the library");
            var replacement = StellarTrackLibrary.forMass(49);
            helper.assertTrue(!begin(replacement, 73).phaseDurations(replacement).equals(state.phaseDurations(track)),
                "New runs use new tracks");
            helper.assertTrue(state.visualState(replacement, 6500, 0.5F).equals(state.visualState(track, 6500, 0.5F)),
                "Running state samples its frozen track");
            helper.assertTrue(StellarEvolutionState.fromTag(frozen).toTag().equals(frozen), "Frozen snapshot reload ignores replacement");
            helper.assertTrue(!StellarTrackLibrary.replaceTracksFromJson(new JsonArray()), "Incomplete library rejected");
            helper.assertTrue(StellarTrackLibrary.forMass(49) == replacement, "Invalid reload keeps the last valid library");
        } finally {
            StellarTrackLibrary.replaceTracks(original);
        }
        helper.succeed();
    }

    private static void migration(GameTestHelper helper) {
        var track = StellarTrackLibrary.forMass(49);
        var state = begin(track, 73);
        CompoundTag legacy = state.toTag();
        legacy.putInt("stellarFormatVersion", 2);
        var snapshot = legacy.getCompoundOrEmpty("stellarTrackSnapshot");
        snapshot.getCompoundOrEmpty("definition").putInt("version", 2);
        var nodes = snapshot.getListOrEmpty("phaseNodes");
        var result = nodes.getCompoundOrEmpty(nodes.size() - 1).copy();
        result.putString("phaseId", "white_dwarf");
        result.putString("nodeId", "old_result");
        nodes.add(result);
        int[] durations = legacy.getIntArray("stellarPhaseDurations").orElseThrow();
        durations = Arrays.copyOf(durations, durations.length + 1);
        durations[durations.length - 1] = 9;
        legacy.putIntArray("stellarPhaseDurations", durations);
        legacy.putInt(StellarEvolutionState.TOTAL_DURATION_KEY, 12009);
        legacy.putInt(StellarEvolutionState.PHASE_INDEX_KEY, nodes.size() - 1);
        var untouched = legacy.copy();
        var loaded = StellarEvolutionState.fromTag(legacy);
        helper.assertTrue(legacy.equals(untouched), "Migration preserves the caller's saved tag");
        helper.assertTrue(loaded.totalDurationTicks() == 12000 && loaded.phaseIndex() == track.phaseNodes().size() - 1,
            "V2 result stage removed without rescheduling physical stages");
        helper.assertTrue(loaded.eventPlan().equals(state.eventPlan()), "V2 migration preserves event offsets and seeds");
        helper.assertTrue(loaded.phaseProgress() == 1 && loaded.isComplete(), "Result node maps to physical completion");
        helper.succeed();
    }

    private static void invalidSnapshot(GameTestHelper helper) {
        var saved = begin(StellarTrackLibrary.forMass(56), 73).toTag();
        for (Consumer<CompoundTag> corrupt : List.<Consumer<CompoundTag>>of(
            tag -> tag.putDouble("stellarMetallicityZ", Double.NaN),
            tag -> tag.putInt("stellarFormatVersion", 99),
            tag -> tag.putIntArray("stellarPhaseDurations", new int[]{-1}),
            tag -> tag.remove("stellarTrackSnapshot"),
            tag -> tag.putString("stellarNodeId", "missing_node")
        )) {
            var tag = saved.copy();
            corrupt.accept(tag);
            boolean rejected = false;
            try {
                StellarEvolutionState.fromTag(tag);
            } catch (IllegalArgumentException | IllegalStateException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "Corrupt active snapshot rejected");
        }
        helper.succeed();
    }

    private static void boundaries(GameTestHelper helper) {
        for (var track : StellarTrackLibrary.tracks()) {
            var state = begin(track, 73);
            var durations = state.phaseDurations(track);
            long time = state.totalStartGameTime();
            int index = state.scheduleStartIndex();
            for (int duration : durations) {
                state.update(time + duration - 1, track);
                helper.assertTrue(state.phaseIndex() == index, "No early phase transition");
                time += duration;
                if (index < track.phaseNodes().size() - 1) {
                    helper.assertTrue(state.update(time, track), "Transition reports a crossed boundary");
                    helper.assertTrue(state.phaseIndex() == ++index && state.phaseProgress() == 0, "Exact boundary enters next stage");
                }
            }
            state.update(time, track);
            helper.assertTrue(state.isComplete(), "Physical stages complete at the exact duration budget");
            helper.assertTrue(state.initialMass() == track.definition().massAnvils() && state.initialSize() == 32
                && state.initialEnergy() == 32 && state.currentMass() == state.initialMass(), "Gameplay input fields unchanged");
            state.markTerminalApplied();
            helper.assertTrue(!state.isActive(), "Terminal latch closes the run");
        }
        helper.succeed();
    }
}
