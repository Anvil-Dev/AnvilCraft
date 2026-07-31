package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerChangeBlockTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerClickBlockTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerHurtEntityTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHitPiezoelectricCrystalTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilLootingTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilOnGroundTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.BlockComparatorTurnOverTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ConnectFluidContainersTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ConvertBeaconTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DevourerDevourTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DispenserRepairIronGolem;
import dev.dubhe.anvilcraft.advancements.criterion.ElectricAllergyTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.EnterPowerGridTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.FireReforgeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.HeatCollectorTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.InWorldRecipeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MagnetLiftingAnvilTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MilkTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MineralFountainCreateTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerPlaceTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerShuttleTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerKilledEntityByAnvilHammerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.UseItemTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.VoidCollectorTrigger;
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

    public static void register(IEventBus eventBus) {
        ModCriterionTriggers.REGISTER.register(eventBus);
    }
}
