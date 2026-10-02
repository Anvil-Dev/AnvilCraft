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
    @SerializedName("Graphics & Rendering")
    @Comment("Adjusts settings related to sky, lighting, models, and world rendering")
    @CollapsibleObject
    public GraphicsSettings graphics = new GraphicsSettings();

    public static class GraphicsSettings {
        @Comment("Controls the render distance of the Heliostats' BER")
        @BoundedDiscrete(min = 32, max = 512)
        public int heliostatsRenderDistance = 128;

        @Comment("Controls whether to render the sunflower head model for Heliostats in the Sunflower Plains biome")
        public boolean heliostatsSunflowerModel = true;

        @Comment("Controls whether to render lines between power transmitters")
        public boolean renderPowerTransmitterLines = true;

        @Comment("Controls whether to render the shared orbital rings in the overworld-like dimension")
        public boolean renderOverworldLikeSkyRings = true;

        @SerializedName("Overworld Celestial Bodies")
        @Comment(
            """
            Controls the render mode of Overworld sky
            "Vanilla" keeps the original Overworld sky
            "Special" replaces only the sun and moon with models, including libration and a continuous eight-day lunar phase cycle
            World lighting is unchanged
            """
        )
        public OverworldSkyRenderMode overworldSkyRenderMode = OverworldSkyRenderMode.VANILLA;

        public enum OverworldSkyRenderMode implements TranslatableEnum {
            @SerializedName("Vanilla")
            VANILLA,
            @SerializedName("Special")
            SPECIAL,
            ;

            public boolean isSpecial() {
                return this == OverworldSkyRenderMode.SPECIAL;
            }
        }

        @Comment(
            """
            Controls the quality of Mun lighting
            "Potato" uses Standard lighting and ambient occlusion without custom shadows, retaining vanilla entity shadows
            "Standard" adds terrain, animated entity, and colored translucent shadows
            "Vanilla" uses the vanilla rendering pipeline without Mun shaders, retaining the cloudless Mun sky, Overworld and moving stars
            Rendering failures will fall-back this setting to "Vanilla"
            Shadow range is limited by render distance
            """
        )
        public MunLightingQuality munLightingQuality = MunLightingQuality.STANDARD;

        public enum MunLightingQuality implements TranslatableEnum {
            @SerializedName("Potato")
            POTATO,
            @SerializedName("Standard")
            STANDARD,
            @SerializedName("Vanilla")
            OFF,
            ;

            public boolean isEnabled() {
                return this != MunLightingQuality.OFF;
            }
        }

        @SerializedName("Stellar Emission Rendering")
        @Comment(
            """
            Controls the render mode of stellar
            "Vanilla" restores the original stellar surface and layered halos
            "Standard" enables temperature-based emission and smooth halos
            On rendering failure, it falls back to "Vanilla" without changing this setting until resources are reloaded
            Changes take effect immediately
            """
        )
        public CelestialRenderMode stellarRenderMode = CelestialRenderMode.STANDARD;

        @SerializedName("Planet Atmosphere Rendering")
        @Comment(
            """
            Controls the render mode of planet atmosphere
            "Vanilla" restores the original translucent atmosphere shell
            "Standard" enables a thicker volume atmosphere
            On rendering failure, it falls back to "Vanilla" without changing this setting until resources are reloaded
            Changes take effect immediately
            """
        )
        public CelestialRenderMode planetAtmosphereRenderMode = CelestialRenderMode.STANDARD;

        public enum CelestialRenderMode implements TranslatableEnum {
            @SerializedName("Vanilla")
            VANILLA,
            @SerializedName("Standard")
            STANDARD,
        }

        @SerializedName("Render Gravitational Lensing Effects")
        @Comment("Adjusts settings related to the gravitational lensing effect of Black/White Holes")
        @CollapsibleObject
        public GravitationalLens gravitationalLens = new GravitationalLens();

        public static class GravitationalLens {
            @Comment("Controls whether to render the gravitational lensing post-processing effect near Black/White Holes")
            public boolean enabled = false;

            @SerializedName("Maximum Render Count")
            @Comment(
                """
                Controls the maximum number of Black/White Holes rendered (2-256)
                Higher values render more effects, lower values improve performance
                """
            )
            @BoundedDiscrete(min = 2, max = 256)
            public int maxHoleCount = 8;

            @Comment("Controls the maximum distance at which Black/White Holes render gravitational lensing (in blocks)")
            @BoundedDiscrete(min = 16, max = 512)
            public int maxDistance = 256;

            @SerializedName("Gravitational Lensing Strength")
            @Comment("Controls the lens distortion strength around Black/White Holes (higher values distort light more; default: 0.002)")
            public double strength = 1.0 / 512.0;

            @Comment("Controls the event horizon radius of Black/White Holes in screen UV units (default: 0.083)")
            public double eventHorizonRadius = 1.0 / 12.0;

            @SerializedName("Gravitational Lensing Perspective Scale")
            @Comment(
                """
                Controls the reference distance for perspective scaling
                At this distance, effect = config size
                Closer = bigger
                """
            )
            public double perspectiveScale = 10.0;

            @SerializedName("Gravitational Lensing Direction")
            @Comment(
                """
                Controls the lens direction
                >0 = convex (gravitational pull toward center), <0 = concave (push away)
                Magnitude scales strength
                """
            )
            public double direction = 1.0;
        }

        @SerializedName("Enlarged Block Rendering in Sifting/Unpacking Tables")
        @Comment(
            """
            Controls whether to render block-state items in sifting and unpacking tables
            with the enlarged block model pick instead of regular scattered item rendering
            """
        )
        public boolean renderBlockModelInSiftingAndUnpacking = true;

        @Comment("Controls whether to always vertically displays items in item frames on horizontal surfaces")
        public boolean renderVerticalModelsInHorizontalItemFrame = false;

        @Comment("Controls how long the affect-range outline stays visible after looking away (in ticks)")
        @BoundedDiscrete(min = 0, max = 600)
        public int affectRangeOutlinePersistTicks = 100;

        @Comment("Controls the maximum brightness of stellar coronas")
        @BoundedDiscrete(min = 1.0, max = 64.0)
        public float stellarMaxExposure = 24.0f;

        @Comment("Controls how far the sky darkens at the peak of a visual eclipse")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public float overworldLikeEclipseDarken = 0.72f;

        @Comment("Controls the scale of the shared orbital rings in the overworld-like dimension")
        @BoundedDiscrete(min = 100.0, max = 10000.0)
        public float overworldLikeSkyRingScale = 1200.0f;
    }

    @SerializedName("Effects & Sounds")
    @Comment("Adjusts settings related to particles, sound effects, and audio-visual feedback")
    @CollapsibleObject
    public EffectsSettings effects = new EffectsSettings();

    public static class EffectsSettings {
        @SerializedName("Render Power Transmission Line Bloom")
        @Comment("Controls whether to apply the bloom effect to lasers and power transmitter lines")
        public boolean renderBloomEffect = false;

        @Comment("Controls whether to apply the Scanline post-processing effect to 3D structure previews")
        public boolean renderScanPreviewExtraEffect = true;

        @Comment("Controls whether to display anvil levitate animation")
        public boolean displayAnvilAnimation = true;

        @Comment("Controls whether to display block bounce animation in Giant Anvil's shockwaves")
        public boolean displayGiantAnvilShockBlockBounceAnimation = true;

        @Comment("Controls whether to display particles in Giant Anvil's shockwaves")
        public boolean displayGiantAnvilShockParticles = true;

        @Comment("Controls the number of particles spawned per block by Giant Anvil's shockwave effects")
        @BoundedDiscrete(min = 0, max = 5)
        public int giantAnvilShockParticlesCount = 1;

        @Comment("Controls the probability (0.0-1.0) that each block spawns particles from Giant Anvil's shockwave effects")
        @BoundedDiscrete(min = 0, max = 1)
        public double giantAnvilShockParticlesChance = 0.8;

        @Comment("Controls whether to play sounds when Giant Anvil triggers its shock mechanism")
        public boolean playGiantAnvilShockSound = true;

        @SerializedName("Display Redstone EMP Particles")
        @Comment("Controls whether to display particles in redstone EMP")
        public boolean displayRedstoneEmpParticles = true;

        @Comment("Controls whether to display exhaust particles when flying with Ionocraft Backpack")
        public boolean displayIonocraftBackpackExhaustParticles = true;

        @Comment("Controls the preview mode when placing multipart blocks (Giant Anvil, Large Crate, etc.)")
        public MultiPartPreviewMode multiPartPreviewMode = MultiPartPreviewMode.OUTLINE;

        public enum MultiPartPreviewMode implements TranslatableEnum {
            @SerializedName("Ghost")
            GHOST,
            @SerializedName("Outline")
            OUTLINE,
            @SerializedName("Off")
            OFF,
            ;

            public boolean isEnabled() {
                return this != MultiPartPreviewMode.OFF;
            }

            public boolean isGhost() {
                return this == MultiPartPreviewMode.GHOST;
            }

            public boolean isOutline() {
                return this == MultiPartPreviewMode.OUTLINE;
            }
        }

        @Comment("Controls the opacity of the ghost (solid) preview when placing multipart blocks")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double multiPartPreviewGhostOpacity = 0.3;

        @Comment("Controls the opacity of the outline preview when placing multipart blocks")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double multiPartPreviewOutlineOpacity = 0.5;

        @Comment("Controls the volume of energy weapons' firing sounds (multiplier)")
        @BoundedDiscrete(min = 0.0, max = 2.0)
        public float energyWeaponSoundVolume = 1.0f;

        @Comment("Controls the maximum number of particles per ring in the Redstone EMP shockwave")
        @BoundedDiscrete(min = 8, max = 64)
        public int redstoneEmpMaxRingParticles = 48;

        @Comment("Controls the duration of the block bounce animation in Giant Anvil's shockwaves (in ticks)")
        @BoundedDiscrete(min = 1, max = 100)
        public int giantAnvilShockBounceDurationTicks = 16;

        @Comment("Controls the maximum bounce height of blocks in Giant Anvil's shockwaves")
        @BoundedDiscrete(min = 0.0, max = 2.0)
        public float giantAnvilShockBounceAmplitude = 0.85f;

        @Comment("Controls the shortest silence before the first Mun music track (in seconds)")
        @BoundedDiscrete(min = 0, max = 3600)
        public int munMusicFirstSilenceMinSeconds = 30;

        @Comment("Controls the longest silence before the first Mun music track (in seconds)")
        @BoundedDiscrete(min = 0, max = 3600)
        public int munMusicFirstSilenceMaxSeconds = 90;

        @Comment("Controls the shortest silence between Mun music tracks (in seconds)")
        @BoundedDiscrete(min = 0, max = 3600)
        public int munMusicNextSilenceMinSeconds = 60;

        @Comment("Controls the longest silence between Mun music tracks (in seconds)")
        @BoundedDiscrete(min = 0, max = 3600)
        public int munMusicNextSilenceMaxSeconds = 180;
    }

    @SerializedName("UI & HUD")
    @Comment("Adjusts settings related to the interface, HUD elements, and tooltips")
    @CollapsibleObject
    public UISettings ui = new UISettings();

    public static class UISettings {
        @Comment("Controls the goggle info activation mode")
        public GoggleInfoActivationMode goggleInfoActivationMode = GoggleInfoActivationMode.WEARING_OR_HOLDING_HAMMER;

        public enum GoggleInfoActivationMode implements TranslatableEnum {
            @SerializedName("Always Show")
            ALWAYS_SHOW,
            @SerializedName("When Wearing Hammer")
            WEARING_HAMMER,
            @SerializedName("When Holding Hammer")
            HOLDING_HAMMER,
            @SerializedName("When Wearing or Holding Hammer")
            WEARING_OR_HOLDING_HAMMER,
            @SerializedName("Toggle with Key")
            TOGGLE_WITH_KEY,
        }

        @Comment("Controls whether to render power component tooltips when Jade is present")
        public boolean showGoggleTooltipWhenJadePresent = true;

        @Comment("Controls the scale of the Anvil Hammer wheel")
        @BoundedDiscrete(min = 0.5, max = 2.0)
        public float anvilHammerWheelScale = 1.0f;

        @SerializedName("Show Storage Stored ID")
        @Comment("Controls whether to add a tooltip line that shows the stored storage ID")
        public boolean showStorageStoredId = false;

        @SerializedName("Use Legacy Creative Inventory")
        @Comment(
            """
            Controls whether to use the legacy flat creative inventory layout instead of the sectioned layout with banners
            Requires restart to take effect
            """
        )
        public boolean useLegacyCreativeTab = false;

        @SerializedName("Fold Colored Item Variants")
        @Comment(
            """
            Controls whether to fold 16-color item families into one representative item with a right-click variant picker
            Applies to both creative inventory layouts (requires restart)
            """
        )
        public boolean enableCreativeVariantPicker = false;

        @Comment(
            """
            Controls the maximum liquid enchantment level (included) displayed as Roman numerals
            in Auto Enchanting Table's Liquid Enchantment mode
            """
        )
        @BoundedDiscrete(min = 0, max = 255)
        public int liquidEnchantmentLevelRomanNumeralLimit = 10;

        @SerializedName("Display Capacitor Count in HUD")
        @Comment("Controls whether to display charged capacitor counts in HUD")
        public boolean displayCapacitorCountInHud = true;

        @Comment("Controls the display cycle of recipe preview animations (in milliseconds)")
        @BoundedDiscrete(min = 200, max = 10000)
        public int recipePreviewCycleMillis = 1500;

        @Comment("Controls how long the Storage Terminal's missing station flyout stays visible (in ticks)")
        @BoundedDiscrete(min = 0, max = 100)
        public int storageFlyoutHoldTicks = 25;

        @Comment("Controls how fast the Filter item cycles through its stored items (in milliseconds)")
        @BoundedDiscrete(min = 100, max = 5000)
        public int filterItemDisplayIntervalMillis = 1000;

        @Comment("Controls how many item types the Storage Terminal tooltip shows at once")
        @BoundedDiscrete(min = 1, max = 64)
        public int storageTooltipMaxVisibleTypes = 9;

        @Comment("Controls how many fluids the fluid tank tooltip shows at once")
        @BoundedDiscrete(min = 1, max = 64)
        public int fluidTankTooltipMaxVisibleFluids = 5;

        @Comment("Controls how quickly 3D building rod previews follow the player view")
        @BoundedDiscrete(min = 1.0, max = 100.0)
        public double buildingRodPreviewFollowRate = 18.0;

        @Comment("Controls how long a thought tooltip stays visible at most (in seconds)")
        @BoundedDiscrete(min = 0.1, max = 10.0)
        public double thoughtMaxSeconds = 1.0;
        @SerializedName("Apply Changes When Closing Category Settings")
        @Comment("Controls whether closing a category settings screen applies or discards the changes")
        public ExitBehaviourMode exitCategorySettingBehaviour = ExitBehaviourMode.CANCEL;

        public enum ExitBehaviourMode implements TranslatableEnum {
            @SerializedName("Apply Changes")
            CONFIRM,

            @SerializedName("Discard Changes")
            CANCEL
        }

        @SerializedName("Weatherproof Chestplate HUD")
        @CollapsibleObject
        public WeatherproofChestplateHud weatherproofChestplateHud = new WeatherproofChestplateHud();

        public static class WeatherproofChestplateHud {
            @Comment("Controls whether to display Weatherproof Chestplate current power in HUD")
            public boolean enabled = true;

            @SerializedName("Scale")
            @Comment("Controls the HUD scale")
            @BoundedDiscrete(min = 0, max = 8)
            public float scale = 0.75f;

            @SerializedName("X Position")
            @Comment("Controls the HUD x position")
            public int x = 8;

            @SerializedName("Y Position")
            @Comment("Controls the HUD y position")
            public int y = 8;
        }
    }

    @SerializedName("Controls & Interaction")
    @Comment("Adjusts settings related to key inversion, interaction methods, and control habits")
    @CollapsibleObject
    public ControlsSettings controls = new ControlsSettings();

    public static class ControlsSettings {
        @Comment("Controls whether to swap insert/collect to left-click and keep extract/place on right-click (Left Collect, Right Place)")
        public boolean invertMouseOverrideActions = false;

        @Comment(
            "Controls whether to swap the fluid port bucket actions in the storage screen so that left-click stores and right-click pours"
        )
        public boolean invertFluidPortBucketActions = false;

        @Comment("Controls the mouse sensitivity when rotating 3D structure previews")
        @BoundedDiscrete(min = 0.1, max = 2.0)
        public float previewRotationSensitivity = 0.5f;

        @SerializedName("Building Rod Blueprint Key Set Type")
        @Comment("Controls the key set type used by the Building Rod blueprint mode")
        public BuildingRodKeySetType buildingRodKeySet = BuildingRodKeySetType.OPTIMIZED;

        public enum BuildingRodKeySetType implements TranslatableEnum {
            @SerializedName("Traditional")
            TRADITIONAL,
            @SerializedName("Optimized")
            OPTIMIZED,
            ;

            public boolean isTraditional() {
                return this == BuildingRodKeySetType.TRADITIONAL;
            }
        }
    }
}
