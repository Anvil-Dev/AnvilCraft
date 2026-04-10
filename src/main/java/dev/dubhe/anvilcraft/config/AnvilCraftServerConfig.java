package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.fml.config.ModConfig;

@Config(name = AnvilCraft.MOD_ID, type = ModConfig.Type.SERVER)
public class AnvilCraftServerConfig {
    @Comment("Maximum radius of giant anvil's shock behavior")
    @BoundedDiscrete(max = 16, min = 4)
    public int giantAnvilMaxShockRadius = 16;

    @Comment("Maximum depth a lightning strike can reach")
    @BoundedDiscrete(max = 16, min = 1)
    public int lightningStrikeDepth = 2;

    @Comment("Maximum radius a lightning strike can reach")
    public int lightningStrikeRadius = 1;

    @Comment("Maximum length a magnet attracts")
    @BoundedDiscrete(max = 8, min = 0)
    public int magnetAttractsDistance = 5;

    @Comment("Maximum radius a handheld magnet attracts")
    @BoundedDiscrete(max = 16, min = 1)
    public double magnetItemAttractsRadius = 8;

    @Comment("Redstone EMP length generated per block dropped by the anvil")
    @SerializedName("Redstone EMP Radius Per Block")
    @BoundedDiscrete(max = 64, min = 1)
    public int redstoneEmpRadius = 6;

    @Comment("Maximum length of redstone EMP")
    @SerializedName("Redstone Emp Max Radius")
    @BoundedDiscrete(max = 64, min = 1)
    public int redstoneEmpMaxRadius = 24;

    @Comment("Maximum cooldown time of chute (in ticks)")
    @BoundedDiscrete(max = 80, min = 1)
    public int chuteMaxCooldown = 8;

    @Comment("Maximum cooldown time of batch crafter (in ticks)")
    @BoundedDiscrete(max = 80, min = 1)
    public int batchCrafterCooldown = 8;

    @Comment("Maximum cooldown time of batch cutter (in ticks)")
    @BoundedDiscrete(max = 80, min = 1)
    public int batchCutterCooldown = 8;

    @Comment("The maximum search radius of the geode")
    @SerializedName("Geode Maximum Search Radius")
    @BoundedDiscrete(max = 512, min = 64)
    public int geodeRadius = 64;

    @Comment("The search interval of the geode")
    @SerializedName("Geode Search Interval")
    @BoundedDiscrete(max = 8, min = 1)
    public int geodeInterval = 4;

    @Comment("The search cooldown of the geode (in seconds)")
    @SerializedName("Geode Search Cooldown")
    @BoundedDiscrete(max = 30, min = 5)
    public int geodeCooldown = 5;

    @Comment("Power grid range of power transmitter")
    @SerializedName("Range of Power Transmitter")
    @BoundedDiscrete(max = 64, min = 1)
    public int powerTransmitterRange = 8;

    @Comment("Power grid range of remote power transmitter")
    @SerializedName("Range of Remote Power Transmitter")
    @BoundedDiscrete(max = 64, min = 1)
    public int remotePowerTransmitterRange = 16;

    @Comment("The maximum number of logs that can be cut per level of Felling enchantment")
    @BoundedDiscrete(max = 24, min = 2)
    public int fellingBlockPerLevel = 2;

    @Comment("Should show anvil levitate animation")
    public boolean displayAnvilAnimation = true;

    @Comment("Maximum cooldown of load monitor")
    @BoundedDiscrete(max = 60, min = 1)
    public int loadMonitor = 10;

    @Comment("Maximum size for connecting transparent crafting table to form matrix")
    @BoundedDiscrete(max = 32, min = 3)
    public int transparentCraftingTableMaxMatrixSize = 15;

    @CollapsibleObject
    public PowerConverter powerConverter = new PowerConverter();

    @Comment("Is laser do impact checking")
    public boolean isLaserDoImpactChecking = true;

    @Comment("Induction light block ripening cooldown")
    public int inductionLightBlockRipeningCooldown = 400;

    @Comment("Induction light block ripening range")
    @BoundedDiscrete(min = 0, max = 100)
    public int inductionLightBlockRipeningRange = 5;

    @Comment("The number of ticks between heliostat detections")
    @SerializedName("Heliostats detection interval")
    @BoundedDiscrete(max = 20, min = 1)
    public int heliostatsDetectionInterval = 4;

    @Comment("Iono Craft Backpack Max Flight Time in ticks")
    public int ionoCraftBackpackMaxFlightTime = 1200 * 20;

    @Comment("Giant anvil maxCount fall damage")
    @BoundedDiscrete(max = 100, min = 0)
    public int giantAnvilFallDamageMax = 40;

    @Comment("Block Devourer upward chain devouring blocks within tag #anvilcraft:block_devourer_chain_devouring")
    public boolean blockDevourerUpwardChainDevouring = true;

    @Comment("Block Devourer upward chain devouring maxCount distance")
    @BoundedDiscrete(max = 15, min = 0)
    public int blockDevourerUpwardChainDevouringDistance = 8;

    @Comment("Block Placer recursive retrieval of container max distance")
    @BoundedDiscrete(max = 20, min = 0)
    public int blockPlacerRecursiveRetrievalDistanceMax = 7;

    @Comment("Combining items with Enchanted Books beyond max level in Royal Anvil")
    public boolean royalAnvilBeyondMaxLevel = false;

    @Comment("Combining items with Enchanted Books beyond max level in Frost Anvil")
    public boolean frostAnvilBeyondMaxLevel = false;

    @Comment("Combining items with Enchanted Books beyond max level in Ember Anvil")
    public boolean emberAnvilBeyondMaxLevel = false;

    @Comment("Combining items with Enchanted Books beyond max level in Transcendence Anvil")
    public boolean transcendenceAnvilBeyondMaxLevel = true;

    public static class PowerConverter {
        @Comment("The working interval of power converters")
        @BoundedDiscrete(min = 1, max = 60)
        public int powerConverterCountdown = 10;

        @Comment("Energy efficiency of energy converters (1 kW => xx FE/t)")
        @BoundedDiscrete(min = 1, max = 1000)
        public int powerConverterEfficiency = 80;

        @Comment("Power loss of energy converters")
        public double powerConverterLoss = 0.1;
    }

    @Comment("Anvil collision craft speed (m/tick)")
    public int anvilCollisionCraftSpeed = 16;

    @Comment("Press shift and right click to take out all totem stored in the holding amulet box")
    public boolean amuletBoxTakeOutAllTotem = true;

    @Comment("Pushing or pulling a sliding rail will chain to other rails")
    public boolean slidingRailStickToEachOther = false;

    @Comment("Whether to clean fluid after updating Menger Sponge")
    public boolean cleanFluidAfterUpdateMengerSponge = false;

    @Comment("The max size of the entries in multiphases' recover station")
    public int multiphaseRecoverMaxSize = 20;
}
