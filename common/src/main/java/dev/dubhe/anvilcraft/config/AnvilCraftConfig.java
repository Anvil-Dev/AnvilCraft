package dev.dubhe.anvilcraft.config;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = AnvilCraft.MOD_ID)
public class AnvilCraftConfig {

    public static AnvilCraftConfig INSTANCE;

    public static void init() {
        INSTANCE = Configuration.registerConfig(AnvilCraftConfig.class, ConfigFormats.yaml()).getConfigInstance();
    }

    @Configurable
    @Configurable.Comment("Maximum number of items processed by the anvil at the same time")
    public int anvilEfficiency = 64;

    @Configurable
    @Configurable.Comment("Maximum depth a lightning strike can reach")
    public int lightningStrikeDepth = 2;

    @Configurable
    @Configurable.Comment("Maximum distance a magnet attracts")
    public int magnetAttractsDistance = 5;
    @Configurable
    @Configurable.Comment("Maximum radius a handheld magnet attracts")
    public double magnetItemAttractsRadius = 8;

    @Configurable
    @Configurable.Comment("Redstone EMP distance generated per block dropped by the anvil")
    public int redstoneEmpRadius = 6;

    @Configurable
    @Configurable.Comment("Maximum distance of redstone EMP")
    public int redstoneEmpMaxRadius = 24;

    @Configurable
    @Configurable.Comment("Maximum cooldown time of chute (in ticks)")
    public int chuteMaxCooldown = 4;
}