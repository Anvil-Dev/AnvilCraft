package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.PressureType;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.WindSpeed;
import dev.dubhe.anvilcraft.block.entity.megastructure.DysonSphereHandler;
import dev.dubhe.anvilcraft.init.ModMegastructures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class DysonSphereTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_dyson_supply", DysonSphereTests::supply,
        "port_dyson_small_stars", DysonSphereTests::smallStars,
        "port_dyson_persistence", DysonSphereTests::persistence,
        "port_dyson_transformation", DysonSphereTests::transformation
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_dyson"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private record Fixture(CelestialForgingAnvilBlockEntity be, CelestialForgingAnvilFluidInterfaceBlockEntity fluid) {
    }

    private static Fixture fixture(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlock(pos, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos.offset(2, 0, 0), ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.getDefaultState(), Block.UPDATE_CLIENTS);
        var be = (CelestialForgingAnvilBlockEntity) level.getBlockEntity(pos);
        be.setAmplify(false);
        be.setAmplifierPresent(false);
        be.setLocked(true);
        be.setCelestialBodyData(brown(32));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
        var fluid = (CelestialForgingAnvilFluidInterfaceBlockEntity) level.getBlockEntity(pos.offset(2, 0, 0));
        return new Fixture(be, fluid);
    }

    private static GiantPlanetData brown(int energy) {
        return new GiantPlanetData(CelestialBodyClass.BROWN_DWARF, PressureType.GAS, WindSpeed.HIGH,
            RingType.NONE, 64, 0, 1, 13, 2, 3, true, energy);
    }

    private static void feed(Fixture fixture, DysonSphereHandler handler, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = fixture.fluid.getInternalFluidHandler().insert(FluidResource.of(ModFluids.PRIMORDIAL_MATTER.get()),
                amount, transaction);
            if (inserted != amount) throw new IllegalStateException("Fixture supply rejected " + amount);
            transaction.commit();
        }
        handler.serverTick(fixture.be);
    }

    private static void supply(GameTestHelper helper) {
        var fixture = fixture(helper);
        var handler = new DysonSphereHandler("dyson_sphere_brown_dwarf");
        int base = 81000;
        helper.assertTrue(handler.getOutputPower(fixture.be) == base, "Unamplified brown dwarf produces base power");
        int[] amounts = {249, 250, 499, 500, 999, 1000, 1999, 2000};
        int[] powers = {base, base * 3 / 2, base * 3 / 2, base * 2, base * 2, base * 3, base * 3, base * 5};
        for (int sample = 0; sample < amounts.length; sample++) {
            handler.onBuild(fixture.be);
            for (int tick = 0; tick < 39; tick++) feed(fixture, handler, amounts[sample]);
            helper.assertTrue(handler.getOutputPower(fixture.be) == base, "Boost waits for 40 stable ticks");
            feed(fixture, handler, amounts[sample]);
            helper.assertTrue(handler.getOutputPower(fixture.be) == powers[sample], "Source supply tier " + amounts[sample]);
            feed(fixture, handler, 0);
            helper.assertTrue(handler.getOutputPower(fixture.be) == base, "Interrupted supply immediately removes boost");
        }
        for (int tick = 0; tick < 40; tick++) feed(fixture, handler, 250);
        feed(fixture, handler, 500);
        helper.assertTrue(handler.getOutputPower(fixture.be) == base, "Changing tier restarts stabilization");
        fixture.be.setCelestialBodyData(brown(0));
        fixture.be.getAnvilInventory().setItem(3, ModBlocks.CONFINED_ENERGY_ANVILON.asStack(32));
        helper.assertTrue(handler.getOutputPower(fixture.be) == base, "Old giant data falls back to energy input");
        try (Transaction transaction = Transaction.openRoot()) {
            fixture.fluid.getInternalFluidHandler().insert(FluidResource.of(Fluids.WATER), 1000, transaction);
            transaction.commit();
        }
        feed(fixture, handler, 1000);
        helper.assertTrue(fixture.fluid.drainFluid(Fluids.WATER) == 1000, "Matter processing preserves unrelated fluids");
        helper.succeed();
    }

    private static void smallStars(GameTestHelper helper) {
        var fixture = fixture(helper);
        fixture.be.setAmplify(true);
        fixture.be.setAmplifierPresent(true);
        var handler = new DysonSphereHandler("dyson_sphere_small");
        var classes = new CelestialBodyClass[]{CelestialBodyClass.M_MAIN, CelestialBodyClass.K_MAIN, CelestialBodyClass.G_MAIN};
        int[] boosted = {80000, 60000, 50000};
        for (int sample = 0; sample < classes.length; sample++) {
            fixture.be.setCelestialBodyData(new StarData(classes[sample], 32, 255, 180, 120, 0, 1, 2, 32, null));
            handler.onBuild(fixture.be);
            for (int tick = 0; tick < 40; tick++) feed(fixture, handler, 2000);
            helper.assertTrue(handler.getOutputPower(fixture.be) == boosted[sample], "Small star gain " + classes[sample]);
        }
        fixture.be.setAmplifierPresent(false);
        helper.assertTrue(handler.getOutputPower(fixture.be) == 0, "Stellar Dyson power still requires a physical amplifier");
        helper.succeed();
    }

    private static CompoundTag save(DysonSphereHandler handler) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        handler.saveAdditional(output);
        return output.buildResult();
    }

    private static void persistence(GameTestHelper helper) {
        var fixture = fixture(helper);
        var handler = new DysonSphereHandler("dyson_sphere_brown_dwarf");
        for (int tick = 0; tick < 39; tick++) feed(fixture, handler, 2500);
        var saved = save(handler);
        var loaded = new DysonSphereHandler("dyson_sphere_brown_dwarf");
        loaded.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(save(loaded).equals(saved), "Supply streak and accumulated excess survive disk reload");
        var update = new CompoundTag();
        loaded.writeUpdateTag(update, helper.getLevel().registryAccess());
        var client = new DysonSphereHandler("dyson_sphere_brown_dwarf");
        client.readUpdateTag(update, helper.getLevel().registryAccess());
        helper.assertTrue(save(client).equals(saved), "Client state matches server persistence");
        feed(fixture, loaded, 2500);
        helper.assertTrue(loaded.getOutputPower(fixture.be) == 405000, "Reloaded streak reaches boost on tick 40");
        loaded.onClear(fixture.be);
        helper.assertTrue(save(loaded).getLongOr("dysonSphere_dyson_sphere_brown_dwarf_ExcessMatter", -1) == 0,
            "Clearing structure resets stored excess");
        var old = brown(0).toTag();
        old.remove("energy");
        helper.assertTrue(GiantPlanetData.fromTag(old).energy() == 0
            && GiantPlanetData.fromTag(brown(32).toTag()).energy() == 32, "Giant energy supports old and new data");
        helper.succeed();
    }

    private static void transformation(GameTestHelper helper) {
        var fixture = fixture(helper);
        var be = fixture.be;
        be.addToSearchHistory(be.getCelestialBodyData(), be.getPlanetaryResourceSet());
        be.getMaterialContainer().setItem(0, ModItems.DYSON_SPHERE_COMPONENT.asStack(8));
        var options = be.getClientVisibleOptions();
        int index = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equals(ModMegastructures.DYSON_SPHERE_BROWN_DWARF.getId())) index = i;
        }
        helper.assertTrue(index >= 0, "Brown dwarf offers the ring-two Dyson sphere");
        be.buildMegastructure(index);
        helper.assertTrue(be.getMaterialContainer().getItem(0).isEmpty(), "Actual build consumes eight components");
        var handler = (DysonSphereHandler) be.getMegastructureManager().getActiveHandler(be);
        long remaining = 12800000;
        while (remaining > 1) {
            int excess = (int) Math.min(78000, remaining - 1);
            feed(fixture, handler, excess + 2000);
            remaining -= excess;
        }
        helper.assertTrue(be.getCelestialBodyData() instanceof GiantPlanetData, "No conversion one mB below threshold");
        feed(fixture, handler, 2001);
        helper.assertTrue(be.getCelestialBodyData() instanceof StarData, "Conversion occurs at 12800 buckets of excess");
        var star = (StarData) be.getCelestialBodyData();
        helper.assertTrue(star.specialRedDwarf() && star.bodyClass() == CelestialBodyClass.M_MAIN
            && star.size() == 64 && star.energy() == 32 && star.axialTilt() == 13 && star.rotationSpeed() == 2,
            "Transformation preserves source physical values and marks the special red dwarf");
        helper.assertTrue(star.bodyUuid().equals(StarData.uuidFromBodySeed(be.getBodySeed())), "Stable body-seed identity");
        helper.assertTrue(be.isAmplify() && be.isLocked() && !be.hasActiveMegastructure() && be.getSearchHistory().isEmpty(),
            "Transformation removes brown sphere and history, locks the stellar mode");
        helper.assertTrue(!star.usesLargeStellarRings(), "Special red dwarf uses small stellar rings even at size 64");
        var next = CelestialRefactorRegistry.getOptions(star, true, be.getPlanetaryResourceSet());
        helper.assertTrue(next.stream().anyMatch(option -> option.id().equals(ModMegastructures.DYSON_SPHERE_SMALL.getId()))
            && next.stream().noneMatch(option -> option.id().equals(ModMegastructures.STELLAR_EVOLUTION_ACCELERATOR.getId())),
            "Special red dwarf offers a small Dyson sphere and excludes evolution");
        be.setAmplify(false);
        be.normalizeRedDwarfState();
        helper.assertTrue(be.isAmplify(), "Old special-red-dwarf saves normalize back to stellar mode");
        be.normalizeRedDwarfState();
        helper.assertTrue(be.getCelestialBodyData() == star, "Normalization is idempotent");
        helper.succeed();
    }
}
