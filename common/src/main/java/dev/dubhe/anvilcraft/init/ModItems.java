package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.data.recipe.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.generator.recipe.TimeWarpRecipesLoader;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.crafting.ShapedTagRecipeBuilder;
import dev.dubhe.anvilcraft.item.AmethystAxeItem;
import dev.dubhe.anvilcraft.item.AmethystHoeItem;
import dev.dubhe.anvilcraft.item.AmethystPickaxeItem;
import dev.dubhe.anvilcraft.item.AmethystShovelItem;
import dev.dubhe.anvilcraft.item.AmethystSwordItem;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.CapacitorItem;
import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.CursedItem;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.item.EmptyCapacitorItem;
import dev.dubhe.anvilcraft.item.GeodeItem;
import dev.dubhe.anvilcraft.item.GuideBookItem;
import dev.dubhe.anvilcraft.item.LevitationPowderItem;
import dev.dubhe.anvilcraft.item.MagnetItem;
import dev.dubhe.anvilcraft.item.ModFoods;
import dev.dubhe.anvilcraft.item.RoyalAnvilHammerItem;
import dev.dubhe.anvilcraft.item.RoyalAxeItem;
import dev.dubhe.anvilcraft.item.RoyalHoeItem;
import dev.dubhe.anvilcraft.item.RoyalPickaxeItem;
import dev.dubhe.anvilcraft.item.RoyalShovelItem;
import dev.dubhe.anvilcraft.item.RoyalSwordItem;
import dev.dubhe.anvilcraft.item.RoyalUpgradeTemplateItem;
import dev.dubhe.anvilcraft.item.SeedsPackItem;
import dev.dubhe.anvilcraft.item.TopazItem;
import dev.dubhe.anvilcraft.item.UtusanItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;

@SuppressWarnings("unused")
public class ModItems {
    public static final ItemEntry<MagnetItem> MAGNET = REGISTRATE
        .item("magnet", properties -> new MagnetItem(properties.durability(255)))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .define('A', Items.ENDER_PEARL)
            .define('B', ModItems.MAGNET_INGOT)
            .define('C', Items.REDSTONE)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider)
        )
        .register();
    public static final ItemEntry<GeodeItem> GEODE = REGISTRATE
        .item("geode", GeodeItem::new)
        .register();
    public static final ItemEntry<? extends PickaxeItem> AMETHYST_PICKAXE = REGISTRATE
        .item("amethyst_pickaxe", AmethystPickaxeItem::new)
        .recipe((ctx, provider) ->
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().asItem().getDefaultInstance())
                .pattern("AAA")
                .pattern(" B ")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider)
        )
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.PICKAXES, ItemTags.CLUSTER_MAX_HARVESTABLES)
        .register();
    public static final ItemEntry<? extends AxeItem> AMETHYST_AXE = REGISTRATE
        .item("amethyst_axe", AmethystAxeItem::new)
        .recipe((ctx, provider) ->
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().asItem().getDefaultInstance())
                .pattern("AA")
                .pattern("AB")
                .pattern(" B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider)
        )
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.AXES)
        .register();
    public static final ItemEntry<? extends HoeItem> AMETHYST_HOE = REGISTRATE
        .item("amethyst_hoe", AmethystHoeItem::new)
        .recipe((ctx, provider) ->
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().asItem().getDefaultInstance())
                .pattern("AA")
                .pattern(" B")
                .pattern(" B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider)
        )
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.HOES)
        .register();
    public static final ItemEntry<? extends SwordItem> AMETHYST_SWORD = REGISTRATE
        .item("amethyst_sword", AmethystSwordItem::new)
        .recipe((ctx, provider) ->
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().asItem().getDefaultInstance())
                .pattern("A")
                .pattern("A")
                .pattern("B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider)
        )
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SWORDS)
        .register();
    public static final ItemEntry<? extends ShovelItem> AMETHYST_SHOVEL = REGISTRATE
        .item("amethyst_shovel", AmethystShovelItem::new)
        .recipe((ctx, provider) ->
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().asItem().getDefaultInstance())
                .pattern("A")
                .pattern("B")
                .pattern("B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider)
        )
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SHOVELS)
        .register();
    public static final ItemEntry<Item> COCOA_LIQUOR = REGISTRATE
        .item("cocoa_liquor", Item::new)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 2)
            .requires(ModItems.COCOA_POWDER)
            .requires(ModItems.COCOA_POWDER)
            .requires(ModItems.COCOA_BUTTER)
            .unlockedBy("has_coco_powder", RegistrateRecipeProvider.has(ModItems.COCOA_POWDER))
            .unlockedBy("has_coco_butter", RegistrateRecipeProvider.has(ModItems.COCOA_BUTTER))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> COCOA_BUTTER = REGISTRATE
        .item("cocoa_butter", Item::new)
        .register();
    public static final ItemEntry<Item> COCOA_POWDER = REGISTRATE
        .item("cocoa_powder", Item::new)
        .register();
    public static final ItemEntry<Item> CREAM = REGISTRATE
        .item("cream", Item::new)
        .register();
    public static final ItemEntry<Item> FLOUR = REGISTRATE
        .item("flour", Item::new)
        .tag(ModItemTags.FLOUR, ModItemTags.WHEAT_FLOUR, ModItemTags.FLOUR_FORGE, ModItemTags.WHEAT_FLOUR_FORGE)
        .recipe((ctx, provider) -> AnvilRecipe.Builder.create(RecipeCategory.MISC, ctx.get().getDefaultInstance())
            .type(AnvilRecipeType.STAMPING)
            .hasBlock(ModBlocks.STAMPING_PLATFORM.get())
            .hasItemIngredient(new Vec3(0.0, -0.75, 0.0), Items.WHEAT)
            .spawnItem(new Vec3(0.0, -0.75, 0.0), ctx.get())
            .spawnItem(new Vec3(0.0, -0.75, 0.0), 0.25, Items.WHEAT_SEEDS)
            .save(provider))
        .register();
    public static final ItemEntry<Item> DOUGH = REGISTRATE
        .item("dough", Item::new)
        .tag(ModItemTags.DOUGH, ModItemTags.WHEAT_DOUGH, ModItemTags.DOUGH_FORGE, ModItemTags.WHEAT_DOUGH_FORGE)
        .register();
    public static final ItemEntry<Item> CHOCOLATE = REGISTRATE
        .item("chocolate", properties -> new Item(properties.food(ModFoods.CHOCOLATE)))
        .tag(ModItemTags.FOODS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("CDC")
            .pattern("ABA")
            .define('A', ModItems.COCOA_LIQUOR)
            .define('B', ModItems.COCOA_BUTTER)
            .define('C', ModItems.CREAM)
            .define('D', Items.SUGAR)
            .unlockedBy("has_cocoa_liquor", RegistrateRecipeProvider.has(ModItems.COCOA_LIQUOR))
            .unlockedBy("has_cocoa_butter", RegistrateRecipeProvider.has(ModItems.COCOA_BUTTER))
            .unlockedBy("has_cream", RegistrateRecipeProvider.has(ModItems.CREAM))
            .unlockedBy("has_sugar", RegistrateRecipeProvider.has(Items.SUGAR))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> CHOCOLATE_BLACK = REGISTRATE
        .item("chocolate_black", p -> new Item(p.food(ModFoods.CHOCOLATE_BLACK)))
        .tag(ModItemTags.FOODS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("BCB")
            .pattern("AAA")
            .define('A', ModItems.COCOA_LIQUOR)
            .define('B', ModItems.COCOA_BUTTER)
            .define('C', Items.SUGAR)
            .unlockedBy("has_cocoa_butter", RegistrateRecipeProvider.has(ModItems.COCOA_LIQUOR))
            .unlockedBy("has_cream", RegistrateRecipeProvider.has(ModItems.CREAM))
            .unlockedBy("has_sugar", RegistrateRecipeProvider.has(Items.SUGAR))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> CHOCOLATE_WHITE = REGISTRATE
        .item("chocolate_white", p -> new Item(p.food(ModFoods.CHOCOLATE_WHITE)))
        .tag(ModItemTags.FOODS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("BCB")
            .pattern("AAA")
            .define('A', ModItems.COCOA_BUTTER)
            .define('B', ModItems.CREAM)
            .define('C', Items.SUGAR)
            .unlockedBy("has_butter", RegistrateRecipeProvider.has(ModItems.COCOA_BUTTER))
            .unlockedBy("has_cream", RegistrateRecipeProvider.has(ModItems.CREAM))
            .unlockedBy("has_sugar", RegistrateRecipeProvider.has(Items.SUGAR))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> CREAMY_BREAD_ROLL = REGISTRATE
        .item("creamy_bread_roll", p -> new Item(p.food(ModFoods.CREAMY_BREAD_ROLL)))
        .tag(ModItemTags.FOODS)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get())
            .requires(Items.BREAD)
            .requires(Items.SUGAR)
            .requires(ModItems.CREAM)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.CREAM))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> BEEF_MUSHROOM_STEW_RAW = REGISTRATE
        .item("beef_mushroom_stew_raw", Item::new)
        .properties(properties -> properties.stacksTo(1))
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get())
            .requires(Items.BEEF)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(Items.BOWL)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.BEEF))
            .save(provider)
        )
        .register();
    public static final ItemEntry<BowlFoodItem> BEEF_MUSHROOM_STEW = REGISTRATE
        .item("beef_mushroom_stew", p -> new BowlFoodItem(p.food(ModFoods.BEEF_MUSHROOM_STEW)))
        .properties(properties -> properties.stacksTo(1))
        .tag(ModItemTags.FOODS)
        .register();
    public static final ItemEntry<Item> UTUSAN_RAW = REGISTRATE
        .item("utusan_raw", Item::new)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get())
            .requires(Items.SPIDER_EYE)
            .requires(Items.PUFFERFISH)
            .requires(Items.POISONOUS_POTATO)
            .requires(Items.LILY_OF_THE_VALLEY)
            .requires(Items.WITHER_ROSE)
            .unlockedBy("has_spider_eye", RegistrateRecipeProvider.has(Items.SPIDER_EYE))
            .save(provider)
        )
        .register();
    public static final ItemEntry<UtusanItem> UTUSAN = REGISTRATE
        .item("utusan", UtusanItem::new)
        .register();
    public static final ItemEntry<Item> MAGNET_INGOT = REGISTRATE
        .item("magnet_ingot", Item::new)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 9)
                .requires(ModBlocks.MAGNET_BLOCK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.MAGNET_BLOCK))
                .save(provider, AnvilCraft.of("craft/magnet_ingot_9"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 8)
                .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.HOLLOW_MAGNET_BLOCK))
                .save(provider, AnvilCraft.of("craft/magnet_ingot_8"));
        })
        .register();
    public static final ItemEntry<Item> SPONGE_GEMMULE = REGISTRATE
        .item("sponge_gemmule", Item::new)
        .register();
    // 皇家钢系
    public static final ItemEntry<Item> ROYAL_STEEL_INGOT = REGISTRATE
        .item("royal_steel_ingot", Item::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.ROYAL_STEEL_BLOCK)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("royal_steel_ingot_from_royal_steel_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT)
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_NUGGET)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_NUGGET.get()),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_NUGGET))
                .save(provider, AnvilCraft.of("royal_steel_ingot_from_royal_steel_nugget"));
        })
        .register();
    public static final ItemEntry<Item> ROYAL_STEEL_NUGGET = REGISTRATE
        .item("royal_steel_nugget", Item::new)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.ROYAL_STEEL_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_NUGGET.get()),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_NUGGET))
            .save(provider)
        )
        .register();
    // 工具
    public static final ItemEntry<? extends Item> ROYAL_STEEL_PICKAXE = REGISTRATE
        .item("royal_steel_pickaxe", RoyalPickaxeItem::new)
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_AXE = REGISTRATE
        .item("royal_steel_axe", RoyalAxeItem::new)
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_SHOVEL = REGISTRATE
        .item("royal_steel_shovel", RoyalShovelItem::new)
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_HOE = REGISTRATE
        .item("royal_steel_hoe", RoyalHoeItem::new)
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_SWORD = REGISTRATE
        .item("royal_steel_sword", RoyalSwordItem::new)
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .register();
    // 升级模板
    public static final ItemEntry<RoyalUpgradeTemplateItem> ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE = REGISTRATE
        .item("royal_steel_upgrade_smithing_template", RoyalUpgradeTemplateItem::new)
        .register();

    // 诅咒黄金系
    public static final ItemEntry<CursedItem> CURSED_GOLD_INGOT = REGISTRATE
        .item("cursed_gold_ingot", CursedItem::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.CURSED_GOLD_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CURSED_GOLD_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.CURSED_GOLD_BLOCK))
                .save(provider, AnvilCraft.of("craft/cursed_gold_ingot_1"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.CURSED_GOLD_NUGGET)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_NUGGET.get()),
                    AnvilCraftDatagen.has(ModItems.CURSED_GOLD_NUGGET))
                .save(provider, AnvilCraft.of("craft/cursed_gold_ingot_2"));
        })
        .register();
    public static final ItemEntry<CursedItem> CURSED_GOLD_NUGGET = REGISTRATE
        .item("cursed_gold_nugget", CursedItem::new)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModItems.CURSED_GOLD_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT.get()),
                AnvilCraftDatagen.has(ModItems.CURSED_GOLD_INGOT))
            .save(provider))
        .register();
    public static final ItemEntry<TopazItem> TOPAZ = REGISTRATE
        .item("topaz", TopazItem::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.TOPAZ_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.TOPAZ_BLOCK))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> RUBY = REGISTRATE
        .item("ruby", Item::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RUBY_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.RUBY_BLOCK))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> SAPPHIRE = REGISTRATE
        .item("sapphire", Item::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.SAPPHIRE_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.SAPPHIRE_BLOCK))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> RESIN = REGISTRATE
        .item("resin", Item::new)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RESIN_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.RESIN_BLOCK))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> AMBER = REGISTRATE
        .item("amber", Item::new)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.AMBER_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.AMBER_BLOCK))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> HARDEND_RESIN = REGISTRATE
        .item("hardend_resin", Item::new)
        .register();
    public static final ItemEntry<Item> WOOD_FIBER = REGISTRATE
        .item("wood_fiber", Item::new)
        .register();
    public static final ItemEntry<Item> CIRCUIT_BOARD = REGISTRATE
        .item("circuit_board", Item::new)
        .register();
    public static final ItemEntry<Item> PRISMARINE_BLADE = REGISTRATE
        .item("prismarine_blade", Item::new)
        .register();
    public static final ItemEntry<Item> PRISMARINE_CLUSTER = REGISTRATE
        .item("prismarine_cluster", Item::new)
        .register();
    public static final ItemEntry<Item> SEA_HEART_SHELL = REGISTRATE
        .item("sea_heart_shell", Item::new)
        .register();
    public static final ItemEntry<Item> SEA_HEART_SHELL_SHARD = REGISTRATE
        .item("sea_heart_shell_shard", Item::new)
        .register();
    public static final ItemEntry<CapacitorItem> CAPACITOR = REGISTRATE
        .item("capacitor", CapacitorItem::new)
        .tag(ModItemTags.CAPACITOR)
        .register();
    public static final ItemEntry<Item> MAGNETOELECTRIC_CORE = REGISTRATE
        .item("magnetoelectric_core", Item::new)
        .model((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().getDefaultInstance())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.COPPER_INGOT)
                .define('B', ModItemTags.GLASS)
                .define('C', ModBlocks.HOLLOW_MAGNET_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.GLASS),
                    RegistrateRecipeProvider.has(ModItemTags.GLASS)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    RegistrateRecipeProvider.has(Items.COPPER_INGOT)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.HOLLOW_MAGNET_BLOCK),
                    RegistrateRecipeProvider.has(ModBlocks.HOLLOW_MAGNET_BLOCK)
                )
                .save(provider);
            ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().getDefaultInstance())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.COPPER_INGOT)
                .define('B', ModItemTags.GLASS_FORGE)
                .define('C', ModBlocks.HOLLOW_MAGNET_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.GLASS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.GLASS_FORGE)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    RegistrateRecipeProvider.has(Items.COPPER_INGOT)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.HOLLOW_MAGNET_BLOCK),
                    RegistrateRecipeProvider.has(ModBlocks.HOLLOW_MAGNET_BLOCK)
                )
                .save(provider, RecipeBuilder.getDefaultRecipeId(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<EmptyCapacitorItem> CAPACITOR_EMPTY = REGISTRATE
        .item("capacitor_empty", EmptyCapacitorItem::new)
        .tag(ModItemTags.CAPACITOR)
        .recipe((ctx, provider) -> ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get().getDefaultInstance())
            .pattern(" B ")
            .pattern("ACA")
            .pattern(" B ")
            .define('A', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
            .define('B', ModBlocks.COPPER_PRESSURE_PLATE)
            .define('C', ModItems.RESIN)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.HEAVY_WEIGHTED_PRESSURE_PLATE),
                RegistrateRecipeProvider.has(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.COPPER_PRESSURE_PLATE),
                RegistrateRecipeProvider.has(ModBlocks.COPPER_PRESSURE_PLATE)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RESIN),
                RegistrateRecipeProvider.has(ModItems.RESIN)
            )
            .save(provider))
        .register();

    public static final ItemEntry<AnvilHammerItem> ANVIL_HAMMER = REGISTRATE
        .item("anvil_hammer", AnvilHammerItem::new)
        .properties(properties -> properties.durability(35))
        .model((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', Items.ANVIL)
            .define('B', Items.LIGHTNING_ROD)
            .define('C', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ANVIL), RegistrateRecipeProvider.has(Items.ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD),
                RegistrateRecipeProvider.has(Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), RegistrateRecipeProvider.has(Items.IRON_INGOT))
            .save(provider))
        .register();

    public static final ItemEntry<RoyalAnvilHammerItem> ROYAL_ANVIL_HAMMER = REGISTRATE
        .item("royal_anvil_hammer", RoyalAnvilHammerItem::new)
        .properties(properties -> properties.durability(150))
        .model((ctx, provider) -> {
        })
        .register();
    public static final ItemEntry<CrabClawItem> CRAB_CLAW = REGISTRATE
        .item("crab_claw", CrabClawItem::new)
        .model((ctx, provider) -> {
        })
        .register();
    public static final ItemEntry<Item> TUNGSTEN_NUGGET = REGISTRATE
        .item("tungsten_nugget", Item::new)
        .tag(ModItemTags.TUNGSTEN_NUGGETS, ModItemTags.TUNGSTEN_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TUNGSTEN_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.TUNGSTEN_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TUNGSTEN_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.TUNGSTEN_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> TUNGSTEN_INGOT = REGISTRATE
        .item("tungsten_ingot", Item::new)
        .tag(ModItemTags.TUNGSTEN_INGOTS, ModItemTags.TUNGSTEN_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TUNGSTEN_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TUNGSTEN_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.TUNGSTEN_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TUNGSTEN_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.TUNGSTEN_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TUNGSTEN_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.TUNGSTEN_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(Items.ANCIENT_DEBRIS)
                .type(AnvilRecipeType.TIMEWARP)
                .hasBlock(
                    ModBlocks.CORRUPTED_BEACON.get(),
                    new Vec3(0.0, -2.0, 0.0),
                    Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ctx.get())
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ModItems.ROYAL_STEEL_INGOT)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.NETHER_STAR)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.ANCIENT_DEBRIS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()), AnvilCraftDatagen.has(ctx.get()))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.NETHER_STAR),
                    AnvilCraftDatagen.has(Items.NETHER_STAR)
                )
                .save(provider, AnvilCraft.of(
                        "timewarp/"
                            + BuiltInRegistries.ITEM.getKey(Items.ANCIENT_DEBRIS.asItem()).getPath()
                            + "_first"
                    )
                );
        })
        .register();
    public static final ItemEntry<Item> TITANIUM_NUGGET = REGISTRATE
        .item("titanium_nugget", Item::new)
        .tag(ModItemTags.TITANIUM_NUGGETS, ModItemTags.TITANIUM_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TITANIUM_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.TITANIUM_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TITANIUM_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.TITANIUM_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> TITANIUM_INGOT = REGISTRATE
        .item("titanium_ingot", Item::new)
        .tag(ModItemTags.TITANIUM_INGOTS, ModItemTags.TITANIUM_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TITANIUM_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TITANIUM_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.TITANIUM_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TITANIUM_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.TITANIUM_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TITANIUM_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.TITANIUM_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> ZINC_NUGGET = REGISTRATE
        .item("zinc_nugget", Item::new)
        .tag(ModItemTags.ZINC_NUGGETS, ModItemTags.ZINC_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.ZINC_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.ZINC_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.ZINC_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.ZINC_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> ZINC_INGOT = REGISTRATE
        .item("zinc_ingot", Item::new)
        .tag(ModItemTags.ZINC_INGOTS, ModItemTags.ZINC_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.ZINC_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.ZINC_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.ZINC_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.ZINC_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.ZINC_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.ZINC_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.ZINC_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> TIN_NUGGET = REGISTRATE
        .item("tin_nugget", Item::new)
        .tag(ModItemTags.TIN_NUGGETS, ModItemTags.TIN_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TIN_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.TIN_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TIN_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.TIN_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> TIN_INGOT = REGISTRATE
        .item("tin_ingot", Item::new)
        .tag(ModItemTags.TIN_INGOTS, ModItemTags.TIN_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TIN_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TIN_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.TIN_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TIN_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.TIN_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TIN_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.TIN_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> LEAD_NUGGET = REGISTRATE
        .item("lead_nugget", Item::new)
        .tag(ModItemTags.LEAD_NUGGETS, ModItemTags.LEAD_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.LEAD_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.LEAD_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.LEAD_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.LEAD_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> LEAD_INGOT = REGISTRATE
        .item("lead_ingot", Item::new)
        .tag(ModItemTags.LEAD_INGOTS, ModItemTags.LEAD_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.LEAD_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.LEAD_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.LEAD_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.LEAD_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.LEAD_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.LEAD_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.LEAD_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.LEAD_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.LEAD_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> SILVER_NUGGET = REGISTRATE
        .item("silver_nugget", Item::new)
        .tag(ModItemTags.SILVER_NUGGETS, ModItemTags.SILVER_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.SILVER_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.SILVER_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.SILVER_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.SILVER_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> SILVER_INGOT = REGISTRATE
        .item("silver_ingot", Item::new)
        .tag(ModItemTags.SILVER_INGOTS, ModItemTags.SILVER_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.SILVER_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SILVER_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.SILVER_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.SILVER_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.SILVER_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.SILVER_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.SILVER_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.SILVER_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.SILVER_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> URANIUM_NUGGET = REGISTRATE
        .item("uranium_nugget", Item::new)
        .tag(ModItemTags.URANIUM_NUGGETS, ModItemTags.URANIUM_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.URANIUM_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS),
                    RegistrateRecipeProvider.has(ModItemTags.URANIUM_INGOTS)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.URANIUM_INGOTS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.URANIUM_INGOTS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> URANIUM_INGOT = REGISTRATE
        .item("uranium_ingot", Item::new)
        .tag(ModItemTags.URANIUM_INGOTS, ModItemTags.URANIUM_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.URANIUM_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.URANIUM_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.URANIUM_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.URANIUM_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.URANIUM_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.URANIUM_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.URANIUM_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
        })
        .register();
    public static final ItemEntry<Item> COPPER_NUGGET = REGISTRATE
        .item("copper_nugget", Item::new)
        .tag(ModItemTags.COPPER_NUGGETS, ModItemTags.COPPER_NUGGETS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(Ingredient.of(Items.COPPER_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    RegistrateRecipeProvider.has(Items.COPPER_INGOT)
                )
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.COPPER_INGOT)
                .requires(ctx.get(), 9)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ctx.get()),
                    RegistrateRecipeProvider.has(ctx.get())
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> BRONZE_INGOT = REGISTRATE
        .item("bronze_ingot", Item::new)
        .tag(ModItemTags.BRONZE_INGOTS, ModItemTags.BRONZE_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.BRONZE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRONZE_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.BRONZE_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRONZE_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.BRONZE_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
            AnvilRecipe.Builder.create(RecipeCategory.MISC, ctx.get().getDefaultInstance())
                .type(AnvilRecipeType.SUPER_HEATING)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.TIN_INGOTS)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.COPPER_INGOT)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get().asItem(), 2)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TIN_INGOTS)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT)
                )
                .save(
                    provider,
                    AnvilCraft.of(
                        "heating/" + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath()
                    )
                );
            AnvilRecipe.Builder.create(RecipeCategory.MISC, ctx.get().getDefaultInstance())
                .type(AnvilRecipeType.SUPER_HEATING)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.TIN_INGOTS_FORGE)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.COPPER_INGOT)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get().asItem(), 2)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.TIN_INGOTS_FORGE)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT)
                )
                .save(
                    provider,
                    AnvilCraft.of(
                        "heating/" + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge"
                    )
                );
        })
        .register();

    //    public static final ItemEntry<Item> BRONZE_NUGGET = REGISTRATE
    //        .item("bronze_nugget", Item::new)
    //        .tag(ModItemTags.BRONZE_NUGGETS, ModItemTags.BRONZE_NUGGETS_FORGE)
    //        .recipe((ctx, provider) -> {
    //            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
    //                .requires(ModItemTags.BRONZE_INGOTS)
    //                .unlockedBy(
    //                    AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS),
    //                    AnvilCraftDatagen.has(ModItemTags.BRONZE_INGOTS)
    //                )
    //                .save(provider);
    //            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
    //                .requires(ModItemTags.BRONZE_INGOTS_FORGE)
    //                .unlockedBy(
    //                    AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS_FORGE),
    //                    AnvilCraftDatagen.has(ModItemTags.BRONZE_INGOTS_FORGE)
    //                )
    //                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
    //        })
    //        .register();

    public static final ItemEntry<Item> BRASS_INGOT = REGISTRATE
        .item("brass_ingot", Item::new)
        .tag(ModItemTags.BRASS_INGOTS, ModItemTags.BRASS_INGOTS_FORGE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.BRASS_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRASS_NUGGETS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.BRASS_NUGGETS),
                    RegistrateRecipeProvider.has(ModItemTags.BRASS_NUGGETS)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRASS_NUGGETS_FORGE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.BRASS_NUGGETS_FORGE),
                    RegistrateRecipeProvider.has(ModItemTags.BRASS_NUGGETS_FORGE)
                )
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
            AnvilRecipe.Builder.create(RecipeCategory.MISC, ctx.get().getDefaultInstance())
                .type(AnvilRecipeType.SUPER_HEATING)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.ZINC_INGOTS)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.COPPER_INGOT)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get().asItem(), 2)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.ZINC_INGOTS)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT)
                )
                .save(
                    provider,
                    AnvilCraft.of(
                        "heating/" + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath()
                    )
                );
            AnvilRecipe.Builder.create(RecipeCategory.MISC, ctx.get().getDefaultInstance())
                .type(AnvilRecipeType.SUPER_HEATING)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.ZINC_INGOTS_FORGE)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.COPPER_INGOT)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get().asItem(), 2)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.ZINC_INGOTS_FORGE)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT)
                )
                .save(
                    provider,
                    AnvilCraft.of(
                        "heating/" + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge"
                    )
                );
        })
        .register();

    //    public static final ItemEntry<Item> BRASS_NUGGET = REGISTRATE
    //        .item("brass_nugget", Item::new)
    //        .tag(ModItemTags.BRASS_NUGGETS, ModItemTags.BRASS_NUGGETS_FORGE)
    //        .recipe((ctx, provider) -> {
    //            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
    //                .requires(ModItemTags.BRASS_INGOTS)
    //                .unlockedBy(
    //                    AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS),
    //                    AnvilCraftDatagen.has(ModItemTags.BRASS_INGOTS)
    //                )
    //                .save(provider);
    //            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
    //                .requires(ModItemTags.BRASS_INGOTS_FORGE)
    //                .unlockedBy(
    //                    AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS_FORGE),
    //                    AnvilCraftDatagen.has(ModItemTags.BRASS_INGOTS_FORGE)
    //                )
    //                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_forge");
    //        })
    //        .register();


    public static final ItemEntry<Item> AMULET_BOX = REGISTRATE
        .item("amulet_box", Item::new)
        .register();

    public static final ItemEntry<Item> NETHERITE_CRYSTAL_NUCLEUS = REGISTRATE
        .item("netherite_crystal_nucleus", Item::new)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                .pattern("ABA")
                .define('A', ModBlocks.TUNGSTEN_PRESSURE_PLATE)
                .define('B', Items.NETHERITE_SCRAP)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.TUNGSTEN_PRESSURE_PLATE),
                    RegistrateRecipeProvider.has(ModBlocks.TUNGSTEN_PRESSURE_PLATE)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.NETHERITE_SCRAP),
                    RegistrateRecipeProvider.has(Items.NETHERITE_SCRAP)
                )
                .save(provider);
            TimeWarpRecipesLoader.timeWarp(
                RecipeItem.of(ctx.get()),
                RecipeItem.of(Items.ANCIENT_DEBRIS)
            );
        })
        .register();

    public static final ItemEntry<GuideBookItem> GUIDE_BOOK = REGISTRATE
        .item("guide_book", GuideBookItem::new)
        .properties(p -> p.stacksTo(1))
        .model((ctx, provider) -> {
        })
        .lang("AnvilCraft Guide Book")
        .register();

    public static final ItemEntry<Item> LIME_POWDER = REGISTRATE
        .item("lime_powder", Item::new)
        .register();

    public static final ItemEntry<LevitationPowderItem> LEVITATION_POWDER = REGISTRATE
        .item("levitation_powder", LevitationPowderItem::new)
        .register();

    public static final ItemEntry<DiskItem> DISK = REGISTRATE
        .item("disk", DiskItem::new)
        .properties(p -> p.stacksTo(1))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.IRON_INGOT)
            .define('C', ModItems.MAGNET_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN),
                RegistrateRecipeProvider.has(ModItems.HARDEND_RESIN)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                RegistrateRecipeProvider.has(Items.IRON_INGOT)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT),
                RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT)
            )
            .save(provider))
        .register();

    public static final ItemEntry<Item> RAW_ZINC = REGISTRATE
        .item("raw_zinc", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_ZINC, ModItemTags.RAW_ZINC_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_ZINC)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_ZINC),
                AnvilCraftDatagen.has(ModBlocks.RAW_ZINC)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> RAW_TIN = REGISTRATE
        .item("raw_tin", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_TIN, ModItemTags.RAW_TIN_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TIN)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_TIN),
                AnvilCraftDatagen.has(ModBlocks.RAW_TIN)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> RAW_TITANIUM = REGISTRATE
        .item("raw_titanium", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_TITANIUM, ModItemTags.RAW_TITANIUM_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TITANIUM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_TITANIUM),
                AnvilCraftDatagen.has(ModBlocks.RAW_TITANIUM)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> RAW_TUNGSTEN = REGISTRATE
        .item("raw_tungsten", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_TUNGSTEN, ModItemTags.RAW_TUNGSTEN_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_TUNGSTEN)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_TUNGSTEN),
                AnvilCraftDatagen.has(ModBlocks.RAW_TUNGSTEN)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> RAW_LEAD = REGISTRATE
        .item("raw_lead", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_LEAD, ModItemTags.RAW_LEAD_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_LEAD)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_LEAD),
                AnvilCraftDatagen.has(ModBlocks.RAW_LEAD)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> RAW_SILVER = REGISTRATE
        .item("raw_silver", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_SILVER, ModItemTags.RAW_SILVER_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_SILVER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_SILVER),
                AnvilCraftDatagen.has(ModBlocks.RAW_SILVER)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> RAW_URANIUM = REGISTRATE
        .item("raw_uranium", Item::new)
        .tag(ModItemTags.RAW_ORES, ModItemTags.RAW_ORES_FORGE, ModItemTags.RAW_URANIUM, ModItemTags.RAW_URANIUM_FORGE)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RAW_URANIUM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RAW_URANIUM),
                AnvilCraftDatagen.has(ModBlocks.RAW_URANIUM)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> VOID_MATTER = REGISTRATE
        .item("void_matter", Item::new)
        .tag(ModItemTags.VOID_RESISTANT)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.VOID_MATTER_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.VOID_MATTER_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.VOID_MATTER_BLOCK)
            )
            .save(provider))
        .register();
    public static final ItemEntry<Item> EARTH_CORE_SHARD = REGISTRATE
        .item("earth_core_shard", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.EARTH_CORE_SHARD_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.EARTH_CORE_SHARD_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.EARTH_CORE_SHARD_BLOCK)
            )
            .save(provider))
        .register();
    public static final ItemEntry<SeedsPackItem> SEEDS_PACK = REGISTRATE
        .item("seeds_pack", SeedsPackItem::new)
        .register();

    public static void register() {
    }
}
