package dev.dubhe.anvilcraft.config;

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
    public int redstoneEmpRadius = 6;

    @Comment("Maximum distance of redstone EMP")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 1)
    public int redstoneEmpMaxRadius = 24;

    @Comment("Maximum cooldown time of chute (in ticks)")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 64, min = 1)
    public int chuteMaxCooldown = 4;
}