package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/** Resolves registered megastructures for a Celestial Forging Anvil context. */
public final class CelestialRefactorRegistry {
    private CelestialRefactorRegistry() {
    }

    /**
     * Returns the options available for a locked celestial body.
     *
     * <p>Every definition is evaluated independently.  This removes the old
     * chain of name-based filters and lets add-ons contribute prerequisites,
     * ring variants and materials through the registry.</p>
     */
    public static List<CelestialRefactorOption> getOptions(
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (body == null) return Collections.emptyList();
        if (!hasRegisteredDefinitions()) {
            return legacyOptions(body, amplified, resources);
        }

        Megastructure.Context context = new Megastructure.Context(body, amplified, resources);
        List<CelestialRefactorOption> options = new ArrayList<>();
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.isAvailable(context)) continue;
            int ring = definition.ring(context);
            if (!isRingSupported(ring, amplified)) continue;
            options.add(CelestialRefactorOption.resolve(definition, context));
        }
        return options;
    }

    /** Resolves one registered option by stable ID for persisted CFA state. */
    public static @Nullable CelestialRefactorOption getOption(
        ResourceLocation id,
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (body == null) return null;
        Megastructure definition = get(id);
        if (definition != null) {
            Megastructure.Context context = new Megastructure.Context(body, amplified, resources);
            // Availability is a build-time condition. An already-built
            // structure remains resolvable while the celestial body evolves.
            return CelestialRefactorOption.resolve(definition, context);
        }
        // Legacy/fallback registries may not expose a Holder for the ID.
        for (CelestialRefactorOption option : getOptions(body, amplified, resources)) {
            if (id.equals(option.id())) return option;
        }
        return null;
    }

    /** Returns a definition by ID, or {@code null} when an add-on was removed. */
    public static @Nullable Megastructure get(ResourceLocation id) {
        return ModRegistries.MEGASTRUCTURE.get(id);
    }

    /**
     * Maps the old name/ring pair used by 1.21 NBT to a stable definition ID.
     */
    public static @Nullable ResourceLocation findLegacyId(
        String name,
        int ring,
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        Megastructure.Context context = body == null ? null : new Megastructure.Context(body, amplified, resources);
        ResourceLocation nameMatch = null;
        for (Megastructure definition : ModRegistries.MEGASTRUCTURE) {
            if (!definition.name().equals(name)) continue;
            nameMatch = definition.id();
            if (context != null && definition.ring(context) == ring) return definition.id();
        }
        if (nameMatch != null) return nameMatch;
        for (CelestialRefactorOption option : getOptionsForRing(0, 6)) {
            if (option.megastructure().equals(name) && option.ring() == ring) return option.id();
        }
        return null;
    }

    /**
     * Resolves the numeric option index stored by pre-registry 1.21 worlds.
     * The old index is tied to the former hard-coded option order and must not
     * be interpreted using the iteration order of the extensible registry.
     */
    public static @Nullable ResourceLocation findLegacyIdByIndex(
        int index,
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (index < 0 || body == null) return null;
        List<CelestialRefactorOption> options = legacyOptions(body, amplified, resources);
        return index < options.size() ? options.get(index).id() : null;
    }

    /** Compatibility helper retained for blueprint/UI code and add-ons. */
    public static int getInnermostRing(CelestialBodyData body, boolean amplified) {
        boolean isLarge = body.size() >= 48;
        int ring = switch (body) {
            case StarData star -> star.specialRedDwarf() ? 2 : (isLarge ? 5 : 4);
            case GiantPlanetData ignored -> 2;
            case RockyPlanetData ignored -> 1;
            case SpecialCelestialBodyData special -> special.isErrorPlanet() ? 0 : 1;
        };
        return amplified ? Math.max(ring, 4) : ring;
    }

    /**
     * Compatibility view of the old hard-coded list.  New code should use
     * {@link #getOptions(CelestialBodyData, boolean, PlanetaryResourceSet)}.
     */
    public static List<CelestialRefactorOption> getOptionsForRing(int innermostRing, int maxRing) {
        List<CelestialRefactorOption> options = new ArrayList<>();
        String prefix = "screen.anvilcraft.cfa.megastructure.";
        if (innermostRing <= 1 && 1 <= maxRing) {
            options.add(CelestialRefactorOption.withMaterial(1, "planet_excavator", ringModel(1, "excavator"),
                prefix + "planet_excavator", ModBlocks.RUBY_PRISM.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(1, "planet_exctractor", ringModel(1, "exctractor"),
                prefix + "planet_exctractor", ModBlocks.PUMP.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(1, "eco_station", ringModel(1, "eco_station"),
                prefix + "eco_station", ModBlocks.TEMPERING_GLASS.asItem(), 64));
            options.add(CelestialRefactorOption.withMaterial(1, "temple", ringModel(1, "temple"),
                prefix + "temple", ModBlocks.ENCHANTED_GOLD_BLOCK.asItem(), 64));
        }
        if (innermostRing <= 2 && 2 <= maxRing) {
            options.add(CelestialRefactorOption.withMaterial(2, "giant_planet_exctractor",
                ringModel(2, "exctractor"), prefix + "giant_planet_exctractor", ModBlocks.PUMP.asItem(), 32));
            options.add(CelestialRefactorOption.withMaterial(2, "dyson_sphere_brown_dwarf",
                ringModel(2, "dyson_sphere"), prefix + "dyson_sphere_brown_dwarf",
                ModItems.DYSON_SPHERE_COMPONENT.get(), 8));
        }
        if (innermostRing <= 4 && 4 <= maxRing) {
            options.add(CelestialRefactorOption.withMaterial(4, "stellar_ring_collider", ringModel(4, "collider"),
                prefix + "stellar_ring_collider", ModItems.STELLAR_RING_COMPONENT.get(), 8));
            options.add(CelestialRefactorOption.withMaterial(4, "dyson_sphere_small", ringModel(4, "dyson_sphere"),
                prefix + "dyson_sphere_small", ModItems.DYSON_SPHERE_COMPONENT.get(), 16));
            options.add(CelestialRefactorOption.withMaterial(4, "magnetar_coil", ringModel(4, "coil"),
                prefix + "magnetar_coil", ModItems.MAGNETAR_COIL_COMPONENT.get(), 4));
            options.add(CelestialRefactorOption.withMaterial(4, "penrose_sphere", ringModel(4, "penrose_sphere"),
                prefix + "penrose_sphere", ModItems.PENROSE_SPHERE_COMPONENT.get(), 8));
            options.add(CelestialRefactorOption.withMaterial(4, "matter_decompressor",
                ringModel(4, "matter_decompressor"), prefix + "matter_decompressor",
                ModItems.MATTER_DECOMPRESSOR_COMPONENT.get(), 2));
            options.add(CelestialRefactorOption.withMaterial(4, "wormhole_stabilizer",
                ringModel(4, "wormhole_stabilizer"), prefix + "wormhole_stabilizer",
                ModItems.WORMHOLE_STABILIZER_COMPONENT.get(), 4));
            options.add(CelestialRefactorOption.withMaterial(5, "stellar_evolution_accelerator",
                ringModel(5, "stellar_evolution_accelerator"), prefix + "stellar_evolution_accelerator",
                ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT.get(), 8));
        }
        if (innermostRing <= 5 && 5 <= maxRing) {
            options.add(CelestialRefactorOption.withMaterial(5, "dyson_sphere_large", ringModel(5, "dyson_sphere"),
                prefix + "dyson_sphere_large", ModItems.DYSON_SPHERE_COMPONENT.get(), 32));
            options.add(CelestialRefactorOption.withMaterial(6, "stellar_evolution_accelerator",
                ringModel(6, "stellar_evolution_accelerator"), prefix + "stellar_evolution_accelerator",
                ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT.get(), 8));
        }
        return options;
    }

    private static boolean hasRegisteredDefinitions() {
        return ModRegistries.MEGASTRUCTURE.iterator().hasNext();
    }

    private static boolean isRingSupported(int ring, boolean amplified) {
        return amplified ? ring >= 4 && ring <= 6 : ring >= 1 && ring <= 2;
    }

    private static List<CelestialRefactorOption> legacyOptions(
        CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (body instanceof SpecialCelestialBodyData special && special.isErrorPlanet()) {
            return Collections.emptyList();
        }
        int innermostRing = getInnermostRing(body, amplified);
        int maxRing = amplified ? 6 : 2;
        List<CelestialRefactorOption> options = getOptionsForRing(innermostRing, maxRing);
        if (!hasLiquid(body)) options.removeIf(option -> "planet_exctractor".equals(option.megastructure()));
        if (!(body instanceof GiantPlanetData)) {
            options.removeIf(option -> "giant_planet_exctractor".equals(option.megastructure()));
        }
        if (!((body instanceof GiantPlanetData brown && brown.brownDwarf())
            || (body instanceof StarData star && star.specialRedDwarf()))) {
            options.removeIf(option -> "dyson_sphere_brown_dwarf".equals(option.megastructure()));
        }
        if (!(body instanceof StarData star && star.size() < 48
            && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
            && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(option -> "stellar_ring_collider".equals(option.megastructure()));
        }
        if (body instanceof StarData star && (star.specialRedDwarf() || star.bodyClass() == CelestialBodyClass.WHITE_DWARF
            || star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
            || star.bodyClass() == CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(option -> "stellar_evolution_accelerator".equals(option.megastructure()));
        }
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.NEUTRON_STAR)) {
            options.removeIf(option -> "magnetar_coil".equals(option.megastructure()));
        }
        if (body instanceof StarData star) {
            boolean large = star.size() >= 48;
            options.removeIf(option -> "stellar_evolution_accelerator".equals(option.megastructure())
                && ((large && option.ring() == 5) || (!large && option.ring() == 6)));
        }
        if (!(body instanceof StarData star
            && star.bodyClass() != CelestialBodyClass.NEUTRON_STAR
            && star.bodyClass() != CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(option -> "dyson_sphere_small".equals(option.megastructure())
                || "dyson_sphere_large".equals(option.megastructure()));
        } else {
            boolean large = star.size() >= 48;
            options.removeIf(option -> "dyson_sphere_small".equals(option.megastructure()) && large);
            options.removeIf(option -> "dyson_sphere_large".equals(option.megastructure()) && !large);
        }
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE)) {
            options.removeIf(option -> "penrose_sphere".equals(option.megastructure()));
        }
        if (!(body instanceof StarData star && star.bodyClass() == CelestialBodyClass.BLACK_HOLE && amplified)) {
            options.removeIf(option -> "wormhole_stabilizer".equals(option.megastructure()));
        }
        if (!(body instanceof StarData star && (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
            || star.bodyClass() == CelestialBodyClass.BLACK_HOLE))) {
            options.removeIf(option -> "matter_decompressor".equals(option.megastructure()));
        }
        if (resources != null) {
            options.removeIf(option -> "eco_station".equals(option.megastructure()) && !isEcoStationEligible(resources));
            options.removeIf(option -> "temple".equals(option.megastructure()) && !resources.hasCivilization());
        }
        return options;
    }

    private static boolean hasLiquid(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rocky) return rocky.liquidCoverage() != LiquidCoverage.NONE;
        if (body instanceof SpecialCelestialBodyData special) {
            LiquidCoverage coverage = special.liquidCoverage();
            return coverage != null && coverage != LiquidCoverage.NONE;
        }
        return false;
    }

    private static boolean isEcoStationEligible(PlanetaryResourceSet resources) {
        return !resources.hasCivilization()
            && (!resources.getBiologicalItems().isEmpty() || !resources.getBiologicalFluids().isEmpty());
    }

    private static ResourceLocation ringModel(int ring, String megastructure) {
        return AnvilCraft.of("block/celestial_forging_anvil_ring_" + ring + "_" + megastructure);
    }
}
