package dev.dubhe.anvilcraft.data.advancement;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumAdvancementProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.advancements.criterion.ConnectFluidContainersTrigger;
import dev.dubhe.anvilcraft.api.advancement.AdvancementLineHelper;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ModAdvancementsHandler {
    @SuppressWarnings("unused")
    public static void init(RegistrumAdvancementProvider provider) {
        HolderLookup.Provider registries = provider.getProvider();
        HolderGetter<Item> itemLookup = registries.lookupOrThrow(Registries.ITEM);
        HolderGetter<Block> blockLookup = registries.lookupOrThrow(Registries.BLOCK);
        HolderGetter<EntityType<?>> entityTypeLookup = registries.lookupOrThrow(Registries.ENTITY_TYPE);
        AdvancementLineHelper mainLine = new AdvancementLineHelper(AnvilCraft.MOD_ID);
        AdvancementHolder root = mainLine.next()
            .display(
                ModBlocks.ROYAL_ANVIL.asItem(),
                Component.translatable("advancements.anvilcraft.root.title"),
                Component.translatable("advancements.anvilcraft.root.description"),
                AnvilCraft.of("gui/misc/background/advancement"),
                AdvancementType.TASK,
                false,
                true,
                false
            )
            .playerFirstDetected("join")
            .rewardLoot(ModLootTables.ADVANCEMENT_ROOT)
            .save(provider, "root");

        AdvancementLineHelper clawLine = mainLine.createBranch();
        AdvancementHolder crabClaw = clawLine.next()
            .display(
                ModItems.CRAB_CLAW,
                Component.translatable("advancements.anvilcraft.crab_claw.title"),
                Component.translatable("advancements.anvilcraft.crab_claw.description"),
                AdvancementType.TASK
            )
            .hasItems("has_crab_claw", ModItems.CRAB_CLAW)
            .save(provider, "crab_claw");
        AdvancementHolder placer = clawLine.next()
            .display(
                ModBlocks.BLOCK_PLACER.asItem(),
                Component.translatable("advancements.anvilcraft.placer.title"),
                Component.translatable("advancements.anvilcraft.placer.description"),
                AdvancementType.TASK
            )
            .placerPlace("placer_place_placer", ModBlocks.BLOCK_PLACER)
            .save(provider, "block_placer");
        AdvancementHolder placerShuttle = clawLine.createBranch().next()
            .display(
                ModBlocks.SMART_BLOCK_PLACER.asItem(),
                Component.translatable("advancements.anvilcraft.placer_shuttle.title"),
                Component.translatable("advancements.anvilcraft.placer_shuttle.description"),
                AdvancementType.GOAL,
                true
            )
            .placerShuttle("placer_shuttle")
            .save(provider, "placer_shuttle");
        AdvancementHolder devourer = clawLine.next()
            .display(
                ModBlocks.BLOCK_DEVOURER.asItem(),
                Component.translatable("advancements.anvilcraft.devourer.title"),
                Component.translatable("advancements.anvilcraft.devourer.description"),
                AdvancementType.CHALLENGE
            )
            .devourerDevour("devourer_devour_devourer", ModBlocks.BLOCK_DEVOURER)
            .save(provider, "block_devourer");

        AdvancementLineHelper geodeLine = mainLine.createBranch();
        AdvancementHolder geode = geodeLine.next()
            .display(
                ModItems.GEODE,
                Component.translatable("advancements.anvilcraft.geode.title"),
                Component.translatable("advancements.anvilcraft.geode.description"),
                AdvancementType.TASK
            )
            .consumeItem("use_geode", itemLookup, ModItems.GEODE)
            .save(provider, "geode");
        AdvancementHolder amethystPickaxe = geodeLine.next()
            .display(
                ModItems.AMETHYST_PICKAXE,
                Component.translatable("advancements.anvilcraft.amethyst_pickaxe.title"),
                Component.translatable("advancements.anvilcraft.amethyst_pickaxe.description"),
                AdvancementType.TASK
            )
            .recipeAnc("crafting_amethyst_pickaxe", "amethyst_pickaxe")
            .save(provider, "amethyst_pickaxe");
        AdvancementHolder topaz = geodeLine.next()
            .display(
                ModItems.TOPAZ,
                Component.translatable("advancements.anvilcraft.topaz.title"),
                Component.translatable("advancements.anvilcraft.topaz.description"),
                AdvancementType.GOAL
            )
            .consumeItem("use_topaz", itemLookup, ModItems.TOPAZ)
            .save(provider, "topaz");
        AdvancementHolder liftingAnvil = geodeLine.next()
            .display(
                ModBlocks.MAGNET_BLOCK,
                Component.translatable("advancements.anvilcraft.lifting_anvil.title"),
                Component.translatable("advancements.anvilcraft.lifting_anvil.description"),
                AdvancementType.TASK
            )
            .liftingAnvil("lifting_anvil")
            .anvilOnGround("anvil_on_ground")
            .save(provider, "lifting_anvil");

        AdvancementLineHelper autoLine = mainLine.createBranch();
        AdvancementHolder redstoneMilker = autoLine.next()
            .display(
                Blocks.DISPENSER,
                Component.translatable("advancements.anvilcraft.redstone_milker.title"),
                Component.translatable("advancements.anvilcraft.redstone_milker.description"),
                AdvancementType.TASK
            )
            .milk("milk")
            .save(provider, "redstone_milker");
        AdvancementHolder realLooting = autoLine.next()
            .display(
                Blocks.ANVIL,
                Component.translatable("advancements.anvilcraft.real_looting.title"),
                Component.translatable("advancements.anvilcraft.real_looting.description"),
                AdvancementType.TASK
            )
            .anvilLooting("anvil_looting")
            .save(provider, "real_looting");
        AdvancementHolder ironMeterReversal = autoLine.next()
            .display(
                Blocks.IRON_BLOCK,
                Component.translatable("advancements.anvilcraft.iron_meter_reversal.title"),
                Component.translatable("advancements.anvilcraft.iron_meter_reversal.description"),
                AdvancementType.GOAL
            )
            .anvilLooting("anvil_looting_iron_golem", entityTypeLookup, EntityType.IRON_GOLEM)
            .repairIronGolem("repair_iron_golem")
            .save(provider, "iron_meter_reversal");

        AdvancementHolder dang = mainLine.next()
            .display(
                Blocks.ANVIL,
                Component.translatable("advancements.anvilcraft.dang.title"),
                Component.translatable("advancements.anvilcraft.dang.description"),
                AdvancementType.GOAL
            )
            .inWorldRecipe("anything_anvil_crafting")
            .save(provider, "dang");

        AdvancementLineHelper stoneLine = mainLine.createBranch();
        AdvancementHolder stoneCrusher = stoneLine.next()
            .display(
                Blocks.SAND,
                Component.translatable("advancements.anvilcraft.stone_crusher.title"),
                Component.translatable("advancements.anvilcraft.stone_crusher.description"),
                AdvancementType.TASK
            )
            .inWorldRecipeAnc("crush_cobblestone", "block_crush/gravel")
            .inWorldRecipeAnc("crush_gravel", "block_crush/sand")
            .save(provider, "stone_crusher");
        AdvancementHolder fossick = stoneLine.next()
            .display(
                Items.GOLD_NUGGET,
                Component.translatable("advancements.anvilcraft.fossick.title"),
                Component.translatable("advancements.anvilcraft.fossick.description"),
                AdvancementType.TASK
            )
            .inWorldRecipeAnc("mesh", "mesh/sand")
            .save(provider, "fossick");

        AdvancementLineHelper iceLine = mainLine.createBranch();
        AdvancementHolder iceMaker = iceLine.next()
            .display(
                Items.ICE,
                Component.translatable("advancements.anvilcraft.ice_maker.title"),
                Component.translatable("advancements.anvilcraft.ice_maker.description"),
                AdvancementType.TASK
            )
            .inWorldRecipeAnc("make_ice", "squeezing/powder_snow_cauldron_from_snow_block")
            .save(provider, "ice_maker");
        AdvancementHolder four281 = iceLine.next()
            .display(
                Items.BLUE_ICE,
                Component.translatable("advancements.anvilcraft.four281.title"),
                Component.translatable("advancements.anvilcraft.four281.description"),
                AdvancementType.TASK
            )
            .inWorldRecipeAnc("packed_ice", "block_compress/packed_ice")
            .inWorldRecipeAnc("blue_ice", "block_compress/blue_ice")
            .save(provider, "4281");

        AdvancementLineHelper stampingLine = mainLine.createBranch();
        AdvancementHolder vanillaIronPlate = stampingLine.next()
            .display(
                Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                Component.translatable("advancements.anvilcraft.vanilla_iron_plate.title"),
                Component.translatable("advancements.anvilcraft.vanilla_iron_plate.description"),
                AdvancementType.TASK
            )
            .inWorldRecipeAnc("heavy_weighted_pressure_plate", "stamping/heavy_weighted_pressure_plate")
            .save(provider, "vanilla_iron_plate");
        AdvancementHolder recyclingDiamonds = stampingLine.next()
            .display(
                Items.DIAMOND,
                Component.translatable("advancements.anvilcraft.recycling_diamonds.title"),
                Component.translatable("advancements.anvilcraft.recycling_diamonds.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .inWorldRecipeAnc("diamond_pickaxe", "item_crush/tool/diamond_pickaxe_2_diamond")
            .inWorldRecipeAnc("diamond_axe", "item_crush/tool/diamond_axe_2_diamond")
            .inWorldRecipeAnc("diamond_sword", "item_crush/tool/diamond_sword_2_diamond")
            .inWorldRecipeAnc("diamond_hoe", "item_crush/tool/diamond_hoe_2_diamond")
            .inWorldRecipeAnc("diamond_shovel", "item_crush/tool/diamond_shovel_2_diamond")
            .inWorldRecipeAnc("diamond_helmet", "item_crush/armor/diamond_helmet_2_diamond")
            .inWorldRecipeAnc("diamond_chestplate", "item_crush/armor/diamond_chestplate_2_diamond")
            .inWorldRecipeAnc("diamond_leggings", "item_crush/armor/diamond_leggings_2_diamond")
            .inWorldRecipeAnc("diamond_boots", "item_crush/armor/diamond_boots_2_diamond")
            .inWorldRecipeAnc("diamond_horse_armor", "item_crush/armor/diamond_horse_armor_2_diamond")
            .save(provider, "recycling_diamonds");

        AdvancementHolder allInOne = mainLine.next()
            .display(
                ModItems.ANVIL_HAMMER,
                Component.translatable("advancements.anvilcraft.all_in_one.title"),
                Component.translatable("advancements.anvilcraft.all_in_one.description"),
                AdvancementType.TASK
            )
            .recipeAnc("anvil_hammer", "anvil_hammer")
            .recipeAnc("royal_anvil_hammer", "smithing/royal_anvil_hammer")
            .recipeAnc("ember_anvil_hammer", "smithing/ember_anvil_hammer")
            .recipeAnc("transcendence_anvil_hammer", "smithing/transcendence_anvil_hammer")
            .hammerLeftClick("left_click")
            .hammerRightClick("right_click")
            .hammerShiftRightClick("shift_right_click")
            .hammerHurt("hurt_entity")
            .requireAdvs(
                List.of("anvil_hammer", "royal_anvil_hammer", "ember_anvil_hammer", "transcendence_anvil_hammer"),
                List.of("left_click"),
                List.of("right_click"),
                List.of("shift_right_click"),
                List.of("hurt_entity")
            )
            .save(provider, "all_in_one");

        AdvancementHolder saltedFishTurnsOver = mainLine.createBranch().next()
            .display(
                ModBlocks.BLOCK_COMPARATOR,
                Component.translatable("advancements.anvilcraft.salted_fish_turns_over.title"),
                Component.translatable("advancements.anvilcraft.salted_fish_turns_over.description"),
                AdvancementType.TASK,
                true
            )
            .blockComparatorTurnOver("block_comparator_turn_over")
            .save(provider, "salted_fish_turns_over");

        AdvancementLineHelper killingLine = mainLine.createBranch();
        AdvancementHolder hammerAndNail = killingLine.next()
            .display(
                ModItems.ANVIL_HAMMER,
                Component.translatable("advancements.anvilcraft.hammer.title"),
                Component.translatable("advancements.anvilcraft.hammer.description"),
                AdvancementType.CHALLENGE
            )
            .hammerKill("kill_zombie", entityTypeLookup, EntityType.ZOMBIE)
            .hammerKill("kill_skeleton", entityTypeLookup, EntityType.SKELETON)
            .hammerKill("kill_creeper", entityTypeLookup, EntityType.CREEPER)
            .hammerKill("kill_spider", entityTypeLookup, EntityType.SPIDER)
            .hammerKill("kill_pig", entityTypeLookup, EntityType.PIG)
            .hammerKill("kill_cow", entityTypeLookup, EntityType.COW)
            .hammerKill("kill_sheep", entityTypeLookup, EntityType.SHEEP)
            .hammerKill("kill_chicken", entityTypeLookup, EntityType.CHICKEN)
            .save(provider, "hammer");
        AdvancementHolder superKill = killingLine.next()
            .display(
                ModItems.ROYAL_ANVIL_HAMMER,
                Component.translatable("advancements.anvilcraft.super_kill.title"),
                Component.translatable("advancements.anvilcraft.super_kill.description"),
                AdvancementType.CHALLENGE
            )
            .hammerHurt("super_kill", 80)
            .save(provider, "super_kill");

        AdvancementLineHelper electLine = mainLine.createBranch();
        AdvancementHolder heartsOfIron = electLine.next()
            .display(
                ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK,
                Component.translatable("advancements.anvilcraft.hearts_of_iron.title"),
                Component.translatable("advancements.anvilcraft.hearts_of_iron.description"),
                AdvancementType.TASK
            )
            .recipeAnc("craft_magnetoelectric_core", "magnetoelectric_core")
            .save(provider, "hearts_of_iron");

        AdvancementLineHelper generateElectLine = electLine.createBranch();
        AdvancementHolder notBeacon = generateElectLine.next()
            .display(
                ModBlocks.CHARGE_COLLECTOR,
                Component.translatable("advancements.anvilcraft.not_beacon.title"),
                Component.translatable("advancements.anvilcraft.not_beacon.description"),
                AdvancementType.TASK
            )
            .recipeAnc("craft_charge_collector", "charge_collector")
            .playerPlace("place_charge_collector", ModBlocks.CHARGE_COLLECTOR)
            .save(provider, "not_beacon");
        AdvancementHolder lighter = generateElectLine.next()
            .display(
                ModBlocks.PIEZOELECTRIC_CRYSTAL,
                Component.translatable("advancements.anvilcraft.lighter.title"),
                Component.translatable("advancements.anvilcraft.lighter.description"),
                AdvancementType.TASK
            )
            .hitPiezoelectricCrystal("hit_piezoelectric_crystal")
            .save(provider, "lighter");

        AdvancementHolder networking = electLine.next()
            .display(
                ModBlocks.TRANSMISSION_POLE,
                Component.translatable("advancements.anvilcraft.networking.title"),
                Component.translatable("advancements.anvilcraft.networking.description"),
                AdvancementType.TASK
            )
            .recipeAnc("craft_transmission_pole", "transmission_pole")
            .playerPlace("place_transmission_pole", ModBlocks.TRANSMISSION_POLE)
            .save(provider, "networking");
        AdvancementHolder electricFieldRhythm = electLine.next()
            .display(
                ModItems.ANVIL_HAMMER,
                Component.translatable("advancements.anvilcraft.electric_filed_rhythm.title"),
                Component.translatable("advancements.anvilcraft.electric_filed_rhythm.description"),
                AdvancementType.TASK
            )
            .enterPowerGrid("enter_power_grid")
            .save(provider, "electric_filed_rhythm");

        AdvancementHolder industrialGradeSmelting = mainLine.next()
            .display(
                ModBlocks.HEATER,
                Component.translatable("advancements.anvilcraft.industrial_grade_smelting.title"),
                Component.translatable("advancements.anvilcraft.industrial_grade_smelting.description"),
                AdvancementType.GOAL
            )
            .inWorldRecipeTypeAnc("super_heating", "super_heating")
            .save(provider, "industrial_grade_smelting");

        AdvancementHolder waterFlowsDownhill = mainLine.createBranch().next()
            .display(
                ModItems.PIPE,
                Component.translatable("advancements.anvilcraft.water_flows_downhill.title"),
                Component.translatable("advancements.anvilcraft.water_flows_downhill.description"),
                AdvancementType.TASK
            )
            .addCriterion("connect_fluid_containers", ConnectFluidContainersTrigger.TriggerInstance.connectFluidContainers())
            .save(provider, "water_flows_downhill");

        AdvancementHolder nobleMetal = mainLine.next()
            .display(
                ModItems.ROYAL_STEEL_INGOT,
                Component.translatable("advancements.anvilcraft.noble_metal.title"),
                Component.translatable("advancements.anvilcraft.noble_metal.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .hasItemAny("has_", ModBlocks.ROYAL_STEEL_BLOCK, ModItems.ROYAL_STEEL_INGOT, ModItems.ROYAL_STEEL_NUGGET)
            .save(provider, "noble_metal");

        AdvancementHolder overseer = mainLine.createBranch().next()
            .display(
                ModBlocks.OVERSEER,
                Component.translatable("advancements.anvilcraft.overseer.title"),
                Component.translatable("advancements.anvilcraft.overseer.description"),
                AdvancementType.TASK
            )
            .recipeAnc("craft_overseer", "overseer")
            .save(provider, "overseer");

        AdvancementHolder smithingTable = mainLine.next()
            .display(
                ModBlocks.ROYAL_SMITHING_TABLE,
                Component.translatable("advancements.anvilcraft.smithing_table.title"),
                Component.translatable("advancements.anvilcraft.smithing_table.description"),
                AdvancementType.TASK
            )
            .recipeAnc("craft_smithing", "smithing/royal_smithing_table")
            .save(provider, "smithing_table");

        AdvancementHolder durableGoods = mainLine.createBranch().next()
            .display(
                ModItems.ROYAL_STEEL_PICKAXE,
                Component.translatable("advancements.anvilcraft.durable_goods.title"),
                Component.translatable("advancements.anvilcraft.durable_goods.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .recipeAnc("royal_steel_pickaxe", "smithing/royal_steel_pickaxe")
            .recipeAnc("royal_steel_axe", "smithing/royal_steel_axe")
            .recipeAnc("royal_steel_shovel", "smithing/royal_steel_shovel")
            .recipeAnc("royal_steel_hoe", "smithing/royal_steel_hoe")
            .recipeAnc("royal_steel_sword", "smithing/royal_steel_sword")
            .save(provider, "durable_goods");

        AdvancementHolder royalBlacksmith = mainLine.next()
            .display(
                ModBlocks.ROYAL_ANVIL,
                Component.translatable("advancements.anvilcraft.royal_blacksmith.title"),
                Component.translatable("advancements.anvilcraft.royal_blacksmith.description"),
                AdvancementType.TASK
            )
            .hasItems("has_royal_anvil", ModBlocks.ROYAL_ANVIL)
            .hasItems("has_royal_smithing_table", ModBlocks.ROYAL_SMITHING_TABLE)
            .hasItems("has_royal_grindstone", ModBlocks.ROYAL_GRINDSTONE)
            .save(provider, "royal_blacksmith");
        AdvancementHolder wither = mainLine.next()
            .display(
                ModBlocks.CORRUPTED_BEACON,
                Component.translatable("advancements.anvilcraft.wither.title"),
                Component.translatable("advancements.anvilcraft.wither.description"),
                AdvancementType.TASK
            )
            .convertBeacon("convert_beacon")
            .save(provider, "wither");
        AdvancementHolder ripVanWinkle = mainLine.next()
            .display(
                ModBlocks.CORRUPTED_BEACON,
                Component.translatable("advancements.anvilcraft.rip_van_winkle.title"),
                Component.translatable("advancements.anvilcraft.rip_van_winkle.description"),
                AdvancementType.GOAL
            )
            .inWorldRecipeTypeAnc("time_warp_recipe", "time_warp")
            .save(provider, "rip_van_winkle");

        AdvancementLineHelper frostLine = mainLine.createBranch();
        AdvancementHolder frostMetal = frostLine.next()
            .display(
                ModItems.FROST_METAL_INGOT,
                Component.translatable("advancements.anvilcraft.frost_metal.title"),
                Component.translatable("advancements.anvilcraft.frost_metal.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .hasItemAny("has_", ModBlocks.FROST_METAL_BLOCK, ModItems.FROST_METAL_INGOT, ModItems.FROST_METAL_NUGGET)
            .save(provider, "frost_metal");
        AdvancementHolder taiShangWangQing = frostLine.next()
            .display(
                ModItems.FROST_METAL_SWORD,
                Component.translatable("advancements.anvilcraft.tai_shang_wang_qing.title"),
                Component.translatable("advancements.anvilcraft.tai_shang_wang_qing.description"),
                AdvancementType.CHALLENGE
            )
            .hurt(
                "hurt",
                49,
                ModItems.FROST_METAL_SWORD,
                ModItems.FROST_METAL_AXE,
                ModItems.FROST_METAL_PICKAXE,
                ModItems.FROST_METAL_SHOVEL,
                ModItems.FROST_METAL_HOE,
                ModItems.FROST_METAL_HEAVY_HALBERD
            )
            .save(provider, "tai_shang_wang_qing");

        AdvancementLineHelper emberLine = mainLine.createBranch();
        AdvancementHolder forAeons = emberLine.next()
            .display(
                ModItems.OIL_BUCKET,
                Component.translatable("advancements.anvilcraft.for_aeons.title"),
                Component.translatable("advancements.anvilcraft.for_aeons.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .inWorldRecipeAnc("oil_from_raw_beef", "time_warp/oil_from_foods/raw_beef")
            .inWorldRecipeAnc("oil_from_raw_chicken", "time_warp/oil_from_foods/raw_chicken")
            .inWorldRecipeAnc("oil_from_raw_fish", "time_warp/oil_from_foods/raw_fish")
            .inWorldRecipeAnc("oil_from_raw_mutton", "time_warp/oil_from_foods/raw_mutton")
            .inWorldRecipeAnc("oil_from_raw_porkchop", "time_warp/oil_from_foods/raw_porkchop")
            .inWorldRecipeAnc("oil_from_raw_rabbit", "time_warp/oil_from_foods/raw_rabbit")
            .inWorldRecipeAnc("oil_from_piglin_head", "time_warp/oil_from_piglin_head")
            .inWorldRecipeAnc("oil_from_rotten_flesh", "time_warp/oil_from_rotten_flesh")
            .inWorldRecipeAnc("oil_from_spider_eye", "time_warp/oil_from_spider_eye")
            .inWorldRecipeAnc("oil_from_zombie_head", "time_warp/oil_from_zombie_head")
            .save(provider, "for_aeons");
        AdvancementHolder forgedOverEons = emberLine.next()
            .display(
                ModItems.EMBER_METAL_INGOT,
                Component.translatable("advancements.anvilcraft.forged_over_eons.title"),
                Component.translatable("advancements.anvilcraft.forged_over_eons.description"),
                AdvancementType.GOAL
            )
            .requireAny()
            .hasItems("has_ember_metal_block", ModBlocks.EMBER_METAL_BLOCK)
            .hasItems("has_ember_metal_ingot", ModItems.EMBER_METAL_INGOT)
            .hasItems("has_ember_metal_nugget", ModItems.EMBER_METAL_NUGGET)
            .save(provider, "forged_over_eons");

        AdvancementHolder iceAndFire = emberLine.createBranch().next()
            .display(
                ModBlocks.EMBER_ANVIL,
                Component.translatable("advancements.anvilcraft.ice_and_fire.title"),
                Component.translatable("advancements.anvilcraft.ice_and_fire.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .hasItems(
                "frost_and_ember_workstations",
                ModBlocks.EMBER_ANVIL,
                ModBlocks.EMBER_GRINDSTONE,
                ModBlocks.EMBER_SMITHING_TABLE,
                ModBlocks.FROST_ANVIL,
                ModBlocks.FROST_GRINDSTONE,
                ModBlocks.FROST_SMITHING_TABLE
            )
            .save(provider, "ice_and_fire");

        AdvancementHolder selfInFlaming = emberLine.next()
            .display(
                ModItems.EMBER_METAL_PICKAXE,
                Component.translatable("advancements.anvilcraft.self_in_flaming.title"),
                Component.translatable("advancements.anvilcraft.self_in_flaming.description"),
                AdvancementType.TASK
            )
            .fireReforge("fire_reforge")
            .save(provider, "self_in_flaming");

        AdvancementLineHelper gemLine = mainLine.createBranch();
        AdvancementHolder gemTransform = gemLine.next()
            .display(
                ModItems.RUBY,
                Component.translatable("advancements.anvilcraft.gem_transform.title"),
                Component.translatable("advancements.anvilcraft.gem_transform.description"),
                AdvancementType.TASK
            )
            .requireAny()
            .inWorldRecipeAnc("emerald_block", "time_warp/emerald_block")
            .inWorldRecipeAnc("ruby_block", "time_warp/ruby_block")
            .inWorldRecipeAnc("sapphire_block", "time_warp/sapphire_block")
            .inWorldRecipeAnc("topaz_block", "time_warp/topaz_block")
            .save(provider, "gem_transform");

        AdvancementHolder laser = gemLine.createBranch().next()
            .display(
                ModBlocks.RUBY_LASER,
                Component.translatable("advancements.anvilcraft.laser.title"),
                Component.translatable("advancements.anvilcraft.laser.description"),
                AdvancementType.TASK
            )
            .hasItems("has_ruby_laser", ModBlocks.RUBY_LASER)
            .save(provider, "laser");

        AdvancementHolder heatUtilizing = gemLine.next()
            .display(
                ModBlocks.HEAT_COLLECTOR,
                Component.translatable("advancements.anvilcraft.heat_utilizing.title"),
                Component.translatable("advancements.anvilcraft.heat_utilizing.description"),
                AdvancementType.TASK
            )
            .hasItems("has_heat_collector", ModBlocks.HEAT_COLLECTOR)
            .save(provider, "heat_utilizing");

        AdvancementLineHelper nuclearLine = gemLine.createBranch();
        AdvancementHolder isotopeDecayBattery = nuclearLine.next()
            .display(
                ModBlocks.URANIUM_BLOCK,
                Component.translatable("advancements.anvilcraft.isotope_decay_battery.title"),
                Component.translatable("advancements.anvilcraft.isotope_decay_battery.description"),
                AdvancementType.CHALLENGE
            )
            .heatCollectOn("nuclear_sources", BlockStatePredicate.builder().of(ModBlocks.URANIUM_BLOCK, ModBlocks.PLUTONIUM_BLOCK))
            .save(provider, "isotope_decay_battery");
        AdvancementHolder nuclearPower10a = nuclearLine.next()
            .display(
                ModBlocks.HEAT_COLLECTOR,
                Component.translatable("advancements.anvilcraft.nuclear_power_10a.title"),
                Component.translatable("advancements.anvilcraft.nuclear_power_10a.description"),
                AdvancementType.GOAL
            )
            .heatCollectOn("collect_overheated", BlockStatePredicate.builder().of(blockLookup, ModBlockTags.OVERHEATED_BLOCKS))
            .save(provider, "nuclear_power_10a");

        AdvancementHolder superHeat = gemLine.next()
            .display(
                ModBlocks.HEAT_COLLECTOR,
                Component.translatable("advancements.anvilcraft.super_heat.title"),
                Component.translatable("advancements.anvilcraft.super_heat.description"),
                AdvancementType.GOAL
            )
            .heatCollectorOutput("super_heat", MinMaxBounds.Ints.atLeast(HeatCollectorBlockEntity.MAX_OUTPUT_POWER))
            .save(provider, "super_heat");

        AdvancementLineHelper oreLine = mainLine.createBranch();
        AdvancementHolder orePoint = oreLine.next()
            .display(
                ModBlocks.MINERAL_FOUNTAIN,
                Component.translatable("advancements.anvilcraft.ore_point.title"),
                Component.translatable("advancements.anvilcraft.ore_point.description"),
                AdvancementType.TASK
            )
            .mineralFountainCreate("mineral_fountain_create")
            .save(provider, "ore_point");
        AdvancementHolder miningVoid = oreLine.next()
            .display(
                ModItems.VOID_MATTER,
                Component.translatable("advancements.anvilcraft.mining_void.title"),
                Component.translatable("advancements.anvilcraft.mining_void.description"),
                AdvancementType.TASK
            )
            .hasItems("has_void_matter", ModItems.VOID_MATTER)
            .save(provider, "mining_void");

        AdvancementHolder voidGenerateEnergy = oreLine.createBranch().next()
            .display(
                ModBlocks.VOID_ENERGY_COLLECTOR,
                Component.translatable("advancements.anvilcraft.void_generate_energy.title"),
                Component.translatable("advancements.anvilcraft.void_generate_energy.description"),
                AdvancementType.TASK
            )
            .voidCollectorWork("void_collector_work")
            .save(provider, "void_generate_energy");

        AdvancementLineHelper mengerSpongeLine = oreLine.createBranch();
        AdvancementHolder saikouScrubber = mengerSpongeLine.next()
            .display(
                ModBlocks.MENGER_SPONGE,
                Component.translatable("advancements.anvilcraft.saikou_scrubber.title"),
                Component.translatable("advancements.anvilcraft.saikou_scrubber.description"),
                AdvancementType.TASK
            )
            .hasItems("has_menger_sponge", ModBlocks.MENGER_SPONGE)
            .save(provider, "saikou_scrubber");
        AdvancementHolder infinityCapacity = mengerSpongeLine.next()
            .display(
                ModBlocks.LARGE_FLUID_TANK,
                Component.translatable("advancements.anvilcraft.infinity_capacity.title"),
                Component.translatable("advancements.anvilcraft.infinity_capacity.description"),
                AdvancementType.CHALLENGE
            )
            .hasItems("has_menger_sponge", ModBlocks.MENGER_SPONGE)
            .save(provider, "infinity_capacity");

        AdvancementLineHelper neutronLine = oreLine.createBranch();
        AdvancementHolder shulkerBoxWithinShulkerBox = neutronLine.next()
            .display(
                ModBlocks.SPACE_OVERCOMPRESSOR,
                Component.translatable("advancements.anvilcraft.shulker_box_within_shulker_box.title"),
                Component.translatable("advancements.anvilcraft.shulker_box_within_shulker_box.description"),
                AdvancementType.TASK
            )
            .hasItems("has_menger_sponge", ModBlocks.MENGER_SPONGE)
            .save(provider, "shulker_box_within_shulker_box");
        AdvancementHolder spoonOfNeutronStar = neutronLine.next()
            .display(
                ModItems.NEUTRONIUM_INGOT,
                Component.translatable("advancements.anvilcraft.spoon_of_neutron_star.title"),
                Component.translatable("advancements.anvilcraft.spoon_of_neutron_star.description"),
                AdvancementType.GOAL
            )
            .hasItems("has_menger_sponge", ModBlocks.MENGER_SPONGE)
            .save(provider, "spoon_of_neutron_star");

        AdvancementHolder giantAge = mainLine.next()
            .display(
                ModBlocks.GIANT_ANVIL,
                Component.translatable("advancements.anvilcraft.giant_age.title"),
                Component.translatable("advancements.anvilcraft.giant_age.description"),
                AdvancementType.TASK
            )
            .hasItems("has_giant_anvil", ModBlocks.GIANT_ANVIL)
            .save(provider, "giant_age");

        AdvancementHolder anvilAccelerator = mainLine.createBranch().next()
            .display(
                ModBlocks.ACCELERATION_RING,
                Component.translatable("advancements.anvilcraft.anvil_accelerator.title"),
                Component.translatable("advancements.anvilcraft.anvil_accelerator.description"),
                AdvancementType.TASK
            )
            .requireAll()
            .hasItems("has_acceleration_ring", ModBlocks.ACCELERATION_RING)
            .hasItems("has_deflection_ring", ModBlocks.DEFLECTION_RING)
            .save(provider, "anvil_accelerator");

        AdvancementLineHelper sideLine1 = mainLine.createBranch();
        AdvancementHolder newMatter = sideLine1.next()
            .display(
                ModItems.MULTIPHASE_MATTER,
                Component.translatable("advancements.anvilcraft.new_matter.title"),
                Component.translatable("advancements.anvilcraft.new_matter.description"),
                AdvancementType.GOAL
            )
            .requireAny()
            .recipeAnc("uranium", "anvil_collision/anvil_tier_1_and_redstone_block_32")
            .recipeAnc("multiphase_matter", "anvil_collision/ember_anvil_and_frost_metal_block_32")
            .recipeAnc("negative_matter", "anvil_collision/anvil_tier_1_and_levitation_powder_block_32")
            .save(provider, "new_matter");
        AdvancementHolder anvilon = sideLine1.next()
            .display(
                ModBlocks.CONFINED_SPACE_ANVILON,
                Component.translatable("advancements.anvilcraft.anvilon.title"),
                Component.translatable("advancements.anvilcraft.anvilon.description"),
                AdvancementType.CHALLENGE
            )
            .requireAny()
            .recipeAnc("mass_16", "anvil_collision/anvil_tier_0_and_giant_anvil_32")
            .recipeAnc("energy_8", "anvil_collision/anvil_tier_0_and_giant_anvil_128")
            .recipeAnc("time_8", "anvil_collision/anvil_tier_0_and_corrupted_beacon_32")
            .recipeAnc("energy_4_beacon", "anvil_collision/anvil_tier_0_and_corrupted_beacon_128")
            .recipeAnc("space_8", "anvil_collision/anvil_tier_0_and_space_overcompressor_32")
            .recipeAnc("energy_4_space", "anvil_collision/anvil_tier_0_and_space_overcompressor_128")
            .save(provider, "anvilon");

        AdvancementHolder overheated = mainLine.next()
            .display(
                ModBlocks.OVERHEATED_EMBER_METAL_BLOCK,
                Component.translatable("advancements.anvilcraft.overheated.title"),
                Component.translatable("advancements.anvilcraft.overheated.description"),
                AdvancementType.GOAL
            )
            .requireAny()
            .recipeAnc("uranium_heat", "anvil_collision/anvil_tier_2_and_uranium_block_256")
            .recipeAnc("plutonium_heat", "anvil_collision/anvil_tier_2_and_plutonium_block_256")
            .save(provider, "overheated");
        AdvancementHolder transcendence = mainLine.next()
            .display(
                ModBlocks.TRANSCENDIUM_BLOCK,
                Component.translatable("advancements.anvilcraft.transcendence.title"),
                Component.translatable("advancements.anvilcraft.transcendence.description"),
                AdvancementType.GOAL
            )
            .requireAny()
            .hasItemAny("has_", ModBlocks.TRANSCENDIUM_BLOCK, ModItems.TRANSCENDIUM_INGOT, ModItems.TRANSCENDIUM_NUGGET)
            .save(provider, "transcendence");

        AdvancementHolder electricAllergy = mainLine.createBranch().next()
            .display(
                ModBlocks.TRANSCENDENCE_ANVIL,
                Component.translatable("advancements.anvilcraft.electric_allergy.title"),
                Component.translatable("advancements.anvilcraft.electric_allergy.description"),
                AdvancementType.CHALLENGE
            )
            .requireAny()
            .electricAllergy("electric_allergy")
            .save(provider, "electric_allergy");

        AdvancementHolder theEnd = mainLine.next()
            .display(
                ModBlocks.TRANSCENDENCE_ANVIL,
                Component.translatable("advancements.anvilcraft.the_end.title"),
                Component.translatable("advancements.anvilcraft.the_end.description"),
                AdvancementType.CHALLENGE
            )
            .hasItems("has_transcendence_anvil", ModBlocks.TRANSCENDENCE_ANVIL)
            .save(provider, "the_end");
        AdvancementHolder theStart = mainLine.next()
            .display(
                ModBlocks.CELESTIAL_FORGING_ANVIL,
                Component.translatable("advancements.anvilcraft.the_start.title"),
                Component.translatable("advancements.anvilcraft.the_start.description"),
                AdvancementType.CHALLENGE
            )
            .hasItems("has_celestial_forging_anvil", ModBlocks.CELESTIAL_FORGING_ANVIL)
            .save(provider, "the_start");
    }
}
