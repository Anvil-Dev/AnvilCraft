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
        @Comment("Controls the maximum render distance of the Heliostats' BER (in blocks)")
        @BoundedDiscrete(min = 32, max = 512)
        public int heliostatsRenderDistance = 128;

        @Comment("Controls whether to render the sunflower head model for Heliostats in the Sunflower Plains biome")
        public boolean heliostatsSunflowerModel = true;

        @Comment("Controls whether to render lines between Transmission Poles")
        public boolean renderPowerTransmitterLines = true;

        @Comment("Controls whether to render the shared orbital rings in the Overworld-Like dimension")
        public boolean renderOverworldLikeSkyRings = true;

        @Comment("Controls the sky darkening at the peak of a visual eclipse")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double overworldLikeEclipseDarken = 0.72;

        @Comment("Controls the scale of the shared orbital rings in the Overworld-Like dimension")
        @BoundedDiscrete(min = 100.0, max = 10000.0)
        public double overworldLikeSkyRingScale = 1200.0;

        @Comment(
            """
            Controls the quality of Mun lighting
            "Potato" uses Standard lighting and ambient occlusion without custom shadows, retaining vanilla entity shadows
            "Standard" adds terrain, animated entity, and colored translucent shadows
            "Vanilla" uses the vanilla rendering pipeline without Mun shaders, retaining the cloudless Mun sky, Overworld and moving stars
            Rendering failures will fall-back this setting to "Vanilla"
            Shadow range is limited by render distance"""
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
            Controls the render mode of stellar emission
            "Vanilla" restores the original stellar surface and layered halos
            "Standard" enables temperature-based emission and smooth halos
            On rendering failure, it falls back to "Vanilla" without changing this setting until resources are reloaded
            Changes take effect immediately"""
        )
        public CelestialRenderMode stellarRenderMode = CelestialRenderMode.STANDARD;

        @Comment("Controls the maximum brightness of stellar coronas")
        @BoundedDiscrete(min = 1.0, max = 64.0)
        public double stellarMaxExposure = 24.0;

        @SerializedName("Planet Atmosphere Rendering")
        @Comment(
            """
            Controls the render mode of the planet atmosphere
            "Vanilla" restores the original translucent atmosphere shell
            "Standard" enables a thicker volume atmosphere
            On rendering failure, it falls back to "Vanilla" without changing this setting until resources are reloaded
            Changes take effect immediately"""
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

            @SerializedName("Max Render Count")
            @Comment(
                """
                Controls the maximum number of Black/White Holes rendered (2-256)
                Higher values render more effects, lower values improve performance"""
            )
            @BoundedDiscrete(min = 2, max = 256)
            public int maxHoleCount = 8;

            @Comment("Controls the maximum distance at which Black/White Holes render gravitational lensing (in blocks)")
            @BoundedDiscrete(min = 16, max = 512)
            public int maxDistance = 256;

            @Comment("Controls the lens distortion strength around Black/White Holes (higher values distort light more; default: 0.002)")
            public double strength = 1.0 / 512.0;

            @Comment("Controls the event horizon radius of Black/White Holes in screen UV units (default: 0.083)")
            public double eventHorizonRadius = 1.0 / 12.0;

            @Comment(
                """
                Controls the reference distance for perspective scaling (in blocks)
                At this distance, effect = config size
                Closer = bigger"""
            )
            public double perspectiveScale = 10.0;

            @Comment(
                """
                Controls the lens direction
                >0 = convex (gravitational pull toward center), <0 = concave (push away)
                Magnitude scales strength"""
            )
            public double direction = 1.0;
        }

        @SerializedName("Enlarged Block Rendering in Sifting/Unpacking Tables")
        @Comment(
            """
            Controls whether to render block-state items in Sifting Tables and Unpacking Tables
            With the enlarged block model pick instead of regular scattered item rendering"""
        )
        public boolean renderBlockModelInSiftingAndUnpacking = true;

        @Comment("Controls whether to always display items vertically in Item Frames on horizontal surfaces")
        public boolean renderVerticalModelsInHorizontalItemFrame = false;

        @Comment("Controls the time the Affect Range outline stays visible after looking away (in ticks)")
        @BoundedDiscrete(min = 0, max = 600)
        public int affectRangeOutlinePersistTicks = 100;
    }

    @SerializedName("Effects & Sounds")
    @Comment("Adjusts settings related to particles, sound effects, and audio-visual feedback")
    @CollapsibleObject
    public EffectsSettings effects = new EffectsSettings();

    public static class EffectsSettings {
        @SerializedName("Render Power Transmission Line Bloom")
        @Comment("Controls whether to apply the bloom effect to Lasers and power transmission lines")
        public boolean renderBloomEffect = false;

        @Comment("Controls whether to apply the Scanline post-processing effect to 3D structure previews")
        public boolean renderScanPreviewExtraEffect = true;

        @Comment("Controls whether to display the Ascending Block animation")
        public boolean displayAnvilAnimation = true;

        @Comment("Controls whether to display block bounce animation in the Giant Anvil's Shock")
        public boolean displayGiantAnvilShockBlockBounceAnimation = true;

        @Comment("Controls whether to display particles in the Giant Anvil's Shock")
        public boolean displayGiantAnvilShockParticles = true;

        @Comment("Controls the number of particles spawned per block by the Giant Anvil's Shock")
        @BoundedDiscrete(min = 0, max = 5)
        public int giantAnvilShockParticlesCount = 1;

        @Comment("Controls the probability that each block spawns particles from the Giant Anvil's Shock (0.0-1.0)")
        @BoundedDiscrete(min = 0, max = 1)
        public double giantAnvilShockParticlesChance = 0.8;

        @Comment("Controls the duration of the block bounce animation in the Giant Anvil's Shock (in ticks)")
        @BoundedDiscrete(min = 1, max = 100)
        public int giantAnvilShockBounceDurationTicks = 16;

        @Comment("Controls the maximum bounce height of blocks in the Giant Anvil's Shock (in blocks)")
        @BoundedDiscrete(min = 0.0, max = 2.0)
        public double giantAnvilShockBounceAmplitude = 0.85;

        @Comment("Controls whether to play sounds when the Giant Anvil triggers its Shock")
        public boolean playGiantAnvilShockSound = true;

        @SerializedName("Display Redstone EMP Particles")
        @Comment("Controls whether to display particles in the Redstone EMP")
        public boolean displayRedstoneEmpParticles = true;

        @SerializedName("Redstone EMP Max Ring Particles")
        @Comment("Controls the maximum number of particles per ring in the Redstone EMP")
        @BoundedDiscrete(min = 8, max = 64)
        public int redstoneEmpMaxRingParticles = 48;

        @Comment("Controls whether to display exhaust particles when flying with the Ionocraft Backpack")
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

        @Comment("Controls the volume of Energy Weapons' firing sounds (multiplier)")
        @BoundedDiscrete(min = 0.0, max = 2.0)
        public double energyWeaponSoundVolume = 1.0;

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
        @Comment("Controls whether to show Overseer loaded chunks on Xaero's World Map and Minimap")
        public volatile boolean showOverseerMapHighlights = true;

        @Comment("Controls the Goggle info activation mode")
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

        @Comment("Controls whether to render Goggle tooltips when Jade is present")
        public boolean showGoggleTooltipWhenJadePresent = true;

        @Comment("Controls the scale of the Anvil Hammer wheel")
        @BoundedDiscrete(min = 0.5, max = 2.0)
        public double anvilHammerWheelScale = 1.0;

        @SerializedName("Show Storage Stored ID")
        @Comment("Controls whether to add a tooltip line that shows the stored Storage ID")
        public boolean showStorageStoredId = false;

        @SerializedName("Use Legacy Creative Inventory")
        @Comment(
            """
            Controls whether to use the legacy flat creative inventory layout instead of the sectioned layout with banners
            Requires restart to take effect"""
        )
        public boolean useLegacyCreativeTab = false;

        @SerializedName("Fold Colored Item Variants")
        @Comment(
            """
            Controls whether to fold 16-color item families into one representative item with a right-click variant picker
            Applies to both creative inventory layouts (requires restart)"""
        )
        public boolean enableCreativeVariantPicker = false;

        @Comment("Controls the maximum liquid enchantment level (included) displayed as Roman numerals in Auto Enchanting Table's Liquid Enchantment mode")
        @BoundedDiscrete(min = 0, max = 255)
        public int liquidEnchantmentLevelRomanNumeralLimit = 10;

        @SerializedName("Display Capacitor Count in HUD")
        @Comment("Controls whether to display charged Capacitor counts in HUD")
        public boolean displayCapacitorCountInHud = true;

        @Comment("Controls the display cycle of recipe preview animations (in milliseconds)")
        @BoundedDiscrete(min = 200, max = 10000)
        public int recipePreviewCycleMillis = 1500;

        @Comment("Controls the time the Storage Screen's missing workbench flyout stays visible (in ticks)")
        @BoundedDiscrete(min = 0, max = 100)
        public int storageFlyoutHoldTicks = 25;

        @Comment("Controls the speed at which the Filter item cycles through its stored items (in milliseconds)")
        @BoundedDiscrete(min = 100, max = 5000)
        public int filterItemDisplayIntervalMillis = 1000;

        @Comment("Controls the number of item types a storage's tooltip shows at once")
        @BoundedDiscrete(min = 1, max = 64)
        public int storageTooltipMaxVisibleTypes = 9;

        @Comment("Controls the number of fluids the Fluid Tank tooltip shows at once")
        @BoundedDiscrete(min = 1, max = 64)
        public int fluidTankTooltipMaxVisibleFluids = 5;

        @Comment("Controls the speed at which 3D Building Rod previews follow the player view")
        @BoundedDiscrete(min = 1.0, max = 100.0)
        public double buildingRodPreviewFollowRate = 18.0;

        @Comment("Controls the maximum time a Thought tooltip stays visible (in seconds)")
        @BoundedDiscrete(min = 0.1, max = 10.0)
        public double thoughtMaxSeconds = 1.0;

        @SerializedName("Apply Changes When Closing Category Settings")
        @Comment(
            """
            Controls the action when a category settings screen is closed
            "Apply Changes" saves the changes, "Discard Changes" reverts them"""
        )
        public ExitBehaviourMode exitCategorySettingBehaviour = ExitBehaviourMode.CANCEL;

        public enum ExitBehaviourMode implements TranslatableEnum {
            @SerializedName("Apply Changes")
            CONFIRM,

            @SerializedName("Discard Changes")
            CANCEL
        }

        @SerializedName("Weatherproof Chestplate HUD")
        @Comment("Adjusts settings related to the Weatherproof Chestplate HUD")
        @CollapsibleObject
        public WeatherproofChestplateHud weatherproofChestplateHud = new WeatherproofChestplateHud();

        public static class WeatherproofChestplateHud {
            @Comment("Controls whether to display Weatherproof Chestplate current power in HUD")
            public boolean enabled = true;

            @SerializedName("Scale")
            @Comment("Controls the HUD scale")
            @BoundedDiscrete(min = 0, max = 8)
            public double scale = 0.75;

            @SerializedName("X Position")
            @Comment("Controls the HUD x position (in pixels)")
            public int x = 8;

            @SerializedName("Y Position")
            @Comment("Controls the HUD y position (in pixels)")
            public int y = 8;
        }
    }

    @SerializedName("Controls & Interaction")
    @Comment("Adjusts settings related to key inversion, interaction methods, and control habits")
    @CollapsibleObject
    public ControlsSettings controls = new ControlsSettings();

    public static class ControlsSettings {
        @SerializedName("Keep Charged Jump Charge")
        @Comment(
            "Whether the Charged Jump keeps its progress after releasing sneak. "
            + "When disabled, releasing sneak clears the charge immediately"
        )
        public boolean keepChargedJumpCharge = false;

        @Comment("Controls whether to swap insert/collect to left-click and keep extract/place on right-click (Left Collect, Right Place)")
        public boolean invertMouseOverrideActions = false;

        @Comment(
            "Controls whether to swap the Storage Fluid Port bucket actions so that left-click stores and right-click pours"
        )
        public boolean invertFluidPortBucketActions = false;

        @SerializedName("Storage Bulk Action Key")
        @Comment("Controls the key for storing matching items, taking eight stacks or buckets, and crafting eight stacks. "
            + "Defaults to Alt; Space or both keys can also be enabled")
        public StorageBulkActionKey storageBulkActionKey = StorageBulkActionKey.ALT;

        @Comment("Controls the mouse sensitivity when rotating 3D structure previews")
        @BoundedDiscrete(min = 0.1, max = 2.0)
        public double previewRotationSensitivity = 0.5;

        @SerializedName("Building Rod Blueprint Key Set Type")
        @Comment("Controls the key set type used by the Building Rod blueprint mode")
        public BuildingRodKeySetType buildingRodKeySet = BuildingRodKeySetType.OPTIMIZED;

        public enum StorageBulkActionKey implements TranslatableEnum {
            @SerializedName("Alt")
            ALT,
            @SerializedName("Space")
            SPACE,
            @SerializedName("Alt or Space")
            ALT_OR_SPACE;

            public boolean usesAlt() {
                return this != StorageBulkActionKey.SPACE;
            }

            public boolean usesSpace() {
                return this != StorageBulkActionKey.ALT;
            }
        }

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
