package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerClickBlockTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerHurtEntityTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHitPiezoelectricCrystalTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilLootingTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilOnGroundTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ConvertBeaconTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DevourerDevourTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DispenserRepairIronGolem;
import dev.dubhe.anvilcraft.advancements.criterion.FireReforgeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.HeatCollectorTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.InWorldRecipeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MagnetLiftingAnvilTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MilkTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MineralFountainCreateTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerPlaceTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerKilledEntityByAnvilHammerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerWearAnvilHammerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.UseItemTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriterionTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> REGISTER =
        DeferredRegister.create(Registries.TRIGGER_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, PlacerPlaceTrigger> PLACER_PLACE_BLOCK = REGISTER.register(
        "placer_place_block",
        PlacerPlaceTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, DevourerDevourTrigger> DEVOURER_DEVOUR_BLOCK = REGISTER.register(
        "devourer_devour_block",
        DevourerDevourTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, UseItemTrigger> USE_ITEM = REGISTER.register("use_item", UseItemTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, MagnetLiftingAnvilTrigger> LIFTING_ANVIL = REGISTER.register(
        "lifting_anvil",
        MagnetLiftingAnvilTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilOnGroundTrigger> ANVIL_ON_GROUND = REGISTER.register(
        "anvil_on_ground",
        AnvilOnGroundTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, MilkTrigger> MILK = REGISTER.register("milk", MilkTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, AnvilLootingTrigger> ANVIL_LOOTING = REGISTER.register(
        "anvil_looting",
        AnvilLootingTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, DispenserRepairIronGolem> REPAIR_IRON_GOLEM = REGISTER.register(
        "repair_iron_golem",
        DispenserRepairIronGolem::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, InWorldRecipeTrigger> IN_WORLD_RECIPE = REGISTER.register(
        "in_world_recipe",
        InWorldRecipeTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHammerClickBlockTrigger> ANVIL_HAMMER_CLICK_BLOCK = REGISTER.register(
        "anvil_hammer_click_block",
        AnvilHammerClickBlockTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHammerHurtEntityTrigger> ANVIL_HAMMER_HURT_ENTITY = REGISTER.register(
        "anvil_hammer_hurt_entity",
        AnvilHammerHurtEntityTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, PlayerKilledEntityByAnvilHammerTrigger> PLAYER_KILLED_ENTITY_BY_ANVIL_HAMMER =
        REGISTER.register("player_killed_entity_by_anvil_hammer", PlayerKilledEntityByAnvilHammerTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, AnvilHitPiezoelectricCrystalTrigger> ANVIL_HIT_PIEZOELECTRIC_CRYSTAL =
        REGISTER.register("anvil_hit_piezoelectric_crystal", AnvilHitPiezoelectricCrystalTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, PlayerWearAnvilHammerTrigger> PLAYER_WEAR_ANVIL_HAMMER = REGISTER.register(
        "player_wear_anvil_hammer",
        PlayerWearAnvilHammerTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, ConvertBeaconTrigger> CONVERT_BEACON = REGISTER.register(
        "convert_beacon",
        ConvertBeaconTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, FireReforgeTrigger> FIRE_REFORGE = REGISTER.register(
        "fire_reforge",
        FireReforgeTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, HeatCollectorTrigger> HEAT_COLLECTOR_COLLECT = REGISTER.register(
        "heat_collector_collect",
        HeatCollectorTrigger::new
    );

    public static final DeferredHolder<CriterionTrigger<?>, MineralFountainCreateTrigger> MINERAL_FOUNTAIN_CREATE = REGISTER.register(
        "mineral_fountain_crate",
        MineralFountainCreateTrigger::new
    );

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
