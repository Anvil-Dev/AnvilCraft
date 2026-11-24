package dev.dubhe.anvilcraft.data.advancement;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumAdvancementProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerHurtEntityTrigger;
import dev.dubhe.anvilcraft.api.advancement.AdvancementLineHelper;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTerminal;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class AnvilCraftAdvancement {
    public static void init(RegistrumAdvancementProvider provider) {
        AdvancementLineHelper mainLine = new AdvancementLineHelper(provider, AnvilCraft.MOD_ID);
        mainLine.next()
            .root(ModBlocks.ROYAL_ANVIL, SharedTextures.bg("misc", "advancement"))
            .playerFirstDetected("join")
            .rewardLoot(ModLootTables.ADVANCEMENT_ROOT)
            .save("root");

        AdvancementLineHelper wisdomLine = mainLine.createBranch();
        wisdomLine.next()
            .task(ModBlocks.MONOLITH, "wisdom_tree")
            .monolithTouch("monolith_touch")
            .save("wisdom_tree");

        AdvancementLineHelper diceLine = mainLine.createBranch();
        diceLine.next()
            .goal(ModBlocks.REDSTONE_DICE, "dice_triple_six")
            .diceTripleSix("dice_triple_six")
            .save("dice_triple_six");

        AdvancementLineHelper tankLine = mainLine.createBranch();
        tankLine.next()
            .task(ModBlocks.FISH_TANK, "fish_tank")
            .recipeAnc("craft_fish_tank", "fish_tank")
            .playerPlace("place_fish_tank", ModBlocks.FISH_TANK)
            .requireAll()
            .save("fish_tank");
        tankLine.next()
            .task(ModItems.BREATHING_HELMET, "breathing_helmet")
            .wearBreathingHelmet("wear_breathing_helmet")
            .save("breathing_helmet");
        tankLine.next()
            .goal(ModBlocks.LUNAR_ROCK, "moon_landing")
            .moonLanding("moon_landing")
            .save("moon_landing");
        tankLine.next()
            .challenge(ModBlocks.LUNAR_SOIL, "moon_far_side")
            .moonFarSide("moon_far_side")
            .save("moon_far_side");

        AdvancementLineHelper clawLine = mainLine.createBranch();
        clawLine.next()
            .task(ModItems.CRAB_CLAW, "crab_claw")
            .hasItems("has_crab_claw", ModItems.CRAB_CLAW)
            .save("crab_claw");
        clawLine.next()
            .task(ModBlocks.BLOCK_PLACER, "placer")
            .placerPlace("placer_place_placer", ModBlocks.BLOCK_PLACER)
            .save("block_placer");
        clawLine.createBranch().next()
            .goal(ModBlocks.SMART_BLOCK_PLACER, "placer_shuttle", true)
            .placerShuttle("placer_shuttle")
            .save("placer_shuttle");
        clawLine.next()
            .challenge(ModBlocks.BLOCK_DEVOURER, "devourer")
            .devourerDevour("devourer_devour_devourer", ModBlocks.BLOCK_DEVOURER)
            .save("block_devourer");

        AdvancementLineHelper geodeLine = mainLine.createBranch();
        geodeLine.next()
            .task(ModItems.GEODE, "geode")
            .useItem("use_geode", ModItems.GEODE)
            .save("geode");
        geodeLine.next()
            .task(ModItems.AMETHYST_PICKAXE, "amethyst_pickaxe")
            .recipeAnc("crafting_amethyst_pickaxe", "amethyst_pickaxe")
            .save("amethyst_pickaxe");
        geodeLine.next()
            .goal(ModItems.TOPAZ, "topaz")
            .useItem("use_topaz", ModItems.TOPAZ)
            .save("topaz");
        geodeLine.next()
            .task(ModBlocks.MAGNET_BLOCK, "lifting_anvil")
            .liftingAnvil("lifting_anvil")
            .anvilOnGround("anvil_on_ground")
            .save("lifting_anvil");

        AdvancementLineHelper autoLine = mainLine.createBranch();
        autoLine.next()
            .task(Blocks.DISPENSER, "redstone_milker")
            .milk("milk")
            .save("redstone_milker");
        autoLine.next()
            .task(Blocks.ANVIL, "real_looting")
            .anvilLooting("anvil_looting")
            .save("real_looting");
        autoLine.next()
            .goal(Blocks.IRON_BLOCK, "iron_meter_reversal")
            .anvilLooting("anvil_looting_iron_golem", EntityType.IRON_GOLEM)
            .repairIronGolem("repair_iron_golem")
            .save("iron_meter_reversal");

        mainLine.next()
            .goal(Blocks.ANVIL, "dang")
            .inWorldRecipe("anything_anvil_crafting")
            .save("dang");

        AdvancementLineHelper foodLine = mainLine.createBranch();
        foodLine.next()
            .task(ModFoodItems.CANNED_FOOD, "compressed_biscuit")
            .compressedBiscuit("compressed_biscuit")
            .save("compressed_biscuit");
        foodLine.next()
            .challenge(ModItems.TIN_CAN, "big_stomach")
            .bigStomach("big_stomach")
            .save("big_stomach");

        mainLine.createBranch().next()
            .goal(ModItems.PILL_BOX, "pill_box_eight")
            .pillBoxEight("pill_box_eight")
            .save("pill_box_eight");

        AdvancementLineHelper stoneLine = mainLine.createBranch();
        stoneLine.next()
            .task(Blocks.SAND, "stone_crusher")
            .inWorldRecipeAnc("crush_cobblestone", "block_crush/gravel")
            .inWorldRecipeAnc("crush_gravel", "block_crush/sand")
            .save("stone_crusher");
        stoneLine.next()
            .task(Items.GOLD_NUGGET, "fossick")
            .inWorldRecipeAnc("mesh", "mesh/sand")
            .save("fossick");

        AdvancementLineHelper iceLine = mainLine.createBranch();
        iceLine.next()
            .task(Items.ICE, "ice_maker")
            .inWorldRecipeAnc("make_ice", "squeezing/powder_snow_cauldron_from_snow_block")
            .save("ice_maker");
        iceLine.next()
            .task(Items.BLUE_ICE, "four281")
            .inWorldRecipeAnc("packed_ice", "block_compress/packed_ice")
            .inWorldRecipeAnc("blue_ice", "block_compress/blue_ice")
            .save("4281");

        AdvancementLineHelper stampingLine = mainLine.createBranch();
        stampingLine.next()
            .task(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, "vanilla_iron_plate")
            .inWorldRecipeAnc("heavy_weighted_pressure_plate", "stamping/heavy_weighted_pressure_plate")
            .save("vanilla_iron_plate");
        stampingLine.next()
            .task(Items.DIAMOND, "recycling_diamonds")
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
            .save("recycling_diamonds");

        stampingLine.next()
            .goal(Items.IRON_INGOT, "circular_economy")
            .inWorldRecipeAnc("chainmail_boots", "item_crush/armor/chainmail_boots_2_chain")
            .inWorldRecipeAnc("chainmail_chestplate", "item_crush/armor/chainmail_chestplate_2_chain")
            .inWorldRecipeAnc("chainmail_helmet", "item_crush/armor/chainmail_helmet_2_chain")
            .inWorldRecipeAnc("chainmail_leggings", "item_crush/armor/chainmail_leggings_2_chain")
            .inWorldRecipeAnc("leather_boots", "item_crush/armor/leather_boots_2_leather")
            .inWorldRecipeAnc("leather_chestplate", "item_crush/armor/leather_chestplate_2_leather")
            .inWorldRecipeAnc("leather_helmet", "item_crush/armor/leather_helmet_2_leather")
            .inWorldRecipeAnc("leather_leggings", "item_crush/armor/leather_leggings_2_leather")
            .inWorldRecipeAnc("leather_horse_armor", "item_crush/armor/leather_horse_armor_2_leather")
            .inWorldRecipeAnc("golden_boots", "item_crush/armor/golden_boots_2_gold_ingot")
            .inWorldRecipeAnc("golden_chestplate", "item_crush/armor/golden_chestplate_2_gold_ingot")
            .inWorldRecipeAnc("golden_helmet", "item_crush/armor/golden_helmet_2_gold_ingot")
            .inWorldRecipeAnc("golden_leggings", "item_crush/armor/golden_leggings_2_gold_ingot")
            .inWorldRecipeAnc("golden_horse_armor", "item_crush/armor/golden_horse_armor_2_gold_ingot")
            .inWorldRecipeAnc("golden_axe", "item_crush/tool/golden_axe_2_gold_ingot")
            .inWorldRecipeAnc("golden_hoe", "item_crush/tool/golden_hoe_2_gold_ingot")
            .inWorldRecipeAnc("golden_pickaxe", "item_crush/tool/golden_pickaxe_2_gold_ingot")
            .inWorldRecipeAnc("golden_shovel", "item_crush/tool/golden_shovel_2_gold_ingot")
            .inWorldRecipeAnc("golden_sword", "item_crush/tool/golden_sword_2_gold_ingot")
            .inWorldRecipeAnc("iron_boots", "item_crush/armor/iron_boots_2_iron_ingot")
            .inWorldRecipeAnc("iron_chestplate", "item_crush/armor/iron_chestplate_2_iron_ingot")
            .inWorldRecipeAnc("iron_helmet", "item_crush/armor/iron_helmet_2_iron_ingot")
            .inWorldRecipeAnc("iron_leggings", "item_crush/armor/iron_leggings_2_iron_ingot")
            .inWorldRecipeAnc("iron_horse_armor", "item_crush/armor/iron_horse_armor_2_iron_ingot")
            .inWorldRecipeAnc("iron_axe", "item_crush/tool/iron_axe_2_iron_ingot")
            .inWorldRecipeAnc("iron_hoe", "item_crush/tool/iron_hoe_2_iron_ingot")
            .inWorldRecipeAnc("iron_pickaxe", "item_crush/tool/iron_pickaxe_2_iron_ingot")
            .inWorldRecipeAnc("iron_shovel", "item_crush/tool/iron_shovel_2_iron_ingot")
            .inWorldRecipeAnc("iron_sword", "item_crush/tool/iron_sword_2_iron_ingot")
            .inWorldRecipeAnc("diamond_boots", "item_crush/armor/diamond_boots_2_diamond")
            .inWorldRecipeAnc("diamond_chestplate", "item_crush/armor/diamond_chestplate_2_diamond")
            .inWorldRecipeAnc("diamond_helmet", "item_crush/armor/diamond_helmet_2_diamond")
            .inWorldRecipeAnc("diamond_leggings", "item_crush/armor/diamond_leggings_2_diamond")
            .inWorldRecipeAnc("diamond_horse_armor", "item_crush/armor/diamond_horse_armor_2_diamond")
            .inWorldRecipeAnc("diamond_axe", "item_crush/tool/diamond_axe_2_diamond")
            .inWorldRecipeAnc("diamond_hoe", "item_crush/tool/diamond_hoe_2_diamond")
            .inWorldRecipeAnc("diamond_pickaxe", "item_crush/tool/diamond_pickaxe_2_diamond")
            .inWorldRecipeAnc("diamond_shovel", "item_crush/tool/diamond_shovel_2_diamond")
            .inWorldRecipeAnc("diamond_sword", "item_crush/tool/diamond_sword_2_diamond")
            .requirements(new AdvancementRequirements(List.of(
                List.of("chainmail_boots", "chainmail_chestplate", "chainmail_helmet", "chainmail_leggings"),
                List.of("leather_boots", "leather_chestplate", "leather_helmet", "leather_leggings", "leather_horse_armor"),
                List.of("golden_boots", "golden_chestplate", "golden_helmet", "golden_leggings", "golden_horse_armor",
                    "golden_axe", "golden_hoe", "golden_pickaxe", "golden_shovel", "golden_sword"),
                List.of("iron_boots", "iron_chestplate", "iron_helmet", "iron_leggings", "iron_horse_armor",
                    "iron_axe", "iron_hoe", "iron_pickaxe", "iron_shovel", "iron_sword"),
                List.of("diamond_boots", "diamond_chestplate", "diamond_helmet", "diamond_leggings", "diamond_horse_armor",
                    "diamond_axe", "diamond_hoe", "diamond_pickaxe", "diamond_shovel", "diamond_sword")
            )))
            .save("circular_economy");

        mainLine.next()
            .task(ModItems.ANVIL_HAMMER, "all_in_one")
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
            .save("all_in_one");

        AdvancementLineHelper killingLine = mainLine.createBranch();
        killingLine.next()
            .challenge(ModItems.ANVIL_HAMMER, "hammer")
            .hammerKill("kill_zombie", EntityType.ZOMBIE)
            .hammerKill("kill_skeleton", EntityType.SKELETON)
            .hammerKill("kill_creeper", EntityType.CREEPER)
            .hammerKill("kill_spider", EntityType.SPIDER)
            .hammerKill("kill_pig", EntityType.PIG)
            .hammerKill("kill_cow", EntityType.COW)
            .hammerKill("kill_sheep", EntityType.SHEEP)
            .hammerKill("kill_chicken", EntityType.CHICKEN)
            .save("hammer");
        killingLine.next()
            .challenge(ModItems.ROYAL_ANVIL_HAMMER, "super_kill")
            .addCriterion("super_kill", AnvilHammerHurtEntityTrigger.TriggerInstance.hurtEntity(80, ModItems.ROYAL_ANVIL_HAMMER.get()))
            .save("super_kill");

        AdvancementLineHelper elecLine = mainLine.createBranch();
        elecLine.next()
            .task(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK, "hearts_of_iron")
            .recipeAnc("craft_magnetoelectric_core", "magnetoelectric_core")
            .save("hearts_of_iron");

        AdvancementLineHelper firepowerLine = elecLine.createBranch();
        firepowerLine.next()
            .task(ModBlocks.CHARGER, "charger")
            .recipe("craft_charger", AnvilCraft.of("charger"))
            .playerPlace("place_charger", ModBlocks.CHARGER)
            .requireAll()
            .save("charger");
        firepowerLine.next()
            .task(ModItems.SUPER_CAPACITOR, "supercapacitor")
            .hasItems("has_supercapacitor", ModItems.SUPER_CAPACITOR)
            .save("supercapacitor");

        AdvancementLineHelper armoryLine = firepowerLine.createBranch();
        armoryLine.next()
            .challenge(ModItems.ENERGY_WEAPON_PLATFORM, "not_enough_firepower")
            .hasItems("has_anvil_railgun", ModItems.ANVIL_RAILGUN)
            .hasItems("has_laser_gun", ModItems.LASER_GUN)
            .hasItems("has_tesla_gun", ModItems.TESLA_GUN)
            .hasItems("has_corrupted_beacon_activator", ModItems.CORRUPTED_BEACON_ACTIVATOR)
            .hasItems("has_spectral_weapon_launcher", ModItems.SPECTRAL_WEAPON_LAUNCHER)
            .requireAll()
            .save("not_enough_firepower");

        firepowerLine.next()
            .challenge(ModItems.ANVIL_RAILGUN, "railgun_collision")
            .railgunCollision("railgun_collision")
            .save("railgun_collision");

        AdvancementLineHelper genelecLine = elecLine.createBranch();

        elecLine.next()
            .task(ModBlocks.CHARGE_COLLECTOR, "not_beacon")
            .recipeAnc("craft_charge_collector", "charge_collector")
            .playerPlace("place_charge_collector", ModBlocks.CHARGE_COLLECTOR)
            .save("not_beacon");
        elecLine.next()
            .task(ModBlocks.PIEZOELECTRIC_CRYSTAL, "lighter")
            .hitPiezoelectricCrystal("hit_piezoelectric_crystal")
            .save("lighter");

        genelecLine.next()
            .task(ModBlocks.TRANSMISSION_POLE, "networking")
            .recipeAnc("craft_transmission_pole", "transmission_pole")
            .playerPlace("place_transmission_pole", ModBlocks.TRANSMISSION_POLE)
            .save("networking");
        genelecLine.next()
            .task(ModItems.ANVIL_HAMMER, "electric_filed_rhythm")
            .enterPowerGrid("enter_power_grid")
            .save("electric_filed_rhythm");

        AdvancementLineHelper voltageLine = genelecLine.createBranch();
        voltageLine.next()
            .goal(ModBlocks.TRANSMISSION_POLE, "high_voltage_tower")
            .highVoltageTower("high_voltage_tower")
            .save("high_voltage_tower");

        mainLine.createBranch().next()
            .task(ModBlocks.BLOCK_COMPARATOR, "salted_fish_turns_over", true)
            .blockComparatorTurnOver("block_comparator_turn_over")
            .save("salted_fish_turns_over");

        AdvancementLineHelper industrialLine = mainLine.createBranch();
        industrialLine.next()
            .goal(ModBlocks.HEATER, "industrial_grade_smelting")
            .inWorldRecipeTypeAnc("super_heating", "super_heating")
            .save("industrial_grade_smelting");
        industrialLine.createBranch().next()
            .task(ModBlocks.PIPE_NODE, "water_flows_downhill")
            .pipeConnectContainers("pipe_connect_containers")
            .save("water_flows_downhill");
        industrialLine.next()
            .task(ModItems.ROYAL_STEEL_INGOT, "noble_metal")
            .requireAny()
            .hasItemAny("has_", ModBlocks.ROYAL_STEEL_BLOCK, ModItems.ROYAL_STEEL_INGOT, ModItems.ROYAL_STEEL_NUGGET)
            .save("noble_metal");

        industrialLine.next()
            .task(ModBlocks.OVERSEER_BLOCK, "overseer")
            .recipeAnc("craft_overseer", "overseer")
            .save("overseer");

        industrialLine.next()
            .task(ModBlocks.ROYAL_SMITHING_TABLE, "smithing_table")
            .recipeAnc("craft_smithing", "smithing/royal_smithing_table")
            .save("smithing_table");

        industrialLine.createBranch().next()
            .task(ModItems.ROYAL_STEEL_PICKAXE, "durable_goods")
            .requireAny()
            .recipeAnc("royal_steel_pickaxe", "smithing/royal_steel_pickaxe")
            .recipeAnc("royal_steel_axe", "smithing/royal_steel_axe")
            .recipeAnc("royal_steel_shovel", "smithing/royal_steel_shovel")
            .recipeAnc("royal_steel_hoe", "smithing/royal_steel_hoe")
            .recipeAnc("royal_steel_sword", "smithing/royal_steel_sword")
            .save("durable_goods");

        industrialLine.next()
            .task(ModBlocks.ROYAL_ANVIL, "royal_blacksmith")
            .recipeAnc("craft_royal_anvil", "smithing/royal_anvil")
            .recipeAnc("craft_royal_smithing_table", "smithing/royal_smithing_table")
            .recipeAnc("craft_royal_grindstone", "smithing/royal_grindstone")
            .playerPlace("place_royal_anvil", ModBlocks.ROYAL_ANVIL)
            .playerPlace("place_royal_smithing_table", ModBlocks.ROYAL_SMITHING_TABLE)
            .playerPlace("place_royal_grindstone", ModBlocks.ROYAL_GRINDSTONE)
            .requireAll()
            .save("royal_blacksmith");
        industrialLine.next()
            .task(ModBlocks.CORRUPTED_BEACON, "wither")
            .convertBeacon("convert_beacon")
            .save("wither");
        industrialLine.next()
            .goal(ModBlocks.CORRUPTED_BEACON, "rip_van_winkle")
            .inWorldRecipeTypeAnc("time_warp_recipe", "time_warp")
            .save("rip_van_winkle");

        AdvancementLineHelper frostLine = industrialLine.createBranch();
        frostLine.next()
            .task(ModItems.FROST_METAL_INGOT, "frost_metal")
            .requireAny()
            .hasItemAny("has_", ModBlocks.FROST_METAL_BLOCK, ModItems.FROST_METAL_INGOT, ModItems.FROST_METAL_NUGGET)
            .save("frost_metal");
        frostLine.next()
            .challenge(ModItems.FROST_METAL_SWORD, "tai_shang_wang_qing")
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
            .save("tai_shang_wang_qing");

        AdvancementLineHelper emberLine = industrialLine.createBranch();
        emberLine.next()
            .task(ModItems.OIL_BUCKET, "for_aeons")
            .requireAny()
            .inWorldRecipeAnc("oil_from_raw_beef", "time_warp/oil_from_medium_meat")
            .inWorldRecipeAnc("oil_from_raw_chicken", "time_warp/oil_from_small_meat")
            .inWorldRecipeAnc("oil_from_raw_fish", "time_warp/oil_from_small_meat")
            .inWorldRecipeAnc("oil_from_raw_mutton", "time_warp/oil_from_medium_meat")
            .inWorldRecipeAnc("oil_from_raw_porkchop", "time_warp/oil_from_medium_meat")
            .inWorldRecipeAnc("oil_from_raw_rabbit", "time_warp/oil_from_medium_meat")
            .inWorldRecipeAnc("oil_from_piglin_head", "time_warp/oil_from_large_meat")
            .inWorldRecipeAnc("oil_from_rotten_flesh", "time_warp/oil_from_small_meat")
            .inWorldRecipeAnc("oil_from_spider_eye", "time_warp/oil_from_small_meat")
            .inWorldRecipeAnc("oil_from_zombie_head", "time_warp/oil_from_large_meat")
            .save("for_aeons");
        emberLine.next()
            .goal(ModItems.EMBER_METAL_INGOT, "forged_over_eons")
            .requireAny()
            .hasItems("has_ember_metal_block", ModBlocks.EMBER_METAL_BLOCK)
            .hasItems("has_ember_metal_ingot", ModItems.EMBER_METAL_INGOT)
            .hasItems("has_ember_metal_nugget", ModItems.EMBER_METAL_NUGGET)
            .save("forged_over_eons");
        emberLine.createBranch().next()
            .challenge(ModBlocks.EMBER_ANVIL, "ice_and_fire")
            .requireAll()
            .recipeAnc("craft_ember_smithing_table", "smithing/ember_smithing_table")
            .recipeAnc("craft_ember_anvil", "smithing/ember_anvil")
            .recipeAnc("craft_ember_grindstone", "smithing/ember_grindstone")
            .recipeAnc("craft_frost_smithing_table", "smithing/frost_smithing_table")
            .recipeAnc("craft_frost_anvil", "smithing/frost_anvil")
            .recipeAnc("craft_frost_grindstone", "smithing/frost_grindstone")
            .playerPlace("place_ember_smithing_table", ModBlocks.EMBER_SMITHING_TABLE)
            .playerPlace("place_ember_anvil", ModBlocks.EMBER_ANVIL)
            .playerPlace("place_ember_grindstone", ModBlocks.EMBER_GRINDSTONE)
            .playerPlace("place_frost_smithing_table", ModBlocks.FROST_SMITHING_TABLE)
            .playerPlace("place_frost_anvil", ModBlocks.FROST_ANVIL)
            .playerPlace("place_frost_grindstone", ModBlocks.FROST_GRINDSTONE)
            .save("ice_and_fire");
        emberLine.next()
            .task(ModItems.EMBER_METAL_PICKAXE, "self_in_flaming")
            .fireReforge("fire_reforge")
            .save("self_in_flaming");

        AdvancementLineHelper oreLine = industrialLine.createBranch();
        oreLine.next()
            .task(ModBlocks.MINERAL_FOUNTAIN, "ore_point")
            .mineralFountainCreate("mineral_fountain_create")
            .save("ore_point");
        oreLine.next()
            .task(ModItems.VOID_MATTER, "mining_void")
            .hasItems("has_void_matter", ModItems.VOID_MATTER)
            .save("mining_void");

        AdvancementLineHelper voidLine = oreLine.createBranch();
        voidLine.createBranch().next()
            .task(ModBlocks.VOID_ENERGY_COLLECTOR, "void_generate_energy")
            .voidEnergyCollectorWorking("void_energy_collector_working")
            .save("void_generate_energy");

        AdvancementLineHelper spongeLine = voidLine.createBranch();
        spongeLine.next()
            .task(ModBlocks.MENGER_SPONGE, "saikou_scrubber")
            .inWorldRecipeAnc("multiblock_menger_sponge", "multiblock/menger_sponge")
            .inWorldRecipeAnc("multiblock_menger_sponge_2", "multiblock/menger_sponge_2")
            .requireAny()
            .save("saikou_scrubber");
        spongeLine.createBranch().next()
            .task(ModBlocks.MENGER_SPONGE, "understand_fractals", true)
            .inWorldRecipeAnc("menger_sponge_2", "multiblock/menger_sponge_2")
            .save("understand_fractals");
        spongeLine.next()
            .challenge(ModBlocks.LARGE_FLUID_TANK, "infinity_capacity")
            .multiBlockForm("multi_block_form")
            .save("infinity_capacity");

        AdvancementLineHelper spaceLine = voidLine.createBranch();
        spaceLine.next()
            .task(ModBlocks.SPACE_OVERCOMPRESSOR, "shulker_box_within_shulker_box")
            .hasItems("has_space_overcompressor", ModBlocks.SPACE_OVERCOMPRESSOR)
            .save("shulker_box_within_shulker_box");
        spaceLine.next()
            .goal(ModItems.NEUTRONIUM_INGOT, "spoon_of_neutron_star")
            .hasItems("has_neutronium_ingot", ModItems.NEUTRONIUM_INGOT)
            .save("spoon_of_neutron_star");

        AdvancementLineHelper gemLine = industrialLine.createBranch();
        gemLine.next()
            .task(ModItems.RUBY, "gem_transform")
            .requireAny()
            .inWorldRecipeAnc("emerald_block", "time_warp/emerald_block")
            .inWorldRecipeAnc("ruby_block", "time_warp/ruby_block")
            .inWorldRecipeAnc("sapphire_block", "time_warp/sapphire_block")
            .inWorldRecipeAnc("topaz_block", "time_warp/topaz_block")
            .save("gem_transform");

        AdvancementLineHelper laserLine = gemLine.createBranch();
        laserLine.next()
            .task(ModBlocks.RUBY_LASER, "laser")
            .recipeAnc("craft_ruby_laser", "ruby_laser")
            .playerPlace("place_ruby_laser", ModBlocks.RUBY_LASER)
            .requireAll()
            .save("laser");
        gemLine.next()
            .task(ModBlocks.HEAT_COLLECTOR, "heat_utilizing")
            .recipeAnc("craft_heat_collector", "heat_collector")
            .playerPlace("place_heat_collector", ModBlocks.HEAT_COLLECTOR)
            .requireAll()
            .save("heat_utilizing");

        AdvancementLineHelper nuclearLine = gemLine.createBranch();
        nuclearLine.next()
            .challenge(ModBlocks.URANIUM_BLOCK, "isotope_decay_battery")
            .heatCollectOn("nuclear_sources", BlockStatePredicate.builder().of(ModBlocks.URANIUM_BLOCK, ModBlocks.PLUTONIUM_BLOCK))
            .save("isotope_decay_battery");

        nuclearLine.next()
            .challenge(ModBlocks.HEAT_COLLECTOR, "nuclear_power_10a")
            .heatCollectOn("collect_overheated", BlockStatePredicate.builder().of(ModBlockTags.OVERHEATED_BLOCKS))
            .save("nuclear_power_10a");

        gemLine.next()
            .goal(ModBlocks.HEAT_COLLECTOR, "super_heat")
            .heatCollectorOutput("super_heat", MinMaxBounds.Ints.atLeast(HeatCollectorBlockEntity.SUPER_HEAT_OUTPUT_POWER))
            .save("super_heat");

        industrialLine.next()
            .task(ModBlocks.GIANT_ANVIL, "giant_age")
            .hasItems("has_giant_anvil", ModBlocks.GIANT_ANVIL)
            .save("giant_age");

        AdvancementLineHelper storageLine = industrialLine.createBranch();
        storageLine.next()
            .task(ModBlocks.LARGE_CRATE, "large_crate")
            .inWorldRecipeAnc("multiblock_large_crate", "multiblock/large_crate")
            .inWorldRecipeAnc("multiblock_conversion_large_crate", "multiblock_conversion/large_crate")
            .requireAny()
            .save("large_crate");
        storageLine.next()
            .goal(ModBlocks.SHULKER_CONTAINER, "shulker_container")
            .shulkerContainerUpgrade("shulker_container_upgrade")
            .save("shulker_container");
        storageLine.next()
            .challenge(ModBlocks.HYPERDIMENSION_STORAGE_STATION, "hyperdimension_station")
            .hyperdimensionStationUpgrade("hyperdimension_station_upgrade")
            .save("hyperdimension_station");

        AdvancementLineHelper cauldronLine = industrialLine.createBranch();
        cauldronLine.next()
            .task(ModBlocks.LARGE_CAULDRON, "superpower")
            .inWorldRecipeAnc("multiblock_large_cauldron", "multiblock/large_cauldron")
            .inWorldRecipeAnc("multiblock_conversion_large_cauldron", "multiblock_conversion/large_cauldron")
            .requireAny()
            .save("superpower");
        cauldronLine.next()
            .goal(Items.POTION, "cocktail", true)
            .largeCauldronCocktail("large_cauldron_cocktail")
            .save("cocktail");

        industrialLine.createBranch().next()
            .task(ModBlocks.ACCELERATION_RING, "anvil_accelerator")
            .inWorldRecipeAnc("multiblock_acceleration_ring", "multiblock/acceleration_ring")
            .inWorldRecipeAnc("multiblock_conversion_acceleration_ring", "multiblock_conversion/acceleration_ring")
            .inWorldRecipeAnc("multiblock_deflection_ring", "multiblock/deflection_ring")
            .inWorldRecipeAnc("multiblock_conversion_deflection_ring", "multiblock_conversion/deflection_ring")
            .requirements(new AdvancementRequirements(List.of(
                List.of("multiblock_acceleration_ring", "multiblock_conversion_acceleration_ring"),
                List.of("multiblock_deflection_ring", "multiblock_conversion_deflection_ring")
            )))
            .save("anvil_accelerator");

        AdvancementLineHelper sideLine1 = industrialLine.createBranch();
        sideLine1.next()
            .goal(ModItems.MULTIPHASE_MATTER, "new_matter")
            .requireAny()
            .recipeAnc("uranium", "anvil_collision/anvil_tier_1_and_redstone_block_32")
            .recipeAnc("multiphase_matter", "anvil_collision/ember_anvil_and_frost_metal_block_32")
            .recipeAnc("negative_matter", "anvil_collision/anvil_tier_1_and_levitation_powder_block_32")
            .save("new_matter");

        AdvancementLineHelper spacesuitLine = sideLine1.createBranch();
        spacesuitLine.next()
            .challenge(ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE, "full_spacesuit")
            .fullSpacesuit("full_spacesuit")
            .save("full_spacesuit");
        spacesuitLine.next()
            .challenge(ModItems.WEATHERPROOF_SPACESUIT_BOOTS, "void_walker")
            .voidWalker("void_walker")
            .save("void_walker");

        sideLine1.next()
            .challenge(ModBlocks.CONFINED_SPACE_ANVILON, "anvilon")
            .requireAny()
            .recipeAnc("mass_16", "anvil_collision/anvil_tier_0_and_giant_anvil_32")
            .recipeAnc("energy_8", "anvil_collision/anvil_tier_0_and_giant_anvil_128")
            .recipeAnc("time_8", "anvil_collision/anvil_tier_0_and_corrupted_beacon_32")
            .recipeAnc("energy_4_beacon", "anvil_collision/anvil_tier_0_and_corrupted_beacon_128")
            .recipeAnc("space_8", "anvil_collision/anvil_tier_0_and_space_overcompressor_32")
            .recipeAnc("energy_4_space", "anvil_collision/anvil_tier_0_and_space_overcompressor_128")
            .save("anvilon");

        industrialLine.next()
            .goal(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK, "overheated")
            .requireAny()
            .recipeAnc("uranium_heat", "anvil_collision/anvil_tier_2_and_uranium_block_256")
            .recipeAnc("plutonium_heat", "anvil_collision/anvil_tier_2_and_plutonium_block_256")
            .save("overheated");

        industrialLine.next()
            .goal(ModBlocks.TRANSCENDIUM_BLOCK, "transcendence")
            .requireAny()
            .hasItemAny("has_", ModBlocks.TRANSCENDIUM_BLOCK, ModItems.TRANSCENDIUM_INGOT, ModItems.TRANSCENDIUM_NUGGET)
            .save("transcendence");
        industrialLine.createBranch().next()
            .challenge(ModBlocks.CELESTIAL_FORGING_ANVIL, "electric_allergy", true)
            .requireAny()
            .electricAllergy("electric_allergy")
            .save("electric_allergy");
        industrialLine.next()
            .challenge(ModBlocks.TRANSCENDENCE_ANVIL, "the_end")
            .hasItems("has_transcendence_anvil", ModBlocks.TRANSCENDENCE_ANVIL)
            .save("the_end");
        industrialLine.next()
            .challenge(ModBlocks.CELESTIAL_FORGING_ANVIL, "the_start")
            .inWorldRecipeAnc("multiblock_celestial_forging_anvil", "multiblock/celestial_forging_anvil")
            .save("the_start");

        industrialLine.createBranch().next()
            .challenge(ModItems.DYSON_SPHERE_COMPONENT, "hitchhiker_guide")
            .celestialVisit("visited_1", 1)
            .celestialVisit("visited_2", 2)
            .celestialVisit("visited_3", 3)
            .celestialVisit("visited_4", 4)
            .celestialVisit("visited_5", 5)
            .celestialVisit("visited_6", 6)
            .celestialVisit("visited_7", 7)
            .celestialVisit("visited_8", 8)
            .celestialVisit("visited_9", 9)
            .celestialVisit("visited_10", 10)
            .celestialVisit("visited_11", 11)
            .requireAll()
            .save("hitchhiker_guide");

        AdvancementLineHelper deliveryLine = industrialLine.createBranch();
        deliveryLine.next()
            .goal(ModBlocks.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE, "wormhole_delivery")
            .wormholeDelivery("wormhole_delivery")
            .save("wormhole_delivery");

        AdvancementLineHelper dysonLine = industrialLine.createBranch();
        dysonLine.next()
            .challenge(ModItems.DYSON_SPHERE_COMPONENT, "dyson_sphere")
            .dysonSphere("dyson_sphere")
            .save("dyson_sphere");
        dysonLine.next()
            .challenge(ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT, "inexhaustible")
            .inexhaustible("inexhaustible")
            .infinitePower("infinite_power")
            .requireAll()
            .save("inexhaustible");

        industrialLine.createBranch().next()
            .challenge(ModItems.EARTH_CORE_SHARD, "efficiency", true)
            .planetDetonation("efficiency")
            .save("efficiency");
        industrialLine.createBranch().next()
            .challenge(Items.PLAYER_HEAD, "cannon_fodder", true)
            .planetaryCollapseDeath("cannon_fodder")
            .save("cannon_fodder");
        AdvancementLineHelper blackHoleLine = industrialLine.createBranch();
        blackHoleLine.next()
            .challenge(ModBlocks.BLACK_HOLE, "black_hole")
            .celestialEvolution("black_hole", StellarTerminal.Kind.BLACK_HOLE)
            .save("black_hole");
        blackHoleLine.next()
            .task(ModItems.PENROSE_SPHERE_COMPONENT, "gamma_laser")
            .gammaLaser("gamma_laser")
            .save("gamma_laser");
        industrialLine.createBranch().next()
            .challenge(ModItems.MAGNETAR_COIL_COMPONENT, "neutron_star")
            .celestialEvolution("neutron_star", StellarTerminal.Kind.NEUTRON_STAR)
            .save("neutron_star");
        industrialLine.createBranch().next()
            .task(ModBlocks.CELESTIAL_FORGING_ANVIL, "come_at_night", true)
            .starContactDeath("come_at_night")
            .save("come_at_night");
        industrialLine.next()
            .challenge(ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT, "no_remnant", true)
            .celestialEvolution("no_remnant", MinMaxBounds.Ints.exactly(64))
            .save("no_remnant");
        industrialLine.next()
            .challenge(ModItems.CIVILIZATION_CATALYST, "new_cycle", true)
            .addCriterion("new_cycle", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
            .rewards(AdvancementRewards.Builder.experience(1395))
            .save("new_cycle");
    }
}
