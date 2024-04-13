package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.crafting.ShapedTagRecipeBuilder;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.CursedItem;
import dev.dubhe.anvilcraft.item.GeodeItem;
import dev.dubhe.anvilcraft.item.MagnetItem;
import dev.dubhe.anvilcraft.item.ModFoods;
import dev.dubhe.anvilcraft.item.ModTiers;
import dev.dubhe.anvilcraft.item.RoyalUpgradeTemplateItem;
import dev.dubhe.anvilcraft.item.TopazItem;
import dev.dubhe.anvilcraft.item.UtusanItem;
import net.minecraft.ChatFormatting;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

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
        .item("amethyst_pickaxe", properties ->
            new PickaxeItem(ModTiers.AMETHYST, 1, -2.8f, properties) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                    return stack;
                }

                @Override
                public void appendHoverText(
                    @NotNull ItemStack stack,
                    @Nullable Level level,
                    @NotNull List<Component> tooltipComponents,
                    @NotNull TooltipFlag isAdvanced
                ) {
                    super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
                    tooltipComponents
                        .add(Component.translatable("item.anvilcraft.amethyst_pickaxe.tooltip")
                            .withStyle(ChatFormatting.GRAY));
                }
            })
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
        .tag(ModItemTags.PICKAXES)
        .register();
    public static final ItemEntry<? extends AxeItem> AMETHYST_AXE = REGISTRATE
        .item("amethyst_axe", properties -> new AxeItem(ModTiers.AMETHYST, 7, -3.2f, properties) {
            @Override
            public @NotNull ItemStack getDefaultInstance() {
                ItemStack stack = super.getDefaultInstance();
                stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                stack.enchant(Enchantments.MOB_LOOTING, 3);
                return stack;
            }
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ModItemTags.AXES)
        .register();
    public static final ItemEntry<? extends HoeItem> AMETHYST_HOE = REGISTRATE
        .item("amethyst_hoe", properties -> new HoeItem(ModTiers.AMETHYST, -1, -2.0f, properties) {
            @Override
            public @NotNull ItemStack getDefaultInstance() {
                ItemStack stack = super.getDefaultInstance();
                stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                return stack;
            }
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ModItemTags.HOES)
        .register();
    public static final ItemEntry<? extends SwordItem> AMETHYST_SWORD = REGISTRATE
        .item("amethyst_sword", properties -> new SwordItem(ModTiers.AMETHYST, 3, -2.4f, properties) {
            @Override
            public @NotNull ItemStack getDefaultInstance() {
                ItemStack stack = super.getDefaultInstance();
                stack.enchant(Enchantments.MOB_LOOTING, 3);
                return stack;
            }
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ModItemTags.SWORDS)
        .register();
    public static final ItemEntry<? extends ShovelItem> AMETHYST_SHOVEL = REGISTRATE
        .item("amethyst_shovel", properties -> new ShovelItem(ModTiers.AMETHYST, 1.5f, -3.0f, properties) {
            @Override
            public @NotNull ItemStack getDefaultInstance() {
                ItemStack stack = super.getDefaultInstance();
                stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                return stack;
            }
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ModItemTags.SHOVELS)
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
        .tag(ModItemTags.FLOUR, ModItemTags.WHEAT_FLOUR)
        .register();
    public static final ItemEntry<Item> DOUGH = REGISTRATE
        .item("dough", Item::new)
        .tag(ModItemTags.DOUGH, ModItemTags.WHEAT_DOUGH)
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
            .define('A', ModItems.COCOA_BUTTER)
            .define('B', ModItems.CREAM)
            .define('C', Items.SUGAR)
            .unlockedBy("has_cocoa_butter", RegistrateRecipeProvider.has(ModItems.COCOA_BUTTER))
            .unlockedBy("has_cream", RegistrateRecipeProvider.has(ModItems.CREAM))
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
    public static final ItemEntry<Item> DEBRIS_SCRAP = REGISTRATE
        .item("debris_scrap", Item::new)
        .register();
    public static final ItemEntry<Item> NETHER_STAR_SHARD = REGISTRATE
        .item("nether_star_shard", Item::new)
        .register();
    public static final ItemEntry<Item> NETHERITE_CORE = REGISTRATE
        .item("netherite_core", Item::new)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.DEBRIS_SCRAP)
            .define('B', ModItems.NETHER_STAR_SHARD)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.DEBRIS_SCRAP))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> NETHERITE_COIL = REGISTRATE
        .item("netherite_coil", Item::new)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
            .define('B', ModItems.NETHERITE_CORE)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.NETHERITE_CORE))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> ELYTRA_FRAME = REGISTRATE
        .item("elytra_frame", Item::new)
        .register();
    public static final ItemEntry<Item> ELYTRA_MEMBRANE = REGISTRATE
        .item("elytra_membrane", Item::new)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AB")
            .pattern("BB")
            .define('A', ModItems.ELYTRA_FRAME)
            .define('B', Items.PHANTOM_MEMBRANE)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.ELYTRA_FRAME))
            .save(provider)
        )
        .register();
    public static final ItemEntry<Item> BARK = REGISTRATE
        .item("bark", Item::new)
        .register();
    public static final ItemEntry<Item> PULP = REGISTRATE
        .item("pulp", Item::new)
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
    public static final ItemEntry<? extends Item> ROYAL_STEEL_PICKAXE = REGISTRATE
        .item("royal_steel_pickaxe", properties -> new PickaxeItem(Tiers.DIAMOND, 1, -2.8f,
            properties.durability(2559)) {
            @Override
            public @NotNull ItemStack getDefaultInstance() {
                return super.getDefaultInstance();
            }
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .register();
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
    public static final ItemEntry<AnvilHammerItem> ANVIL_HAMMER = REGISTRATE
        .item("anvil_hammer", properties -> new AnvilHammerItem(properties.durability(35)))
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

    public static void register() {
    }
}
