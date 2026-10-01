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

        @Comment("Adjusts settings related to gravitational lens")
        @CollapsibleObject
        public GravitationalLens gravitationalLens = new GravitationalLens();

        public static class GravitationalLens {
            @Comment("Controls whether to render the gravitational lensing post-processing effect near Black Holes")
            public boolean enabled = false;

            @Comment("Controls the maximum number of Black/White Holes rendered (2-256)")
            @BoundedDiscrete(min = 2, max = 256)
            public int maxHoleCount = 8;

            @Comment("Controls the lens distortion strength")
            public double strength = 1.0 / 512.0;

            @Comment("Controls the event horizon radius in screen UV units")
            public double eventHorizonRadius = 1.0 / 12.0;

            @Comment(
                """
                Controls the reference distance for perspective scaling
                At this distance, effect = config size
                Closer = bigger
                """
            )
            public double perspectiveScale = 10.0;

            @Comment(
                """
                Controls the lens direction
                >0 = convex (gravitational pull toward center), <0 = concave (push away)
                Magnitude scales strength
                """
            )
            public double direction = 1.0;
        }

        @Comment("Render block-state items in sifting and unpacking tables with the enlarged block model pick")
        public boolean renderBlockModelInSiftingAndUnpacking = true;

        @Comment("Controls whether to always vertically displays items in item frames on horizontal surfaces")
        public boolean renderVerticalModelsInHorizontalItemFrame = false;
    }

    @SerializedName("Effects & Sounds")
    @Comment("Adjusts settings related to particles, sound effects, and audio-visual feedback")
    @CollapsibleObject
    public EffectsSettings effects = new EffectsSettings();

    public static class EffectsSettings {
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
    }

    @SerializedName("UI & HUD")
    @Comment("Adjusts settings related to sky, lighting, models, and world rendering")
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

        @Comment("Do not render power component tooltip when jade present")
        public boolean showGoggleTooltipWhenJadePresent = true;

        @Comment("Controls the scale of the Anvil Hammer wheel")
        @BoundedDiscrete(min = 0.5, max = 2.0)
        public float anvilHammerWheelScale = 1.0f;

        @SerializedName("Show Storage Stored ID")
        @Comment("Controls whether to add a tooltip line that shows the stored storage ID")
        public boolean showStorageStoredId = false;

        @Comment("Controls whether to use the legacy flat creative inventory layout instead of the sectioned layout with banners")
        // @NeedRestart(RestartType.GAME)
        public boolean useLegacyCreativeTab = false;

        @Comment("Controls whether to fold 16-color item families into one representative item with a right-click variant picker in the creative inventory")
        public boolean enableCreativeVariantPicker = false;

        @Comment("Controls the minimum liquid enchantment level (included) displayed as Roman numerals in Auto Enchanting Table's Liquid Enchantment mode")
        @BoundedDiscrete(min = 0, max = 255)
        public int liquidEnchantmentLevelRomanNumeralLimit = 10;

        @SerializedName("Display Capacitor Count in HUD")
        @Comment("Controls whether to display charged capacitor counts in HUD")
        public boolean displayCapacitorCountInHud = true;

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
