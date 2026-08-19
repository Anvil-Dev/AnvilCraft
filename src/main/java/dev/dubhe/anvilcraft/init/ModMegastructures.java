package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.Megastructure;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.megastructure.AcceleratorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.ColliderHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.DysonSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.EcoStationHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExcavatorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExtractorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.GiantExtractorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.MagnetarCoilHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.MatterDecompressorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.PenroseSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.TempleHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import javax.annotation.Nullable;

/** Built-in Celestial Forging Anvil megastructure definitions. */
public final class ModMegastructures {
    private static final DeferredRegister<Megastructure> REGISTER = DeferredRegister.create(
        ModRegistryKeys.MEGASTRUCTURE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<Megastructure, Megastructure> PLANET_EXCAVATOR = register(
        "planet_excavator",
        id -> Megastructure.builder(id, "planet_excavator")
            .prerequisite(context -> isPlanet(context) && !isErrorPlanet(context))
            .ring(1)
            .model(1, ringModel(1, "excavator"))
            .material(ModBlocks.RUBY_PRISM.asItem(), 16)
            .handler(ExcavatorHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> PLANET_EXTRACTOR = register(
        "planet_exctractor",
        id -> Megastructure.builder(id, "planet_exctractor")
            .prerequisite(context -> isPlanet(context) && !isErrorPlanet(context) && hasLiquid(context))
            .ring(1)
            .model(1, ringModel(1, "exctractor"))
            .material(ModBlocks.PUMP.asItem(), 16)
            .handler(ExtractorHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> ECO_STATION = register(
        "eco_station",
        id -> Megastructure.builder(id, "eco_station")
            .prerequisite(context -> isPlanet(context)
                && !isErrorPlanet(context)
                && isEcoStationEligible(context.resources()))
            .ring(1)
            .model(1, ringModel(1, "eco_station"))
            .material(ModBlocks.TEMPERING_GLASS.asItem(), 64)
            .handler(EcoStationHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> TEMPLE = register(
        "temple",
        id -> Megastructure.builder(id, "temple")
            .prerequisite(context -> isPlanet(context)
                && !isErrorPlanet(context)
                && (context.resources() == null || context.resources().hasCivilization()))
            .ring(1)
            .model(1, ringModel(1, "temple"))
            .material(ModBlocks.ENCHANTED_GOLD_BLOCK.asItem(), 64)
            .handler(TempleHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> GIANT_PLANET_EXTRACTOR = register(
        "giant_planet_exctractor",
        id -> Megastructure.builder(id, "giant_planet_exctractor")
            .prerequisite(context -> context.body() instanceof GiantPlanetData)
            .ring(2)
            .model(2, ringModel(2, "exctractor"))
            .material(ModBlocks.PUMP.asItem(), 32)
            .handler(GiantExtractorHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> DYSON_SPHERE_BROWN_DWARF = register(
        "dyson_sphere_brown_dwarf",
        id -> Megastructure.builder(id, "dyson_sphere_brown_dwarf")
            .prerequisite(ModMegastructures::isBrownDwarfOrRemnant)
            .ring(2)
            .rotation(ModMegastructures::bodySynchronizedRotation)
            .model(2, ringModel(2, "dyson_sphere"))
            .material(ModItems.DYSON_SPHERE_COMPONENT.get(), 8)
            .handler(() -> new DysonSphereHandler("dyson_sphere_brown_dwarf"))
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> STELLAR_RING_COLLIDER = register(
        "stellar_ring_collider",
        id -> Megastructure.builder(id, "stellar_ring_collider")
            .prerequisite(context -> context.body() instanceof StarData star
                && star.size() < 48
                && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
                && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)
            .ring(4)
            .model(4, ringModel(4, "collider"))
            .material(ModItems.STELLAR_RING_COMPONENT.get(), 8)
            .handler(ColliderHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> DYSON_SPHERE_SMALL = register(
        "dyson_sphere_small",
        id -> Megastructure.builder(id, "dyson_sphere_small")
            .prerequisite(context -> isOrdinaryStar(context, false))
            .ring(4)
            .rotation(ModMegastructures::bodySynchronizedRotation)
            .model(4, ringModel(4, "dyson_sphere"))
            .material(ModItems.DYSON_SPHERE_COMPONENT.get(), 16)
            .handler(() -> new DysonSphereHandler("dyson_sphere_small"))
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> DYSON_SPHERE_LARGE = register(
        "dyson_sphere_large",
        id -> Megastructure.builder(id, "dyson_sphere_large")
            .prerequisite(context -> isOrdinaryStar(context, true))
            .ring(5)
            .rotation(ModMegastructures::bodySynchronizedRotation)
            .model(5, ringModel(5, "dyson_sphere"))
            .material(ModItems.DYSON_SPHERE_COMPONENT.get(), 32)
            .handler(() -> new DysonSphereHandler("dyson_sphere_large"))
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> MAGNETAR_COIL = register(
        "magnetar_coil",
        id -> Megastructure.builder(id, "magnetar_coil")
            .prerequisite(context -> context.body() instanceof StarData star
                && star.bodyClass() == CelestialBodyClass.NEUTRON_STAR)
            .ring(4)
            .model(4, ringModel(4, "coil"))
            .material(ModItems.MAGNETAR_COIL_COMPONENT.get(), 4)
            .handler(MagnetarCoilHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> PENROSE_SPHERE = register(
        "penrose_sphere",
        id -> Megastructure.builder(id, "penrose_sphere")
            .prerequisite(ModMegastructures::isBlackHole)
            .ring(4)
            .rotation(context -> -bodySynchronizedRotation(context))
            .model(4, ringModel(4, "penrose_sphere"))
            .material(ModItems.PENROSE_SPHERE_COMPONENT.get(), 8)
            .handler(PenroseSphereHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> MATTER_DECOMPRESSOR = register(
        "matter_decompressor",
        id -> Megastructure.builder(id, "matter_decompressor")
            .prerequisite(context -> context.body() instanceof StarData star
                && (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                    || star.bodyClass() == CelestialBodyClass.BLACK_HOLE))
            .ring(4)
            .model(4, ringModel(4, "matter_decompressor"))
            .material(ModItems.MATTER_DECOMPRESSOR_COMPONENT.get(), 2)
            .handler(MatterDecompressorHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> WORMHOLE_STABILIZER = register(
        "wormhole_stabilizer",
        id -> Megastructure.builder(id, "wormhole_stabilizer")
            .prerequisite(context -> context.amplified() && isBlackHole(context))
            .ring(4)
            .model(4, ringModel(4, "wormhole_stabilizer"))
            .material(ModItems.WORMHOLE_STABILIZER_COMPONENT.get(), 4)
            .handler(WormholeStabilizerHandler::new)
            .build()
    );
    public static final DeferredHolder<Megastructure, Megastructure> STELLAR_EVOLUTION_ACCELERATOR = register(
        "stellar_evolution_accelerator",
        id -> Megastructure.builder(id, "stellar_evolution_accelerator")
            .prerequisite(context -> context.body() instanceof StarData star
                && !star.specialRedDwarf()
                && star.bodyClass() != CelestialBodyClass.WHITE_DWARF
                && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
                && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)
            .ring(context -> context.body().size() >= 48 ? 6 : 5)
            .model(5, ringModel(5, "stellar_evolution_accelerator"))
            .model(6, ringModel(6, "stellar_evolution_accelerator"))
            .material(ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT.get(), 8)
            .handler(AcceleratorHandler::new)
            .auxiliary()
            .build()
    );

    private ModMegastructures() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    private static DeferredHolder<Megastructure, Megastructure> register(
        String name,
        Function<ResourceLocation, Megastructure> factory
    ) {
        ResourceLocation id = AnvilCraft.of(name);
        return REGISTER.register(name, () -> factory.apply(id));
    }

    private static ResourceLocation ringModel(int ring, String name) {
        return AnvilCraft.of("block/celestial_forging_anvil_ring_" + ring + "_" + name);
    }

    private static boolean isPlanet(Megastructure.Context context) {
        return context.body() instanceof RockyPlanetData || context.body() instanceof SpecialCelestialBodyData;
    }

    private static boolean isErrorPlanet(Megastructure.Context context) {
        return context.body() instanceof SpecialCelestialBodyData special && special.isErrorPlanet();
    }

    private static boolean hasLiquid(Megastructure.Context context) {
        if (context.body() instanceof RockyPlanetData rocky) {
            return rocky.liquidCoverage() != LiquidCoverage.NONE;
        }
        if (context.body() instanceof SpecialCelestialBodyData special) {
            LiquidCoverage coverage = special.liquidCoverage();
            return coverage != null && coverage != LiquidCoverage.NONE;
        }
        return false;
    }

    private static boolean isEcoStationEligible(@Nullable PlanetaryResourceSet resources) {
        if (resources == null) return true;
        if (resources.hasCivilization()) return false;
        return !resources.getBiologicalItems().isEmpty() || !resources.getBiologicalFluids().isEmpty();
    }

    private static boolean isOrdinaryStar(Megastructure.Context context, boolean large) {
        return context.body() instanceof StarData star
            && (star.size() >= 48) == large
            && !star.specialRedDwarf()
            && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
            && star.bodyClass() != CelestialBodyClass.BLACK_HOLE;
    }

    private static boolean isBrownDwarfOrRemnant(Megastructure.Context context) {
        if (context.amplified()) return false;
        if (context.body() instanceof GiantPlanetData brown) return brown.brownDwarf();
        return context.body() instanceof StarData star
            && star.specialRedDwarf()
            && star.bodyClass() == CelestialBodyClass.M_MAIN;
    }

    private static boolean isBlackHole(Megastructure.Context context) {
        return context.body() instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE;
    }

    private static float bodySynchronizedRotation(Megastructure.RotationContext context) {
        if (context.body() instanceof StarData star) {
            return context.bodyRotation() * CelestialBodyData.getVisualRotationSpeed(star.rotationSpeed());
        }
        return context.baseRotation();
    }
}
