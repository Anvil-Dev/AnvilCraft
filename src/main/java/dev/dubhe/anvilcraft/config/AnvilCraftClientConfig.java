package dev.dubhe.anvilcraft.config;

import dev.anvilcraft.lib.config.BoundedDiscrete;
import dev.anvilcraft.lib.config.CollapsibleObject;
import dev.anvilcraft.lib.config.Comment;
import dev.anvilcraft.lib.config.Config;
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
    public boolean doNotShowTooltipWhenJadePresent = true;

    @Comment("Render lines between power transmitters")
    public boolean renderPowerTransmitterLines = true;

    @Comment("Bloom effect on laser and power transmitter lines.")
    public boolean renderBloomEffect = false;

    @Comment("A vertical item frame vertically displays items")
    public boolean verticalItemFrame = false;

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
