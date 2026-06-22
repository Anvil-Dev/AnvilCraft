package dev.dubhe.anvilcraft.config;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.fml.config.ModConfig;

@Config(name = AnvilCraft.MOD_ID, type = ModConfig.Type.CLIENT)
public class AnvilCraftClientConfig {
    @Comment("The mode of the anvil hammer goggle info")
    public GoggleMode goggleMode = GoggleMode.WEARING_HAMMER;

    @Comment("Render distance of heliostats block entity")
    @BoundedDiscrete(min = 32, max = 512)
    public int heliostatsRenderDistance = 128;

    @Comment("Heliostats render sunflower head model in Sunflower Plains biome")
    public boolean heliostatsSunflowerModel = true;

    @Comment("Do not render power component tooltip when jade present")
    public boolean doNotShowTooltipWhenJadePresent = false;

    @Comment("Enable ground heave shockwave particles and sound when giant anvil triggers shock mechanism")
    public boolean groundHeaveParticlesEnabled = true;

    @Comment("Number of particles per block spawned by ground heave effect")
    @BoundedDiscrete(max = 5, min = 0)
    public int groundHeaveParticleCount = 1;

    @Comment("Probability (0.0-1.0) each block spawns ground heave particles")
    @BoundedDiscrete(max = 1, min = 0)
    public double groundHeaveParticleChance = 0.8;

    @Comment("Enable redstone EMP particle effects")
    public boolean redstoneEmpParticlesEnabled = true;

    @Comment("Render lines between power transmitters")
    public boolean renderPowerTransmitterLines = true;

    @Comment("Bloom effect on laser and power transmitter lines.")
    public boolean renderBloomEffect = false;

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

    @Comment("A vertical item frame vertically displays items")
    public boolean verticalItemFrame = false;

    @Comment("Enable exhaust particles when flying with Ionocraft Backpack")
    public boolean ionoCraftBackpackExhaustParticlesEnabled = true;

    @CollapsibleObject
    public IonoCraftBackpackHud ionoCraftBackpackHud = new IonoCraftBackpackHud();

    @Comment("Add a tooltip line that shows multiphase stored ID")
    public boolean showMultiphaseStoredId = false;

    public static class IonoCraftBackpackHud {
        @Comment("If true, will show Ionocraft Backpack current power in hud")
        public boolean enabled = true;

        @Comment("The Gui Hud Scale")
        @BoundedDiscrete(min = 0, max = 8)
        public float hudScale = 0.75f;

        @Comment("The gui hud x position")
        public int hudX = 8;

        @Comment("The gui hud y position")
        public int hudY = 8;
    }

    public enum GoggleMode {
        ALWAYS_SHOW,
        WEARING_HAMMER,
        HOLDING_HAMMER,
        TOGGLE_WITH_KEY
    }
}
