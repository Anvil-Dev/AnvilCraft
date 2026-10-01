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

        @Comment("Controls the range in which the Active Silencer mutes sounds (in blocks)")
        @BoundedDiscrete(min = 3, max = 64)
        public int activeSilencerRange = 31;

        @Comment("Controls the minimum size of the crafting table multiblock the Giant Anvil can craft")
        @BoundedDiscrete(min = 3, max = 15)
        public int giantAnvilMultiblockMinSize = 3;

        @Comment("Controls the maximum size of the crafting table multiblock the Giant Anvil can craft")
        @BoundedDiscrete(min = 3, max = 31)
        public int giantAnvilMultiblockMaxSize = 15;

        @Comment("Controls the safe fall distance in the Mun dimension (in blocks)")
        @BoundedDiscrete(min = 0, max = 512)
        public float munSafeFallDistance = 20.0f;

        @Comment("Controls the blocks per damage point of fall damage in the Mun dimension")
        @BoundedDiscrete(min = 0.5, max = 64)
        public float munBlocksPerDamage = 6.0f;

        @Comment("Controls the delay before an Overworld-like dimension collapses (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int overworldLikeCollapseDelayTicks = 40;

        @Comment("Controls the bonus Neutronium Ingot chance per enchantment level when transcending (in %)")
        @BoundedDiscrete(min = 0, max = 100)
        public int transcendiumBonusIngotChancePerEnchantment = 10;

        @Comment("Controls the experience points granted per block of Liquid Experience")
        @BoundedDiscrete(min = 0, max = 1000)
        public int expFluidXpPerBlock = 50;

        @Comment("Controls how many neighbouring Void Matter blocks are needed for Void Matter to decay")
        @BoundedDiscrete(min = 1, max = 6)
        public int voidMatterDecayThreshold = 5;

        @Comment("Controls the chance that a settled source of Cement solidifies per random tick")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double cementSolidifyChance = 0.1;

        @Comment("Controls the chance that a Hollow Magnet converts the block it pulls")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double hollowMagnetConvertChance = 0.005;

        @Comment("Controls the chance that the Block Devourer suppresses the drops of the block it eats")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double blockDevourerDropSuppressionChance = 0.05;

        @Comment("Controls the chance that Redhot blocks absorb water")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double redhotWaterAbsorbChance = 0.5;

        @Comment("Controls the chance that Ember blocks absorb water")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double emberBlockWaterAbsorptionChance = 0.5;

        @Comment("Controls the damage a falling Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 100.0)
        public float anvilFallDamagePerBlock = 2.0f;

        @Comment("Controls the damage a falling Giant Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public float giantAnvilFallDamagePerBlock = 10.0f;

        @Comment("Controls the speed a powered Sliding Rail launches entities with")
        @BoundedDiscrete(min = 0.0, max = 10.0)
        public float slidingRailLaunchSpeed = 0.35f;

        @Comment("Controls the strength an unpowered Sliding Rail pulls entities with")
        @BoundedDiscrete(min = 0.0, max = 10.0)
        public float slidingRailPullStrength = 0.15f;

        @Comment("Controls how much of an entity's motion a Sliding Rail preserves")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public float slidingRailDamping = 0.8f;

        @Comment("Controls how long a Detector Sliding Rail stays powered (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int detectorSlidingRailHoldTicks = 20;

        @Comment("Controls the damage dealt by Heaters")
        @BoundedDiscrete(min = 0.0, max = 100.0)
        public float heaterDamage = 4.0f;

        @Comment("Controls the chance that using an item on a Chipped Anvil repairs it")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double chippedAnvilRepairChance = 0.9;

        @Comment("Controls the chance that using an item on a Damaged Anvil repairs it")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double damagedAnvilRepairChance = 0.2;

        @Comment("Controls how often Chocolate Blocks apply their effects (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int chocolateBlockEffectPeriod = 80;

        @Comment("Controls how long the effects of Chocolate Blocks last (in ticks)")
        @BoundedDiscrete(min = 1, max = 24000)
        public int chocolateBlockEffectDuration = 180;

        @Comment("Controls the chance that overheated Ember Metal turns into a Netherite Block")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double overheatedEmberMetalNetheriteChance = 0.05;

        @Comment("Controls how many times an Enchanted Gold Ingot makes Piglins barter")
        @BoundedDiscrete(min = 1, max = 16)
        public int enchantedGoldBarterMultiplier = 4;
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

        @Comment("Controls the maximum distance between Heliostats and the block irradiating them (in blocks)")
        @BoundedDiscrete(min = 1, max = 256)
        public int heliostatsMaxIrradiationDistance = 64;

        @Comment("Controls the cooldown between Tesla Tower strikes (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int teslaTowerStrikeCooldown = 80;

        @Comment("Controls the FE extracted per tick by the Discharger")
        @BoundedDiscrete(min = 1, max = 1000000)
        public int dischargerFePerTick = 10000;

        @Comment("Controls the FE capacity of the Fe Collector")
        @BoundedDiscrete(min = 1000, max = 1000000000)
        public int feCollectorMaxEnergy = 1000000;

        @Comment("Controls the FE converted per tick by the Fe Collector")
        @BoundedDiscrete(min = 1, max = 1000000)
        public int feCollectorFePerTick = 10000;

        @Comment("Controls the interval at which the Item Splitter distributes items (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int itemSplitterInterval = 8;

        @Comment("Controls the maximum distance the Item Splitter distributes items to")
        @BoundedDiscrete(min = 1, max = 64)
        public int itemSplitterMaxDistance = 16;

        @Comment("Controls the charge the Charge Collector accepts per incoming window")
        @BoundedDiscrete(min = 1, max = 1024)
        public double chargeCollectorMaxPowerPerIncoming = 128;

        @Comment("Controls the input sampling cooldown of the Charge Collector (in ticks)")
        @BoundedDiscrete(min = 1, max = 20)
        public int chargeCollectorInputCooldown = 2;

        @Comment("Controls the output cooldown of the Charge Collector (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int chargeCollectorOutputCooldown = 10;

        @Comment("Controls the base output power of the Infinite Collector")
        @BoundedDiscrete(min = 0, max = 4096)
        public int infiniteCollectorBasePower = 256;

        @Comment("Controls the range in which the Infinite Collector collects charges")
        @BoundedDiscrete(min = 1, max = 8)
        public int infiniteCollectorRange = 3;

        @Comment("Controls the power consumed by the Mass Energy Inverter")
        @BoundedDiscrete(min = 1, max = 65536)
        public int massEnergyInverterPower = 1024;

        @Comment("Controls the mass injected per tick by the Mass Energy Inverter")
        @BoundedDiscrete(min = 1, max = 1000)
        public int massEnergyInverterMassPerTick = 5;

        @Comment("Controls the maximum output of the Space Overcompressor per conversion")
        @BoundedDiscrete(min = 1, max = 6400)
        public int spaceOvercompressorMaxOutputPerTime = 640;

        @Comment("Controls how many items the Large Cauldron processes in parallel")
        @BoundedDiscrete(min = 1, max = 24)
        public int largeCauldronMaxProcessEfficiency = 9;

        @Comment("Controls how many tropical fish a Fish Tank can hold")
        @BoundedDiscrete(min = 1, max = 16)
        public int fishTankMaxTropicalFish = 4;

        @Comment("Controls the interval between radiation explosions of the Neutron Irradiator (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int neutronIrradiatorRadiationInterval = 40;

        @Comment("Controls how many Anvilons the Neutron Irradiator needs to select a type")
        @BoundedDiscrete(min = 1, max = 9)
        public int neutronIrradiatorTypeThreshold = 6;

        @Comment("Controls the FE capacity of the Propel Piston")
        @BoundedDiscrete(min = 1000000, max = 1600000000)
        public int propelPistonMaxEnergy = 160000000;

        @Comment("Controls the fluid output of megastructures such as the Extractor per tick (in mB)")
        @BoundedDiscrete(min = 1, max = 10000)
        public int megastructureFluidPerTick = 250;

        @Comment("Controls the fluid capacity of the Drain (in mB)")
        @BoundedDiscrete(min = 250, max = 64000)
        public int drainCapacity = 4000;

        @Comment("Controls the interval between Drain operations (in ticks)")
        @BoundedDiscrete(min = 1, max = 100)
        public int drainInterval = 5;
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

            @Comment("Controls the maximum height of the Plasma Jets' chimney walls")
            @BoundedDiscrete(min = 1, max = 8)
            public int maxTubeHeight = 4;
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

        @Comment("Controls how long the Infinite Fluid Tank asks for confirmation before it can be broken (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int infiniteTankBreakConfirmDurationTicks = 200;

        @Comment("Controls the fluid flow a fluid pipe gains per block of height difference (in mB/tick)")
        @BoundedDiscrete(min = 1, max = 1000)
        public int pipeFlowPerHeight = 50;

        @Comment("Controls the maximum fluid flow of a fluid pipe (in mB/tick)")
        @BoundedDiscrete(min = 1, max = 100000)
        public int pipeMaxFlowRate = 2000;

        @Comment("Controls how many stacks the Large Cauldron accepts per input slot")
        @BoundedDiscrete(min = 1, max = 64)
        public int largeCauldronInputStackMultiplier = 9;

        @Comment("Controls the range of the Local Storage Terminal (in blocks)")
        @BoundedDiscrete(min = 1, max = 256)
        public int localTerminalRange = 32;

        @Comment("Controls the range of the Shulker Storage Terminal (in blocks)")
        @BoundedDiscrete(min = 1, max = 512)
        public int shulkerTerminalRange = 64;

        @Comment("Controls how many storage operations can be undone")
        @BoundedDiscrete(min = 0, max = 64)
        public int storageUndoDepth = 4;
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

        @Comment("Controls the liquid XP cost per bookshelf level of the Auto Enchanting Table (in mB)")
        @BoundedDiscrete(min = 0, max = 10000)
        public int autoEnchantingTableExpCostPerShelf = 400;

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

        @Comment("Controls how many totems the Amulet Box can store")
        @BoundedDiscrete(min = 1, max = 64)
        public int amuletBoxCapacity = 16;

        @Comment("Controls whether eternal items can be killed by the void (falling out of the world)")
        public boolean eternalItemsVoidKillable = false;

        @Comment("Controls the maximum number of blocks a single Building Rod operation may place")
        @BoundedDiscrete(min = 1, max = 65536)
        public int buildingRodMaxBlocks = 4000;

        @Comment("Controls the item cooldown applied when a Magnet is used (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int magnetItemCooldown = 5;

        @Comment("Controls how much durability a Frost Metal Ingot restores when repairing an item")
        @BoundedDiscrete(min = 1, max = 10000)
        public int frostMetalIngotRepairAmount = 1080;

        @Comment("Controls how much durability a Frost Metal Nugget restores when repairing an item")
        @BoundedDiscrete(min = 1, max = 1000)
        public int frostMetalNuggetRepairAmount = 120;

        @Comment("Controls how long the Totem of Rage keeps its owner alive (in ticks)")
        @BoundedDiscrete(min = 20, max = 24000)
        public int totemOfRageDuration = 1200;

        @Comment("Controls the experience a villager gains when an Experience Gem is used on it")
        @BoundedDiscrete(min = 0, max = 1000)
        public int expGemVillagerXp = 20;

        @Comment("Controls how much a villager ages up when an Experience Gem is used on it (in ticks)")
        @BoundedDiscrete(min = 1, max = 24000)
        public int expGemAgeAddition = 120;

        @Comment("Controls how long the Portable Anvil takes to use (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int portableAnvilUseTicks = 40;

        @Comment("Controls the radius in which the Cursed Golden Apple affects villagers")
        @BoundedDiscrete(min = 1, max = 64)
        public int cursedGoldenAppleSearchRadius = 16;

        @Comment("Controls how much durability Fire Reforging restores per tick")
        @BoundedDiscrete(min = 1, max = 1000)
        public int fireReforgingRepairPerTick = 10;

        @Comment("Controls the chance that Providence grants an extra roll")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public float providenceExtraRollChance = 0.25f;

        @Comment("Controls the chance that Providence grants a third roll")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public float providenceThirdRollChance = 0.05f;

        @Comment("Controls how long the Resonator takes to mine a block by resonance (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int resonatorMiningTicks = 20;

        @Comment("Controls the durability the Resonator consumes per resonance mining operation")
        @BoundedDiscrete(min = 1, max = 10000)
        public int resonatorDurabilityCost = 128;

        @Comment("Controls how many Anvil Railgun rounds can be loaded (at most 30 to fit the ammo bitmask)")
        @BoundedDiscrete(min = 1, max = 30)
        public int anvilRailgunMaxAmmo = 16;

        @Comment("Controls the base cooldown of the Tesla Gun (in ticks, shortened by Quick Charge)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int teslaGunCooldown = 80;
        @Comment("Controls how long the Charged Jump ability takes to charge (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int chargedJumpChargeTicks = 20;

        @Comment("Controls how long the Charged Jump bar holds its progress after releasing (in ticks)")
        @BoundedDiscrete(min = 0, max = 200)
        public int chargedJumpChargeHoldTicks = 20;

        @Comment("Controls how long the Charged Jump bar takes to decay after holding (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int chargedJumpChargeDecayTicks = 10;

        @Comment("Controls the jump height multiplier of a fully charged jump")
        @BoundedDiscrete(min = 1.0, max = 10.0)
        public double chargedJumpMaxHeightMultiplier = 3.5;

        @Comment("Controls how often the helmet refreshes Night Vision (in ticks, above the 200 tick flicker threshold)")
        @BoundedDiscrete(min = 200, max = 2400)
        public int nightVisionRefreshTicks = 210;
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

        @Comment("Controls how many power grid entries the power grid command shows")
        @BoundedDiscrete(min = 1, max = 4096)
        public int powergridInfoLimit = 256;
    }
}
