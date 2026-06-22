package dev.dubhe.anvilcraft.block.entity.celestial;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of all celestial body classes identifiable from the mass-radius diagram.
 * Each constant stores its diagram pixel color (RGB) and classification flags.
 */
public enum CelestialBodyClass {
    // === Planetary (no amplifier needed) ===
    LARGE_MOON(0x999966, false, false, false),
    ROCKY_NO_LIQUID(0x669933, false, false, false),
    ROCKY_LOW_LIQUID(0x339933, false, false, false),
    ROCKY_MED_LIQUID(0x339999, false, false, false),
    ROCKY_HIGH_LIQUID(0x33CCCC, false, false, false),
    ICE_GIANT(0x336699, false, false, false),
    GAS_GIANT(0x666699, false, false, false),
    BROWN_DWARF(0x330000, false, false, true),

    // === Main sequence stars ===
    M_MAIN(0x660000, true, true, false),
    K_MAIN(0xCC6600, true, true, false),
    G_MAIN(0xCC9933, true, true, false),
    F_MAIN(0xCCCC66, true, true, false),
    A_MAIN(0xCCCCCC, true, true, false),
    B_MAIN(0x66CCCC, true, true, false),
    O_MAIN(0x0066CC, true, true, false),

    // === Red giants ===
    M_GIANT(0x990000, true, false, true),
    K_GIANT(0xFF6600, true, false, true),
    G_GIANT(0xFFCC00, true, false, true),
    F_GIANT(0xFFFF66, true, false, true),

    // === Blue giants ===
    A_GIANT(0xCCFFCC, true, false, true),
    B_GIANT(0x66FFFF, true, false, true),
    O_GIANT(0x0099FF, true, false, true),

    // === Red supergiants ===
    M_SUPERGIANT(0xFF0000, true, false, true),
    K_SUPERGIANT(0xFF9900, true, false, true),
    G_SUPERGIANT(0xFFCC66, true, false, true),
    F_SUPERGIANT(0xFFFF99, true, false, true),

    // === Blue supergiants ===
    A_SUPERGIANT(0xFFFFFF, true, false, true),
    B_SUPERGIANT(0x99FFFF, true, false, true),
    O_SUPERGIANT(0x33CCFF, true, false, true),

    // === White dwarf ===
    WHITE_DWARF(0x666666, true, false, true),

    // === Stellar remnants (special rendering) ===
    NEUTRON_STAR(0x000001, true, false, false),
    BLACK_HOLE(0x000002, true, false, false);

    private final int rgb;
    /**
     * -- GETTER --
     * Whether this body requires amplifier mode.
     */
    @Getter
    private final boolean stellar;
    /**
     * -- GETTER --
     * Main sequence stars — step 2 uses age_temp, step 3 needed.
     */
    @Getter
    private final boolean mainSequence;
    private final boolean step2UsesSp;

    private static final Map<Integer, CelestialBodyClass> BY_RGB = new HashMap<>();

    static {
        for (CelestialBodyClass c : values()) {
            BY_RGB.put(c.rgb, c);
        }
    }

    CelestialBodyClass(int rgb, boolean stellar, boolean mainSequence, boolean step2UsesSp) {
        this.rgb = rgb;
        this.stellar = stellar;
        this.mainSequence = mainSequence;
        this.step2UsesSp = step2UsesSp;
    }

    public int rgb() {
        return rgb;
    }

    /**
     * Whether step 2 uses age_temp_sp instead of age_temp.
     */
    public boolean step2UsesSp() {
        return step2UsesSp;
    }

    /**
     * Whether step 3 (age_radius lookup) is needed.
     */
    public boolean needsStep3() {
        return stellar || this == BROWN_DWARF;
    }

    /**
     * Whether this is an extreme compact object (black hole or neutron star)
     * that requires a singularity crystal to store.
     */
    public boolean isExtreme() {
        return this == BLACK_HOLE || this == NEUTRON_STAR;
    }

    /**
     * Planetary bodies (including brown dwarf, excluding large moon).
     */
    public boolean isPlanetary() {
        return !stellar;
    }

    /**
     * Rocky planet types (for step 2 special matching).
     */
    public boolean isRockyPlanet() {
        return this == ROCKY_NO_LIQUID || this == ROCKY_LOW_LIQUID
               || this == ROCKY_MED_LIQUID || this == ROCKY_HIGH_LIQUID;
    }

    /**
     * The accepted step 2 color for this body class. Rocky planets all use ROCKY_LOW_LIQUID rgb.
     */
    public int step2MatchRgb() {
        return isRockyPlanet() ? ROCKY_LOW_LIQUID.rgb : rgb;
    }

    @Nullable
    public static CelestialBodyClass fromRgb(int rgb) {
        return BY_RGB.get(rgb);
    }
}
