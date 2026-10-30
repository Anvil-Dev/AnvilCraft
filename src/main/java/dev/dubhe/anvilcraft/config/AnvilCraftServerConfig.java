package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.util.TranslatableEnum;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.FireCauldronBlock;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.fluids.FluidType;

@Config(name = AnvilCraft.MOD_ID, type = ModConfig.Type.SERVER)
public class AnvilCraftServerConfig {
    @SerializedName("Overworld Celestial Bodies")
    @Comment("Server-wide Overworld sky and lunar travel mode. Vanilla keeps the original sky and midnight travel window. "
        + "Special uses modeled celestial bodies and permits travel when the Moon is overhead. Applies to every player.")
    public OverworldSkyMode overworldSkyMode = OverworldSkyMode.VANILLA;

    public enum OverworldSkyMode implements TranslatableEnum {
        @SerializedName("Vanilla")
        VANILLA,
        @SerializedName("Special")
        SPECIAL
    }

    @SerializedName("World & Physics")
    @Comment("Adjusts settings related to world physics, anvil impacts, and block behavior")
    @CollapsibleObject
    public WorldSettings world = new WorldSettings();

    public static class WorldSettings {
        @Comment("Controls the number of orbital integration substeps per tick for freely falling items and blocks (1 = legacy motion)")
        @BoundedDiscrete(min = 1, max = 64)
        public int orbitIntegrationSubsteps = 8;

        @Comment("Controls whether to enable Schwarzschild periapsis advance around attractive gravity sources")
        public boolean relativisticPrecession = true;

        @Comment("Controls the minimum strength a gravity source needs to produce relativistic precession")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double relativisticPrecessionMinStrength = 10.0;

        @Comment(
            """
            Controls the effective speed of light used by orbital precession (in blocks/tick)
            Larger values weaken the effect"""
        )
        @BoundedDiscrete(min = 16, max = 4096)
        public int orbitalSpeedOfLight = 64;

        @Comment("Controls the maximum depth a lightning strike can reach (in blocks)")
        @BoundedDiscrete(min = 1, max = 16)
        public int lightningStrikeDepth = 2;

        @Comment("Controls the maximum radius a lightning strike can reach (in blocks)")
        public int lightningStrikeRadius = 1;

        @Comment("Controls the maximum radius of the Giant Anvil's Shock (in blocks)")
        @BoundedDiscrete(min = 4, max = 16)
        public int giantAnvilMaxShockRadius = 16;

        @SerializedName("Anvil Collision Explosion Speed Threshold")
        @Comment("Controls the minimum collision speed at which Anvils explode instead of merely stopping (in blocks/tick)")
        public int anvilCollisionCraftSpeed = 16;

        @SerializedName("Redstone EMP Radius Per Block")
        @Comment("Controls the length of Redstone EMP generated per block dropped by the Anvil (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int redstoneEmpRadius = 6;

        @SerializedName("Redstone EMP Max Radius")
        @Comment("Controls the maximum length of Redstone EMP (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int redstoneEmpMaxRadius = 24;

        @Comment("Controls the maximum distance for the Block Placer to recursively retrieve items from containers (in blocks)")
        @BoundedDiscrete(min = 0, max = 20)
        public int blockPlacerMaxRecursiveRetrievalDistance = 7;

        @Comment("Controls whether the Block Devourer devours blocks upward in a chain (see #anvilcraft:block_devourer_chain_devouring)")
        public boolean blockDevourerUpwardChainDevouring = true;

        @Comment("Controls the maximum distance of the Block Devourer's upward chain devouring (in blocks)")
        @BoundedDiscrete(min = 0, max = 15)
        public int blockDevourerUpwardChainDevouringDistance = 8;

        @Comment("Controls whether the Block Devourer protects containers (blocks that can store items) from being devoured")
        public boolean blockDevourerProtectContainers = false;

        @Comment("Controls the chance that the Block Devourer suppresses the drops of the block it eats")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double blockDevourerDropSuppressionChance = 0.05;

        @Comment("Controls whether pushing or pulling a Sliding Rail chains to other Sliding Rails")
        public boolean slidingRailStickToEachOther = false;

        @Comment("Controls the range in which the Active Silencer mutes sounds (in blocks)")
        @BoundedDiscrete(min = 3, max = 64)
        public int activeSilencerRange = 31;

        @Comment("Controls the minimum size of the Crafting Table multiblock the Giant Anvil can craft (in blocks)")
        @BoundedDiscrete(min = 3, max = 15)
        public int giantAnvilMultiblockMinSize = 3;

        @Comment("Controls the maximum size of the Crafting Table multiblock the Giant Anvil can craft (in blocks)")
        @BoundedDiscrete(min = 3, max = 31)
        public int giantAnvilMultiblockMaxSize = 15;

        @Comment("Controls the safe fall distance in the Mun dimension (in blocks)")
        @BoundedDiscrete(min = 0, max = 512)
        public double munSafeFallDistance = 20.0;

        @Comment("Controls the fall distance per damage in the Mun dimension (in blocks/damage)")
        @BoundedDiscrete(min = 0.5, max = 64)
        public double munBlocksPerDamage = 6.0;

        @Comment("Controls the delay before an Overworld-Like dimension collapses (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int overworldLikeCollapseDelayTicks = 40;

        @Comment("Controls the bonus Neutronium Ingot chance per enchantment level when transcending (in %)")
        @BoundedDiscrete(min = 0, max = 100)
        public int transcendiumBonusIngotChancePerEnchantment = 10;

        @SerializedName("EXP Fluid XP Per Block")
        @Comment("Controls the experience points granted per block of EXP Fluid (in XP/block)")
        @BoundedDiscrete(min = 0, max = 1000)
        public int expFluidXpPerBlock = 50;

        @Comment("Controls the number of neighbouring Void Matter Blocks needed for a Void Matter Block to decay")
        @BoundedDiscrete(min = 1, max = 6)
        public int voidMatterDecayThreshold = 5;

        @Comment("Controls the chance that a settled source of Cement solidifies per random tick")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double cementSolidifyChance = 0.1;

        @Comment("Controls the chance that Redhot Blocks absorb water")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double redhotWaterAbsorbChance = 0.5;

        @Comment("Controls the chance that Ember Blocks absorb water")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double emberBlockWaterAbsorptionChance = 0.5;

        @Comment("Controls the water absorption chance of Cut Ember Metal Blocks")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double cutEmberMetalBlockWaterAbsorptionChance = 0.1;

        @Comment("Controls the chance that an Overheated Block of Ember Metal turns into a Block of Netherite")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double overheatedEmberMetalNetheriteChance = 0.05;

        @SerializedName("NeoForge Fall Damage Per Block")
        @Comment("Controls the damage a falling NeoForge deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double neoforgeFallDamagePerBlock = 2.0;

        @SerializedName("NeoForge Max Fall Damage")
        @Comment("Controls the maximum fall damage dealt by the NeoForge (-1 means infinity)")
        @BoundedDiscrete(min = -1, max = 1000)
        public int neoforgeMaxFallDamage = 20;

        @SerializedName("NeoForge Damage Chance")
        @Comment("Controls the chance that a falling NeoForge takes damage on impact")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double neoforgeAnvilDamageChance = 0.01;

        @Comment("Controls the damage a falling Royal Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double royalAnvilFallDamagePerBlock = 2.0;

        @Comment("Controls the maximum fall damage dealt by the Royal Anvil (-1 means infinity)")
        @BoundedDiscrete(min = -1, max = 1000)
        public int royalAnvilMaxFallDamage = 80;

        @Comment("Controls the damage a falling Frost Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double frostAnvilFallDamagePerBlock = 2.0;

        @Comment("Controls the maximum fall damage dealt by the Frost Anvil (-1 means infinity)")
        @BoundedDiscrete(min = -1, max = 1000)
        public int frostAnvilMaxFallDamage = 120;

        @Comment("Controls the damage a falling Ember Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double emberAnvilFallDamagePerBlock = 2.0;

        @Comment("Controls the maximum fall damage dealt by the Ember Anvil (-1 means infinity)")
        @BoundedDiscrete(min = -1, max = 1000)
        public int emberAnvilMaxFallDamage = 120;

        @Comment("Controls the damage a falling Transcendence Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double transcendenceAnvilFallDamagePerBlock = 2.0;

        @Comment("Controls the maximum fall damage dealt by the Transcendence Anvil (-1 means infinity)")
        @BoundedDiscrete(min = -1, max = 1000)
        public int transcendenceAnvilMaxFallDamage = -1;

        @Comment("Controls the damage a falling Giant Anvil deals per block fallen")
        @BoundedDiscrete(min = 0.0, max = 1000.0)
        public double giantAnvilFallDamagePerBlock = 10.0;

        @Comment("Controls the maximum fall damage dealt by the Giant Anvil (-1 means infinity)")
        @BoundedDiscrete(min = -1, max = 1000)
        public int giantAnvilMaxFallDamage = 40;

        @Comment("Controls the speed a powered Sliding Rail launches entities with (in blocks/tick)")
        @BoundedDiscrete(min = 0.0, max = 10.0)
        public double slidingRailLaunchSpeed = 0.35;

        @Comment("Controls the strength an unpowered Sliding Rail pulls entities with")
        @BoundedDiscrete(min = 0.0, max = 10.0)
        public double slidingRailPullStrength = 0.15;

        @Comment("Controls the fraction of an entity's motion a Sliding Rail preserves")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double slidingRailDamping = 0.8;

        @Comment(
            """
            Controls the maximum number of blocks a sliding structure may push (also scales the Detector Sliding Rail signal)
            A piston push limit changed by another mod takes precedence over this value"""
        )
        @BoundedDiscrete(min = 1, max = 256)
        public int slidingRailMaxPushDepth = 12;

        @Comment("Controls the duration a Detector Sliding Rail stays powered (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int detectorSlidingRailHoldTicks = 20;

        @Comment("Controls the damage dealt by Heaters")
        @BoundedDiscrete(min = 0.0, max = 100.0)
        public double heaterDamage = 4.0;

        @Comment(
            """
            Controls the chance that using an item on a Chipped Anvil repairs it
            Higher rolls consume the item without repairing anything"""
        )
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double chippedAnvilRepairChance = 0.9;

        @Comment(
            """
            Controls the chance that using an item on a Damaged Anvil repairs it to a full Anvil
            Rolls between this chance and the Chipped Anvil repair chance degrade it to a Chipped Anvil instead"""
        )
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double damagedAnvilRepairChance = 0.1;

        @Comment("Controls the interval at which Chocolate Blocks apply their effects (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int chocolateBlockEffectPeriod = 80;

        @Comment("Controls the effect duration of Chocolate Blocks (in ticks)")
        @BoundedDiscrete(min = 1, max = 24000)
        public int chocolateBlockEffectDuration = 180;

        @Comment("Controls the number of times an Enchanted Gold Ingot makes Piglins barter")
        @BoundedDiscrete(min = 1, max = 16)
        public int enchantedGoldBarterMultiplier = 4;

        @Comment("Controls the duration of the Monolith hint before it fades (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int monolithHintTicks = 100;

        @Comment("Controls the radius the Monolith hint searches for Monolith Cores (in blocks)")
        @BoundedDiscrete(min = 1, max = 32)
        public int monolithHintRange = 5;

        @Comment("Controls the duration of the Monolith return confirmation (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int monolithReturnConfirmationTicks = 60;

        @Comment("Controls the time the Monolith Core takes to accept an offering (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int monolithOfferingTicks = 100;

        @Comment("Controls the time the Monolith Core takes to dissolve an offering (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int monolithDissolveTicks = 60;

        @Comment("Controls the radius of the Black Hole's gravity field (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int blackHoleRadius = 7;

        @Comment("Controls the strength of the Black Hole's gravity field")
        @BoundedDiscrete(min = -1000.0, max = 1000.0)
        public double blackHoleStrength = 10.0;

        @Comment("Controls the radius of the White Hole's gravity field (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int whiteHoleRadius = 7;

        @Comment("Controls the strength of the White Hole's gravity field")
        @BoundedDiscrete(min = -1000.0, max = 1000.0)
        public double whiteHoleStrength = -10.0;
    }

    @SerializedName("Machines & Logistics")
    @Comment("Adjusts settings related to machines, power transmission, and storage logistics")
    @CollapsibleObject
    public MachinesSettings machines = new MachinesSettings();

    public static class MachinesSettings {
        @Comment("Controls the maximum cooldown time of the Chute (in ticks)")
        @BoundedDiscrete(min = 1, max = 80)
        public int chuteMaxCooldown = 8;

        @Comment("Controls the maximum cooldown time of the Batch Crafter (in ticks)")
        @BoundedDiscrete(min = 1, max = 80)
        public int batchCrafterCooldown = 8;

        @Comment("Controls the maximum cooldown time of the Batch Cutter (in ticks)")
        @BoundedDiscrete(min = 1, max = 80)
        public int batchCutterCooldown = 8;

        @Comment("Controls the cooldown time of the Load Monitor (in ticks)")
        @BoundedDiscrete(min = 1, max = 60)
        public int loadMonitorCooldown = 10;

        @Comment("Controls the maximum size of the matrix formed by connecting Transparent Crafting Tables")
        @BoundedDiscrete(min = 3, max = 32)
        public int transparentCraftingTableMaxMatrixSize = 15;

        @Comment("Controls the ripening cooldown of the Induction Light (in ticks)")
        public int inductionLightBlockRipeningCooldown = 400;

        @Comment("Controls the ripening range of the Induction Light (in blocks)")
        @BoundedDiscrete(min = 0, max = 100)
        public int inductionLightBlockRipeningRange = 5;

        @Comment("Controls the interval between Heliostats detections (in ticks)")
        @BoundedDiscrete(min = 1, max = 20)
        public int heliostatsDetectionInterval = 4;

        @Comment("Controls the maximum distance between Heliostats and the block irradiating them (in blocks)")
        @BoundedDiscrete(min = 1, max = 256)
        public int heliostatsMaxIrradiationDistance = 64;

        @Comment("Controls the working interval of the Mineral Fountain (in ticks)")
        @BoundedDiscrete(min = 2, max = 1200)
        public int mineralFountainInterval = 20;

        @Comment("Controls the cooldown between Tesla Tower strikes (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int teslaTowerStrikeCooldown = 80;

        @SerializedName("Discharger FE Per Tick")
        @Comment("Controls the FE extracted per tick by the Discharger (in FE/tick)")
        @BoundedDiscrete(min = 1, max = 1000000)
        public int dischargerFePerTick = 10000;

        @SerializedName("FE Collector Max Energy")
        @Comment("Controls the maximum energy of the FE Collector (in FE)")
        @BoundedDiscrete(min = 1000, max = 1000000000)
        public int feCollectorMaxEnergy = 1000000;

        @SerializedName("FE Collector FE Per Tick")
        @Comment("Controls the FE converted per tick by the FE Collector (in FE/tick)")
        @BoundedDiscrete(min = 1, max = 1000000)
        public int feCollectorFePerTick = 10000;

        @Comment("Controls the interval at which the Item Splitter distributes items (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int itemSplitterInterval = 8;

        @Comment("Controls the maximum distance the Item Splitter distributes items to (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int itemSplitterMaxDistance = 16;

        @Comment("Controls the charge the Charge Collector accepts per incoming window (in kW)")
        @BoundedDiscrete(min = 1, max = 1024)
        public double chargeCollectorMaxPowerPerIncoming = 128;

        @Comment("Controls the input sampling cooldown of the Charge Collector (in ticks)")
        @BoundedDiscrete(min = 1, max = 20)
        public int chargeCollectorInputCooldown = 2;

        @Comment("Controls the output cooldown of the Charge Collector (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int chargeCollectorOutputCooldown = 10;

        @Comment("Controls the base output power of the Infinite Collector (in kW)")
        @BoundedDiscrete(min = 0, max = 4096)
        public int infiniteCollectorBasePower = 256;

        @Comment("Controls the range in which the Infinite Collector collects heat and charges and measures void mass (in blocks)")
        @BoundedDiscrete(min = 1, max = 8)
        public int infiniteCollectorRange = 3;

        @Comment("Controls the power consumed by the Mass Energy Inverter (in kW)")
        @BoundedDiscrete(min = 1, max = 65536)
        public int massEnergyInverterPower = 1024;

        @Comment("Controls the mass injected by the Mass Energy Inverter (in mass/tick)")
        @BoundedDiscrete(min = 1, max = 1000)
        public double massEnergyInverterMassPerTick = 5.0;

        @Comment("Controls the maximum output of the Space Overcompressor per conversion")
        @BoundedDiscrete(min = 1, max = 6400)
        public int spaceOvercompressorMaxOutputPerTime = 640;

        @Comment("Controls the number of items the Large Cauldron processes in parallel")
        @BoundedDiscrete(min = 1, max = 24)
        public int largeCauldronMaxProcessEfficiency = 9;

        @Comment("Controls the number of stacks the Large Cauldron accepts per input slot")
        @BoundedDiscrete(min = 1, max = 64)
        public int largeCauldronInputStackMultiplier = 9;

        @Comment("Controls the capacity of each Large Cauldron tank (in mB)")
        @BoundedDiscrete(min = 1000, max = 1000000)
        public int largeCauldronTankCapacity = 64000;

        @Comment("Controls the maximum number of tropical fish a Fish Tank can hold")
        @BoundedDiscrete(min = 1, max = 16)
        public int fishTankMaxTropicalFish = 4;

        @Comment("Controls the interval between radiation explosions of the Neutron Irradiator (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int neutronIrradiatorRadiationInterval = 40;

        @Comment("Controls the number of Confined Anvilons the Neutron Irradiator needs to select a type")
        @BoundedDiscrete(min = 1, max = 9)
        public int neutronIrradiatorTypeThreshold = 6;

        @Comment("Controls the maximum energy of the Propel Piston (in FE)")
        @BoundedDiscrete(min = 1000000, max = 1600000000)
        public int propelPistonMaxEnergy = 160000000;

        @Comment("Controls the fluid output of megastructures such as the Extractor per tick (in mB/tick)")
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

            @Comment("Controls the maximum height of the Plasma Jets' tube walls (in blocks)")
            @BoundedDiscrete(min = 1, max = 8)
            public int maxTubeHeight = 4;
        }

        @SerializedName("Transmission Pole Range")
        @Comment("Controls the power grid range of Transmission Poles (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int powerTransmitterRange = 8;

        @SerializedName("Remote Transmission Pole Range")
        @Comment("Controls the power grid range of Remote Transmission Poles (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int remotePowerTransmitterRange = 16;

        @Comment("Adjusts settings related to Power Converters")
        @CollapsibleObject
        public PowerConverter powerConverter = new PowerConverter();

        public static class PowerConverter {
            @Comment("Controls the working interval of Power Converters (in ticks)")
            @BoundedDiscrete(min = 1, max = 60)
            public int workInterval = 10;

            @Comment("Controls the energy efficiency of Power Converters (1 kW => xx FE/t)")
            @BoundedDiscrete(min = 1, max = 1000)
            public int efficiency = 100;

            @Comment("Controls the power loss of Power Converters")
            public double loss = 0.1;
        }

        @Comment("Controls the maximum size of the Storages' Recover Station")
        public int storageRecoverMaxSize = 20;

        @Comment("Adjusts settings related to the Storage Port")
        @CollapsibleObject
        public StoragePort storagePort = new StoragePort();

        public static class StoragePort {
            @Comment("Controls the working interval between the Storage Port's scans and item moves (in ticks)")
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

        @Comment("Controls the time a Fluid Tank containing infinite fluid waits for break confirmation (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int infiniteTankBreakConfirmDurationTicks = 200;

        @Comment("Controls the amount at which a Fluid Tank counts as infinite (in mB)")
        @BoundedDiscrete(min = 1000, max = 2000000000)
        public int fluidTankInfinityThreshold = 12800000;

        @Comment("Controls the fluid flow a Pipe gains per block of height difference (in mB/tick)")
        @BoundedDiscrete(min = 1, max = 1000)
        public int pipeFlowPerHeight = 50;

        @Comment("Controls the maximum fluid flow of a Pipe (in mB/tick)")
        @BoundedDiscrete(min = 1, max = 100000)
        public int pipeMaxFlowRate = 2000;

        @Comment("Controls the range of the Local Terminal (in blocks)")
        @BoundedDiscrete(min = 1, max = 256)
        public int localTerminalRange = 32;

        @Comment("Controls the range of the Shulker Terminal (in blocks)")
        @BoundedDiscrete(min = 1, max = 512)
        public int shulkerTerminalRange = 64;

        @Comment("Controls the number of storage operations that can be undone")
        @BoundedDiscrete(min = 0, max = 64)
        public int storageUndoDepth = 4;

        @Comment(
            """
            Controls the maximum heat the Heat Collector can accumulate (in kW)
            The super_heat advancement requires reaching 4096"""
        )
        @BoundedDiscrete(min = 1, max = 1048576)
        public int heatCollectorMaxOutputPower = 4096;

        @Comment(
            """
            Controls the interval of power grid logic (in ticks)
            It is also the unit of the FE/t accounting"""
        )
        @BoundedDiscrete(min = 1, max = 200)
        public int powerGridTick = 20;
    }

    @SerializedName("Equipment & Enchanting")
    @Comment("Adjusts settings related to enchantments, anvil enchanting limits, and equipment")
    @CollapsibleObject
    public EquipmentSettings equipment = new EquipmentSettings();

    public static class EquipmentSettings {
        @Comment("Controls the maximum number of logs cut per level of the Felling enchantment")
        @BoundedDiscrete(min = 2, max = 24)
        public int fellingBlockPerLevel = 4;

        @Comment("Controls the working interval of the Auto Enchanting Table (in ticks)")
        @BoundedDiscrete(min = 1, max = 1000)
        public int autoEnchantingTableInterval = 80;

        @Comment("Controls the maximum valid bookshelves of the Auto Enchanting Table")
        @BoundedDiscrete(min = 1, max = 80)
        public int autoEnchantingTableMaxBookshelf = 15;

        @SerializedName("Auto Enchanting Table EXP Cost Per Shelf")
        @Comment("Controls the EXP Fluid cost per bookshelf level of the Auto Enchanting Table (in mB)")
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

        @Comment("Controls the maximum distance a Block of Magnet attracts (in blocks)")
        @BoundedDiscrete(min = 0, max = 8)
        public int magnetAttractsDistance = 5;

        @Comment("Controls the maximum radius a Handheld Magnet attracts (in blocks)")
        @BoundedDiscrete(min = 1, max = 16)
        public double handheldMagnetAttractsRadius = 8;

        @Comment("Controls the item cooldown applied when a Handheld Magnet is used (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int handheldMagnetCooldown = 5;

        @Comment("Controls whether Lasers are blocked by the collision shapes of blocks along their path")
        public boolean laserImpactChecking = true;

        @SerializedName("Laser Gun Mining Chain Limit")
        @Comment(
            """
            Controls the maximum ore vein size searched while mining with a Laser Gun
            Ore beyond this limit is not chain-mined (default: 64)"""
        )
        @BoundedDiscrete(min = 1, max = 4096)
        public int laserOreClusterMaxSize = 64;

        @SerializedName("Geode Max Search Radius")
        @Comment("Controls the maximum search radius of the Geode (in blocks)")
        @BoundedDiscrete(min = 64, max = 512)
        public int geodeRadius = 64;

        @SerializedName("Geode Search Interval")
        @Comment("Controls the search interval of the Geode (in blocks)")
        @BoundedDiscrete(min = 1, max = 8)
        public int geodeInterval = 4;

        @SerializedName("Geode Search Cooldown")
        @Comment("Controls the search cooldown of the Geode (in seconds)")
        @BoundedDiscrete(min = 5, max = 30)
        public int geodeCooldown = 5;

        @Comment("Controls whether pressing shift and right-clicking takes out all totems stored in the Amulet Box")
        public boolean amuletBoxTakeOutAllTotem = true;

        @Comment("Controls the maximum number of totems the Amulet Box can store")
        @BoundedDiscrete(min = 1, max = 64)
        public int amuletBoxCapacity = 16;

        @Comment("Controls whether Eternal items can be killed by the void (falling out of the world)")
        public boolean eternalItemsVoidKillable = false;

        @Comment("Controls the maximum number of blocks a single Building Rod operation may place")
        @BoundedDiscrete(min = 1, max = 65536)
        public int buildingRodMaxBlocks = 4096;

        @Comment("Controls the maximum energy of the Building Rod (in FE)")
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int buildingRodMaxEnergy = 8000000;

        @Comment("Controls the durability a Frost Metal Ingot restores when repairing an item")
        @BoundedDiscrete(min = 1, max = 10000)
        public int frostMetalIngotRepairAmount = 1080;

        @Comment("Controls the durability a Frost Metal Nugget restores when repairing an item")
        @BoundedDiscrete(min = 1, max = 1000)
        public int frostMetalNuggetRepairAmount = 120;

        @Comment("Controls the duration the Totem of Rage keeps its owner alive (in ticks)")
        @BoundedDiscrete(min = 20, max = 24000)
        public int totemOfRageDuration = 1200;

        @SerializedName("EXP Gem Villager XP")
        @Comment("Controls the experience a Villager gains when an EXP Gem is used on it (in XP)")
        @BoundedDiscrete(min = 0, max = 1000)
        public int expGemVillagerXp = 20;

        @SerializedName("EXP Gem Age Addition")
        @Comment("Controls the amount a Villager ages up when an EXP Gem is used on it (in ticks)")
        @BoundedDiscrete(min = 1, max = 24000)
        public int expGemAgeAddition = 120;

        @Comment("Controls the time the Portable Anvil takes to use (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int portableAnvilUseTicks = 40;

        @Comment("Controls the search radius in which the Cursed Golden Apple affects Players (in blocks)")
        @BoundedDiscrete(min = 1, max = 64)
        public int cursedGoldenAppleSearchRadius = 16;

        @Comment("Controls the durability Fire Reforging restores per tick")
        @BoundedDiscrete(min = 1, max = 1000)
        public int fireReforgingRepairPerTick = 10;

        @Comment("Controls the chance that Providence grants an extra roll")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double providenceExtraRollChance = 0.25;

        @Comment("Controls the chance that Providence grants a third roll")
        @BoundedDiscrete(min = 0.0, max = 1.0)
        public double providenceThirdRollChance = 0.05;

        @Comment("Controls the time the Resonator takes to mine a block by resonance (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int resonatorMiningTicks = 20;

        @Comment("Controls the durability the Resonator consumes per resonance mining operation")
        @BoundedDiscrete(min = 1, max = 10000)
        public int resonatorDurabilityCost = 128;

        @Comment(
            """
            Controls the maximum energy of Energy Weapons (in FE)
            The default matches the item component value of 640M"""
        )
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int energyWeaponMaxEnergy = 640000000;

        @Comment("Controls the maximum number of Anvil Railgun rounds that can be loaded")
        @BoundedDiscrete(min = 1, max = 30)
        public int anvilRailgunMaxAmmo = 16;

        @Comment("Controls the minimum energy the Anvil Railgun needs to fire (in FE)")
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int anvilRailgunMinShotEnergy = 2000000;

        @Comment("Controls the base cooldown of the Tesla Gun, shortened by Quick Charge (in ticks)")
        @BoundedDiscrete(min = 1, max = 1200)
        public int teslaGunCooldown = 80;

        @Comment("Controls the energy the Tesla Gun consumes per shot (in FE)")
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int teslaGunShotEnergy = 4000000;

        @Comment("Controls the energy the Corrupted Beacon Activator consumes per pulse (in FE)")
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int corruptedBeaconPulseEnergy = 200000;

        @Comment("Controls the energy the Spectral Weapon Launcher consumes per shot (in FE)")
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int spectralLauncherShotEnergy = 1600000;

        @Comment("Controls the charge time of the Charged Jump ability (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int chargedJumpChargeTicks = 10;

        @Comment("Controls the time the Charged Jump bar holds its progress after releasing (in ticks)")
        @BoundedDiscrete(min = 0, max = 200)
        public int chargedJumpChargeHoldTicks = 10;

        @Comment("Controls the time the Charged Jump bar takes to decay after holding (in ticks)")
        @BoundedDiscrete(min = 1, max = 200)
        public int chargedJumpChargeDecayTicks = 10;

        @Comment("Controls the jump height multiplier of a fully charged jump")
        @BoundedDiscrete(min = 1.0, max = 10.0)
        public double chargedJumpMaxHeightMultiplier = 3.5;

        @Comment("Controls the Night Vision refresh interval of the Weatherproof Spacesuit Helmet (in ticks)")
        @BoundedDiscrete(min = 200, max = 2400)
        public int nightVisionRefreshTicks = 210;

        @Comment("Controls the maximum energy of the Weatherproof Chestplate (in FE)")
        @BoundedDiscrete(min = 1, max = 2000000000)
        public int weatherproofChestplateMaxEnergy = 160000000;

        @Comment("Controls the energy the Weatherproof Chestplate consumes per tick of flight (in FE)")
        @BoundedDiscrete(min = 1, max = 1000000)
        public int weatherproofChestplateFlightConsumption = 5000;

        @Comment("Controls the duration of the mob effect an Amulet grants while its condition holds (in ticks)")
        @BoundedDiscrete(min = 1, max = 100000)
        public int amuletEffectRefreshTicks = 210;
    }

    @SerializedName("Commands")
    @Comment("Adjusts settings related to the commands the Spacetime Supercomputer may execute")
    @CollapsibleObject
    public CommandsSettings commands = new CommandsSettings();

    public static class CommandsSettings {
        @SerializedName("Allow /locate biome Command")
        @Comment("Controls whether to allow the /locate biome command")
        public boolean allowLocateBiomeCommand = true;

        @SerializedName("Allow /locate structure Command")
        @Comment("Controls whether to allow the /locate structure command")
        public boolean allowLocateStructureCommand = true;

        @SerializedName("Allow /locate poi Command")
        @Comment("Controls whether to allow the /locate poi command")
        public boolean allowLocatePoiCommand = true;

        @SerializedName("Allow /time add Command")
        @Comment("Controls whether to allow the /time add command")
        public boolean allowTimeAddCommand = true;

        @SerializedName("Allow /tick sprint Command")
        @Comment("Controls whether to allow the /tick sprint command")
        public boolean allowTickSprintCommand = true;

        @Comment("Controls the number of power grid entries the power grid command shows")
        @BoundedDiscrete(min = 1, max = 4096)
        public int powergridInfoLimit = 256;
    }
}
