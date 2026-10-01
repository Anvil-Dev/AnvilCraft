package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.FireCauldronBlock;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.fluids.FluidType;

@Config(name = AnvilCraft.MOD_ID, type = ModConfig.Type.SERVER)
public class AnvilCraftServerConfig {
    @SerializedName("World & Physics")
    @Comment("Adjusts settings related to world physics, anvil impacts, and block behavior")
    @CollapsibleObject
    public WorldSettings world = new WorldSettings();

    public static class WorldSettings {
        @Comment("Controls the number of orbital integration substeps per tick for freely falling items and blocks (1 = legacy motion)")
        @BoundedDiscrete(min = 1, max = 64)
        public int orbitIntegrationSubsteps = 8;

        @Comment("Controls whether to enable Schwarzschild periapsis advance around attractive gravity sources of strength at least 10")
        public boolean relativisticPrecession = true;

        @Comment("Controls the effective speed of light in blocks per tick used by orbital precession; larger values weaken the effect")
        @BoundedDiscrete(min = 16, max = 4096)
        public int orbitalSpeedOfLight = 64;

        @Comment("Controls the maximum depth a lightning strike can reach")
        @BoundedDiscrete(max = 16, min = 1)
        public int lightningStrikeDepth = 2;

        @Comment("Controls the maximum radius a lightning strike can reach")
        public int lightningStrikeRadius = 1;

        @Comment("Controls the maximum radius of the Giant Anvil's shock behavior")
        @BoundedDiscrete(max = 16, min = 4)
        public int giantAnvilMaxShockRadius = 16;

        @Comment("Controls the maximum fall damage dealt by the Giant Anvil")
        @BoundedDiscrete(max = 100, min = 0)
        public int giantAnvilMaxFallDamage = 40;

        @SerializedName("Anvil Collision Explosion Speed Threshold")
        @Comment("Controls the minimum collision speed at which anvils explode instead of merely stopping (in blocks/tick)")
        public int anvilCollisionCraftSpeed = 16;

        @SerializedName("Redstone EMP Radius Per Block")
        @Comment("Controls the length of redstone EMP generated per block dropped by the anvil")
        @BoundedDiscrete(max = 64, min = 1)
        public int redstoneEmpRadius = 6;

        @SerializedName("Redstone EMP Max Radius")
        @Comment("Controls the maximum length of redstone EMP")
        @BoundedDiscrete(max = 64, min = 1)
        public int redstoneEmpMaxRadius = 24;

        @Comment("Controls the maximum distance for the Block Placer to recursively retrieve items from containers")
        @BoundedDiscrete(max = 20, min = 0)
        public int blockPlacerMaxRecursiveRetrievalDistance = 7;

        @Comment("Controls whether the Block Devourer devours blocks upward in a chain (see #anvilcraft:block_devourer_chain_devouring)")
        public boolean blockDevourerUpwardChainDevouring = true;

        @Comment("Controls the maximum distance of the Block Devourer's upward chain devouring")
        @BoundedDiscrete(max = 15, min = 0)
        public int blockDevourerUpwardChainDevouringDistance = 8;

        @Comment("Controls whether the Block Devourer protects containers (blocks that can store items) from being devoured")
        public boolean blockDevourerProtectContainers = false;

        @Comment("Controls whether pushing or pulling a sliding rail chains to other rails")
        public boolean slidingRailStickToEachOther = false;
    }

    @SerializedName("Machines & Logistics")
    @Comment("Adjusts settings related to machines, power transmission, and storage logistics")
    @CollapsibleObject
    public MachinesSettings machines = new MachinesSettings();

    public static class MachinesSettings {
        @Comment("Controls the maximum cooldown time of the Chute (in ticks)")
        @BoundedDiscrete(max = 80, min = 1)
        public int chuteMaxCooldown = 8;

        @Comment("Controls the maximum cooldown time of the Batch Crafter (in ticks)")
        @BoundedDiscrete(max = 80, min = 1)
        public int batchCrafterCooldown = 8;

        @Comment("Controls the maximum cooldown time of the Batch Cutter (in ticks)")
        @BoundedDiscrete(max = 80, min = 1)
        public int batchCutterCooldown = 8;

        @Comment("Controls the cooldown time of the Load Monitor (in ticks)")
        @BoundedDiscrete(max = 60, min = 1)
        public int loadMonitorCooldown = 10;

        @Comment("Controls the maximum size of the matrix formed by connecting Transparent Crafting Tables")
        @BoundedDiscrete(max = 32, min = 3)
        public int transparentCraftingTableMaxMatrixSize = 15;

        @Comment("Controls the ripening cooldown of the Induction Light (in ticks)")
        public int inductionLightBlockRipeningCooldown = 400;

        @Comment("Controls the ripening range of the Induction Light")
        @BoundedDiscrete(min = 0, max = 100)
        public int inductionLightBlockRipeningRange = 5;

        @Comment("Controls the number of ticks between Heliostats detections")
        @BoundedDiscrete(max = 20, min = 1)
        public int heliostatsDetectionInterval = 4;

        @Comment("Controls the working interval of the Mineral Fountain (in ticks)")
        @BoundedDiscrete(min = 2, max = 1200)
        public int mineralFountainInterval = 20;

        @Comment("Adjusts settings related to Plasma Jets")
        @CollapsibleObject
        public PlasmaJets plasmaJets = new PlasmaJets();

        public static class PlasmaJets {
            @Comment("Controls the maximum duration of Plasma Jets (in ticks)")
            @BoundedDiscrete(min = 10 * 20, max = 24 * 60 * 60 * 20)
            public int maxDuration = 10 * 60 * 20;

            @Comment("Controls the amount of fuel consumed per cycle by Fire Cauldron-based Plasma Jets (in layers)")
            @BoundedDiscrete(min = 1, max = FireCauldronBlock.MAX_LEVEL)
            public int cauldronConsumeAmount = 1;

            @Comment("Controls the extended duration of a single consumption of Fire Cauldron-based Plasma Jets (in ticks)")
            @BoundedDiscrete(min = 5 * 20, max = 12 * 60 * 60 * 20)
            public int cauldronExtraDuration = 5 * 60 * 20;

            @Comment("Controls the amount of fuel consumed per cycle by Fish Tank-based Plasma Jets (in mB)")
            @BoundedDiscrete(min = 1, max = FluidType.BUCKET_VOLUME)
            public int fishTankConsumeAmount = 1;

            @Comment("Controls the extended duration of a single consumption of Fish Tank-based Plasma Jets (in ticks)")
            @BoundedDiscrete(min = 1, max = 12 * 60 * 60 * 20)
            public int fishTankExtraDuration = 24;
        }

        @Comment("Controls the power grid range of Power Transmitters")
        @BoundedDiscrete(max = 64, min = 1)
        public int powerTransmitterRange = 8;

        @Comment("Controls the power grid range of Remote Transmission Poles")
        @BoundedDiscrete(max = 64, min = 1)
        public int remotePowerTransmitterRange = 16;

        @Comment("Adjusts settings related to power converters")
        @CollapsibleObject
        public PowerConverter powerConverter = new PowerConverter();

        public static class PowerConverter {
            @Comment("Controls the working interval of power converters (in ticks)")
            @BoundedDiscrete(min = 1, max = 60)
            public int workInterval = 10;

            @Comment("Controls the energy efficiency of power converters (1 kW => xx FE/t)")
            @BoundedDiscrete(min = 1, max = 1000)
            public int efficiency = 100;

            @Comment("Controls the power loss of power converters")
            public double loss = 0.1;
        }

        @Comment("Controls the maximum size of the entries in the storages' recover station")
        public int storageRecoverMaxSize = 20;

        @Comment("Adjusts settings related to the Storage Port")
        @CollapsibleObject
        public StoragePort storagePort = new StoragePort();

        public static class StoragePort {
            @Comment("Controls the working interval of the Storage Port (in ticks), which limits how often it scans and moves items")
            @BoundedDiscrete(min = 1, max = 1200)
            public int workInterval = 5;

            @Comment("Controls the maximum number of items the Storage Port moves per scan")
            @BoundedDiscrete(min = 1, max = 1024)
            public int maxItemsPerScan = 64;
        }

        @Comment("Adjusts settings related to the Hyperdimension Uploader")
        @CollapsibleObject
        public HyperdimensionUploader hyperdimensionUploader = new HyperdimensionUploader();

        public static class HyperdimensionUploader {
            @Comment("Controls the working interval of the Hyperdimension Uploader (in ticks)")
            @BoundedDiscrete(min = 1, max = 1200)
            public int workInterval = 5;

            @Comment("Controls the maximum number of items the Hyperdimension Uploader moves to the bound storage per scan")
            @BoundedDiscrete(min = 1, max = 1024)
            public int maxItemsPerScan = 64;
        }
    }

    @SerializedName("Equipment & Enchanting")
    @Comment("Adjusts settings related to enchantments, anvil enchanting limits, and equipment")
    @CollapsibleObject
    public EquipmentSettings equipment = new EquipmentSettings();

    public static class EquipmentSettings {
        @Comment("Controls the maximum number of logs cut per level of the Felling enchantment")
        @BoundedDiscrete(max = 24, min = 2)
        public int fellingBlockPerLevel = 4;

        @Comment("Controls the working interval of the Auto Enchanting Table (in ticks)")
        @BoundedDiscrete(min = 1, max = 1000)
        public int autoEnchantingTableInterval = 80;

        @Comment("Controls the maximum valid bookshelves of the Auto Enchanting Table")
        @BoundedDiscrete(min = 1, max = 80)
        public int autoEnchantingTableMaxBookshelf = 15;

        @Comment("Controls the maximum selectable level of the Auto Enchanting Table's Liquid Enchantment mode")
        @BoundedDiscrete(min = 1, max = 15)
        public int liquidEnchantmentMaxLevel = 15;

        @Comment("Controls whether items can be combined with Enchanted Books beyond the maximum level in the Royal Anvil")
        public boolean royalAnvilBeyondMaxLevel = false;

        @Comment("Controls whether items can be combined with Enchanted Books beyond the maximum level in the Frost Anvil")
        public boolean frostAnvilBeyondMaxLevel = false;

        @Comment("Controls whether items can be combined with Enchanted Books beyond the maximum level in the Ember Anvil")
        public boolean emberAnvilBeyondMaxLevel = false;

        @Comment("Controls whether items can be combined with Enchanted Books beyond the maximum level in the Transcendence Anvil")
        public boolean transcendenceAnvilBeyondMaxLevel = true;

        @Comment("Controls the maximum length a magnet attracts")
        @BoundedDiscrete(max = 8, min = 0)
        public int magnetAttractsDistance = 5;

        @Comment("Controls the maximum radius a handheld magnet attracts")
        @BoundedDiscrete(max = 16, min = 1)
        public double magnetItemAttractsRadius = 8;

        @Comment("Controls whether lasers are blocked by the collision shapes of blocks along their path")
        public boolean laserImpactChecking = true;

        @SerializedName("Laser Gun Mining Chain Limit")
        @Comment(
            """
            Controls the maximum ore vein size searched while mining with a Laser Gun
            Ore beyond this limit is not chain-mined (default: 64)
            """
        )
        @BoundedDiscrete(min = 1, max = 4096)
        public int laserOreClusterMaxSize = 64;

        @SerializedName("Geode Maximum Search Radius")
        @Comment("Controls the maximum search radius of the Geode")
        @BoundedDiscrete(max = 512, min = 64)
        public int geodeRadius = 64;

        @SerializedName("Geode Search Interval")
        @Comment("Controls the search interval of the Geode")
        @BoundedDiscrete(max = 8, min = 1)
        public int geodeInterval = 4;

        @SerializedName("Geode Search Cooldown")
        @Comment("Controls the search cooldown of the Geode (in seconds)")
        @BoundedDiscrete(max = 30, min = 5)
        public int geodeCooldown = 5;

        @Comment("Controls whether pressing shift and right-clicking takes out all totems stored in the Amulet Box")
        public boolean amuletBoxTakeOutAllTotem = true;

        @Comment("Controls whether eternal items can be killed by the void (falling out of the world)")
        public boolean eternalItemsVoidKillable = false;
    }

    @SerializedName("Commands")
    @Comment("Adjusts the commands the Spacetime Supercomputer may execute")
    @CollapsibleObject
    public CommandsSettings commands = new CommandsSettings();

    public static class CommandsSettings {
        @Comment("Controls whether to allow the /locate biome command")
        public boolean allowLocateBiomeCommand = true;

        @Comment("Controls whether to allow the /locate structure command")
        public boolean allowLocateStructureCommand = true;

        @Comment("Controls whether to allow the /locate poi command")
        public boolean allowLocatePoiCommand = true;

        @Comment("Controls whether to allow the /time add command")
        public boolean allowTimeAddCommand = true;

        @Comment("Controls whether to allow the /tick sprint command")
        public boolean allowTickSprintCommand = true;
    }
}
