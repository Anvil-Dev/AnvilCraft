package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.util.TranslatableEnum;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.fml.config.ModConfig;

@Config(name = AnvilCraft.MOD_ID, type = ModConfig.Type.CLIENT)
public class AnvilCraftClientConfig {
    @Comment("Vanilla restores the original translucent atmosphere shell; Standard enables a thicker volume atmosphere. "
        + "Rendering failures fall back to Vanilla until resources reload. Changes take effect immediately.")
    public CelestialRenderingMode planetAtmosphereRenderingMode = CelestialRenderingMode.STANDARD;

    @Comment("Vanilla restores the original stellar shells; Standard enables emissive surfaces and an exterior corona. "
        + "Rendering failures fall back to Vanilla until resources reload. Changes take effect immediately.")
    public CelestialRenderingMode stellarRenderingMode = CelestialRenderingMode.STANDARD;

    public enum CelestialRenderingMode implements TranslatableEnum {
        @SerializedName("Vanilla")
        VANILLA,
        @SerializedName("Standard")
        STANDARD
    }

    @Comment("Show levels above 10 as Roman numerals in auto enchanting table's liquid enchantment mode")
    public boolean liquidEnchantmentRomanNumerals = true;

    @Comment("Building rod blueprint controls")
    public BuildingRodControls buildingRodControls = BuildingRodControls.OPTIMIZED;

    public enum BuildingRodControls implements TranslatableEnum {
        TRADITIONAL,
        OPTIMIZED
    }

    @Comment("Swap insert/collect to left-click and keep extract/place on right-click (Left Collect, Right Place)")
    public boolean invertOverrideAction = false;

    @Comment("The mode of the anvil hammer goggle info")
    public GoggleMode goggleMode = GoggleMode.WEARING_OR_HOLDING_HAMMER;

    @Comment("Scale of the anvil hammer radial menu")
    @BoundedDiscrete(min = 0.5, max = 2.0)
    public float anvilHammerRadialMenuScale = 1.0F;

    @Comment("Render distance of heliostats block entity")
    @BoundedDiscrete(min = 32, max = 512)
    public int heliostatsRenderDistance = 128;

    @Comment("Heliostats render sunflower head model in Sunflower Plains biome")
    public boolean heliostatsSunflowerModel = true;

    @Comment("Do not render power component tooltip when jade present")
    public boolean doNotShowTooltipWhenJadePresent = false;

    @Comment("Render lines between power transmitters")
    public boolean renderPowerTransmitterLines = true;

    @Comment("Bloom effect on laser and power transmitter lines.")
    public boolean renderBloomEffect = true;

    @Comment("Scanline post-processing effect on 3D structure previews.")
    public boolean renderScanPreviewEffect = true;

    @CollapsibleObject
    public GravitationalLens gravitationalLens = new GravitationalLens();

    public static class GravitationalLens {
        @Comment("Gravitational lensing post-processing effect near black holes")
        public boolean renderBlackHoleLensing = true;

        @Comment("Maximum number of black/white holes rendered (2-256). Higher = more holes, lower = better performance.")
        @BoundedDiscrete(min = 2, max = 256)
        public int maxHoleCount = 8;

        @Comment("Lens distortion strength (higher = stronger bending, 0.002 default)")
        public double lensStrength = 1.0 / 512.0;

        @Comment("Event horizon radius (screen UV units, 0.083 default)")
        public double eventHorizonRadius = 1.0 / 12.0;

        @Comment("Reference distance for perspective scaling. At this distance, effect = config size. Closer = bigger.")
        public double lensPerspectiveScale = 10.0;

        @Comment("Lens direction: >0 = convex (gravitational pull toward center), <0 = concave (push away). Magnitude scales strength.")
        public double lensDirection = 1.0;
    }

    @Comment("Enable ground heave shockwave particles and sound when giant anvil triggers shock mechanism")
    public boolean groundHeaveParticlesEnabled = true;

    @Comment("Number of particles per block spawned by ground heave effect")
    @BoundedDiscrete(max = 5, min = 0)
    public int groundHeaveParticleCount = 1;

    @Comment("Probability (0.0-1.0) each block spawns ground heave particles")
    @BoundedDiscrete(max = 1, min = 0)
    public double groundHeaveParticleChance = 0.8;

    @Comment("Render block-state items in sifting and unpacking tables with the enlarged block model pick")
    public boolean siftingUnpackingBlockRenderEnabled = true;

    @Comment("A vertical item frame vertically displays items")
    public boolean verticalItemFrame = false;

    @Comment("Enable exhaust particles when flying with ionocraft backpack")
    public boolean ionoCraftBackpackExhaustParticlesEnabled = true;

    @CollapsibleObject
    @SerializedName(value = "weatherproofChestplateHud", alternate = "ionoCraftBackpackHud")
    public WeatherproofChestplateHud weatherproofChestplateHud = new WeatherproofChestplateHud();

    @Comment("Toggle the behaviour when exiting the Category Setting menu")
    public ExitBehaviourMode exitCategorySettingBehaviour = ExitBehaviourMode.CONFIRM;

    public static class WeatherproofChestplateHud {
        @Comment("If true, will show Weatherproof Chestplate current power in hud")
        public boolean enabled = true;

        @Comment("If true, will show charged capacitor counts in hud")
        public boolean capacitorCountEnabled = true;

        @Comment("The Gui Hud Scale")
        @BoundedDiscrete(min = 0, max = 8)
        public float hudScale = 0.75F;

        @Comment("The gui hud x position")
        public int hudX = 8;

        @Comment("The gui hud y position")
        public int hudY = 8;
    }

    @Comment("Preview mode when placing multipart blocks (large crate, hyperdimension storage station...)")
    public MultiPartPreviewMode multiPartPreviewMode = MultiPartPreviewMode.OUTLINE;

    @Comment("Opacity of the ghost (solid) preview when placing multipart blocks")
    @BoundedDiscrete(min = 0.0, max = 1.0)
    public double multiPartPreviewGhostOpacity = 0.3;

    @Comment("Opacity of the outline preview when placing multipart blocks")
    @BoundedDiscrete(min = 0.0, max = 1.0)
    public double multiPartPreviewOutlineOpacity = 0.5;

    public enum MultiPartPreviewMode implements TranslatableEnum {
        @SerializedName("Ghost")
        GHOST,
        @SerializedName("Outline")
        OUTLINE,
        @SerializedName("Off")
        OFF
    }

    public enum GoggleMode {
        ALWAYS_SHOW,
        WEARING_HAMMER,
        HOLDING_HAMMER,
        WEARING_OR_HOLDING_HAMMER,
        TOGGLE_WITH_KEY
    }

    public enum ExitBehaviourMode {
        CONFIRM,
        CANCEL
    }
}
