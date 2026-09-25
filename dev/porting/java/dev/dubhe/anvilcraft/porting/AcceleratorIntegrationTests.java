package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTerminal;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.block.entity.megastructure.AcceleratorHandler;
import dev.dubhe.anvilcraft.init.ModMegastructures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AcceleratorIntegrationTests {
    private static final Map<Vec3, Integer> EXPLOSIONS = new HashMap<>();
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_accelerator_build", AcceleratorIntegrationTests::build,
        "port_accelerator_pause", AcceleratorIntegrationTests::pause,
        "port_accelerator_save_sync", AcceleratorIntegrationTests::saveSync,
        "port_accelerator_snapshot", AcceleratorIntegrationTests::snapshot,
        "port_accelerator_legacy", AcceleratorIntegrationTests::legacy,
        "port_accelerator_terminals", AcceleratorIntegrationTests::terminals,
        "port_accelerator_unload", AcceleratorIntegrationTests::unload,
        "port_accelerator_special_dwarf", AcceleratorIntegrationTests::specialDwarf
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_accelerator_integration"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Start event) {
        Vec3 center = event.getExplosion().center();
        if (!EXPLOSIONS.containsKey(center)) return;
        if (event.getExplosion().radius() != 10) throw new IllegalStateException("Changed stellar explosion radius");
        EXPLOSIONS.computeIfPresent(center, (pos, count) -> count + 1);
        // Count the real gameplay explosion without destroying other test fixtures.
        event.setCanceled(true);
    }

    private static CelestialForgingAnvilBlockEntity machine(GameTestHelper helper, int mass) {
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(pos, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(), Block.UPDATE_CLIENTS);
        var be = (CelestialForgingAnvilBlockEntity) helper.getLevel().getBlockEntity(pos);
        be.getMegastructureManager().clearAllMegastructures(be);
        be.setAmplify(true);
        be.setAmplifierPresent(true);
        be.setLocked(true);
        be.setStellarMass(mass);
        be.setAgeAnvilCount(32);
        be.setCelestialBodyData(new StarData(mass == 41 ? CelestialBodyClass.BROWN_DWARF : CelestialBodyClass.G_MAIN,
            32, 255, 220, 160, 10, 2, 3, 32, new UUID(0, mass)));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
        return be;
    }

    private static CompoundTag save(AcceleratorHandler handler) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        handler.saveAdditional(output);
        return output.buildResult();
    }

    private static void load(GameTestHelper helper, AcceleratorHandler handler, CompoundTag tag) {
        handler.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    private static int option(CelestialForgingAnvilBlockEntity be) {
        var options = be.getClientVisibleOptions();
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index).id().equals(ModMegastructures.STELLAR_EVOLUTION_ACCELERATOR.getId())) return index;
        }
        return -1;
    }

    private static void build(GameTestHelper helper) {
        var be = machine(helper, 49);
        be.getMaterialContainer().setItem(0, ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT.asStack(16));
        int index = option(be);
        helper.assertTrue(index >= 0, "Valid track has an accelerator option");
        be.setLocked(false);
        be.buildMegastructure(index);
        helper.assertTrue(!be.isAcceleratorActive() && be.getMaterialContainer().getItem(0).getCount() == 16, "Unlocked build rejected");
        be.setLocked(true);
        be.buildMegastructure(index);
        var state = be.getStellarEvolutionState();
        helper.assertTrue(state != null && state.initialMass() == 49 && be.getMaterialContainer().getItem(0).getCount() == 8,
            "Actual build consumes eight components and starts one frozen run");
        var saved = state.toTag();
        be.buildMegastructure(index);
        helper.assertTrue(be.getMaterialContainer().getItem(0).getCount() == 8 && state.toTag().equals(saved),
            "Duplicate build neither consumes materials nor rerolls the run");
        helper.succeed();
    }

    private static void pause(GameTestHelper helper) {
        final var be = machine(helper, 49);
        final var handler = be.getMegastructureManager().getAcceleratorHandler();
        handler.onBuild(be);
        handler.getEvolutionState().shiftTimeline(-Math.min(100, handler.getEvolutionState().totalDurationTicks() / 4));
        be.setAmplifierPresent(false);
        handler.serverTick(be);
        final var visual = handler.getVisualState(be, 0);
        final long start = handler.getEvolutionState().totalStartGameTime();
        final long pausedAt = helper.getLevel().getGameTime();
        helper.runAfterDelay(5, () -> {
            handler.serverTick(be);
            helper.assertTrue(handler.isPaused() && handler.getVisualState(be, 0.8F).equals(visual), "Paused visual clock remains frozen");
            be.setAmplifierPresent(true);
            handler.serverTick(be);
            helper.assertTrue(!handler.isPaused() && handler.getEvolutionState().totalStartGameTime()
                == start + helper.getLevel().getGameTime() - pausedAt, "Resume shifts the clock by the exact pause duration");
            helper.assertTrue(handler.getVisualState(be, 0).equals(visual), "Resume has no visual jump");
            helper.succeed();
        });
    }

    private static void saveSync(GameTestHelper helper) {
        var be = machine(helper, 56);
        var handler = be.getMegastructureManager().getAcceleratorHandler();
        handler.onBuild(be);
        be.setAmplifierPresent(false);
        handler.serverTick(be);
        var loaded = new AcceleratorHandler();
        var tag = save(handler);
        load(helper, loaded, tag);
        helper.assertTrue(tag.equals(save(loaded)), "Root-level native ValueInput/ValueOutput round trip");
        var update = new CompoundTag();
        handler.writeUpdateTag(update, helper.getLevel().registryAccess());
        var client = new AcceleratorHandler();
        client.readUpdateTag(update, helper.getLevel().registryAccess());
        helper.assertTrue(client.isPaused() && client.getEvolutionState().toTag().equals(handler.getEvolutionState().toTag()),
            "Update tag carries the same frozen state and paused clock");
        helper.succeed();
    }

    private static void snapshot(GameTestHelper helper) {
        var be = machine(helper, 49);
        var handler = be.getMegastructureManager().getAcceleratorHandler();
        handler.onBuild(be);
        handler.getEvolutionState().shiftTimeline(-Math.min(100, handler.getEvolutionState().totalDurationTicks() / 4));
        final var visual = handler.getVisualState(be, 0);
        final var plan = handler.getEvolutionState().eventPlan();
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        be.storeDiskData(output);
        var snapshot = output.buildResult();
        helper.assertTrue(snapshot.contains("stellarEvolution") && snapshot.contains("celestialBody"), "Disk stores the complete snapshot");
        handler.restoreSnapshot(be, snapshot);
        helper.assertTrue(handler.getEvolutionState().eventPlan().equals(plan) && handler.getVisualState(be, 0).equals(visual),
            "Capture and restore do not reroll the track or shift phase progress");
        helper.succeed();
    }

    private static void legacy(GameTestHelper helper) {
        final var handler = new AcceleratorHandler();
        var legacy = new CompoundTag();
        legacy.putInt("acceleratorStage", 3);
        legacy.putInt("acceleratorTicksRemaining", 100);
        legacy.putInt("acceleratorTicksTotal", 200);
        legacy.putBoolean("quenchedStarted", true);
        load(helper, handler, legacy);
        helper.assertTrue(!handler.isActive() && handler.getStage() == 0 && handler.getTicksRemaining() == 0,
            "Source clears legacy four-stage data without a supported frozen snapshot");
        helper.assertTrue(!save(handler).getBooleanOr("quenchedStarted", true), "Legacy music state also clears");
        helper.succeed();
    }

    private static void terminals(GameTestHelper helper) {
        for (var track : StellarTrackLibrary.tracks()) {
            var be = machine(helper, track.definition().massAnvils());
            var handler = be.getMegastructureManager().getAcceleratorHandler();
            handler.onBuild(be);
            var state = handler.getEvolutionState();
            int expectedExplosions = (int) state.eventPlan().stream().filter(event -> event.policy().destructive()).count();
            state.shiftTimeline(-state.totalDurationTicks());
            Vec3 center = new Vec3(be.getBlockPos().getX() + 0.5, be.getBodyCenterWorldY(), be.getBlockPos().getZ() + 0.5);
            EXPLOSIONS.put(center, 0);
            try {
                handler.serverTick(be);
                var terminal = track.definition().terminal();
                helper.assertTrue(!handler.isActive() && state.terminalApplied(), "Terminal completes once " + track.trackId());
                helper.assertTrue(EXPLOSIONS.get(center) == expectedExplosions, "Destructive event count " + track.trackId());
                if (terminal.kind() == StellarTerminal.Kind.NONE) {
                    helper.assertTrue(be.getCelestialBodyData() == null && be.getStellarMass() == 0, "Disruption leaves no remnant");
                } else {
                    var remnant = (StarData) be.getCelestialBodyData();
                    CelestialBodyClass expectedClass = switch (terminal.kind()) {
                        case WHITE_DWARF -> CelestialBodyClass.WHITE_DWARF;
                        case NEUTRON_STAR -> CelestialBodyClass.NEUTRON_STAR;
                        case BLACK_HOLE -> CelestialBodyClass.BLACK_HOLE;
                        default -> CelestialBodyClass.BROWN_DWARF;
                    };
                    helper.assertTrue(remnant.bodyClass() == expectedClass, "Remnant class follows the physical outcome");
                    helper.assertTrue(remnant.bodyUuid().equals(new UUID(0, track.definition().massAnvils())),
                        "Remnant keeps its identity");
                    if (terminal.kind() != StellarTerminal.Kind.KEEP) {
                        helper.assertTrue(be.getStellarMass() == terminal.massAnvils(), "Remnant mass follows track outcome");
                        helper.assertTrue(be.getAgeAnvilCount() == 33, "Remnant advances age once");
                        boolean whiteDwarf = terminal.kind() == StellarTerminal.Kind.WHITE_DWARF;
                        helper.assertTrue(remnant.size() == (whiteDwarf ? terminal.size() : 1)
                            && remnant.energy() == (whiteDwarf ? 47 : 64), "Remnant size and energy follow source values");
                    }
                }
                var data = be.getCelestialBodyData();
                handler.serverTick(be);
                helper.assertTrue(be.getCelestialBodyData() == data && EXPLOSIONS.get(center) == expectedExplosions,
                    "Repeated ticks cannot apply terminal or explosion twice");
            } finally {
                EXPLOSIONS.remove(center);
            }
        }
        helper.succeed();
    }

    private static void unload(GameTestHelper helper) {
        var be = machine(helper, 49);
        var handler = be.getMegastructureManager().getAcceleratorHandler();
        handler.onBuild(be);
        final var saved = save(handler);
        be.setRemoved();
        helper.assertTrue(handler.isActive() && save(handler).equals(saved), "Chunk unload preserves evolution state");
        be.clearRemoved();
        helper.getLevel().setBlock(be.getBlockPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!handler.isActive(), "Permanent block removal clears the run");
        helper.assertTrue(be.saveForDrop(helper.getLevel().registryAccess()).contains("stellarTrackSnapshot"),
            "Drop snapshot is captured before permanent cleanup");
        helper.succeed();
    }

    private static void specialDwarf(GameTestHelper helper) {
        var be = machine(helper, 42);
        var star = new StarData(CelestialBodyClass.M_MAIN, 16, 255, 100, 50, 0, 1, 1, 32, null, true);
        helper.assertTrue(StarData.fromTag(star.toTag()).specialRedDwarf() && star.withBodyUuid(UUID.randomUUID()).specialRedDwarf(),
            "Special-red-dwarf flag survives NBT and UUID copy");
        be.setCelestialBodyData(star);
        helper.assertTrue(option(be) == -1, "Special red dwarf cannot offer an evolution build");
        be.getMegastructureManager().getAcceleratorHandler().onBuild(be);
        helper.assertTrue(!be.isAcceleratorActive(), "Special red dwarf cannot start through the handler");
        helper.succeed();
    }
}
