package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;
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

    @SerializedName("Display Redstone EMP Particles")
    @Comment("Enable redstone EMP particle effects")
    public boolean displayRedstoneEmpParticles = true;

    @Comment("Render lines between power transmitters")
    public boolean renderPowerTransmitterLines = true;

    @Comment("Bloom effect on laser and power transmitter lines.")
    public boolean renderBloomEffect = false;

    @Comment("Scanline post-processing effect on 3D structure previews.")
    public boolean renderScanPreviewEffect = true;

    @Comment("A vertical item frame vertically displays items")
    public boolean verticalItemFrame = false;

    @Comment("Enable exhaust particles when flying with Ionocraft Backpack")
    public boolean ionocraftBackpackExhaustParticlesEnabled = true;

    @SerializedName("Ionocraft Backpack HUD")
    @CollapsibleObject
    public IonocraftBackpackHud ionocraftBackpackHud = new IonocraftBackpackHud();

    @SerializedName("Show Multiphase Stored ID")
    @Comment("Add a tooltip line that shows multiphase stored ID")
    public boolean showMultiphaseStoredId = false;

    public static class IonocraftBackpackHud {
        @SerializedName("Enabled")
        @Comment("If true, will show Ionocraft Backpack current power in hud")
        public boolean enabled = true;

        @SerializedName("HUD Scale")
        @Comment("The Gui Hud Scale")
        @BoundedDiscrete(min = 0, max = 8)
        public float hudScale = 0.75f;

        @SerializedName("HUD X Position")
        @Comment("The gui hud x position")
        public int hudX = 8;

        @SerializedName("HUD Y Position")
        @Comment("The gui hud y position")
        public int hudY = 8;
    }

    public enum GoggleMode {
        @SerializedName("Always Show")
        ALWAYS_SHOW,
        @SerializedName("When Wearing Hammer")
        WEARING_HAMMER,
        @SerializedName("When Holding Hammer")
        HOLDING_HAMMER,
        @SerializedName("Toggle with Key")
        TOGGLE_WITH_KEY
    }
}
