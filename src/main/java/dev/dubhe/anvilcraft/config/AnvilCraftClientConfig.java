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
    @Comment("The mode of the anvil hammer goggle info")
    public GoggleMode goggleMode = GoggleMode.WEARING_OR_HOLDING_HAMMER;

    @Comment("Scale of the anvil hammer radial menu")
    @BoundedDiscrete(min = 0.5, max = 2.0)
    public float anvilHammerRadialMenuScale = 1.0f;

    @Comment("Render distance of heliostats block entity")
    @BoundedDiscrete(min = 32, max = 512)
    public int heliostatsRenderDistance = 128;

    @Comment("Heliostats render sunflower head model in Sunflower Plains biome")
    public boolean heliostatsSunflowerModel = true;

    @Comment("Do not render power component tooltip when jade present")
    public boolean doNotShowTooltipWhenJadePresent = false;

    @Comment("Swap insert/collect to left-click and keep extract/place on right-click (Left Collect, Right Place)")
    public boolean invertOverrideAction = false;

    @SerializedName("Show Storage Stored ID")
    @Comment("Add a tooltip line that shows storage stored ID")
    public boolean showStorageStoredId = false;

    @Comment("Use the legacy flat creative inventory layout instead of the sectioned layout with banners")
    public boolean useLegacyCreativeTab = false;

    @Comment("Fold 16-color item families into one representative item with a right-click variant picker in the creative inventory")
    public boolean creativeVariantPickerEnabled = false;

    @Comment("Enable ground heave shockwave particles and sound when giant anvil triggers shock mechanism")
    public boolean groundHeaveParticlesEnabled = true;

    @Comment("Number of particles per block spawned by ground heave effect")
    @BoundedDiscrete(max = 5, min = 0)
    public int groundHeaveParticleCount = 1;

    @Comment("Probability (0.0-1.0) each block spawns ground heave particles")
    @BoundedDiscrete(max = 1, min = 0)
    public double groundHeaveParticleChance = 0.8;

    @SerializedName("Display Redstone EMP Particles")
    @Comment("Enable redstone EMP particle effects")
    public boolean displayRedstoneEmpParticles = true;

    @Comment("Render lines between power transmitters")
    public boolean renderPowerTransmitterLines = true;

    @Comment("Bloom effect on laser and power transmitter lines.")
    public boolean renderBloomEffect = false;

    @Comment("Scanline post-processing effect on 3D structure previews.")
    public boolean renderScanPreviewEffect = true;

    @Comment("Render the shared orbital rings in the overworld-like dimension")
    public boolean renderOverworldLikeSky = true;

    @Comment("Vanilla keeps the original Overworld sky. Special replaces only the sun and moon with models, "
        + "including libration and a continuous eight-day lunar phase cycle. World lighting is unchanged.")
    public OverworldSkyMode overworldSkyMode = OverworldSkyMode.VANILLA;

    @Comment("Mun lighting: Potato uses Standard lighting and ambient occlusion without custom shadows, retaining vanilla entity shadows; "
        + "Standard adds terrain, animated entity, and colored translucent shadows; "
        + "Vanilla uses the vanilla rendering pipeline without Mun shaders, retaining the cloudless Mun sky, Overworld and moving stars. "
        + "Rendering failures switch this setting to Vanilla. Shadow range is limited by render distance.")
    public MunLightingQuality munLightingQuality = MunLightingQuality.STANDARD;

    @Comment("Vanilla restores the original stellar surface and layered halos; "
        + "Standard enables temperature-based emission and smooth halos. "
        + "Rendering failures fall back to Vanilla until resources reload. Changes take effect immediately.")
    public CelestialRenderingMode stellarRenderingMode = CelestialRenderingMode.STANDARD;

    @Comment("Vanilla restores the original translucent atmosphere shell; Standard enables a thicker volume atmosphere. "
        + "Rendering failures fall back to Vanilla until resources reload. Changes take effect immediately.")
    public CelestialRenderingMode planetAtmosphereRenderingMode = CelestialRenderingMode.STANDARD;

    @CollapsibleObject
    public GravitationalLens gravitationalLens = new GravitationalLens();

    public static class GravitationalLens {
        @Comment("Gravitational lensing post-processing effect near black holes")
        public boolean renderBlackHoleLensing = false;

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

    @Comment("Render block-state items in sifting and unpacking tables with the enlarged block model pick")
    public boolean siftingUnpackingBlockRenderEnabled = true;

    @Comment("A vertical item frame vertically displays items")
    public boolean verticalItemFrame = false;

    @Comment("Enable exhaust particles when flying with Ionocraft Backpack")
    public boolean ionocraftBackpackExhaustParticlesEnabled = true;

    @Comment("Show levels above 10 as Roman numerals in auto enchanting table's liquid enchantment mode")
    public boolean liquidEnchantmentRomanNumerals = true;

    @Comment("Preview mode when placing multipart blocks (large crate, hyperdimension storage station...)")
    public MultiPartPreviewMode multiPartPreviewMode = MultiPartPreviewMode.OUTLINE;

    @Comment("Opacity of the ghost (solid) preview when placing multipart blocks")
    @BoundedDiscrete(min = 0.0, max = 1.0)
    public double multiPartPreviewGhostOpacity = 0.3;

    @Comment("Opacity of the outline preview when placing multipart blocks")
    @BoundedDiscrete(min = 0.0, max = 1.0)
    public double multiPartPreviewOutlineOpacity = 0.5;

    public enum CelestialRenderingMode implements TranslatableEnum {
        @SerializedName("Vanilla")
        VANILLA,
        @SerializedName("Standard")
        STANDARD
    }

    public enum OverworldSkyMode implements TranslatableEnum {
        @SerializedName("Vanilla")
        VANILLA,
        @SerializedName("Special")
        SPECIAL
    }

    public enum MunLightingQuality implements TranslatableEnum {
        @SerializedName("Potato")
        POTATO,
        @SerializedName("Standard")
        STANDARD,
        @SerializedName("Off")
        OFF
    }

    public enum GoggleMode implements TranslatableEnum {
        @SerializedName("Always Show")
        ALWAYS_SHOW,
        @SerializedName("When Wearing Hammer")
        WEARING_HAMMER,
        @SerializedName("When Holding Hammer")
        HOLDING_HAMMER,
        @SerializedName("When Wearing or Holding Hammer")
        WEARING_OR_HOLDING_HAMMER,
        @SerializedName("Toggle with Key")
        TOGGLE_WITH_KEY
    }

    public enum MultiPartPreviewMode implements TranslatableEnum {
        @SerializedName("Ghost")
        GHOST,
        @SerializedName("Outline")
        OUTLINE,
        @SerializedName("Off")
        OFF
    }
}
