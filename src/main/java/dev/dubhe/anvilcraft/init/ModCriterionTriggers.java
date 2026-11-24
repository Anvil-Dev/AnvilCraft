package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerChangeBlockTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerClickBlockTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerHurtEntityTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHitPiezoelectricCrystalTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilLootingTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilOnGroundTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.BigStomachTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.BlockComparatorTurnOverTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.CelestialVisitTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.CelestialEvolutionTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.CompressedBiscuitTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ConnectFluidContainersTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ConvertBeaconTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DevourerDevourTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DiceTripleSixTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DispenserRepairIronGolem;
import dev.dubhe.anvilcraft.advancements.criterion.DysonSphereTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ElectricAllergyTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.EnterPowerGridTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.FireReforgeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.FullSpacesuitTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.GammaLaserTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.HeatCollectorTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.HighVoltageTowerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.HyperdimensionStationUpgradeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.InWorldRecipeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.LargeCauldronCocktailTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.InexhaustibleTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.InfinitePowerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MagnetLiftingAnvilTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MilkTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MineralFountainCreateTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MonolithTouchTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MoonFarSideTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MoonLandingTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MultiBlockFormTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PillBoxEightTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PipeConnectContainersTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerPlaceTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerShuttleTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlanetDetonationTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlanetaryCollapseDeathTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerKilledEntityByAnvilHammerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerWearAnvilHammerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.RailgunSelfKillTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.RailgunCollisionTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ShulkerContainerUpgradeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.StarContactDeathTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.UseItemTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.VoidCollectorTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.WormholeDeliveryTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.VoidWalkerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.VoidEnergyCollectorWorkingTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.WearBreathingHelmetTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriterionTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> REGISTER =
        DeferredRegister.create(Registries.TRIGGER_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, PlacerPlaceTrigger> PLACER_PLACE_BLOCK = ModCriterionTriggers.REGISTER.register(
        "placer_place_block",
        PlacerPlaceTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, PlacerShuttleTrigger> PLACER_SHUTTLE = ModCriterionTriggers.REGISTER.register(
        "placer_shuttle",
        PlacerShuttleTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, DevourerDevourTrigger> DEVOURER_DEVOUR_BLOCK =
        ModCriterionTriggers.REGISTER.register(
        "devourer_devour_block",
        DevourerDevourTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, MagnetLiftingAnvilTrigger> LIFTING_ANVIL =
        ModCriterionTriggers.REGISTER.register(
        "lifting_anvil",
        MagnetLiftingAnvilTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilOnGroundTrigger> ANVIL_ON_GROUND = ModCriterionTriggers.REGISTER.register(
        "anvil_on_ground",
        AnvilOnGroundTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, MilkTrigger> MILK =
        ModCriterionTriggers.REGISTER.register("milk", MilkTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, AnvilLootingTrigger> ANVIL_LOOTING = ModCriterionTriggers.REGISTER.register(
        "anvil_looting",
        AnvilLootingTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, DispenserRepairIronGolem> REPAIR_IRON_GOLEM =
        ModCriterionTriggers.REGISTER.register(
        "repair_iron_golem",
        DispenserRepairIronGolem::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, InWorldRecipeTrigger> IN_WORLD_RECIPE = ModCriterionTriggers.REGISTER.register(
        "in_world_recipe",
        InWorldRecipeTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHammerClickBlockTrigger> ANVIL_HAMMER_CLICK_BLOCK =
        ModCriterionTriggers.REGISTER.register(
        "anvil_hammer_click_block",
        AnvilHammerClickBlockTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHammerChangeBlockTrigger> ANVIL_HAMMER_CHANGE_BLOCK =
        ModCriterionTriggers.REGISTER.register(
        "anvil_hammer_change_block",
        AnvilHammerChangeBlockTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHammerHurtEntityTrigger> ANVIL_HAMMER_HURT_ENTITY =
        ModCriterionTriggers.REGISTER.register(
        "anvil_hammer_hurt_entity",
        AnvilHammerHurtEntityTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, PlayerKilledEntityByAnvilHammerTrigger> PLAYER_KILLED_ENTITY_BY_ANVIL_HAMMER =
        ModCriterionTriggers.REGISTER.register("player_killed_entity_by_anvil_hammer", PlayerKilledEntityByAnvilHammerTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHitPiezoelectricCrystalTrigger> ANVIL_HIT_PIEZOELECTRIC_CRYSTAL =
        ModCriterionTriggers.REGISTER.register("anvil_hit_piezoelectric_crystal", AnvilHitPiezoelectricCrystalTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, EnterPowerGridTrigger> ENTER_POWER_GRID =
        ModCriterionTriggers.REGISTER.register(
        "enter_power_grid",
        EnterPowerGridTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, ConvertBeaconTrigger> CONVERT_BEACON = ModCriterionTriggers.REGISTER.register(
        "convert_beacon",
        ConvertBeaconTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, FireReforgeTrigger> FIRE_REFORGE = ModCriterionTriggers.REGISTER.register(
        "fire_reforge",
        FireReforgeTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, ElectricAllergyTrigger> ELECTRIC_ALLERGY =
        ModCriterionTriggers.REGISTER.register(
        "electric_allergy",
        ElectricAllergyTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, ConnectFluidContainersTrigger> CONNECT_FLUID_CONTAINERS =
        ModCriterionTriggers.REGISTER.register(
        "connect_fluid_containers",
        ConnectFluidContainersTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, HeatCollectorTrigger> HEAT_COLLECTOR_COLLECT =
        ModCriterionTriggers.REGISTER.register(
        "heat_collector_collect",
        HeatCollectorTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, VoidCollectorTrigger> VOID_COLLECTOR_COLLECT =
        ModCriterionTriggers.REGISTER.register(
        "void_collector_collect",
        VoidCollectorTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, MineralFountainCreateTrigger> MINERAL_FOUNTAIN_CREATE =
        ModCriterionTriggers.REGISTER.register(
        "mineral_fountain_crate",
        MineralFountainCreateTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, UseItemTrigger> USE_ITEM = ModCriterionTriggers.REGISTER.register(
        "use_item",
        UseItemTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, BlockComparatorTurnOverTrigger> BLOCK_COMPARATOR_TURN_OVER =
        ModCriterionTriggers.REGISTER.register("block_comparator_turn_over", BlockComparatorTurnOverTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, MultiBlockFormTrigger> MULTI_BLOCK_FORM =
        ModCriterionTriggers.REGISTER.register("multi_block_form", MultiBlockFormTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PlayerWearAnvilHammerTrigger> PLAYER_WEAR_ANVIL_HAMMER =
        ModCriterionTriggers.REGISTER.register("player_wear_anvil_hammer", PlayerWearAnvilHammerTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PipeConnectContainersTrigger> PIPE_CONNECT_CONTAINERS =
        ModCriterionTriggers.REGISTER.register("pipe_connect_containers", PipeConnectContainersTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, VoidEnergyCollectorWorkingTrigger> VOID_ENERGY_COLLECTOR_WORKING =
        ModCriterionTriggers.REGISTER.register("void_energy_collector_working", VoidEnergyCollectorWorkingTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, CelestialEvolutionTrigger> CELESTIAL_EVOLUTION =
        REGISTER.register("celestial_evolution", CelestialEvolutionTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PlanetDetonationTrigger> PLANET_DETONATION =
        REGISTER.register("planet_detonation", PlanetDetonationTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PlanetaryCollapseDeathTrigger> PLANETARY_COLLAPSE_DEATH =
        REGISTER.register("planetary_collapse_death", PlanetaryCollapseDeathTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, StarContactDeathTrigger> STAR_CONTACT_DEATH =
        REGISTER.register("star_contact_death", StarContactDeathTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, MonolithTouchTrigger> MONOLITH_TOUCH =
        REGISTER.register("monolith_touch", MonolithTouchTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, DiceTripleSixTrigger> DICE_TRIPLE_SIX =
        REGISTER.register("dice_triple_six", DiceTripleSixTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, MoonLandingTrigger> MOON_LANDING =
        REGISTER.register("moon_landing", MoonLandingTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, MoonFarSideTrigger> MOON_FAR_SIDE =
        REGISTER.register("moon_far_side", MoonFarSideTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, CompressedBiscuitTrigger> COMPRESSED_BISCUIT =
        REGISTER.register("compressed_biscuit", CompressedBiscuitTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, BigStomachTrigger> BIG_STOMACH =
        REGISTER.register("big_stomach", BigStomachTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PillBoxEightTrigger> PILL_BOX_EIGHT =
        REGISTER.register("pill_box_eight", PillBoxEightTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, RailgunCollisionTrigger> RAILGUN_COLLISION =
        REGISTER.register("railgun_collision", RailgunCollisionTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, WearBreathingHelmetTrigger> WEAR_BREATHING_HELMET =
        REGISTER.register("wear_breathing_helmet", WearBreathingHelmetTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, ShulkerContainerUpgradeTrigger> SHULKER_CONTAINER_UPGRADE =
        REGISTER.register("shulker_container_upgrade", ShulkerContainerUpgradeTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, HyperdimensionStationUpgradeTrigger> HYPERDIMENSION_STATION_UPGRADE =
        REGISTER.register("hyperdimension_station_upgrade", HyperdimensionStationUpgradeTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, LargeCauldronCocktailTrigger> LARGE_CAULDRON_COCKTAIL =
        REGISTER.register("large_cauldron_cocktail", LargeCauldronCocktailTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, FullSpacesuitTrigger> FULL_SPACESUIT =
        REGISTER.register("full_spacesuit", FullSpacesuitTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, VoidWalkerTrigger> VOID_WALKER =
        REGISTER.register("void_walker", VoidWalkerTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, GammaLaserTrigger> GAMMA_LASER =
        REGISTER.register("gamma_laser", GammaLaserTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, DysonSphereTrigger> DYSON_SPHERE =
        REGISTER.register("dyson_sphere", DysonSphereTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, InexhaustibleTrigger> INEXHAUSTIBLE =
        REGISTER.register("inexhaustible", InexhaustibleTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, RailgunSelfKillTrigger> RAILGUN_SELF_KILL =
        REGISTER.register("railgun_self_kill", RailgunSelfKillTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, WormholeDeliveryTrigger> WORMHOLE_DELIVERY =
        REGISTER.register("wormhole_delivery", WormholeDeliveryTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, CelestialVisitTrigger> CELESTIAL_VISIT =
        REGISTER.register("celestial_visit", CelestialVisitTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, HighVoltageTowerTrigger> HIGH_VOLTAGE_TOWER =
        REGISTER.register("high_voltage_tower", HighVoltageTowerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, InfinitePowerTrigger> INFINITE_POWER =
        REGISTER.register("infinite_power", InfinitePowerTrigger::new);
    public static void register(IEventBus eventBus) {
        ModCriterionTriggers.REGISTER.register(eventBus);
    }
}
