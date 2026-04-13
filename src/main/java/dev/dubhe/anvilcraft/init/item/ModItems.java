package dev.dubhe.anvilcraft.init.item;

import com.mojang.datafixers.util.Unit;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.lib.v2.registrum.builders.ItemBuilder;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.CreativeModeTabModifier;
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.anvilcraft.lib.v2.registrum.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.registrum.util.nullness.NonNullConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.item.AmethystAxeItem;
import dev.dubhe.anvilcraft.item.AmethystHoeItem;
import dev.dubhe.anvilcraft.item.AmethystPickaxeItem;
import dev.dubhe.anvilcraft.item.AmethystShovelItem;
import dev.dubhe.anvilcraft.item.AmethystSwordItem;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.CapacitorItem;
import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.item.DragonRodItem;
import dev.dubhe.anvilcraft.item.EmberAnvilHammerItem;
import dev.dubhe.anvilcraft.item.EmberMetalAxeItem;
import dev.dubhe.anvilcraft.item.EmberMetalHeavyHalberdItem;
import dev.dubhe.anvilcraft.item.EmberMetalHoeItem;
import dev.dubhe.anvilcraft.item.EmberMetalIngotItem;
import dev.dubhe.anvilcraft.item.EmberMetalPickaxeItem;
import dev.dubhe.anvilcraft.item.EmberMetalResonatorItem;
import dev.dubhe.anvilcraft.item.EmberMetalShovelItem;
import dev.dubhe.anvilcraft.item.EmberMetalSwordItem;
import dev.dubhe.anvilcraft.item.EmptyCapacitorItem;
import dev.dubhe.anvilcraft.item.EmptySuperCapacitorItem;
import dev.dubhe.anvilcraft.item.EnergyWeaponPlatformItem;
import dev.dubhe.anvilcraft.item.ExpGemItem;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.item.FrostMetalAxeItem;
import dev.dubhe.anvilcraft.item.FrostMetalHeavyHalberdItem;
import dev.dubhe.anvilcraft.item.FrostMetalHoeItem;
import dev.dubhe.anvilcraft.item.FrostMetalPickaxeItem;
import dev.dubhe.anvilcraft.item.FrostMetalResonatorItem;
import dev.dubhe.anvilcraft.item.FrostMetalShovelItem;
import dev.dubhe.anvilcraft.item.FrostMetalSwordItem;
import dev.dubhe.anvilcraft.item.GeodeItem;
import dev.dubhe.anvilcraft.item.GuideBookItem;
import dev.dubhe.anvilcraft.item.HeavyHalberdCoreItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.IonoCraftItem;
import dev.dubhe.anvilcraft.item.MagnetItem;
import dev.dubhe.anvilcraft.item.MultiphaseMatterItem;
import dev.dubhe.anvilcraft.item.MultiphaseTranscendiumItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.PillBoxItem;
import dev.dubhe.anvilcraft.item.RecoveryPearl;
import dev.dubhe.anvilcraft.item.ResonatorCoreItem;
import dev.dubhe.anvilcraft.item.RoyalAnvilHammerItem;
import dev.dubhe.anvilcraft.item.RoyalAxeItem;
import dev.dubhe.anvilcraft.item.RoyalHoeItem;
import dev.dubhe.anvilcraft.item.RoyalPickaxeItem;
import dev.dubhe.anvilcraft.item.RoyalShovelItem;
import dev.dubhe.anvilcraft.item.RoyalSteelIngotItem;
import dev.dubhe.anvilcraft.item.RoyalSwordItem;
import dev.dubhe.anvilcraft.item.SeedsPackItem;
import dev.dubhe.anvilcraft.item.SpectralSlingshotItem;
import dev.dubhe.anvilcraft.item.StructureToolItem;
import dev.dubhe.anvilcraft.item.SuperCapacitorItem;
import dev.dubhe.anvilcraft.item.TopazItem;
import dev.dubhe.anvilcraft.item.TranscendenceAnvilHammerItem;
import dev.dubhe.anvilcraft.item.TranscendenceHeavyHalberdItem;
import dev.dubhe.anvilcraft.item.TranscendenceResonatorItem;
import dev.dubhe.anvilcraft.item.abnormal.CursedItem;
import dev.dubhe.anvilcraft.item.abnormal.LevitationItem;
import dev.dubhe.anvilcraft.item.abnormal.RadiationItem;
import dev.dubhe.anvilcraft.item.abnormal.SuperHeavyItem;
import dev.dubhe.anvilcraft.item.amulet.AmuletBoxItem;
import dev.dubhe.anvilcraft.item.amulet.AmuletItem;
import dev.dubhe.anvilcraft.item.amulet.BigAmuletItem;
import dev.dubhe.anvilcraft.item.amulet.ComradeAmuletItem;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.item.template.EmberMetalUpgradeTemplateItem;
import dev.dubhe.anvilcraft.item.template.FrostMetalUpgradeTemplateItem;
import dev.dubhe.anvilcraft.item.template.RoyalSteelUpgradeTemplateItem;
import dev.dubhe.anvilcraft.item.template.TranscendiumUpgradeTemplateItem;
import dev.dubhe.anvilcraft.item.template.frost.DeformationTemplateItem;
import dev.dubhe.anvilcraft.item.template.frost.PermutationTemplateItem;
import dev.dubhe.anvilcraft.item.template.mto.EightToOneTemplateItem;
import dev.dubhe.anvilcraft.item.template.mto.FourToOneTemplateItem;
import dev.dubhe.anvilcraft.item.template.mto.TwoToOneTemplateItem;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import dev.dubhe.anvilcraft.util.registrater.ModelProviderUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;
import static dev.dubhe.anvilcraft.AnvilCraft.of;

@SuppressWarnings({"unused", "CodeBlock2Expr"})
public class ModItems {
    static {
        REGISTRUM.defaultCreativeTab(ModItemGroups.ANVILCRAFT_TOOL.getKey());
    }

    public static final ItemEntry<GuideBookItem> GUIDE_BOOK = REGISTRUM.item("guide_book", GuideBookItem::new)
        .properties(p -> p.stacksTo(1))
        .tag(ItemTags.BOOKSHELF_BOOKS)
        .model(DataGenUtil::noExtraModelOrState)
        .lang("AnvilCraft Guide Book")
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
                .requires(Ingredient.of(Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL))
                .requires(Items.BOOK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.ANVIL))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.CHIPPED_ANVIL))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.DAMAGED_ANVIL))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.BOOK))
                .save(provider);
        })
        .register();
    // 工具
    public static final ItemEntry<MagnetItem> MAGNET = REGISTRUM.item("magnet", properties -> new MagnetItem(properties.durability(255)))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" A ")
                .define('A', Items.ENDER_PEARL)
                .define('B', ModItems.MAGNET_INGOT)
                .define('C', Items.REDSTONE)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
                .save(provider);
        })
        .register();
    public static final ItemEntry<GeodeItem> GEODE = REGISTRUM.item("geode", GeodeItem::new).register();
    public static final ItemEntry<? extends PickaxeItem> AMETHYST_PICKAXE = REGISTRUM.item("amethyst_pickaxe", AmethystPickaxeItem::new)
        .tab(ModItemGroups.ANVILCRAFT_TOOL.getKey(), enchanting(Enchantments.FORTUNE, 3))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, enchanted(ctx.get(), Enchantments.FORTUNE, 3, provider.getProvider()))
                .pattern("AAA")
                .pattern(" B ")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider);
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.PICKAXES, ItemTags.CLUSTER_MAX_HARVESTABLES, Tags.Items.MINING_TOOL_TOOLS)
        .register();
    public static final ItemEntry<? extends AxeItem> AMETHYST_AXE = REGISTRUM.item("amethyst_axe", AmethystAxeItem::new)
        .tab(ModItemGroups.ANVILCRAFT_TOOL.getKey(), enchanting(ModEnchantments.FELLING_KEY, 1))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, enchanted(ctx.get(), ModEnchantments.FELLING_KEY, 1, provider.getProvider()))
                .pattern("AA")
                .pattern("AB")
                .pattern(" B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider);
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.AXES, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<? extends HoeItem> AMETHYST_HOE = REGISTRUM.item("amethyst_hoe", AmethystHoeItem::new)
        .tab(ModItemGroups.ANVILCRAFT_TOOL.getKey(), enchanting(ModEnchantments.HARVEST_KEY, 1))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, enchanted(ctx.get(), ModEnchantments.HARVEST_KEY, 1, provider.getProvider()))
                .pattern("AA")
                .pattern(" B")
                .pattern(" B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider);
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.HOES)
        .register();
    public static final ItemEntry<? extends SwordItem> AMETHYST_SWORD = REGISTRUM.item("amethyst_sword", AmethystSwordItem::new)
        .tab(ModItemGroups.ANVILCRAFT_TOOL.getKey(), enchanting(ModEnchantments.BEHEADING_KEY, 1))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, enchanted(ctx.get(), ModEnchantments.BEHEADING_KEY, 1, provider.getProvider()))
                .pattern("A")
                .pattern("A")
                .pattern("B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider);
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SWORDS, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<? extends ShovelItem> AMETHYST_SHOVEL = REGISTRUM.item("amethyst_shovel", AmethystShovelItem::new)
        .tab(ModItemGroups.ANVILCRAFT_TOOL.getKey(), enchanting(Enchantments.EFFICIENCY, 3))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, enchanted(ctx.get(), Enchantments.EFFICIENCY, 3, provider.getProvider()))
                .pattern("A")
                .pattern("B")
                .pattern("B")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.AMETHYST_SHARD))
                .save(provider);
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SHOVELS)
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_PICKAXE = REGISTRUM.item("royal_steel_pickaxe", RoyalPickaxeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.ROYAL_STEEL_PICKAXE_BASE),
                    Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider, AnvilCraft.of("smithing/royal_steel_pickaxe"));
        })
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.PICKAXES, ItemTags.CLUSTER_MAX_HARVESTABLES, Tags.Items.MINING_TOOL_TOOLS)
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_AXE = REGISTRUM.item("royal_steel_axe", RoyalAxeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.ROYAL_STEEL_AXE_BASE),
                    Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider, AnvilCraft.of("smithing/royal_steel_axe"));
        })
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.AXES, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_SHOVEL = REGISTRUM.item("royal_steel_shovel", RoyalShovelItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.ROYAL_STEEL_SHOVEL_BASE),
                    Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider, AnvilCraft.of("smithing/royal_steel_shovel"));
        })
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SHOVELS)
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_HOE = REGISTRUM.item("royal_steel_hoe", RoyalHoeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.ROYAL_STEEL_HOE_BASE),
                    Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider, AnvilCraft.of("smithing/royal_steel_hoe"));
        })
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.HOES)
        .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_SWORD = REGISTRUM.item("royal_steel_sword", RoyalSwordItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.ROYAL_STEEL_SWORD_BASE),
                    Ingredient.of(ModItems.ROYAL_STEEL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider, AnvilCraft.of("smithing/royal_steel_sword"));
        })
        .properties(properties -> properties.durability(1561))
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SWORDS, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<FrostMetalPickaxeItem> FROST_METAL_PICKAXE = REGISTRUM.item(
        "frost_metal_pickaxe",
        FrostMetalPickaxeItem::new
    ).recipe((ctx, provider) -> {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_PICKAXE_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_pickaxe"));
    }).model((ctx, provider) -> provider.handheld(ctx)).tag(ItemTags.PICKAXES, Tags.Items.MINING_TOOL_TOOLS).register();
    public static final ItemEntry<FrostMetalAxeItem> FROST_METAL_AXE = REGISTRUM.item("frost_metal_axe", FrostMetalAxeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.FROST_METAL_AXE_BASE),
                    Ingredient.of(ModItems.FROST_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/frost_metal_axe"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.AXES, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<FrostMetalShovelItem> FROST_METAL_SHOVEL = REGISTRUM.item(
        "frost_metal_shovel",
        FrostMetalShovelItem::new
    ).recipe((ctx, provider) -> {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.FROST_METAL_SHOVEL_BASE),
                Ingredient.of(ModItems.FROST_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/frost_metal_shovel"));
    }).model((ctx, provider) -> provider.handheld(ctx)).tag(ItemTags.SHOVELS).register();
    public static final ItemEntry<FrostMetalHoeItem> FROST_METAL_HOE = REGISTRUM.item("frost_metal_hoe", FrostMetalHoeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.FROST_METAL_HOE_BASE),
                    Ingredient.of(ModItems.FROST_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/frost_metal_hoe"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.HOES)
        .register();
    public static final ItemEntry<FrostMetalSwordItem> FROST_METAL_SWORD = REGISTRUM.item("frost_metal_sword", FrostMetalSwordItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.FROST_METAL_SWORD_BASE),
                    Ingredient.of(ModItems.FROST_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/frost_metal_sword"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SWORDS, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<EmberMetalPickaxeItem> EMBER_METAL_PICKAXE = REGISTRUM.item(
            "ember_metal_pickaxe",
            EmberMetalPickaxeItem::new
        )
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.EMBER_METAL_PICKAXE_BASE),
                    Ingredient.of(ModItems.EMBER_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/ember_metal_pickaxe"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.PICKAXES, ModItemTags.EXPLOSION_PROOF, ItemTags.CLUSTER_MAX_HARVESTABLES, Tags.Items.MINING_TOOL_TOOLS)
        .register();
    public static final ItemEntry<EmberMetalAxeItem> EMBER_METAL_AXE = REGISTRUM.item("ember_metal_axe", EmberMetalAxeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.EMBER_METAL_AXE_BASE),
                    Ingredient.of(ModItems.EMBER_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/ember_metal_axe"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.AXES, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<EmberMetalShovelItem> EMBER_METAL_SHOVEL = REGISTRUM.item(
        "ember_metal_shovel",
        EmberMetalShovelItem::new
    ).recipe((ctx, provider) -> {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItemTags.EMBER_METAL_SHOVEL_BASE),
                Ingredient.of(ModItems.EMBER_METAL_INGOT),
                RecipeCategory.TOOLS,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
            .save(provider, AnvilCraft.of("smithing/ember_metal_shovel"));
    }).model((ctx, provider) -> provider.handheld(ctx)).tag(ItemTags.SHOVELS).register();
    public static final ItemEntry<EmberMetalHoeItem> EMBER_METAL_HOE = REGISTRUM.item("ember_metal_hoe", EmberMetalHoeItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.EMBER_METAL_HOE_BASE),
                    Ingredient.of(ModItems.EMBER_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/ember_metal_hoe"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.HOES)
        .register();
    public static final ItemEntry<EmberMetalSwordItem> EMBER_METAL_SWORD = REGISTRUM.item("ember_metal_sword", EmberMetalSwordItem::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItemTags.EMBER_METAL_SWORD_BASE),
                    Ingredient.of(ModItems.EMBER_METAL_INGOT),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
                .save(provider, AnvilCraft.of("smithing/ember_metal_sword"));
        })
        .model((ctx, provider) -> provider.handheld(ctx))
        .tag(ItemTags.SWORDS, Tags.Items.MELEE_WEAPON_TOOLS)
        .register();
    public static final ItemEntry<AnvilHammerItem> ANVIL_HAMMER = REGISTRUM.item("anvil_hammer", AnvilHammerItem::new)
        .properties(properties -> properties.durability(35))
        .tag(ItemTags.MACE_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE, ModItemTags.ANVIL_HAMMER)
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', Items.ANVIL)
            .define('B', Items.LIGHTNING_ROD)
            .define('C', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ANVIL), RegistrumRecipeProvider.has(Items.ANVIL))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD), RegistrumRecipeProvider.has(Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), RegistrumRecipeProvider.has(Items.IRON_INGOT))
            .save(provider))
        .register();

    public static final ItemEntry<RoyalAnvilHammerItem> ROYAL_ANVIL_HAMMER = REGISTRUM.item(
            "royal_anvil_hammer",
            RoyalAnvilHammerItem::new
        )
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItems.ANVIL_HAMMER),
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/royal_anvil_hammer"));
        })
        .tag(ItemTags.MACE_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE, ModItemTags.ANVIL_HAMMER)
        .properties(properties -> properties.durability(150))
        .model(DataGenUtil::noExtraModelOrState)
        .register();
    public static final ItemEntry<EmberAnvilHammerItem> EMBER_ANVIL_HAMMER = REGISTRUM.item(
            "ember_anvil_hammer",
            EmberAnvilHammerItem::new
        )
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItems.ROYAL_ANVIL_HAMMER),
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/ember_anvil_hammer"));
        })
        .tag(ItemTags.MACE_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE, ModItemTags.ANVIL_HAMMER)
        .properties(properties -> properties.durability(2031))
        .model(DataGenUtil::noExtraModelOrState)
        .register();
    public static final ItemEntry<TranscendenceAnvilHammerItem> TRANSCENDENCE_ANVIL_HAMMER = REGISTRUM.item(
            "transcendence_anvil_hammer",
            TranscendenceAnvilHammerItem::new
        )
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItems.EMBER_ANVIL_HAMMER),
                    Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK))
                .save(provider, AnvilCraft.of("smithing/transcendence_anvil_hammer"));
        })
        .tag(ItemTags.MACE_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE, ModItemTags.ANVIL_HAMMER)
        .properties(properties -> properties.durability(3156))
        .model(DataGenUtil::noExtraModelOrState)
        .register();
    public static final ItemEntry<DragonRodItem> DRAGON_ROD = REGISTRUM.item("dragon_rod", properties -> new DragonRodItem(properties, 3))
        .properties(properties -> properties.durability(35))
        .tag(ItemTags.DURABILITY_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE, ModItemTags.DRAGON_ROD)
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
            .requires(ModBlocks.BLOCK_DEVOURER)
            .requires(ModItems.ANVIL_HAMMER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), RegistrumRecipeProvider.has(ModBlocks.BLOCK_DEVOURER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.ANVIL_HAMMER), RegistrumRecipeProvider.has(ModItems.ANVIL_HAMMER))
            .save(provider))
        .register();
    public static final ItemEntry<DragonRodItem> ROYAL_DRAGON_ROD = REGISTRUM.item(
            "royal_dragon_rod",
            properties -> new DragonRodItem(properties, 6)
        )
        .properties(properties -> properties.durability(150))
        .tag(ItemTags.DURABILITY_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE, ModItemTags.DRAGON_ROD)
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
                .requires(ModBlocks.BLOCK_DEVOURER)
                .requires(ModItems.ROYAL_ANVIL_HAMMER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), RegistrumRecipeProvider.has(ModBlocks.BLOCK_DEVOURER))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_ANVIL_HAMMER),
                    RegistrumRecipeProvider.has(ModItems.ROYAL_ANVIL_HAMMER)
                )
                .save(provider);
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModItems.DRAGON_ROD),
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.TOOLS,
                    ctx.get()
                )
                .unlocks(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
                )
                .unlocks(AnvilCraftDatagen.hasItem(ModItems.DRAGON_ROD), AnvilCraftDatagen.has(ModItems.DRAGON_ROD))
                .unlocks(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK), AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, ctx.getId().withPrefix("smithing/"));
        })
        .register();
    public static final ItemEntry<DragonRodItem> EMBER_DRAGON_ROD = REGISTRUM.item(
            "ember_dragon_rod",
            properties -> new DragonRodItem(properties, 9)
        )
        .properties(properties -> properties.durability(2031).fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE))
        .tag(ItemTags.DURABILITY_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE, ModItemTags.DRAGON_ROD, ModItemTags.EXPLOSION_PROOF)
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
                .requires(ModBlocks.BLOCK_DEVOURER)
                .requires(ModItems.EMBER_ANVIL_HAMMER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(ModBlocks.BLOCK_DEVOURER))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_ANVIL_HAMMER), AnvilCraftDatagen.has(ModItems.EMBER_ANVIL_HAMMER))
                .save(provider);
            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.ROYAL_DRAGON_ROD),
                Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK)).save(provider, ctx.getId().withPrefix("smithing/"));
        })
        .register();
    public static final ItemEntry<DragonRodItem> TRANSCENDENCE_DRAGON_ROD = REGISTRUM.item(
            "transcendence_dragon_rod",
            properties -> new DragonRodItem(properties, 13)
        )
        .properties(properties -> properties.durability(3156)
            .fireResistant()
            .component(ModComponents.ETERNAL, Eternal.INSTANCE)
            .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
            .component(ModComponents.PROVIDENCE, Providence.INSTANCE))
        .tag(ItemTags.DURABILITY_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE, ModItemTags.DRAGON_ROD)
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ctx.get())
                .requires(ModBlocks.BLOCK_DEVOURER)
                .requires(ModItems.TRANSCENDENCE_ANVIL_HAMMER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLOCK_DEVOURER), AnvilCraftDatagen.has(ModBlocks.BLOCK_DEVOURER))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.TRANSCENDENCE_ANVIL_HAMMER),
                    AnvilCraftDatagen.has(ModItems.TRANSCENDENCE_ANVIL_HAMMER)
                )
                .save(provider);
            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.EMBER_DRAGON_ROD),
                Ingredient.of(ModBlocks.TRANSCENDIUM_BLOCK),
                RecipeCategory.TOOLS,
                ctx.get()
            ).unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK)).save(provider, ctx.getId().withPrefix("smithing/"));
        })
        .register();
    public static final ItemEntry<FrostMetalHeavyHalberdItem> FROST_METAL_HEAVY_HALBERD = REGISTRUM.item(
        "frost_metal_heavy_halberd",
        FrostMetalHeavyHalberdItem::new
    ).tag(
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MACE_ENCHANTABLE,
        ItemTags.TRIDENT_ENCHANTABLE,
        ItemTags.SWORD_ENCHANTABLE,
        ModItemTags.HEAVY_HALBERD,
        ModItemTags.EXPLOSION_PROOF
    ).model(DataGenUtil::noExtraModelOrState).register();
    public static final ItemEntry<EmberMetalHeavyHalberdItem> EMBER_METAL_HEAVY_HALBERD = REGISTRUM.item(
        "ember_metal_heavy_halberd",
        EmberMetalHeavyHalberdItem::new
    ).tag(
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MACE_ENCHANTABLE,
        ItemTags.TRIDENT_ENCHANTABLE,
        ItemTags.SWORD_ENCHANTABLE,
        ModItemTags.HEAVY_HALBERD,
        ModItemTags.EXPLOSION_PROOF
    ).model(DataGenUtil::noExtraModelOrState).register();
    public static final ItemEntry<TranscendenceHeavyHalberdItem> TRANSCENDENCE_HEAVY_HALBERD = REGISTRUM.item(
        "transcendence_heavy_halberd",
        TranscendenceHeavyHalberdItem::new
    ).tag(
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MACE_ENCHANTABLE,
        ItemTags.TRIDENT_ENCHANTABLE,
        ItemTags.SWORD_ENCHANTABLE,
        ModItemTags.HEAVY_HALBERD,
        ModItemTags.EXPLOSION_PROOF
    ).model(DataGenUtil::noExtraModelOrState).register();
    public static final ItemEntry<FrostMetalResonatorItem> FROST_METAL_RESONATOR = REGISTRUM.item(
        "frost_metal_resonator",
        FrostMetalResonatorItem::new
    ).tag(
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MINING_ENCHANTABLE,
        ItemTags.MINING_LOOT_ENCHANTABLE,
        ModItemTags.RESONATOR,
        ModItemTags.EXPLOSION_PROOF
    ).model(DataGenUtil::noExtraModelOrState).register();
    public static final ItemEntry<EmberMetalResonatorItem> EMBER_METAL_RESONATOR = REGISTRUM.item(
        "ember_metal_resonator",
        EmberMetalResonatorItem::new
    ).tag(
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MINING_ENCHANTABLE,
        ItemTags.MINING_LOOT_ENCHANTABLE,
        ModItemTags.RESONATOR,
        ModItemTags.EXPLOSION_PROOF
    ).model(DataGenUtil::noExtraModelOrState).register();
    public static final ItemEntry<TranscendenceResonatorItem> TRANSCENDENCE_RESONATOR = REGISTRUM.item(
        "transcendence_resonator",
        TranscendenceResonatorItem::new
    ).tag(
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MINING_ENCHANTABLE,
        ItemTags.MINING_LOOT_ENCHANTABLE,
        ModItemTags.RESONATOR,
        ModItemTags.EXPLOSION_PROOF
    ).model(DataGenUtil::noExtraModelOrState).register();
    public static final ItemEntry<MultitoolItem> MULTITOOL_ITEM = REGISTRUM.item("multitool", MultitoolItem::new).tag(
        Tags.Items.TOOLS,
        Tags.Items.TOOLS_IGNITER,
        Tags.Items.TOOLS_SHEAR,
        Tags.Items.TOOLS_BRUSH,
        Tags.Items.TOOLS_FISHING_ROD,
        Tags.Items.ENCHANTABLES,
        ItemTags.CREEPER_IGNITERS,
        ItemTags.VANISHING_ENCHANTABLE,
        ItemTags.DURABILITY_ENCHANTABLE,
        ItemTags.MINING_ENCHANTABLE,
        ItemTags.FISHING_ENCHANTABLE,
        ItemTags.STRIDER_TEMPT_ITEMS
    ).properties((properties) -> properties.durability(2031).fireResistant()
    ).model(DataGenUtil::noExtraModelOrState).register();

    public static final ItemEntry<EnergyWeaponPlatformItem> ENERGY_WEAPON_PLATFORM = REGISTRUM
        .item("energy_weapon_platform", EnergyWeaponPlatformItem::new)
        .properties(properties -> properties.stacksTo(1))
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ASS")
                .pattern("ASS")
                .pattern("FPC")
                .define('A', Blocks.ANVIL)
                .define('C', ModBlocks.SPACE_OVERCOMPRESSOR)
                .define('F', Blocks.SMITHING_TABLE)
                .define('P', ModItems.PROCESSOR)
                .define('S', ModItems.SUPER_CAPACITOR)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.ANVIL), RegistrumRecipeProvider.has(Blocks.ANVIL))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.SPACE_OVERCOMPRESSOR),
                    RegistrumRecipeProvider.has(ModBlocks.SPACE_OVERCOMPRESSOR)
                )
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.SMITHING_TABLE), RegistrumRecipeProvider.has(Blocks.SMITHING_TABLE))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PROCESSOR), RegistrumRecipeProvider.has(ModItems.PROCESSOR))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SUPER_CAPACITOR), RegistrumRecipeProvider.has(ModItems.SUPER_CAPACITOR))
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends SpectralSlingshotItem> SPECTRAL_SLINGSHOT = REGISTRUM
        .item("spectral_slingshot", SpectralSlingshotItem::new)
        .tag(
            ItemTags.DURABILITY_ENCHANTABLE,
            ItemTags.CROSSBOW_ENCHANTABLE
        )
        .properties((properties) -> properties.durability(1561))
        .model(DataGenUtil::noExtraModelOrState)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern(" C ")
                .define('A', Items.PHANTOM_MEMBRANE)
                .define('B', ModBlocks.SPECTRAL_ANVIL.asItem())
                .define('C', Items.CROSSBOW)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.PHANTOM_MEMBRANE), RegistrumRecipeProvider.has(Items.PHANTOM_MEMBRANE))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SPECTRAL_ANVIL), RegistrumRecipeProvider.has(ModBlocks.SPECTRAL_ANVIL))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.CROSSBOW), RegistrumRecipeProvider.has(Items.CROSSBOW))
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends AnvilRailgunItem> ANVIL_RAILGUN = REGISTRUM
        .item("anvil_railgun", AnvilRailgunItem::new)
        .properties(properties -> properties.stacksTo(1))
        .model(DataGenUtil::noExtraModelOrState)
        .register();

    public static final ItemEntry<? extends IonoCraftItem> IONOCRAFT = REGISTRUM.item("ionocraft", IonoCraftItem::new)
        .initialProperties(Item.Properties::new)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("BBB")
                .pattern(" C ")
                .define('A', ModItemTags.COPPER_NUGGETS)
                .define('B', Tags.Items.RODS_WOODEN)
                .define('C', ModItemTags.TIN_PLATES)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.COPPER_NUGGETS))
                .unlockedBy(AnvilCraftDatagen.hasItem(Tags.Items.RODS_WOODEN), RegistrumRecipeProvider.has(Tags.Items.RODS_WOODEN))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_PLATES), RegistrumRecipeProvider.has(ModItemTags.TIN_PLATES))
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends IonoCraftBackpackItem> IONOCRAFT_BACKPACK = REGISTRUM.item(
        "ionocraft_backpack",
        IonoCraftBackpackItem::new
    ).initialProperties(() -> new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))).model((ctx, prov) -> {
        ItemModelBuilder offModel = prov.getBuilder(prov.name(ctx.lazy()))
            .parent(new ModelFile.UncheckedModelFile("item/generated"))
            .texture("layer0", "item/ionocraft_backpack_off");
        prov.generated(ctx.lazy())
            .override()
            .predicate(AnvilCraft.of("flight_time"), 0)
            .model(new ModelFile.UncheckedModelFile(offModel.getUncheckedLocation()))
            .end();
    }).tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).recipe((ctx, provider) -> {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ABA")
            .pattern("CDC")
            .define('A', IONOCRAFT.asItem())
            .define('B', ModItemTags.CAPACITOR)
            .define('C', ModItemTags.TIN_PLATES)
            .define('D', Items.LEATHER_CHESTPLATE)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(IONOCRAFT.asItem()), RegistrumRecipeProvider.has(IONOCRAFT.asItem()))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.CAPACITOR), RegistrumRecipeProvider.has(ModItemTags.CAPACITOR))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_PLATES), RegistrumRecipeProvider.has(ModItemTags.TIN_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LEATHER_CHESTPLATE), RegistrumRecipeProvider.has(Items.LEATHER_CHESTPLATE))
            .save(provider);
    }).register();
    // 升级锻造模板
    public static final ItemEntry<RoyalSteelUpgradeTemplateItem> ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE = REGISTRUM.item(
        "royal_steel_upgrade_smithing_template",
        RoyalSteelUpgradeTemplateItem::new
    ).lang("Smithing Template").tag(ModItemTags.TEMPLATES).register();
    public static final ItemEntry<FrostMetalUpgradeTemplateItem> FROST_METAL_UPGRADE_SMITHING_TEMPLATE = REGISTRUM.item(
        "frost_metal_upgrade_smithing_template",
        FrostMetalUpgradeTemplateItem::new
    ).lang("Smithing Template").tag(ModItemTags.TEMPLATES).register();
    public static final ItemEntry<EmberMetalUpgradeTemplateItem> EMBER_METAL_UPGRADE_SMITHING_TEMPLATE = REGISTRUM.item(
        "ember_metal_upgrade_smithing_template",
        EmberMetalUpgradeTemplateItem::new
    ).lang("Smithing Template").tag(ModItemTags.TEMPLATES).register();
    public static final ItemEntry<TranscendiumUpgradeTemplateItem> TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE = REGISTRUM.item(
        "transcendium_upgrade_smithing_template",
        TranscendiumUpgradeTemplateItem::new
    ).lang("Smithing Template").tag(ModItemTags.TEMPLATES).register();

    public static final ItemEntry<PermutationTemplateItem> PERMUTATION_TEMPLATE_ITEM = REGISTRUM.item(
        "permutation_smithing_template",
        PermutationTemplateItem::new
    ).recipe((ctx, provider) -> {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("EEE")
            .pattern("ETV")
            .pattern("VVV")
            .define('E', ModItems.EARTH_CORE_SHARD)
            .define('T', ModItemTags.TEMPLATES)
            .define('V', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_SMITHING_TABLE),
                RegistrumRecipeProvider.has(ModBlocks.FROST_SMITHING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), RegistrumRecipeProvider.has(ModItems.EARTH_CORE_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), RegistrumRecipeProvider.has(ModItems.VOID_MATTER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModItems.DEFORMATION_TEMPLATE_ITEM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.DEFORMATION_TEMPLATE_ITEM),
                RegistrumRecipeProvider.has(ModItems.DEFORMATION_TEMPLATE_ITEM)
            )
            .save(provider, AnvilCraft.of("shapeless/deform_to_permut"));
    }).initialProperties(() -> new Item.Properties().fireResistant()).tag(ModItemTags.TEMPLATES, ModItemTags.EXPLOSION_PROOF).register();
    public static final ItemEntry<DeformationTemplateItem> DEFORMATION_TEMPLATE_ITEM = REGISTRUM.item(
        "deformation_smithing_template",
        DeformationTemplateItem::new
    ).recipe((ctx, provider) -> {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("VVV")
            .pattern("VTE")
            .pattern("EEE")
            .define('E', ModItems.EARTH_CORE_SHARD)
            .define('T', ModItemTags.TEMPLATES)
            .define('V', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FROST_SMITHING_TABLE),
                RegistrumRecipeProvider.has(ModBlocks.FROST_SMITHING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD), RegistrumRecipeProvider.has(ModItems.EARTH_CORE_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER), RegistrumRecipeProvider.has(ModItems.VOID_MATTER))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModItems.PERMUTATION_TEMPLATE_ITEM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.PERMUTATION_TEMPLATE_ITEM),
                RegistrumRecipeProvider.has(ModItems.PERMUTATION_TEMPLATE_ITEM)
            )
            .save(provider, AnvilCraft.of("shapeless/permut_to_deform"));
    }).initialProperties(() -> new Item.Properties().fireResistant()).tag(ModItemTags.TEMPLATES, ModItemTags.EXPLOSION_PROOF).register();

    public static final ItemEntry<TwoToOneTemplateItem> TWO_TO_ONE_SMITHING_TEMPLATE = REGISTRUM.item(
        "two_to_one_smithing_template",
        TwoToOneTemplateItem::new
    ).lang("Two to One Smithing Template").tag(ModItemTags.MULTIPLE_TO_ONE_SMITHING_TEMPLATES).register();
    public static final ItemEntry<FourToOneTemplateItem> FOUR_TO_ONE_SMITHING_TEMPLATE = REGISTRUM.item(
        "four_to_one_smithing_template",
        FourToOneTemplateItem::new
    ).lang("Four to One Smithing Template").tag(ModItemTags.MULTIPLE_TO_ONE_SMITHING_TEMPLATES).register();
    public static final ItemEntry<EightToOneTemplateItem> EIGHT_TO_ONE_SMITHING_TEMPLATE = REGISTRUM.item(
        "eight_to_one_smithing_template",
        EightToOneTemplateItem::new
    ).lang("Eight to One Smithing Template").tag(ModItemTags.MULTIPLE_TO_ONE_SMITHING_TEMPLATES).register();

    public static final ItemEntry<DiskItem> DISK = REGISTRUM.item("disk", DiskItem::new)
        .properties(p -> p.stacksTo(1))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.IRON_INGOT)
            .define('C', ModItems.MAGNET_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), RegistrumRecipeProvider.has(ModItems.HARDEND_RESIN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), RegistrumRecipeProvider.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT), RegistrumRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider))
        .register();

    public static final ItemEntry<FilterItem> FILTER = REGISTRUM.item("filter", FilterItem::new)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ACA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.HOPPER)
            .define('C', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), RegistrumRecipeProvider.has(ModItems.HARDEND_RESIN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HOPPER), RegistrumRecipeProvider.has(Items.HOPPER))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), RegistrumRecipeProvider.has(ModItems.CIRCUIT_BOARD))
            .save(provider))
        .properties((properties) -> properties.stacksTo(16))
        .register();

    public static final ItemEntry<CrabClawItem> CRAB_CLAW = REGISTRUM.item("crab_claw", CrabClawItem::new)
        .model(DataGenUtil::noExtraModelOrState)
        .register();

    public static final ItemEntry<AmuletBoxItem> AMULET_BOX = REGISTRUM.item("amulet_box", AmuletBoxItem::new)
        .properties((properties) -> properties.stacksTo(1))
        .register();

    public static final ItemEntry<Item> TOTEM_OF_RECOVERY = REGISTRUM.item("totem_of_recovery", Item::new)
        .lang("Totem of Recovery")
        .properties((properties) -> properties.stacksTo(1).rarity(Rarity.UNCOMMON))
        .tag(ModItemTags.TOTEM)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("CCC")
                .pattern("BAB")
                .pattern("CCC")
                .define('A', Items.TOTEM_OF_UNDYING)
                .define('B', ModItems.RECOVERY_PEARL)
                .define('C', Items.ECHO_SHARD)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.TOTEM_OF_UNDYING))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.RECOVERY_PEARL))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.ECHO_SHARD))
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> TOTEM_OF_RAGE = REGISTRUM.item("totem_of_rage", Item::new)
        .lang("Totem of Rage")
        .properties((properties) -> properties.stacksTo(1).rarity(Rarity.UNCOMMON))
        .tag(ModItemTags.TOTEM)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("BBB")
                .pattern("CAC")
                .pattern("BBB")
                .define('A', Items.TOTEM_OF_UNDYING)
                .define('B', ModBlocks.CURSED_GOLD_BLOCK)
                .define('C', ModItems.EMBER_METAL_NUGGET)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.TOTEM_OF_UNDYING))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.CURSED_GOLD_BLOCK))
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModItems.EMBER_METAL_NUGGET))
                .save(provider);
        })
        .register();

    private static ItemEntry<? extends AmuletItem> createAmuletItem(
        String type,
        Supplier<DeferredHolder<AmuletType, ?>> typeGetter,
        NonNullConsumer<JewelCraftingRecipe.Builder> builderConsumer
    ) {
        return REGISTRUM.item(
            type + "_amulet", properties -> new AmuletItem(properties) {
                @Override
                public Holder<AmuletType> getType() {
                    return typeGetter.get();
                }
            }
        ).properties(properties -> properties.stacksTo(1)).tag(ModItemTags.AMULET).recipe((ctx, provider) -> {
            JewelCraftingRecipe.Builder builder = JewelCraftingRecipe.builder()
                .requires(ModItems.SILVER_INGOT, 1)
                .result(new ItemStack(ctx.get()));

            builderConsumer.accept(builder);

            builder.save(provider);
        }).register();
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends AmuletItem> ItemEntry<T> createAmuletItem(
        String type,
        Function<Item.Properties, T> factory,
        Supplier<DeferredHolder<AmuletType, ?>> typeGetter,
        NonNullConsumer<JewelCraftingRecipe.Builder> builderConsumer
    ) {
        return REGISTRUM.item(type + "_amulet", factory::apply)
            .properties(properties -> properties.stacksTo(1))
            .tag(ModItemTags.AMULET)
            .recipe((ctx, provider) -> {
                JewelCraftingRecipe.Builder builder = JewelCraftingRecipe.builder()
                    .requires(ModItems.SILVER_INGOT, 1)
                    .result(new ItemStack(ctx.get()));

                builderConsumer.accept(builder);

                builder.save(provider);
            })
            .register();
    }

    private static ItemBuilder<? extends BigAmuletItem, Registrum> createBigAmuletItem(
        String type,
        Supplier<DeferredHolder<AmuletType, ?>> typeGetter
    ) {
        return REGISTRUM.item(
            type + "_amulet", properties -> new BigAmuletItem(properties) {
                @Override
                public Holder<AmuletType> getType() {
                    return typeGetter.get();
                }
            }
        ).properties(properties -> properties.stacksTo(1)).tag(ModItemTags.AMULET);
    }

    public static final ItemEntry<? extends AmuletItem> EMERALD_AMULET = createAmuletItem(
        "emerald",
        () -> ModAmuletTypes.EMERALD,
        builder -> builder.requires(Items.EMERALD_BLOCK)
    );
    public static final ItemEntry<? extends AmuletItem> TOPAZ_AMULET = createAmuletItem(
        "topaz",
        () -> ModAmuletTypes.TOPAZ,
        builder -> builder.requires(ModBlocks.TOPAZ_BLOCK)
    );
    public static final ItemEntry<? extends AmuletItem> RUBY_AMULET = createAmuletItem(
        "ruby",
        () -> ModAmuletTypes.RUBY,
        builder -> builder.requires(ModBlocks.RUBY_BLOCK)
    );
    public static final ItemEntry<? extends AmuletItem> SAPPHIRE_AMULET = createAmuletItem(
        "sapphire",
        () -> ModAmuletTypes.SAPPHIRE,
        builder -> builder.requires(ModBlocks.SAPPHIRE_BLOCK)
    );
    public static final ItemEntry<? extends AmuletItem> ANVIL_AMULET = createAmuletItem(
        "anvil",
        () -> ModAmuletTypes.ANVIL,
        builder -> builder.requires(Items.ANVIL)
    );
    public static final ItemEntry<ComradeAmuletItem> COMRADE_AMULET = createAmuletItem(
        "comrade",
        ComradeAmuletItem::new,
        () -> ModAmuletTypes.COMRADE,
        builder -> builder.requires(Items.NAME_TAG, 4)
    );
    public static final ItemEntry<? extends AmuletItem> FEATHER_AMULET = createAmuletItem(
        "feather",
        () -> ModAmuletTypes.FEATHER,
        builder -> builder.requires(Items.FEATHER, 16).requires(Items.PHANTOM_MEMBRANE, 4)
    );
    public static final ItemEntry<? extends AmuletItem> CAT_AMULET = createAmuletItem(
        "cat",
        () -> ModAmuletTypes.CAT,
        builder -> builder.requires(Items.SALMON, 16).requires(Items.COD, 16)
    );
    public static final ItemEntry<? extends AmuletItem> DOG_AMULET = createAmuletItem(
        "dog",
        () -> ModAmuletTypes.DOG,
        builder -> builder.requires(Items.BONE, 16).requires(ItemTags.MEAT, 16)
    );
    public static final ItemEntry<? extends AmuletItem> SILENCE_AMULET = createAmuletItem(
        "silence",
        () -> ModAmuletTypes.SILENCE,
        builder -> builder.requires(Items.ECHO_SHARD, 16)
    );
    public static final ItemEntry<? extends AmuletItem> ABNORMAL_AMULET = createAmuletItem(
        "abnormal", () -> ModAmuletTypes.ABNORMAL, // TODO: 修改配方
        builder -> builder.requires(ModItems.CURSED_GOLD_INGOT, 1).requires(ModItems.LEVITATION_POWDER, 16)
    );
    public static final ItemEntry<? extends BigAmuletItem> GEM_AMULET = createBigAmuletItem("gem", () -> ModAmuletTypes.GEM).register();
    public static final ItemEntry<? extends BigAmuletItem> NATURE_AMULET = createBigAmuletItem(
        "nature",
        () -> ModAmuletTypes.NATURE
    ).register();

    public static final ItemEntry<CapacitorItem> CAPACITOR = REGISTRUM.item("capacitor", CapacitorItem::new)
        .model(DataGenUtil::noExtraModelOrState)
        .tag(ModItemTags.CAPACITOR)
        .register();
    public static final ItemEntry<EmptyCapacitorItem> CAPACITOR_EMPTY = REGISTRUM.item("capacitor_empty", EmptyCapacitorItem::new)
        .lang("Empty Capacitor")
        .model(DataGenUtil::noExtraModelOrState)
        .tag(ModItemTags.CAPACITOR)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItemTags.COPPER_PLATES)
            .define('B', ModItems.RESIN)
            .unlockedBy("has_copper_plates", RegistrumRecipeProvider.has(ModItemTags.COPPER_PLATES))
            .unlockedBy("has_resin", RegistrumRecipeProvider.has(ModItems.RESIN))
            .save(provider))
        .register();
    public static final ItemEntry<SuperCapacitorItem> SUPER_CAPACITOR = REGISTRUM.item("supercapacitor", SuperCapacitorItem::new)
        .model(DataGenUtil::noExtraModelOrState)
        .register();
    public static final ItemEntry<EmptySuperCapacitorItem> SUPER_CAPACITOR_EMPTY = REGISTRUM.item(
        "supercapacitor_empty",
        EmptySuperCapacitorItem::new
    ).lang("Empty Supercapacitor").model(DataGenUtil::noExtraModelOrState).register();

    public static final ItemEntry<Item> TIN_CAN = REGISTRUM.item("tin_can", Item::new).register();

    public static final ItemEntry<RecoveryPearl> RECOVERY_PEARL = REGISTRUM.item("recovery_pearl", RecoveryPearl::new)
        .properties((properties) -> properties.stacksTo(16))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                .pattern(" B ")
                .pattern("BAB")
                .pattern(" B ")
                .define('A', Items.ENDER_PEARL)
                .define('B', Items.ECHO_SHARD)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(Items.ENDER_PEARL))
                .save(provider);
        })
        .register();

    public static final ItemEntry<SeedsPackItem> SEEDS_PACK = REGISTRUM.item("seeds_pack", SeedsPackItem::new).register();
    public static final ItemEntry<StructureToolItem> STRUCTURE_TOOL = REGISTRUM.item("structure_tool", StructureToolItem::new)
        .model((ctx, provider) -> provider.generated(ctx::get, ResourceLocation.parse("item/paper")))
        .properties(properties -> properties.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
        .register();

    public static final ItemEntry<PillBoxItem> PILL_BOX = REGISTRUM
        .item("pill_box", PillBoxItem::new)
        .properties((properties) -> properties.stacksTo(1))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.HARDEND_RESIN)
                .define('B', ModFoodItems.PILL)
                .unlockedBy("has_hardend_resin", RegistrumRecipeProvider.has(ModItems.HARDEND_RESIN))
                .unlockedBy("has_pill", RegistrumRecipeProvider.has(ModFoodItems.PILL))
                .save(provider);
        })
        .register();

    static {
        ModFoodItems.register();
        REGISTRUM.defaultCreativeTab(ModItemGroups.ANVILCRAFT_INGREDIENTS.getKey());
    }

    public static final ItemEntry<Item> MAGNET_INGOT = REGISTRUM.item("magnet_ingot", Item::new)
        .tag(Tags.Items.INGOTS, ModItemTags.MAGNET_INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.MAGNET_BLOCK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.MAGNET_BLOCK))
                .group(ctx.getId().toString())
                .save(provider, AnvilCraft.of("magnet_ingot_from_block"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 8)
                .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.HOLLOW_MAGNET_BLOCK))
                .save(provider, AnvilCraft.of("magnet_ingot_from_hollow_block"));
        })
        .register();
    public static final ItemEntry<Item> SPONGE_GEMMULE = REGISTRUM.item("sponge_gemmule", Item::new).register();
    // 皇家钢系
    public static final ItemEntry<RoyalSteelIngotItem> ROYAL_STEEL_INGOT = REGISTRUM
        .item("royal_steel_ingot", RoyalSteelIngotItem::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS, Tags.Items.INGOTS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.ROYAL_STEEL_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("royal_steel_ingot_from_royal_steel_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT)
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_NUGGET)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_NUGGET.get()),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_NUGGET)
                )
                .save(provider, AnvilCraft.of("royal_steel_ingot_from_royal_steel_nugget"));
        })
        .register();
    public static final ItemEntry<Item> ROYAL_STEEL_NUGGET = REGISTRUM.item("royal_steel_nugget", Item::new)
        .tag(Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_NUGGET.get()),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_NUGGET)
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends Item> FROST_METAL_INGOT = REGISTRUM.item("frost_metal_ingot", Item::new)
        .tag(Tags.Items.INGOTS, ModItemTags.FROST_METAL_INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.FROST_METAL_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.FROST_METAL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.FROST_METAL_BLOCK)
                )
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.FROST_METAL_NUGGET)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_NUGGET),
                    RegistrumRecipeProvider.has(ModItems.FROST_METAL_NUGGET)
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends Item> FROST_METAL_NUGGET = REGISTRUM.item("frost_metal_nugget", Item::new)
        .tag(Tags.Items.NUGGETS, ModItemTags.FROST_METAL_NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItems.FROST_METAL_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.FROST_METAL_INGOT), AnvilCraftDatagen.has(ModItems.FROST_METAL_INGOT))
                .save(provider);
        })
        .register();

    public static final ItemEntry<EmberMetalIngotItem> EMBER_METAL_INGOT = REGISTRUM
        .item("ember_metal_ingot", EmberMetalIngotItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.EMBER_METAL_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK)
                )
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.EMBER_METAL_NUGGET)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_NUGGETS),
                    RegistrumRecipeProvider.has(ModItemTags.TUNGSTEN_NUGGETS)
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends Item> EMBER_METAL_NUGGET = REGISTRUM.item("ember_metal_nugget", Item::new)
        .tag(Tags.Items.NUGGETS)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItems.EMBER_METAL_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT), AnvilCraftDatagen.has(ModItems.EMBER_METAL_INGOT))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
        })
        .register();

    public static final ItemEntry<? extends Item> TRANSCENDIUM_INGOT = REGISTRUM.item("transcendium_ingot", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.INGOTS, ModItemTags.EXPLOSION_PROOF, ModItemTags.TRANSCENDIUM_INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TRANSCENDIUM_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.TRANSCENDIUM_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.TRANSCENDIUM_BLOCK)
                )
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.TRANSCENDIUM_NUGGET)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_NUGGET),
                    RegistrumRecipeProvider.has(ModItems.TRANSCENDIUM_NUGGET)
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<? extends Item> TRANSCENDIUM_NUGGET = REGISTRUM.item("transcendium_nugget", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.NUGGETS, ModItemTags.EXPLOSION_PROOF, ModItemTags.TRANSCENDIUM_NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItems.TRANSCENDIUM_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TRANSCENDIUM_INGOT), AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
                .save(provider, AnvilCraft.of(ctx.getId().getPath() + "_from_ingot"));
        })
        .register();

    // 诅咒黄金系
    public static final ItemEntry<CursedItem> CURSED_GOLD_INGOT = REGISTRUM.item("cursed_gold_ingot", CursedItem::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS, ItemTags.PIGLIN_LOVED, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.CURSED_GOLD_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.CURSED_GOLD_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.CURSED_GOLD_BLOCK)
                )
                .save(provider, AnvilCraft.of("cursed_gold_ingot_from_cursed_gold_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.CURSED_GOLD_NUGGET)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_NUGGET.get()),
                    AnvilCraftDatagen.has(ModItems.CURSED_GOLD_NUGGET)
                )
                .save(provider, AnvilCraft.of("cursed_gold_ingot_from_cursed_gold_nugget"));
        })
        .register();
    public static final ItemEntry<CursedItem> CURSED_GOLD_NUGGET = REGISTRUM.item("cursed_gold_nugget", CursedItem::new)
        .tag(ItemTags.PIGLIN_LOVED, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItems.CURSED_GOLD_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT.get()), AnvilCraftDatagen.has(ModItems.CURSED_GOLD_INGOT))
                .save(provider);
        })
        .register();
    public static final ItemEntry<TopazItem> TOPAZ = REGISTRUM.item("topaz", TopazItem::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS, Tags.Items.GEMS, ModItemTags.GEMS_TOPAZ)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TOPAZ_BLOCK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.TOPAZ_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RUBY = REGISTRUM.item("ruby", Item::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS, Tags.Items.GEMS, ModItemTags.GEMS_RUBY)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RUBY_BLOCK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.RUBY_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> SAPPHIRE = REGISTRUM.item("sapphire", Item::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS, Tags.Items.GEMS, ModItemTags.GEMS_SAPPHIRE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.SAPPHIRE_BLOCK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.SAPPHIRE_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<ExpGemItem> EXP_GEM = REGISTRUM.item("exp_gem", ExpGemItem::new)
        .tag(ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.EXP_GEM_BLOCK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.EXP_GEM_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RESIN = REGISTRUM.item("resin", Item::new).tag(ModItemTags.RESIN).recipe((ctx, provider) -> {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
            .requires(ModBlocks.RESIN_BLOCK)
            .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.RESIN_BLOCK))
            .save(provider);
    }).register();
    public static final ItemEntry<Item> AMBER = REGISTRUM.item("amber", Item::new)
        .tag(Tags.Items.GEMS, ModItemTags.GEMS_AMBER)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.AMBER_BLOCK)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModBlocks.AMBER_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> HARDEND_RESIN = REGISTRUM.item("hardend_resin", Item::new).register();
    public static final ItemEntry<Item> WOOD_FIBER = REGISTRUM.item("wood_fiber", Item::new).register();
    public static final ItemEntry<Item> CIRCUIT_BOARD = REGISTRUM.item("circuit_board", Item::new).recipe((ctx, provider) -> {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
            .requires(ModItemTags.COPPER_PLATES)
            .requires(ModItems.HARDEND_RESIN)
            .requires(ModItems.HARDEND_RESIN)
            .requires(ModItems.HARDEND_RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.COPPER_PLATES), AnvilCraftDatagen.has(ModItemTags.COPPER_PLATES))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(ModItems.HARDEND_RESIN))
            .save(provider);
    }).register();
    public static final ItemEntry<Item> PROCESSOR = REGISTRUM.item("processor", Item::new).recipe((ctx, provider) -> {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("   ")
            .pattern("CAC")
            .pattern("BBB")
            .define('A', Items.COMPARATOR)
            .define('B', ModItems.HARDEND_RESIN)
            .define('C', ModItemTags.COPPER_NUGGETS)
            .group(ctx.getId().toString())
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(ModItems.HARDEND_RESIN))
            .save(provider);
    }).register();
    public static final ItemEntry<Item> PRISMARINE_BLADE = REGISTRUM.item("prismarine_blade", Item::new).register();
    public static final ItemEntry<Item> PRISMARINE_CLUSTER = REGISTRUM.item("prismarine_cluster", Item::new).register();
    public static final ItemEntry<Item> SEA_HEART_SHELL = REGISTRUM.item("sea_heart_shell", Item::new).register();
    public static final ItemEntry<Item> SEA_HEART_SHELL_SHARD = REGISTRUM.item("sea_heart_shell_shard", Item::new).register();

    public static final ItemEntry<Item> TUNGSTEN_NUGGET = REGISTRUM.item("tungsten_nugget", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.TUNGSTEN_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TUNGSTEN_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS),
                    RegistrumRecipeProvider.has(ModItemTags.TUNGSTEN_INGOTS)
                )
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> TUNGSTEN_INGOT = REGISTRUM.item("tungsten_ingot", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.TUNGSTEN_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TUNGSTEN_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TUNGSTEN_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.TUNGSTEN_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TUNGSTEN_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_NUGGETS),
                    RegistrumRecipeProvider.has(ModItemTags.TUNGSTEN_NUGGETS)
                )
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_TUNGSTEN), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_TUNGSTEN), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_TUNGSTEN_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TUNGSTEN_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_TUNGSTEN_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TUNGSTEN_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<Item> TITANIUM_NUGGET = REGISTRUM.item("titanium_nugget", Item::new)
        .tag(ModItemTags.TITANIUM_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TITANIUM_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS),
                    RegistrumRecipeProvider.has(ModItemTags.TITANIUM_INGOTS)
                )
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> TITANIUM_INGOT = REGISTRUM.item("titanium_ingot", Item::new)
        .tag(ModItemTags.TITANIUM_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TITANIUM_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TITANIUM_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.TITANIUM_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TITANIUM_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_NUGGETS),
                    RegistrumRecipeProvider.has(ModItemTags.TITANIUM_NUGGETS)
                )
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_TITANIUM), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_TITANIUM))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_TITANIUM), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_TITANIUM))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_TITANIUM_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TITANIUM_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_TITANIUM_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TITANIUM_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<Item> ZINC_NUGGET = REGISTRUM.item("zinc_nugget", Item::new)
        .tag(ModItemTags.ZINC_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.ZINC_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS), RegistrumRecipeProvider.has(ModItemTags.ZINC_INGOTS))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> ZINC_INGOT = REGISTRUM.item("zinc_ingot", Item::new)
        .tag(ModItemTags.ZINC_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.ZINC_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.ZINC_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.ZINC_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.ZINC_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.ZINC_NUGGETS))
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_ZINC), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_ZINC))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_ZINC), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_ZINC))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_ZINC_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_ZINC_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_ZINC_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_ZINC_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<Item> TIN_NUGGET = REGISTRUM.item("tin_nugget", Item::new)
        .tag(ModItemTags.TIN_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.TIN_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS), RegistrumRecipeProvider.has(ModItemTags.TIN_INGOTS))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> TIN_INGOT = REGISTRUM.item("tin_ingot", Item::new)
        .tag(ModItemTags.TIN_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.TIN_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TIN_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.TIN_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TIN_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.TIN_NUGGETS))
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_TIN), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_TIN))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_TIN), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_TIN))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_TIN_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TIN_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_TIN_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TIN_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<Item> LEAD_NUGGET = REGISTRUM.item("lead_nugget", Item::new)
        .tag(ModItemTags.LEAD_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.LEAD_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS), RegistrumRecipeProvider.has(ModItemTags.LEAD_INGOTS))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> LEAD_INGOT = REGISTRUM.item("lead_ingot", Item::new)
        .tag(ModItemTags.LEAD_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.LEAD_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.LEAD_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.LEAD_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.LEAD_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.LEAD_NUGGETS))
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_LEAD), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_LEAD))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_LEAD), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_LEAD))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_LEAD_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_LEAD_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_LEAD_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_LEAD_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<Item> SILVER_NUGGET = REGISTRUM.item("silver_nugget", Item::new)
        .tag(ModItemTags.SILVER_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.SILVER_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS), RegistrumRecipeProvider.has(ModItemTags.SILVER_INGOTS))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> SILVER_INGOT = REGISTRUM.item("silver_ingot", Item::new)
        .tag(ModItemTags.SILVER_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.SILVER_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.SILVER_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.SILVER_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.SILVER_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.SILVER_NUGGETS))
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_SILVER), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_SILVER))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_SILVER), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_SILVER))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));

            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_SILVER_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_SILVER_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_SILVER_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_SILVER_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<RadiationItem> URANIUM_NUGGET = REGISTRUM.item("uranium_nugget", RadiationItem::new)
        .tag(ModItemTags.URANIUM_NUGGETS, Tags.Items.NUGGETS, ModItemTags.RADIATIONS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.URANIUM_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS), RegistrumRecipeProvider.has(ModItemTags.URANIUM_INGOTS))
                .save(provider);
        })
        .register();
    public static final ItemEntry<RadiationItem> URANIUM_INGOT = REGISTRUM.item("uranium_ingot", RadiationItem::new)
        .tag(ModItemTags.URANIUM_INGOTS, Tags.Items.INGOTS, ModItemTags.RADIATIONS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.URANIUM_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.URANIUM_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.URANIUM_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.URANIUM_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_NUGGETS),
                    RegistrumRecipeProvider.has(ModItemTags.URANIUM_NUGGETS)
                )
                .save(provider);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.RAW_URANIUM), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_URANIUM))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModItems.RAW_URANIUM), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModItems.RAW_URANIUM))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName()));
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.DEEPSLATE_URANIUM_ORE), RecipeCategory.MISC, ctx.get(), 1, 200)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_URANIUM_ORE))
                .save(provider, AnvilCraft.of("smelting/" + ctx.getName() + "_from_ore"));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModBlocks.DEEPSLATE_URANIUM_ORE), RecipeCategory.MISC, ctx.get(), 1, 100)
                .group(ctx.getId().toString())
                .unlockedBy("has_item", AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_URANIUM_ORE))
                .save(provider, AnvilCraft.of("blasting/" + ctx.getName() + "_from_ore"));
        })
        .register();
    public static final ItemEntry<RadiationItem> PLUTONIUM_NUGGET = REGISTRUM.item("plutonium_nugget", RadiationItem::new)
        .tag(ModItemTags.PLUTONIUM_NUGGETS, Tags.Items.NUGGETS, ModItemTags.RADIATIONS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.PLUTONIUM_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_INGOTS),
                    RegistrumRecipeProvider.has(ModItemTags.PLUTONIUM_INGOTS)
                )
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
        })
        .register();
    public static final ItemEntry<RadiationItem> PLUTONIUM_INGOT = REGISTRUM.item("plutonium_ingot", RadiationItem::new)
        .tag(ModItemTags.PLUTONIUM_INGOTS, Tags.Items.INGOTS, ModItemTags.RADIATIONS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.PLUTONIUM_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLUTONIUM_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.PLUTONIUM_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.PLUTONIUM_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.PLUTONIUM_NUGGETS),
                    RegistrumRecipeProvider.has(ModItemTags.PLUTONIUM_NUGGETS)
                )
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_nuggets"));
        })
        .register();
    public static final ItemEntry<Item> COPPER_NUGGET = REGISTRUM.item("copper_nugget", Item::new)
        .tag(ModItemTags.COPPER_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(Ingredient.of(Items.COPPER_INGOT))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), RegistrumRecipeProvider.has(Items.COPPER_INGOT))
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.COPPER_INGOT)
                .requires(ctx.get(), 9)
                .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()), RegistrumRecipeProvider.has(ctx.get()))
                .save(provider, AnvilCraft.of("copper_ingot_from_nugget"));
        })
        .register();

    public static final ItemEntry<Item> BRONZE_INGOT = REGISTRUM.item("bronze_ingot", Item::new)
        .tag(ModItemTags.BRONZE_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.BRONZE_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRONZE_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.BRONZE_NUGGETS))
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> BRONZE_NUGGET = REGISTRUM.item("bronze_nugget", Item::new)
        .tag(ModItemTags.BRONZE_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.BRONZE_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS), RegistrumRecipeProvider.has(ModItemTags.BRONZE_INGOTS))
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> BRASS_INGOT = REGISTRUM.item("brass_ingot", Item::new)
        .tag(ModItemTags.BRASS_INGOTS, Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.BRASS_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK.asItem()), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRASS_NUGGETS)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_NUGGETS), RegistrumRecipeProvider.has(ModItemTags.BRASS_NUGGETS))
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> BRASS_NUGGET = REGISTRUM.item("brass_nugget", Item::new)
        .tag(ModItemTags.BRASS_NUGGETS, Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItemTags.BRASS_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS), RegistrumRecipeProvider.has(ModItemTags.BRASS_INGOTS))
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> NETHERITE_CRYSTAL_NUCLEUS = REGISTRUM.item("netherite_crystal_nucleus", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .define('A', ModItemTags.TUNGSTEN_PLATES)
                .define('B', Items.NETHERITE_SCRAP)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_PLATES),
                    RegistrumRecipeProvider.has(ModItemTags.TUNGSTEN_PLATES)
                )
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.NETHERITE_SCRAP), RegistrumRecipeProvider.has(Items.NETHERITE_SCRAP))
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> LIME_POWDER = REGISTRUM.item("lime_powder", Item::new).register();

    public static final ItemEntry<LevitationItem> LEVITATION_POWDER = REGISTRUM.item("levitation_powder", LevitationItem::new)
        .tag(ModItemTags.LEVITATIONALS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.LEVITATION_POWDER_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.LEVITATION_POWDER_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.LEVITATION_POWDER_BLOCK)
                )
                .save(provider, ctx.getId().withSuffix("_from_block"));
        })
        .register();

    public static final ItemEntry<Item> RAW_ZINC = REGISTRUM.item("raw_zinc", Item::new)
        .tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_ZINC)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_ZINC_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_ZINC_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_ZINC_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RAW_TIN = REGISTRUM.item("raw_tin", Item::new).tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_TIN)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_TIN_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TIN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_TIN_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RAW_TITANIUM = REGISTRUM.item("raw_titanium", Item::new)
        .tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_TITANIUM)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_TITANIUM_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TITANIUM_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_TITANIUM_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RAW_TUNGSTEN = REGISTRUM.item("raw_tungsten", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant()).tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_TUNGSTEN)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_TUNGSTEN_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_TUNGSTEN_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_TUNGSTEN_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RAW_LEAD = REGISTRUM.item("raw_lead", Item::new)
        .tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_LEAD)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_LEAD_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_LEAD_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_LEAD_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> RAW_SILVER = REGISTRUM.item("raw_silver", Item::new)
        .tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_SILVER)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_SILVER_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_SILVER_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_SILVER_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<RadiationItem> RAW_URANIUM = REGISTRUM.item("raw_uranium", RadiationItem::new)
        .tag(Tags.Items.RAW_MATERIALS, ModItemTags.RAW_URANIUM, ModItemTags.RADIATIONS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.RAW_URANIUM_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.RAW_URANIUM_BLOCK), AnvilCraftDatagen.has(ModBlocks.RAW_URANIUM_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> VOID_MATTER = REGISTRUM.item("void_matter", Item::new)
        .tag(ModItemTags.VOID_RESISTANT)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.VOID_MATTER_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.VOID_MATTER_BLOCK), AnvilCraftDatagen.has(ModBlocks.VOID_MATTER_BLOCK))
                .save(provider);
        })
        .register();
    public static final ItemEntry<Item> EARTH_CORE_SHARD = REGISTRUM.item("earth_core_shard", Item::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.EARTH_CORE_SHARD_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.EARTH_CORE_SHARD_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.EARTH_CORE_SHARD_BLOCK)
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<MultiphaseMatterItem> MULTIPHASE_MATTER = REGISTRUM.item("multiphase_matter", MultiphaseMatterItem::new)
        .properties(Item.Properties::fireResistant)
        .tag(ModItemTags.EXPLOSION_PROOF)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9).requires(ModBlocks.MULTIPHASE_MATTER_BLOCK).unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.MULTIPHASE_MATTER_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.MULTIPHASE_MATTER_BLOCK)
            ).save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
        })
        .register();
    public static final ItemEntry<HeavyHalberdCoreItem> HEAVY_HALBERD_CORE = REGISTRUM.item(
            "heavy_halberd_core",
            HeavyHalberdCoreItem::new
        )
        .properties(Item.Properties::fireResistant)
        .tag(ModItemTags.EXPLOSION_PROOF)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("HHH")
            .pattern("HMH")
            .pattern("HHH")
            .define('H', ModBlocks.HEAVY_IRON_BLOCK)
            .define('M', ModItems.MULTIPHASE_MATTER)
            .unlockedBy("has_heavy_iron_block", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .save(provider))
        .register();
    public static final ItemEntry<ResonatorCoreItem> RESONATOR_CORE = REGISTRUM.item("resonator_core", ResonatorCoreItem::new)
        .properties(Item.Properties::fireResistant)
        .tag(ModItemTags.EXPLOSION_PROOF)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AEA")
            .pattern("EME")
            .pattern("AEA")
            .define('A', Items.AMETHYST_SHARD)
            .define('E', Items.ECHO_SHARD)
            .define('M', ModItems.MULTIPHASE_MATTER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_SHARD), AnvilCraftDatagen.has(Items.AMETHYST_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.ECHO_SHARD), AnvilCraftDatagen.has(Items.ECHO_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MULTIPHASE_MATTER), AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .save(provider))
        .register();

    public static final ItemEntry<MultiphaseTranscendiumItem> MULTIPHASE_TRANSCENDIUM = REGISTRUM.item(
        "multiphase_transcendium",
        MultiphaseTranscendiumItem::new
    ).properties(Item.Properties::fireResistant).tag(ModItemTags.EXPLOSION_PROOF).recipe((ctx, provider) -> {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.MULTIPHASE_MATTER),
                Ingredient.of(ModItems.TRANSCENDIUM_INGOT),
                RecipeCategory.MISC,
                ctx.get()
            )
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE))
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.MULTIPHASE_MATTER))
            .unlocks("hasitem", AnvilCraftDatagen.has(ModItems.TRANSCENDIUM_INGOT))
            .save(provider, of("multiphase_transcendium"));
    }).register();

    public static final ItemEntry<Item> NEGATIVE_MATTER = REGISTRUM.item("negative_matter", Item::new)
        .initialProperties(Item.Properties::new)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModBlocks.NEGATIVE_MATTER_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.NEGATIVE_MATTER_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.NEGATIVE_MATTER_BLOCK)
                )
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_block"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.NEGATIVE_MATTER_NUGGET)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER_NUGGET),
                    RegistrumRecipeProvider.has(ModItems.NEGATIVE_MATTER_NUGGET)
                )
                .save(provider);
        })
        .register();

    public static final ItemEntry<Item> NEGATIVE_MATTER_NUGGET = REGISTRUM.item("negative_matter_nugget", Item::new)
        .initialProperties(Item.Properties::new)
        .tag(Tags.Items.NUGGETS)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 9)
                .requires(ModItems.NEGATIVE_MATTER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER), AnvilCraftDatagen.has(ModItems.NEGATIVE_MATTER))
                .save(provider, AnvilCraft.of(BuiltInRegistries.ITEM.getKey(ctx.get()).getPath() + "_from_ingot"));
        })
        .register();

    public static final ItemEntry<SuperHeavyItem> NEUTRONIUM_INGOT = REGISTRUM.item("neutronium_ingot", SuperHeavyItem::new)
        .tag(Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .properties((properties) -> properties.stacksTo(1))
        .register();
    public static final ItemEntry<SuperHeavyItem> STABLE_NEUTRONIUM_INGOT = REGISTRUM.item("stable_neutronium_ingot", SuperHeavyItem::new)
        .tag(Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .properties((properties) -> properties.stacksTo(1))
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get(), 1)
                .requires(ModItems.NEUTRONIUM_INGOT)
                .requires(ModItems.LEVITATION_POWDER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.NEUTRONIUM_INGOT), AnvilCraftDatagen.has(ModItems.NEUTRONIUM_INGOT))
                .save(provider);
        })
        .register();
    public static final ItemEntry<SuperHeavyItem> CHARGED_NEUTRONIUM_INGOT = REGISTRUM.item(
            "charged_neutronium_ingot",
            SuperHeavyItem::new
        )
        .tag(Tags.Items.INGOTS, ItemTags.BEACON_PAYMENT_ITEMS)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .properties((properties) -> properties.stacksTo(1))
        .register();

    public static final ItemEntry<BucketItem> EXP_BUCKET = REGISTRUM.item(
            "exp_bucket",
            p -> new BucketItem(ModFluids.EXP_FLUID.get(), p)
        )
        .tag(ModItemTags.EXP_BUCKETS, Tags.Items.BUCKETS)
        .initialProperties(() -> new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))
        .model(ModelProviderUtil::bucket)
        .register();

    public static final ItemEntry<BucketItem> OIL_BUCKET = REGISTRUM.item("oil_bucket", p -> new BucketItem(ModFluids.OIL.get(), p))
        .tag(ModItemTags.OIL_BUCKETS, Tags.Items.BUCKETS)
        .initialProperties(() -> new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))
        .model(ModelProviderUtil::bucket)
        .register();

    public static final Object2ObjectMap<Color, ItemEntry<BucketItem>> CEMENT_BUCKETS = registerAllCementBuckets();

    private static Object2ObjectMap<Color, ItemEntry<BucketItem>> registerAllCementBuckets() {
        Object2ObjectMap<Color, ItemEntry<BucketItem>> map = new Object2ObjectOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerCementBucket(color);
            map.put(color, entry);
        }
        return map;
    }

    private static ItemEntry<BucketItem> registerCementBucket(Color color) {
        return REGISTRUM.item("%s_cement_bucket".formatted(color), p -> new BucketItem(ModFluids.SOURCE_CEMENTS.get(color).get(), p))
            .tag(Tags.Items.BUCKETS, ModItemTags.CEMENT_BUCKETS)
            .properties(p -> p.stacksTo(1).craftRemainder(Items.BUCKET))
            .model(ModelProviderUtil::bucket)
            .register();
    }

    public static ItemEntry<BucketItem> MELT_GEM_BUCKET = REGISTRUM.item(
        "melt_gem_bucket",
        p -> new BucketItem(ModFluids.MELT_GEM.get(), p)
    ).tag(Tags.Items.BUCKETS).properties(p -> p.stacksTo(1).craftRemainder(Items.BUCKET)).model(ModelProviderUtil::bucket).register();

    public static void register() {
    }

    public static ItemStack enchanted(ItemLike item, ResourceKey<Enchantment> enchKey, int level, HolderLookup.Provider registries) {
        var stack = item.asItem().getDefaultInstance();
        var holder0 = registries.holder(enchKey);
        if (holder0.isPresent()) {
            stack.enchant(holder0.get(), level);
        } else {
            AnvilCraft.LOGGER.error("", new NoSuchElementException(enchKey.location().toString()));
        }
        // stack.enchant(registries.holderOrThrow(enchKey), level);
        return stack;
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, CreativeModeTabModifier> enchanting(
        ResourceKey<Enchantment> enchKey,
        int level
    ) {
        return (ctx, modifier) -> {
            modifier.accept(enchanted(ctx.get(), enchKey, level, modifier.getParameters().holders()));
        };
    }
}
