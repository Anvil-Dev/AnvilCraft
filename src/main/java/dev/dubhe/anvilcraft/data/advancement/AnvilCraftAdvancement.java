package dev.dubhe.anvilcraft.data.advancement;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumAdvancementProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.advancement.AdvancementLineHelper;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
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
            .hammerHurt("super_kill", 80)
            .save("super_kill");

        AdvancementLineHelper elecLine = mainLine.createBranch();
        elecLine.next()
            .task(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK, "hearts_of_iron")
            .recipeAnc("craft_magnetoelectric_core", "magnetoelectric_core")
            .save("hearts_of_iron");

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
            .hasItems("has_royal_anvil", ModBlocks.ROYAL_ANVIL)
            .hasItems("has_royal_smithing_table", ModBlocks.ROYAL_SMITHING_TABLE)
            .hasItems("has_royal_grindstone", ModBlocks.ROYAL_GRINDSTONE)
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
            .hasItems("has_ember_smithing_table", ModBlocks.EMBER_SMITHING_TABLE)
            .hasItems("has_ember_anvil", ModBlocks.EMBER_ANVIL)
            .hasItems("has_ember_grindstone", ModBlocks.EMBER_GRINDSTONE)
            .hasItems("has_frost_smithing_table", ModBlocks.FROST_SMITHING_TABLE)
            .hasItems("has_frost_anvil", ModBlocks.FROST_ANVIL)
            .hasItems("has_frost_grindstone", ModBlocks.FROST_GRINDSTONE)
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
            .hasItems("has_menger_sponge", ModBlocks.MENGER_SPONGE)
            .save("saikou_scrubber");
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
            .hasItems("has_ruby_laser", ModBlocks.RUBY_LASER)
            .save("laser");
        gemLine.next()
            .task(ModBlocks.HEAT_COLLECTOR, "heat_utilizing")
            .hasItems("has_heat_collector", ModBlocks.HEAT_COLLECTOR)
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
            .heatCollectorOutput("super_heat", MinMaxBounds.Ints.atLeast(HeatCollectorBlockEntity.MAX_OUTPUT_POWER))
            .save("super_heat");

        industrialLine.next()
            .task(ModBlocks.GIANT_ANVIL, "giant_age")
            .hasItems("has_giant_anvil", ModBlocks.GIANT_ANVIL)
            .save("giant_age");
        industrialLine.createBranch().next()
            .task(ModBlocks.ACCELERATION_RING, "anvil_accelerator")
            .requireAll()
            .hasItems("has_acceleration_ring", ModBlocks.ACCELERATION_RING)
            .hasItems("has_deflection_ring", ModBlocks.DEFLECTION_RING)
            .save("anvil_accelerator");

        AdvancementLineHelper sideLine1 = industrialLine.createBranch();
        sideLine1.next()
            .goal(ModItems.MULTIPHASE_MATTER, "new_matter")
            .requireAny()
            .recipeAnc("uranium", "anvil_collision/anvil_tier_1_and_redstone_block_32")
            .recipeAnc("multiphase_matter", "anvil_collision/ember_anvil_and_frost_metal_block_32")
            .recipeAnc("negative_matter", "anvil_collision/anvil_tier_1_and_levitation_powder_block_32")
            .save("new_matter");
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
            .challenge(ModBlocks.TRANSCENDENCE_ANVIL, "electric_allergy")
            .requireAny()
            .electricAllergy("electric_allergy")
            .save("electric_allergy");
        industrialLine.next()
            .challenge(ModBlocks.TRANSCENDENCE_ANVIL, "the_end")
            .hasItems("has_transcendence_anvil", ModBlocks.TRANSCENDENCE_ANVIL)
            .save("the_end");
        industrialLine.next()
            .challenge(ModBlocks.CELESTIAL_FORGING_ANVIL, "the_start")
            .hasItems("has_celestial_forging_anvil", ModBlocks.CELESTIAL_FORGING_ANVIL)
            .save("the_start");
    }
}
