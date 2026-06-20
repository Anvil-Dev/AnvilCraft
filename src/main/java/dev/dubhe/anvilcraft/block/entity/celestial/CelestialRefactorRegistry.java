package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry that maps celestial bodies to their possible Celestial Restriction Ring refactoring options.
 *
 * <p>
 * The innermost ring for each body type:
 * <ul>
 *   <li>Small rocky planets (size &lt; 26): innermost = R1</li>
 *   <li>Small giant planets (size &lt; 26): innermost = R2</li>
 *   <li>Small stars (size &lt; 26): innermost = R4</li>
 *   <li>Large stars (size &gt;= 26): innermost = R5</li>
 * </ul>
 *
 * <p>
 * There are 11 unique megastructures total, distributed across different rings.
 * Variant models (e.g. excavator_off, coil_fix) are rendered in-world separately;
 * in the UI only the main model is shown.
 */
public final class CelestialRefactorRegistry {

    private CelestialRefactorRegistry() {
    }

    /**
     * Get the innermost ring index for a given celestial body.
     *
     * <p>
     * With amplification, the minimum ring is always 4 (stellar-scale).
     */
    public static int getInnermostRing(CelestialBodyData body, boolean amplified) {
        boolean isLarge = body.size() >= 26;
        int ring = switch (body) {
            case StarData ignored -> isLarge ? 5 : 4;
            case GiantPlanetData ignored -> 2;
            case RockyPlanetData ignored -> 1;
            case SpecialCelestialBodyData s -> s.specialType().isErrorPlanet() ? 0 : 1;
        };
        if (amplified) {
            ring = Math.max(ring, 4);
        }
        return ring;
    }

    /**
     * Get available refactoring options for a locked celestial body.
     *
     * <p>
     * Non-amplified CFA has rings 1-3 → shows ring 1+2 megastructures.<br>
     * Amplified CFA has rings 3-5 → shows ring 4+5 megastructures.
     *
     * @param resources the planetary resource set, used to filter options by resource availability;
     *                  may be null (most permissive, all ring-eligible options shown)
     */
    public static List<CelestialRefactorOption> getOptions(CelestialBodyData body, boolean amplified,
                                                           @Nullable PlanetaryResourceSet resources) {
        if (body == null) return Collections.emptyList();
        // Error Planet cannot build megastructures
        if (body instanceof SpecialCelestialBodyData s && s.specialType().isErrorPlanet()) {
            return Collections.emptyList();
        }
        int innermostRing = getInnermostRing(body, amplified);
        int maxRing = amplified ? 5 : 2;
        List<CelestialRefactorOption> options = getOptionsForRing(innermostRing, maxRing);

        // Filter planet_exctractor: rocky/special planet must have liquid
        if (!hasLiquid(body)) {
            options.removeIf(opt -> "planet_exctractor".equals(opt.megastructure()));
        }

        // Filter giant_planet_exctractor: only available for giant planets
        if (!(body instanceof GiantPlanetData)) {
            options.removeIf(opt -> "giant_planet_exctractor".equals(opt.megastructure()));
        }

        // Filter stellar_ring_collider: only available for small stellar bodies (size < 26)
        if (!(body instanceof StarData star && star.size() < 26)) {
            options.removeIf(opt -> "stellar_ring_collider".equals(opt.megastructure()));
        }

        // Filter Dyson Sphere: small for small stars, large for large stars
        if (body instanceof StarData star) {
            boolean isLarge = star.size() >= 26;
            options.removeIf(opt -> "dyson_sphere_small".equals(opt.megastructure()) && isLarge);
            options.removeIf(opt -> "dyson_sphere_large".equals(opt.megastructure()) && !isLarge);
        }

        // Filter eco_station: requires biological resources and no low-level civilization
        if (resources != null) {
            options.removeIf(opt -> "eco_station".equals(opt.megastructure())
                && !isEcoStationEligible(resources));
            // Filter temple: requires low-level civilization
            options.removeIf(opt -> "temple".equals(opt.megastructure())
                && !resources.hasCivilization());
        }

        return options;
    }

    /**
     * Eco station is only eligible when the planet has biological resources
     * and does NOT have a low-level civilization.
     */
    private static boolean hasLiquid(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rocky) return rocky.liquidCoverage() != LiquidCoverage.NONE;
        if (body instanceof SpecialCelestialBodyData s) {
            LiquidCoverage lc = s.liquidCoverage();
            return lc != null && lc != LiquidCoverage.NONE;
        }
        return false;
    }

    private static boolean isEcoStationEligible(PlanetaryResourceSet resources) {
        if (resources.hasCivilization()) return false;
        return !resources.getBiologicalItems().isEmpty()
            || !resources.getBiologicalFluids().isEmpty();
    }

    /**
     * Get available megastructure options for a ring range [innermostRing, maxRing].
     * Inner rings can build any megastructure that outer rings can.
     */
    public static List<CelestialRefactorOption> getOptionsForRing(int innermostRing, int maxRing) {
        List<CelestialRefactorOption> options = new ArrayList<>();
        String prefix = "screen.anvilcraft.cfa.megastructure.";

        if (innermostRing <= 1 && 1 <= maxRing) {
            // Ring 1 megastructures (innermost for small rocky planets)
            options.add(CelestialRefactorOption.withMaterial(1, "planet_excavator",
                ringModel(1, "excavator"), prefix + "planet_excavator",
                ModBlocks.RUBY_PRISM.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(1, "planet_exctractor",
                ringModel(1, "exctractor"), prefix + "planet_exctractor",
                ModBlocks.FLUID_TANK.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(1, "eco_station",
                ringModel(1, "eco_station"), prefix + "eco_station",
                ModBlocks.TEMPERING_GLASS.asItem(), 64));
            options.add(CelestialRefactorOption.withMaterial(1, "temple",
                ringModel(1, "temple"), prefix + "temple",
                net.minecraft.world.item.Items.GOLD_BLOCK, 64));
        }
        if (innermostRing <= 2 && 2 <= maxRing) {
            // Ring 2 megastructures (innermost for small giant planets)
            options.add(CelestialRefactorOption.withMaterial(2, "giant_planet_exctractor",
                ringModel(2, "exctractor"), prefix + "giant_planet_exctractor",
                ModBlocks.FLUID_TANK.asItem(), 32));
        }
        if (innermostRing <= 4 && 4 <= maxRing) {
            // Ring 4 megastructures (innermost for small stars)
            options.add(CelestialRefactorOption.withMaterial(4, "stellar_ring_collider",
                ringModel(4, "collider"), prefix + "stellar_ring_collider",
                ModBlocks.ACCELERATION_RING.asItem(), 16));
            options.add(CelestialRefactorOption.withMaterial(4, "dyson_sphere_small",
                ringModel(4, "dyson_sphere"), prefix + "dyson_sphere_small",
                ModItems.TRANSCENDIUM_INGOT, 16));
            options.add(CelestialRefactorOption.noMaterial(4, "magnetar_coil",
                ringModel(4, "coil"), prefix + "magnetar_coil"));
            options.add(CelestialRefactorOption.noMaterial(4, "penrose_sphere",
                ringModel(4, "penrose_sphere"), prefix + "penrose_sphere"));
            options.add(CelestialRefactorOption.noMaterial(4, "matter_decompressor",
                ringModel(4, "matter_decompressor"), prefix + "matter_decompressor"));
        }
        if (innermostRing <= 5 && 5 <= maxRing) {
            // Ring 5 megastructures (innermost for large stars)
            options.add(CelestialRefactorOption.withMaterial(5, "dyson_sphere_large",
                ringModel(5, "dyson_sphere"), prefix + "dyson_sphere_large",
                ModItems.TRANSCENDIUM_INGOT, 32));
        }
        return options;
    }

    private static ModelResourceLocation ringModel(int ring, String megastructure) {
        return ModelResourceLocation.standalone(
            AnvilCraft.of("block/celestial_forging_anvil_ring_" + ring + "_" + megastructure)
        );
    }
}
