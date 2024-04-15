package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;
import dev.dubhe.anvilcraft.AnvilCraft;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;


@Config(name = AnvilCraft.MOD_ID)
public class AnvilCraftConfig implements ConfigData {

    @Comment("Maximum number of items processed by the anvil at the same time")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 1)
    public int anvilEfficiency = 64;

    @Comment("Maximum depth a lightning strike can reach")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 16, min = 1)
    public int lightningStrikeDepth = 2;

    @Comment("Maximum radius a lightning strike can reach")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 8, min = 0)
    public int lightningStrikeRadius = 1;

    @Comment("Maximum distance a magnet attracts")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 16, min = 1)
    public int magnetAttractsDistance = 5;

    @Comment("Maximum radius a handheld magnet attracts")
    @ConfigEntry.Gui.Tooltip
    public double magnetItemAttractsRadius = 8;

    @Comment("Redstone EMP distance generated per block dropped by the anvil")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 1)
    @SerializedName("Redstone EMP Radius Per Block")
    public int redstoneEmpRadius = 6;

    @Comment("Maximum distance of redstone EMP")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 1)
    @SerializedName("Redstone Emp Max Radius")
    public int redstoneEmpMaxRadius = 24;

    @Comment("Maximum cooldown time of chute (in ticks)")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 80, min = 1)
    public int chuteMaxCooldown = 4;

    @Comment("Maximum cooldown time of auto crafter (in ticks)")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 80, min = 1)
    public int autoCrafterCooldown = 20;

    @Comment("The maximum search radius of the geode")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 512, min = 64)
    @SerializedName("Geode Maximum Search Radius")
    public int geodeRadius = 64;

    @Comment("The search interval of the geode")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 8, min = 1)
    @SerializedName("Geode Search Interval")
    public int geodeInterval = 4;

    @Comment("The search cooldown of the geode (in seconds)")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 30, min = 5)
    @SerializedName("Geode Search Cooldown")
    public int geodeCooldown = 5;

    @Comment("The power transmitter can identify the range of the power transmitter")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 1)
    @SerializedName("Range of Power Transmitter")
    public int powerTransmitterRange = 8;
}