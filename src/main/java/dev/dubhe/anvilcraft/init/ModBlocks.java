package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent.Switch;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.ActiveSilencerBlock;
import dev.dubhe.anvilcraft.block.AdvancedRepeaterBlock;
import dev.dubhe.anvilcraft.block.AmberBlock;
import dev.dubhe.anvilcraft.block.ArrowBlock;
import dev.dubhe.anvilcraft.block.BatchCrafterBlock;
import dev.dubhe.anvilcraft.block.BerryCakeBlock;
import dev.dubhe.anvilcraft.block.BerryCreamBlock;
import dev.dubhe.anvilcraft.block.BlockComparatorBlock;
import dev.dubhe.anvilcraft.block.BlockDevourerBlock;
import dev.dubhe.anvilcraft.block.BlockPlacerBlock;
import dev.dubhe.anvilcraft.block.CakeBaseBlock;
import dev.dubhe.anvilcraft.block.CakeBlock;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.ChargeCollectorBlock;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.block.ChocolateCakeBlock;
import dev.dubhe.anvilcraft.block.ChocolateCreamBlock;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.CreamBlock;
import dev.dubhe.anvilcraft.block.CreativeGeneratorBlock;
import dev.dubhe.anvilcraft.block.CrushingTableBlock;
import dev.dubhe.anvilcraft.block.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.DischargerBlock;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.EmberGrindstone;
import dev.dubhe.anvilcraft.block.EmberMetalBlock;
import dev.dubhe.anvilcraft.block.EmberMetalPillarBlock;
import dev.dubhe.anvilcraft.block.EmberMetalSlabBlock;
import dev.dubhe.anvilcraft.block.EmberMetalStairBlock;
import dev.dubhe.anvilcraft.block.EmberSmithingTableBlock;
import dev.dubhe.anvilcraft.block.EndDustBlock;
import dev.dubhe.anvilcraft.block.FerriteCoreMagnetBlock;
import dev.dubhe.anvilcraft.block.FireCauldronBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.GlowingMetalBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.block.HeavyIronDoorBlock;
import dev.dubhe.anvilcraft.block.HeavyIronPlateBlock;
import dev.dubhe.anvilcraft.block.HeavyIronTrapdoorBlock;
import dev.dubhe.anvilcraft.block.HeavyIronWallBlock;
import dev.dubhe.anvilcraft.block.HeliostatsBlock;
import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.HoneyCauldronBlock;
import dev.dubhe.anvilcraft.block.ImpactPileBlock;
import dev.dubhe.anvilcraft.block.IncandescentMetalBlock;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.block.ItemDetectorBlock;
import dev.dubhe.anvilcraft.block.JewelCraftingTable;
import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.LevitationPowderBlock;
import dev.dubhe.anvilcraft.block.LoadMonitorBlock;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.block.MagnetoElectricCoreBlock;
import dev.dubhe.anvilcraft.block.MeltGemCauldron;
import dev.dubhe.anvilcraft.block.MengerSpongeBlock;
import dev.dubhe.anvilcraft.block.MineralFountainBlock;
import dev.dubhe.anvilcraft.block.MobAmberBlock;
import dev.dubhe.anvilcraft.block.NegativeMatterBlock;
import dev.dubhe.anvilcraft.block.NestingShulkerBoxBlock;
import dev.dubhe.anvilcraft.block.ObsidianCauldron;
import dev.dubhe.anvilcraft.block.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.OverNestingShulkerBoxBlock;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.PiezoelectricCrystalBlock;
import dev.dubhe.anvilcraft.block.PowerConverterBigBlock;
import dev.dubhe.anvilcraft.block.PowerConverterMiddleBlock;
import dev.dubhe.anvilcraft.block.PowerConverterSmallBlock;
import dev.dubhe.anvilcraft.block.RedhotMetalBlock;
import dev.dubhe.anvilcraft.block.ReinforcedConcreteBlock;
import dev.dubhe.anvilcraft.block.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.ResentfulAmberBlock;
import dev.dubhe.anvilcraft.block.ResinBlock;
import dev.dubhe.anvilcraft.block.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.block.RoyalGrindstone;
import dev.dubhe.anvilcraft.block.RoyalSmithingTableBlock;
import dev.dubhe.anvilcraft.block.RubyLaserBlock;
import dev.dubhe.anvilcraft.block.RubyPrismBlock;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import dev.dubhe.anvilcraft.block.SlidingRailBlock;
import dev.dubhe.anvilcraft.block.SlidingRailStopBlock;
import dev.dubhe.anvilcraft.block.SpaceOvercompressorBlock;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.block.StampingPlatformBlock;
import dev.dubhe.anvilcraft.block.StepEffectBlock;
import dev.dubhe.anvilcraft.block.StepEffectSlabBlock;
import dev.dubhe.anvilcraft.block.StepEffectStairBlock;
import dev.dubhe.anvilcraft.block.SupercriticalNestingShulkerBoxBlock;
import dev.dubhe.anvilcraft.block.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.ThermoelectricConverterBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.TransparentCraftingTableBlock;
import dev.dubhe.anvilcraft.block.VoidEnergyCollectorBlock;
import dev.dubhe.anvilcraft.block.VoidMatterBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.plate.EntityCountPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.EntityTypePressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.FireImmunePressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.HealthPercentPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.ItemDurabilityPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PlayerHungerPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PlayerInventoryPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.TimeCountedPressurePlateBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.item.CursedBlockItem;
import dev.dubhe.anvilcraft.item.EndDustBlockItem;
import dev.dubhe.anvilcraft.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.HeliostatsItem;
import dev.dubhe.anvilcraft.item.LevitationPowderBlockItem;
import dev.dubhe.anvilcraft.item.PlaceInWaterBlockItem;
import dev.dubhe.anvilcraft.item.ResinBlockItem;
import dev.dubhe.anvilcraft.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.TeslaTowerItem;
import dev.dubhe.anvilcraft.util.DangerUtil;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import dev.dubhe.anvilcraft.util.ModelProviderUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.SWITCH;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings({"unused", "CodeBlock2Expr"})
public class ModBlocks {

    static {
        REGISTRATE.defaultCreativeTab(ModItemGroups.ANVILCRAFT_FUNCTION_BLOCK.getKey());
    }

    public static final BlockEntry<? extends Block> MAGNET_BLOCK = REGISTRATE
        .block("magnet_block", MagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS,
            ModItemTags.STORAGE_BLOCKS_MAGNET)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.MAGNET,
            BlockTags.NEEDS_STONE_TOOL,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_MAGNET)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> HOLLOW_MAGNET_BLOCK = REGISTRATE
        .block("hollow_magnet_block", HollowMagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.MAGNET,
            BlockTags.NEEDS_STONE_TOOL)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("AAA")
                .pattern("A A")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> FERRITE_CORE_MAGNET_BLOCK = REGISTRATE
        .block("ferrite_core_magnet_block", FerriteCoreMagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::randomTicks)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.MAGNET,
            BlockTags.NEEDS_STONE_TOOL)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .define('B', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_magnet_ingot", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
                .unlockedBy("has_iron_ingot", RegistrateRecipeProvider.has(Tags.Items.INGOTS_IRON))
                .save(provider);
        })
        .register();

    public static final BlockEntry<? extends Block> STAMPING_PLATFORM = REGISTRATE
        .block("stamping_platform", StampingPlatformBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("BAB")
                .pattern("B B")
                .pattern("B B")
                .define('A', ModItemTags.IRON_PLATES)
                .define('B', Tags.Items.INGOTS_IRON)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.IRON_PLATES),
                    AnvilCraftDatagen.has(ModItemTags.IRON_PLATES))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Tags.Items.INGOTS_IRON))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> CRUSHING_TABLE = REGISTRATE
        .block("crushing_table", CrushingTableBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
                .requires(ModBlocks.STAMPING_PLATFORM)
                .requires(Items.GRINDSTONE)
                .unlockedBy(
                    "has_" + Items.GRINDSTONE,
                    AnvilCraftDatagen.has(Items.GRINDSTONE))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Tags.Items.INGOTS_IRON))
                .save(provider, AnvilCraft.of("shapeless_crushing_table_recipe"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("BAB")
                .pattern("B B")
                .pattern("B B")
                .define('A', Items.GRINDSTONE)
                .define('B', Tags.Items.INGOTS_IRON)
                .unlockedBy(
                    "has_" + Items.GRINDSTONE,
                    AnvilCraftDatagen.has(Items.GRINDSTONE))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Tags.Items.INGOTS_IRON))
                .save(provider, AnvilCraft.of("shaped_crushing_table_recipe"));

        })
        .register();
    public static final BlockEntry<? extends Block> CORRUPTED_BEACON = REGISTRATE
        .block("corrupted_beacon", CorruptedBeaconBlock::new)
        .initialProperties(() -> Blocks.BEACON)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<GiantAnvilBlock> GIANT_ANVIL = REGISTRATE
        .block("giant_anvil", GiantAnvilBlock::new)
        .initialProperties(() -> Blocks.ANVIL)
        .loot(SimpleMultiPartBlock::loot)
        .properties(
            p -> p.noOcclusion().strength(4.0F).sound(GiantAnvilBlock.SOUND_TYPE).explosionResistance(1200))
        .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
        .build()
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends SpectralAnvilBlock> SPECTRAL_ANVIL = REGISTRATE
        .block("spectral_anvil", SpectralAnvilBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(p -> p.mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 1200.0F)
            .sound(SoundType.ANVIL))
        .blockstate((ctx, provider) -> {
        })
        .item()
        .tag(ItemTags.ANVIL)
        .build()
        .tag(
            BlockTags.ANVIL,
            ModBlockTags.NON_MAGNETIC,
            ModBlockTags.CANT_BROKEN_ANVIL,
            BlockTags.MINEABLE_WITH_PICKAXE
        )
        .register();

    public static final BlockEntry<? extends Block> ROYAL_ANVIL = REGISTRATE
        .block("royal_anvil", RoyalAnvilBlock::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(Items.ANVIL),
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.MISC,
                    ctx.get().asItem())
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/royal_anvil"));
        })
        .initialProperties(() -> Blocks.ANVIL)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .item()
        .tag(ItemTags.ANVIL)
        .build()
        .tag(BlockTags.ANVIL,
            ModBlockTags.CANT_BROKEN_ANVIL,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_GRINDSTONE = REGISTRATE
        .block("royal_grindstone", RoyalGrindstone::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(Items.GRINDSTONE),
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.MISC,
                    ctx.get().asItem())
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/royal_grindstone"));
        })
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_SMITHING_TABLE = REGISTRATE
        .block("royal_smithing_table", RoyalSmithingTableBlock::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(Items.SMITHING_TABLE),
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.MISC,
                    ctx.get().asItem())
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/royal_smithing_table"));
        })
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .register();

    public static final BlockEntry<EmberAnvilBlock> EMBER_ANVIL = REGISTRATE
        .block("ember_anvil", EmberAnvilBlock::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModBlocks.ROYAL_ANVIL),
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.MISC,
                    ctx.get().asItem())
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/ember_anvil"));
        })
        .initialProperties(() -> Blocks.ANVIL)
        .tag(BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            BlockTags.ANVIL,
            ModBlockTags.CANT_BROKEN_ANVIL,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .properties(properties -> properties.lightLevel(state -> 9)
            .noOcclusion()
            .strength(50.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.ANVIL)
        .build()
        .register();
    public static final BlockEntry<EmberGrindstone> EMBER_GRINDSTONE = REGISTRATE
        .block("ember_grindstone", EmberGrindstone::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModBlocks.ROYAL_GRINDSTONE),
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.MISC,
                    ctx.get().asItem())
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/ember_grindstone"));
        })
        .tag(BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9)
            .noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .register();
    public static final BlockEntry<EmberSmithingTableBlock> EMBER_SMITHING_TABLE = REGISTRATE
        .block("ember_smithing_table", EmberSmithingTableBlock::new)
        .recipe((ctx, provider) -> {
            SmithingTransformRecipeBuilder.smithing(
                    Ingredient.of(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE),
                    Ingredient.of(ModBlocks.ROYAL_SMITHING_TABLE),
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.MISC,
                    ctx.get().asItem())
                .unlocks("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("smithing/ember_smithing_table"));
        })
        .tag(BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9)
            .noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .register();

    public static final BlockEntry<? extends Block> CREATIVE_GENERATOR = REGISTRATE
        .block("creative_generator", CreativeGeneratorBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .item()
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> HEATER = REGISTRATE
        .block("heater", HeaterBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.noOcclusion().lightLevel(state -> state.getValue(OVERLOAD) ? 0 : 15))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("BBB")
                .define('A', Items.TERRACOTTA)
                .define('B', Items.IRON_INGOT)
                .define('C', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.TERRACOTTA), AnvilCraftDatagen.has(Items.TERRACOTTA))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> TRANSMISSION_POLE = REGISTRATE
        .block("transmission_pole", TransmissionPoleBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.noOcclusion().lightLevel(state -> {
            if (state.getValue(TransmissionPoleBlock.HALF) != Vertical3PartHalf.TOP) return 0;
            if (state.getValue(SWITCH) == Switch.OFF) return 0;
            if (state.getValue(OVERLOAD)) return 6;
            return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .item(SimpleMultiPartBlockItem<Vertical3PartHalf>::new)
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("B")
                .pattern("C")
                .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .define('B', Items.LIGHTNING_ROD)
                .define('C', Items.IRON_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD), AnvilCraftDatagen.has(Items.LIGHTNING_ROD))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
                .save(provider);
        })
        .loot(SimpleMultiPartBlock::loot)
        .register();
    public static final BlockEntry<? extends Block> REMOTE_TRANSMISSION_POLE = REGISTRATE
        .block("remote_transmission_pole", RemoteTransmissionPoleBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .loot(SimpleMultiPartBlock::loot)
        .properties(properties -> properties.noOcclusion().lightLevel(state -> {
            if (state.getValue(RemoteTransmissionPoleBlock.HALF) != Vertical4PartHalf.TOP) return 0;
            if (state.getValue(SWITCH) == Switch.OFF) return 0;
            if (state.getValue(OVERLOAD)) return 6;
            return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .item(SimpleMultiPartBlockItem<Vertical4PartHalf>::new)
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("B")
                .pattern("C")
                .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .define('B', ModBlocks.TRANSMISSION_POLE)
                .define('C', Items.ANVIL)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.TRANSMISSION_POLE),
                    AnvilCraftDatagen.has(ModBlocks.TRANSMISSION_POLE))
                .save(provider);
        })
        .register();
    public static final BlockEntry<TeslaTowerBlock> TESLA_TOWER = REGISTRATE
        .block("tesla_tower", TeslaTowerBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .loot(SimpleMultiPartBlock::loot)
        .properties(properties -> properties.noOcclusion().lightLevel(state -> {
            if (state.getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.TOP) return 0;
            if (state.getValue(SWITCH) == Switch.OFF) return 0;
            if (state.getValue(OVERLOAD)) return 6;
            return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .item(TeslaTowerItem::new)
        .model((ctx, provider) -> {
            provider.blockItem(ctx, "_overall");
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("ACA")
                .pattern("ADA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .define('B', ModBlocks.TOPAZ_BLOCK)
                .define('C', ModBlocks.TRANSMISSION_POLE)
                .define('D', ModItems.CIRCUIT_BOARD)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD),
                    AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.TRANSMISSION_POLE),
                    AnvilCraftDatagen.has(ModBlocks.TRANSMISSION_POLE))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.TOPAZ_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.TOPAZ_BLOCK))
                .save(provider);
        })
        .register();
    public static final BlockEntry<InductionLightBlock> INDUCTION_LIGHT = REGISTRATE
        .block("induction_light", InductionLightBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(p -> p.noOcclusion().lightLevel(state -> {
            if (state.getValue(InductionLightBlock.POWERED)) return 0;
            if (state.getValue(InductionLightBlock.OVERLOAD)) return 7;
            return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
                .pattern("A")
                .pattern("B")
                .pattern("A")
                .define('A', Items.IRON_INGOT)
                .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .save(provider);
        })
        .register();
    public static final BlockEntry<ChargeCollectorBlock> CHARGE_COLLECTOR = REGISTRATE
        .block("charge_collector", ChargeCollectorBlock::new)
        .simpleItem()
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern(" A ")
                .pattern("B B")
                .pattern("CCC")
                .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .define('B', Items.COPPER_INGOT)
                .define('C', Items.IRON_INGOT)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .save(provider);
        })
        .register();
    public static final BlockEntry<HeliostatsBlock> HELIOSTATS = REGISTRATE
        .block("heliostats", HeliostatsBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .blockstate((ctx, prov) -> {
        })
        .defaultLoot()
        .item(HeliostatsItem::new)
        .model((a, b) -> {
        })
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
                .pattern("S S")
                .pattern("SFS")
                .pattern(" I ")
                .define('S', ModItemTags.SILVER_PLATES)
                .define('F', Items.SUNFLOWER)
                .define('I', Blocks.IRON_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.SUNFLOWER), AnvilCraftDatagen.has(Items.SUNFLOWER))
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.IRON_BLOCK), AnvilCraftDatagen.has(Blocks.IRON_BLOCK))
                .save(provider, AnvilCraft.of("heliostats_biological"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
                .pattern("SDS")
                .pattern("SCS")
                .pattern(" I ")
                .define('S', ModItemTags.SILVER_PLATES)
                .define('D', Blocks.DAYLIGHT_DETECTOR)
                .define('C', ModItems.CIRCUIT_BOARD)
                .define('I', Blocks.IRON_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.IRON_BLOCK), AnvilCraftDatagen.has(Blocks.IRON_BLOCK))
                .save(provider, AnvilCraft.of("heliostats_electrical"));
        })
        .register();
    public static final BlockEntry<LoadMonitorBlock> LOAD_MONITOR = REGISTRATE
        .block("load_monitor", LoadMonitorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(state -> {
                if (state.getValue(OVERLOAD)) return 6;
                else return 15;
            })
            .noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx, "_0"))
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                .pattern("A")
                .pattern("B")
                .define('A', Items.COMPASS)
                .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.COMPASS), AnvilCraftDatagen.has(Items.COMPASS))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .save(provider);
        })
        .register();
    public static final BlockEntry<PowerConverterSmallBlock> POWER_CONVERTER_SMALL = REGISTRATE
        .block("power_converter_small", PowerConverterSmallBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(state -> {
            if (state.getValue(OVERLOAD)) return 6;
            else return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POWER_CONVERTER_BIG),
                    RecipeCategory.MISC,
                    ctx.get(),
                    9)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_big"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POWER_CONVERTER_MIDDLE),
                    RecipeCategory.MISC,
                    ctx.get(),
                    3)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_middle"));
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<PowerConverterMiddleBlock> POWER_CONVERTER_MIDDLE = REGISTRATE
        .block("power_converter_middle", PowerConverterMiddleBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(state -> {
            if (state.getValue(OVERLOAD)) return 6;
            else return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("A")
                .pattern("A")
                .define('A', ModBlocks.POWER_CONVERTER_SMALL)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_SMALL),
                    AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_SMALL))
                .save(provider, ctx.getId() + "_from_small");
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POWER_CONVERTER_BIG),
                    RecipeCategory.MISC,
                    ctx.get(),
                    3)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_BIG))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<PowerConverterBigBlock> POWER_CONVERTER_BIG = REGISTRATE
        .block("power_converter_big", PowerConverterBigBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(state -> {
            if (state.getValue(OVERLOAD)) return 6;
            else return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("B")
                .define('A', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .define('B', Items.COPPER_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("A")
                .pattern("A")
                .define('A', ModBlocks.POWER_CONVERTER_MIDDLE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_MIDDLE),
                    AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE))
                .save(provider, ctx.getId() + "_from_middle");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModBlocks.POWER_CONVERTER_SMALL)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_MIDDLE),
                    AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE))
                .save(provider, ctx.getId() + "_from_small");
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<PiezoelectricCrystalBlock> PIEZOELECTRIC_CRYSTAL = REGISTRATE
        .block("piezoelectric_crystal", PiezoelectricCrystalBlock::new)
        .simpleItem()
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern(" B ")
                .pattern("ABA")
                .define('A', Items.COPPER_INGOT)
                .define('B', Items.QUARTZ_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.QUARTZ_BLOCK),
                    AnvilCraftDatagen.has(Items.QUARTZ_BLOCK))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern(" B ")
                .pattern("ABA")
                .define('A', Items.COPPER_INGOT)
                .define('B', Items.AMETHYST_BLOCK)
                .group(ctx.getId().toString())
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK),
                    AnvilCraftDatagen.has(Items.AMETHYST_BLOCK))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_amethyst");
        })
        .register();
    public static final BlockEntry<? extends Block> BATCH_CRAFTER = REGISTRATE
        .block("batch_crafter", BatchCrafterBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("ABA")
                .pattern("ADA")
                .pattern("AEA")
                .define('A', Items.GLASS)
                .define('B', Items.CRAFTER)
                .define('D', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .define('E', ModItems.CIRCUIT_BOARD)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.GLASS),
                    AnvilCraftDatagen.has(Items.GLASS)
                ).unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.CRAFTER),
                    AnvilCraftDatagen.has(Items.CRAFTER)
                ).unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                ).unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD),
                    AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD)
                ).save(provider);
        })
        .register();

    public static final BlockEntry<ItemCollectorBlock> ITEM_COLLECTOR = REGISTRATE
        .block("item_collector", ItemCollectorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .blockstate((c, p) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((c, p) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ACA")
                .define('A', Items.IRON_INGOT)
                .define('B', ModItems.MAGNET)
                .define('C', Items.HOPPER)
                .define('D', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.MAGNET), AnvilCraftDatagen.has(ModItems.MAGNET))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.HOPPER), AnvilCraftDatagen.has(Items.HOPPER))
                .save(p);
        })
        .register();
    public static final BlockEntry<ThermoelectricConverterBlock> THERMOELECTRIC_CONVERTER = REGISTRATE
        .block("thermoelectric_converter", ThermoelectricConverterBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .define('B', Items.COPPER_INGOT)
                .define('C', ModBlocks.SAPPHIRE_BLOCK)
                .define('D', Items.BLUE_ICE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.SAPPHIRE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.SAPPHIRE_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.BLUE_ICE), AnvilCraftDatagen.has(Items.BLUE_ICE))
                .save(provider);
        })
        .register();
    public static final BlockEntry<ChargerBlock> CHARGER = REGISTRATE
        .block("charger", ChargerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, prov) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A A")
                .pattern("ABA")
                .pattern("CCC")
                .define('A', Items.COPPER_INGOT)
                .define('B', ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK)
                .define('C', Items.IRON_INGOT)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
                .requires(ModBlocks.DISCHARGER)
                .unlockedBy("hasitme", AnvilCraftDatagen.has(ModBlocks.DISCHARGER))
                .save(provider, AnvilCraft.of("charger_from_discharger"));
        })
        .register();
    public static final BlockEntry<DischargerBlock> DISCHARGER = REGISTRATE
        .block("discharger", DischargerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .blockstate((ctx, prov) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
                .requires(ModBlocks.CHARGER)
                .unlockedBy("hasitme", AnvilCraftDatagen.has(ModBlocks.DISCHARGER))
                .save(provider, AnvilCraft.of("discharger_from_charger"));
        })
        .register();
    public static final BlockEntry<ActiveSilencerBlock> ACTIVE_SILENCER = REGISTRATE
        .block("active_silencer", ActiveSilencerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("ACA")
                .define('A', Items.AMETHYST_BLOCK)
                .define('B', Items.JUKEBOX)
                .define('C', Items.SCULK_SENSOR)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK),
                    AnvilCraftDatagen.has(Items.AMETHYST_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.JUKEBOX), AnvilCraftDatagen.has(Items.JUKEBOX))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.SCULK_SENSOR), AnvilCraftDatagen.has(Items.SCULK_SENSOR))
                .save(provider);
        })
        .register();
    public static final BlockEntry<BlockPlacerBlock> BLOCK_PLACER = REGISTRATE
        .block("block_placer", BlockPlacerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("AAA")
                .pattern("DCB")
                .pattern("AAA")
                .define('A', Items.COBBLESTONE)
                .define('B', ModItems.CRAB_CLAW)
                .define('C', Items.REDSTONE)
                .define('D', Items.HOPPER)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.CRAB_CLAW), AnvilCraftDatagen.has(ModItems.CRAB_CLAW))
                .save(provider);
        })
        .register();
    public static final BlockEntry<BlockDevourerBlock> BLOCK_DEVOURER = REGISTRATE
        .block("block_devourer", BlockDevourerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("DA ")
                .pattern("CBA")
                .pattern("DA ")
                .define('A', Items.NETHERITE_INGOT)
                .define('B', Items.DRAGON_HEAD)
                .define('C', Items.REDSTONE)
                .define('D', Items.COBBLESTONE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.NETHERITE_INGOT),
                    AnvilCraftDatagen.has(Items.NETHERITE_INGOT))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.PISTON), AnvilCraftDatagen.has(Items.PISTON))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.REDSTONE), AnvilCraftDatagen.has(Items.REDSTONE))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.COBBLESTONE), AnvilCraftDatagen.has(Items.COBBLESTONE))
                .save(provider);
        })
        .register();
    public static final BlockEntry<RubyLaserBlock> RUBY_LASER = REGISTRATE
        .block("ruby_laser", RubyLaserBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(it -> {
            if (it.getValue(RubyLaserBlock.SWITCH) == Switch.ON) return 15;
            else return 0;
        }))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AEA")
                .pattern("BDB")
                .pattern("ACA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .define('B', ModBlocks.INDUCTION_LIGHT)
                .define('C', ModItemTags.SILVER_PLATES)
                .define('D', ModBlocks.RUBY_BLOCK)
                .define('E', Items.TINTED_GLASS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.INDUCTION_LIGHT),
                    AnvilCraftDatagen.has(ModBlocks.INDUCTION_LIGHT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.SILVER_PLATES),
                    AnvilCraftDatagen.has(ModItemTags.SILVER_PLATES))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.TINTED_GLASS), AnvilCraftDatagen.has(Items.TINTED_GLASS))
                .save(provider);
        })
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<RubyPrismBlock> RUBY_PRISM = REGISTRATE
        .block("ruby_prism", RubyPrismBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ACA")
                .pattern("CBC")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .define('B', ModBlocks.RUBY_BLOCK)
                .define('C', ModItems.RUBY)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                    AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY), AnvilCraftDatagen.has(ModItems.RUBY))
                .save(provider);
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<BlockComparatorBlock> BLOCK_COMPARATOR = REGISTRATE
        .block("block_comparator", BlockComparatorBlock::new)
        .initialProperties(() -> Blocks.OBSERVER)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("ABA")
                .pattern(" C ")
                .pattern(" D ")
                .define('A', Blocks.OBSERVER)
                .define('B', Blocks.REPEATER)
                .define('C', ModItems.CIRCUIT_BOARD)
                .define('D', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
                .save(provider);
        })
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .register();
    public static final BlockEntry<ItemDetectorBlock> ITEM_DETECTOR = REGISTRATE
        .block("item_detector", ItemDetectorBlock::new)
        .initialProperties(() -> Blocks.DAYLIGHT_DETECTOR)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("CC ")
                .pattern("CBR")
                .pattern("III")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('B', ModItems.CIRCUIT_BOARD)
                .define('R', Blocks.COMPARATOR)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD), AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD))
                .save(provider);
        })
        .blockstate((ctx, provider) -> {
            provider.horizontalBlock(
                ctx.get(),
                state -> DangerUtil.genModModelFile("block/item_detector" + (state.getValue(ItemDetectorBlock.POWERED) ? "_on" : "")).get(),
                0
            );
        })
        .simpleItem()
        .register();

    public static final BlockEntry<ImpactPileBlock> IMPACT_PILE = REGISTRATE
        .block("impact_pile", ImpactPileBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/impact_pile").get()))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern(" A ")
                .pattern(" B ")
                .pattern(" B ")
                .define('A', Blocks.OBSIDIAN)
                .define('B', Items.NETHERITE_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.OBSIDIAN), AnvilCraftDatagen.has(Blocks.OBSIDIAN))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.NETHERITE_INGOT),
                    AnvilCraftDatagen.has(Items.NETHERITE_INGOT))
                .save(provider);
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .register();
    public static final BlockEntry<OverseerBlock> OVERSEER_BLOCK = REGISTRATE
        .block("overseer", OverseerBlock::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .loot(SimpleMultiPartBlock::loot)
        .item(SimpleMultiPartBlockItem<Vertical3PartHalf>::new)
        .model((ctx, provider) -> {
        })
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("ABA")
                .pattern("CBC")
                .define('A', Items.OBSIDIAN)
                .define('B', Items.ENDER_EYE)
                .define('C', ModBlocks.ROYAL_STEEL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.ENDER_EYE), AnvilCraftDatagen.has(Items.ENDER_EYE))
                .save(provider);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<JewelCraftingTable> JEWEL_CRAFTING_TABLE = REGISTRATE
        .block("jewelcrafting_table", JewelCraftingTable::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .item()
        .tag(Tags.Items.VILLAGER_JOB_SITES)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            Tags.Blocks.VILLAGER_JOB_SITES)
        .lang("Jewel Crafting Table")
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABC")
                .pattern("DDD")
                .pattern("F F")
                .define('A', Blocks.GRINDSTONE)
                .define('B', Blocks.GLASS)
                .define('C', Blocks.GRINDSTONE)
                .define('D', Blocks.SMOOTH_STONE)
                .define('F', ItemTags.PLANKS)
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.GRINDSTONE), AnvilCraftDatagen.has(Blocks.GRINDSTONE))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Blocks.STONECUTTER), AnvilCraftDatagen.has(Blocks.STONECUTTER))
                .save(provider);
        })
        .register();
    public static final BlockEntry<TransparentCraftingTableBlock> TRANSPARENT_CRAFTING_TABLE = REGISTRATE
        .block("transparent_crafting_table", TransparentCraftingTableBlock::new)
        .properties(properties -> properties
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(1.5F, 3)
            .sound(SoundType.AMETHYST)
            .noOcclusion()
        )
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern(" A ")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.CRAFTING_TABLE)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.AMETHYST_SHARD))
                .save(provider);
        })
        .register();
    public static final BlockEntry<CrabTrapBlock> CRAB_TRAP = REGISTRATE
        .block("crab_trap", CrabTrapBlock::new)
        .properties(p -> p.sound(SoundType.SCAFFOLDING).strength(2))
        .blockstate((ctx, provider) -> {
        })
        .properties(BlockBehaviour.Properties::noOcclusion)
        .item(PlaceInWaterBlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_AXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("B B")
                .pattern("ABA")
                .define('A', Items.STICK)
                .define('B', Items.STRING)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.STRING))
                .save(provider);
        })
        .register();
    public static final BlockEntry<MengerSpongeBlock> MENGER_SPONGE = REGISTRATE
        .block("menger_sponge", MengerSpongeBlock::new)
        .initialProperties(() -> Blocks.SPONGE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_HOE)
        .simpleItem()
        .register();
    public static final BlockEntry<? extends Block> CHUTE = REGISTRATE
        .block("chute", ChuteBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .item(BlockItem::new)
        .onRegister(blockItem -> Item.BY_BLOCK.put(ModBlocks.SIMPLE_CHUTE.get(), blockItem))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CHUTE)
                .pattern("A A")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.DROPPER)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
                .save(provider);
        })
        .register();
    public static final BlockEntry<MagneticChuteBlock> MAGNETIC_CHUTE = REGISTRATE
        .block("magnetic_chute", MagneticChuteBlock::new)
        .initialProperties(ModBlocks.CHUTE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .item(BlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern(" A ")
                .pattern("ABA")
                .pattern("A A")
                .define('A', ModItems.MAGNET_INGOT)
                .define('B', Items.DROPPER)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.MAGNET_INGOT),
                    AnvilCraftDatagen.has(ModItems.MAGNET_INGOT))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
                .save(provider);
        })
        .register();
    public static final BlockEntry<SimpleChuteBlock> SIMPLE_CHUTE = REGISTRATE
        .block("simple_chute", SimpleChuteBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.CHUTE))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<MineralFountainBlock> MINERAL_FOUNTAIN = REGISTRATE
        .block("mineral_fountain", MineralFountainBlock::new)
        .initialProperties(() -> Blocks.REINFORCED_DEEPSLATE)
        .properties(BlockBehaviour.Properties::noLootTable)
        .simpleItem()
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/mineral_fountain").get()))
        .register();
    public static final BlockEntry<SpaceOvercompressorBlock> SPACE_OVERCOMPRESSOR = REGISTRATE
        .block("space_overcompressor", SpaceOvercompressorBlock::new)
        // .initialProperties(() -> Blocks.SHULKER_BOX)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        // .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .properties(properties -> properties.stacksTo(16))
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .register();

    public static BlockEntry<SlidingRailBlock> SLIDING_RAIL = REGISTRATE
        .block("sliding_rail", SlidingRailBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it
            .mapColor(MapColor.COLOR_GRAY)
            .friction(1.0204082f)
            .pushReaction(PushReaction.PUSH_ONLY)
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
            provider.getVariantBuilder(ctx.get()).forAllStates(blockState -> switch (blockState.getValue(
                SlidingRailBlock.AXIS)) {
                case X -> new ConfiguredModel[]{
                    ConfiguredModel.builder()
                        .modelFile(DangerUtil.genModModelFile("block/sliding_rail").get())
                        .rotationY(90)
                        .buildLast()};
                case Z -> DangerUtil.genConfiguredModel("block/sliding_rail")
                    .get();
                case Y -> DangerUtil.genConfiguredModel("block/sliding_rail_cross")
                    .get();
            });
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 16)
                .pattern("A A")
                .pattern("BAB")
                .pattern("BBB")
                .define('A', Blocks.BLUE_ICE)
                .define('B', Items.IRON_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.BLUE_ICE), AnvilCraftDatagen.has(Blocks.BLUE_ICE))
                .save(provider);
        })
        .register();

    public static BlockEntry<SlidingRailStopBlock> SLIDING_RAIL_STOP = REGISTRATE
        .block("sliding_rail_stop", SlidingRailStopBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it
            .mapColor(MapColor.COLOR_GRAY)
            .friction(0.8241758242f)
            .pushReaction(PushReaction.PUSH_ONLY)
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
            provider.simpleBlock(ctx.get(), DangerUtil.genModModelFile("block/sliding_rail_stop").get());
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                .pattern("A A")
                .pattern("BAB")
                .pattern("BBB")
                .define('A', Blocks.SOUL_SAND)
                .define('B', Items.IRON_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.BLUE_ICE), AnvilCraftDatagen.has(Blocks.BLUE_ICE))
                .save(provider);
        })
        .register();

    public static final BlockEntry<AccelerationRingBlock> ACCELERATION_RING = REGISTRATE
        .block("acceleration_ring", AccelerationRingBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .loot(FlexibleMultiPartBlock::loot)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .item(FlexibleMultiPartBlockItem<DirectionCube3x3PartHalf, DirectionProperty, Direction>::new)
        .build()
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<DeflectionRingBlock> DEFLECTION_RING = REGISTRATE
        .block("deflection_ring", DeflectionRingBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .loot(FlexibleMultiPartBlock::loot)
        .properties(it ->
            it.isSuffocating(ModBlocks::never).noOcclusion()
        )
        .item(FlexibleMultiPartBlockItem<DirectionCube3x3PartHalf, DirectionProperty, Direction>::new)
        .build()
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<VoidEnergyCollectorBlock> VOID_ENERGY_COLLECTOR = REGISTRATE
        .block("void_energy_collector", VoidEnergyCollectorBlock::new)
        .simpleItem()
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("ABA")
                .pattern("CCC")
                .define('A', ModBlocks.VOID_MATTER_BLOCK)
                .define('B', ModBlocks.CHARGE_COLLECTOR)
                .define('C', ModBlocks.HEAVY_IRON_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.VOID_MATTER_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.VOID_MATTER_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.CHARGE_COLLECTOR),
                    AnvilCraftDatagen.has(ModBlocks.CHARGE_COLLECTOR))
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider);
        })
        .register();

    public static final BlockEntry<MagnetoElectricCoreBlock> MAGNETO_ELECTRIC_CORE_BLOCK = REGISTRATE
        .block("magnetoelectric_core", MagnetoElectricCoreBlock::new)
        .initialProperties(() -> Blocks.COPPER_BLOCK)
        .properties((properties) -> properties.lightLevel((blockState) -> 6).noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Tags.Items.INGOTS_COPPER)
                .define('B', Tags.Items.GLASS_BLOCKS)
                .define('C', ModBlocks.HOLLOW_MAGNET_BLOCK)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HOLLOW_MAGNET_BLOCK))
                .save(provider);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
        .register();

    static {
        REGISTRATE.defaultCreativeTab(ModItemGroups.ANVILCRAFT_BUILD_BLOCK.getKey());
    }

    public static final BlockEntry<? extends Block> ROYAL_STEEL_BLOCK = REGISTRATE
        .block("royal_steel_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            ModBlockTags.OVERSEER_BASE,
            Tags.Blocks.STORAGE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> SMOOTH_ROYAL_STEEL_BLOCK = REGISTRATE
        .block("smooth_royal_steel_block", Block::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .simpleItem()
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/smooth_royal_steel_block"));
        })
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_BLOCK = REGISTRATE
        .block("cut_royal_steel_block", Block::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .simpleItem()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("AA")
                .pattern("AA")
                .define('A', ModBlocks.ROYAL_STEEL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_block"));
        })
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_PILLAR = REGISTRATE
        .block("cut_royal_steel_pillar", RotatedPillarBlock::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get()
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_pillar_from_cut_royal_steel_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_pillar_from_royal_steel_block"));
        })
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_SLAB = REGISTRATE
        .block("cut_royal_steel_slab", SlabBlock::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.SLABS,
            ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> provider.slabBlock(
            ctx.get(),
            AnvilCraft.of("block/cut_royal_steel_block"),
            AnvilCraft.of("block/cut_royal_steel_block")))
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                .pattern("AAA")
                .define('A', ModBlocks.CUT_ROYAL_STEEL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.CUT_ROYAL_STEEL_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_slab_from_royal_steel_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_slab_from_cut_royal_steel_block"));
        })
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_STAIRS = REGISTRATE
        .block(
            "cut_royal_steel_stairs",
            (properties) -> new StairBlock(ModBlocks.CUT_ROYAL_STEEL_BLOCK.getDefaultState(), properties))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.STAIRS,
            ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate(
            (ctx, provider) -> provider.stairsBlock(ctx.get(), AnvilCraft.of("block/cut_royal_steel_block")))
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', ModBlocks.CUT_ROYAL_STEEL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.CUT_ROYAL_STEEL_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_stairs_from_royal_steel_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_ROYAL_STEEL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    1
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_royal_steel_stairs_from_cut_royal_steel_block"));
        })
        .register();
    public static final BlockEntry<EmberMetalBlock> EMBER_METAL_BLOCK = REGISTRATE
        .block("ember_metal_block", properties -> new EmberMetalBlock(properties, 0.5d))
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .tag(BlockTags.BEACON_BASE_BLOCKS,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            Tags.Blocks.STORAGE_BLOCKS)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion())
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/ember_metal_block").get()))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.EMBER_METAL_INGOT)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.EMBER_METAL_INGOT),
                    RegistrateRecipeProvider.has(ModItems.EMBER_METAL_INGOT))
                .save(provider);
        })
        .defaultLoot()
        .register();

    public static final BlockEntry<EmberMetalBlock> CUT_EMBER_METAL_BLOCK = REGISTRATE
        .block("cut_ember_metal_block", properties -> new EmberMetalBlock(properties, 0.1d))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion())
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/cut_ember_metal_block").get()))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("AA")
                .pattern("AA")
                .define('A', ModBlocks.EMBER_METAL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.EMBER_METAL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_block"));
        })
        .defaultLoot()
        .register();

    public static final BlockEntry<? extends Block> CUT_EMBER_METAL_PILLAR = REGISTRATE
        .block("cut_ember_metal_pillar", EmberMetalPillarBlock::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_pillar_from_ember_metal_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_pillar_from_cut_ember_metal_block"));
        })
        .register();

    public static final BlockEntry<EmberMetalSlabBlock> CUT_EMBER_METAL_SLAB = REGISTRATE
        .block("cut_ember_metal_slab", EmberMetalSlabBlock::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.SLABS,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                .pattern("AAA")
                .define('A', ModBlocks.CUT_EMBER_METAL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.CUT_EMBER_METAL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_slab_from_ember_metal_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_slab_from_cut_ember_metal_block"));
        })
        .register();

    public static final BlockEntry<EmberMetalStairBlock> CUT_EMBER_METAL_STAIRS = REGISTRATE
        .block(
            "cut_ember_metal_stairs",
            (properties) ->
                new EmberMetalStairBlock(ModBlocks.CUT_EMBER_METAL_BLOCK.getDefaultState(), properties))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.STAIRS,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.STAIRS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', ModBlocks.CUT_EMBER_METAL_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.CUT_EMBER_METAL_BLOCK.asItem()),
                    AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_stairs_from_ember_metal_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_EMBER_METAL_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    1
                )
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_EMBER_METAL_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/cut_ember_metal_stairs_from_cut_ember_metal_block"));
        })
        .register();
    public static final BlockEntry<? extends Block> HEAVY_IRON_BLOCK = REGISTRATE
        .block("heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.noOcclusion().strength(5.0f, 1200f))
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/heavy_iron_block").get()))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', Tags.Items.STORAGE_BLOCKS_IRON)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(Tags.Items.STORAGE_BLOCKS_IRON))
                .save(provider);
        })
        .register();

    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_BLOCK = REGISTRATE
        .block("polished_heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_SLAB = REGISTRATE
        .block("polished_heavy_iron_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> provider.slabBlock(
            ctx.get(),
            AnvilCraft.of("block/polished_heavy_iron_block"),
            AnvilCraft.of("block/polished_heavy_iron_block")))
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.SLABS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_STAIRS = REGISTRATE
        .block(
            "polished_heavy_iron_stairs",
            (properties) -> new StairBlock(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.getDefaultState(), properties))
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) ->
            provider.stairsBlock(ctx.get(), AnvilCraft.of("block/polished_heavy_iron_block")))
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.STAIRS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_BLOCK = REGISTRATE
        .block("cut_heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_SLAB = REGISTRATE
        .block("cut_heavy_iron_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> provider.slabBlock(
            ctx.get(),
            AnvilCraft.of("block/cut_heavy_iron_block"),
            AnvilCraft.of("block/cut_heavy_iron_block")))
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.SLABS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    16)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_STAIRS = REGISTRATE
        .block(
            "cut_heavy_iron_stairs",
            (properties) -> new StairBlock(ModBlocks.CUT_HEAVY_IRON_BLOCK.getDefaultState(), properties))
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> provider.stairsBlock(ctx.get(), AnvilCraft.of("block/cut_heavy_iron_block")))
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.STAIRS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_PLATE = REGISTRATE
        .block("heavy_iron_plate", HeavyIronPlateBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    16)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_SLAB),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_SLAB))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_slab"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_SLAB),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_SLAB))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_slab"));
        })
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_COLUMN = REGISTRATE
        .block("heavy_iron_column", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_BEAM = REGISTRATE
        .block("heavy_iron_beam", HeavyIronBeamBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0f, 1200f))
        .blockstate((ctx, provider) -> {
        })
        .item()
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        })
        .register();

    public static final BlockEntry<HeavyIronWallBlock> HEAVY_IRON_WALL = REGISTRATE
        .block("heavy_iron_wall", HeavyIronWallBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.strength(5.0f, 1200f).noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.WALLS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        })
        .item()
        .tag(ItemTags.WALLS)
        .model((ctx, provide) -> provide.wallInventory(
            "heavy_iron_wall",
            AnvilCraft.of("block/heavy_iron_wall")
        ))
        .build()
        .register();

    public static final BlockEntry<HeavyIronDoorBlock> HEAVY_IRON_DOOR = REGISTRATE
        .block("heavy_iron_door", HeavyIronDoorBlock::new)
        .initialProperties(() -> Blocks.IRON_DOOR)
        .properties(properties -> properties.strength(5.0f, 1200f))
        .loot((l, b) -> {
            l.add(b, l.createDoorTable(b));
        })
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.DOORS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
        })
        .item()
        .tag(ItemTags.DOORS)
        .model((ctx, prov) -> {
            prov.generated(ctx);
        })
        .build()
        .register();

    public static final BlockEntry<HeavyIronTrapdoorBlock> HEAVY_IRON_TRAPDOOR = REGISTRATE
        .block("heavy_iron_trapdoor", HeavyIronTrapdoorBlock::new)
        .initialProperties(() -> Blocks.IRON_TRAPDOOR)
        .properties(properties -> properties.strength(5.0f, 1200f))
        .defaultLoot()
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.TRAPDOORS)
        .recipe((ctx, provider) -> {
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    8)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.POLISHED_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    4)
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.POLISHED_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_polished_heavy_iron_block"));
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CUT_HEAVY_IRON_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy("hasitem", AnvilCraftDatagen.has(ModBlocks.CUT_HEAVY_IRON_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName() + "_from_cut_heavy_iron_block"));
        })
        .item()
        .tag(ItemTags.TRAPDOORS)
        .model((c, p) -> {
            p.blockItem(c, "_bottom");
        })
        .build()
        .register();
    public static final BlockEntry<? extends Block> CURSED_GOLD_BLOCK = REGISTRATE
        .block("cursed_gold_block", Block::new)
        .initialProperties(() -> Blocks.GOLD_BLOCK)
        .item(CursedBlockItem::new)
        .tag(ItemTags.PIGLIN_LOVED,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.CURSED_GOLD_INGOT)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT),
                    AnvilCraftDatagen.has(ModItems.CURSED_GOLD_INGOT))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> ZINC_BLOCK = REGISTRATE
        .block("zinc_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_ZINC)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_ZINC,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.ZINC_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.ZINC_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> TIN_BLOCK = REGISTRATE
        .block("tin_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TIN)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_TIN,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TIN_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TIN_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> TITANIUM_BLOCK = REGISTRATE
        .block("titanium_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TITANIUM)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_TITANIUM,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TITANIUM_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TITANIUM_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> TUNGSTEN_BLOCK = REGISTRATE
        .block("tungsten_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.STORAGE_BLOCKS_TUNGSTEN,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TUNGSTEN)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TUNGSTEN_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TUNGSTEN_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> LEAD_BLOCK = REGISTRATE
        .block("lead_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_LEAD)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_LEAD,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.LEAD_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.LEAD_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> SILVER_BLOCK = REGISTRATE
        .block("silver_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_SILVER)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_SILVER,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.SILVER_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.SILVER_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> URANIUM_BLOCK = REGISTRATE
        .block("uranium_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_URANIUM)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_URANIUM,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.URANIUM_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.URANIUM_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> BRONZE_BLOCK = REGISTRATE
        .block("bronze_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_BRONZE)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_BRONZE,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRONZE_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.BRONZE_INGOTS))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> BRASS_BLOCK = REGISTRATE
        .block("brass_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_BRASS)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_BRASS,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRASS_INGOTS)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.BRASS_INGOTS))
                .save(provider);
        })
        .register();

    public static final BlockEntry<? extends Block> TOPAZ_BLOCK = REGISTRATE
        .block("topaz_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS,
            ModItemTags.STORAGE_BLOCKS_TOPAZ)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TOPAZ)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.TOPAZ)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TOPAZ), AnvilCraftDatagen.has(ModItems.TOPAZ))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> RUBY_BLOCK = REGISTRATE
        .block("ruby_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS,
            ModItemTags.STORAGE_BLOCKS_RUBY)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_RUBY)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.RUBY)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY), AnvilCraftDatagen.has(ModItems.RUBY))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> SAPPHIRE_BLOCK = REGISTRATE
        .block("sapphire_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS,
            ModItemTags.STORAGE_BLOCKS_SAPPHIRE)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_SAPPHIRE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.SAPPHIRE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SAPPHIRE), AnvilCraftDatagen.has(ModItems.SAPPHIRE))
                .save(provider);
        })
        .register();
    public static final BlockEntry<? extends Block> RESIN_BLOCK = REGISTRATE
        .block("resin_block", ResinBlock::new)
        .initialProperties(() -> Blocks.SLIME_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .properties(properties -> properties.sound(SoundType.HONEY_BLOCK))
        .item(ResinBlockItem::new)
        .tag(Tags.Items.STORAGE_BLOCKS,
            ModItemTags.STORAGE_BLOCKS_RESIN)
        .build()
        .tag(Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_RESIN)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.RESIN)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RESIN), AnvilCraftDatagen.has(ModItems.RESIN))
                .save(provider);
        })
        .register();

    public static final BlockEntry<? extends Block> AMBER_BLOCK = REGISTRATE
        .block("amber_block", AmberBlock::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .properties(properties -> properties
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
        )
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS,
            ModItemTags.STORAGE_BLOCKS_AMBER)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_AMBER)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.AMBER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.AMBER), AnvilCraftDatagen.has(ModItems.AMBER))
                .save(provider);
        })
        .register();

    public static final BlockEntry<MobAmberBlock> MOB_AMBER_BLOCK = REGISTRATE
        .block("mob_amber_block", MobAmberBlock::new)
        .blockstate((ctx, provider) -> {
        })
        .item(HasMobBlockItem::new)
        .recipe((ctx, provider) -> {
        })
        .build()
        .initialProperties(ModBlocks.AMBER_BLOCK)
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(ModBlocks.MOB_AMBER_BLOCK))
                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(ModComponents.SAVED_ENTITY)));
            ctx.add(prov, builder);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ResentfulAmberBlock> RESENTFUL_AMBER_BLOCK = REGISTRATE
        .block("resentful_amber_block", ResentfulAmberBlock::new)
        .blockstate((ctx, provider) -> {
        })
        .item(HasMobBlockItem::new)
        .build()
        .initialProperties(ModBlocks.AMBER_BLOCK)
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(ModBlocks.RESENTFUL_AMBER_BLOCK))
                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(ModComponents.SAVED_ENTITY)));
            ctx.add(prov, builder);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> TEMPERING_GLASS = REGISTRATE
        .block("tempering_glass", TransparentBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(properties -> properties
            .explosionResistance(1200.0f)
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never))
        .blockstate((ctx, provider) -> {
            provider.simpleBlock(ctx.get());
            provider.models()
                .cubeAll(ctx.getName(), provider.modLoc("block/" + ctx.getName()))
                .renderType("translucent");
        })
        .item()
        .tag(Tags.Items.GLASS_BLOCKS)
        .build()
        .tag(Tags.Blocks.GLASS_BLOCKS)
        .register();
    public static final BlockEntry<? extends Block> EMBER_GLASS = REGISTRATE
        .block("ember_glass", TransparentBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(properties -> properties
            .explosionResistance(1200)
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never))
        .blockstate((ctx, provider) -> {
            provider.simpleBlock(ctx.get());
            provider.models()
                .cubeAll(ctx.getName(), provider.modLoc("block/" + ctx.getName()))
                .renderType("translucent");
        })
        .tag(BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            Tags.Blocks.GLASS_BLOCKS)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.GLASS_BLOCKS)
        .build()
        .register();
    public static final BlockEntry<? extends Block> CINERITE = REGISTRATE
        .block("cinerite", (b) -> new ColoredFallingBlock(new ColorRGBA(0xDEDEDE), b))
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<? extends Block> QUARTZ_SAND = REGISTRATE
        .block("quartz_sand", (b) -> new ColoredFallingBlock(new ColorRGBA(0xFFFFCD), b))
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<LevitationPowderBlock> LEVITATION_POWDER_BLOCK = REGISTRATE
        .block("levitation_powder_block", LevitationPowderBlock::new)
        .initialProperties(() -> Blocks.SAND)
        .item(LevitationPowderBlockItem::new)
        .recipe((ctx, provider) ->
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ctx.get())
                .requires(ModItems.LEVITATION_POWDER, 9)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.LEVITATION_POWDER), AnvilCraftDatagen.has(ModItems.LEVITATION_POWDER))
                .save(provider, ctx.getId().withSuffix("_from_powders"))
        )
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<ColoredFallingBlock> NETHER_DUST = REGISTRATE
        .block("nether_dust", (b) -> new ColoredFallingBlock(new ColorRGBA(0x8B0000), b))
        .simpleItem()
        .initialProperties(() -> Blocks.BLACK_CONCRETE_POWDER)
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<EndDustBlock> END_DUST = REGISTRATE
        .block("end_dust", EndDustBlock::new)
        .item(EndDustBlockItem::new)
        .build()
        .initialProperties(() -> Blocks.BLACK_CONCRETE_POWDER)
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<ArrowBlock> ARROW = REGISTRATE
        .block("arrow", ArrowBlock::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(p -> p.noOcclusion().noCollission().lightLevel(bs -> 10))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .register();

    public static final BlockEntry<CakeBaseBlock> CAKE_BASE_BLOCK = REGISTRATE
        .block("cake_base_block", CakeBaseBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<CreamBlock> CREAM_BLOCK = REGISTRATE
        .block("cream_block", CreamBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<BerryCreamBlock> BERRY_CREAM_BLOCK = REGISTRATE
        .block("berry_cream_block", BerryCreamBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<ChocolateCreamBlock> CHOCOLATE_CREAM_BLOCK = REGISTRATE
        .block("chocolate_cream_block", ChocolateCreamBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<CakeBlock> CAKE_BLOCK = REGISTRATE
        .block("cake_block", CakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/cake_block").get()))
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<BerryCakeBlock> BERRY_CAKE_BLOCK = REGISTRATE
        .block("berry_cake_block", BerryCakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/berry_cake_block").get()))
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<ChocolateCakeBlock> CHOCOLATE_CAKE_BLOCK = REGISTRATE
        .block("chocolate_cake_block", ChocolateCakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/chocolate_cake_block").get()))
        .item()
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<LargeCakeBlock> LARGE_CAKE = REGISTRATE
        .block("large_cake", LargeCakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((context, provider) -> {
        })
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable()
                .setRandomSequence(ResourceLocation.withDefaultNamespace("blocks/large_cake"));
            ctx.add(prov, builder);
        })
        .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
        .tag(Tags.Items.FOODS,
            Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .register();

    public static final BlockEntry<StepEffectBlock> CHOCOLATE_BLOCK = REGISTRATE
        .block("chocolate_block", p -> new StepEffectBlock(p, StepEffectBlock::stepOnChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CHOCOLATE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CHOCOLATE), AnvilCraftDatagen.has(ModItems.CHOCOLATE))
            .save(provider))
        .register();

    public static final BlockEntry<StepEffectBlock> BLACK_CHOCOLATE_BLOCK = REGISTRATE
        .block("black_chocolate_block", p -> new StepEffectBlock(p, StepEffectBlock::stepOnBlackChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CHOCOLATE_BLACK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CHOCOLATE_BLACK), AnvilCraftDatagen.has(ModItems.CHOCOLATE_BLACK))
            .save(provider))
        .register();

    public static final BlockEntry<StepEffectBlock> WHITE_CHOCOLATE_BLOCK = REGISTRATE
        .block("white_chocolate_block", p -> new StepEffectBlock(p, StepEffectBlock::stepOnWhiteChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CHOCOLATE_WHITE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CHOCOLATE_WHITE), AnvilCraftDatagen.has(ModItems.CHOCOLATE_WHITE))
            .save(provider))
        .register();

    public static final BlockEntry<StepEffectSlabBlock> CHOCOLATE_SLAB = REGISTRATE
        .block("chocolate_slab", p -> new StepEffectSlabBlock(p, StepEffectBlock::stepOnChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.SLABS)
        .blockstate((ctx, provider) -> {
            provider.slabBlock(ctx.get(), AnvilCraft.of("block/chocolate_block"),
                AnvilCraft.of("block/chocolate_block"));
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                .pattern("AAA")
                .define('A', ModBlocks.CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CHOCOLATE_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final BlockEntry<StepEffectSlabBlock> BLACK_CHOCOLATE_SLAB = REGISTRATE
        .block("black_chocolate_slab", p -> new StepEffectSlabBlock(p, StepEffectBlock::stepOnBlackChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.SLABS)
        .blockstate((ctx, provider) -> {
            provider.slabBlock(ctx.get(), AnvilCraft.of("block/black_chocolate_block"),
                AnvilCraft.of("block/black_chocolate_block"));
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                .pattern("AAA")
                .define('A', ModBlocks.BLACK_CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final BlockEntry<StepEffectSlabBlock> WHITE_CHOCOLATE_SLAB = REGISTRATE
        .block("white_chocolate_slab", p -> new StepEffectSlabBlock(p, StepEffectBlock::stepOnWhiteChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.SLABS)
        .blockstate((ctx, provider) -> {
            provider.slabBlock(ctx.get(), AnvilCraft.of("block/white_chocolate_block"),
                AnvilCraft.of("block/white_chocolate_block"));
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                .pattern("AAA")
                .define('A', ModBlocks.WHITE_CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(),
                    2)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final BlockEntry<StepEffectStairBlock> CHOCOLATE_STAIRS = REGISTRATE
        .block("chocolate_stairs", p -> new StepEffectStairBlock(
            ModBlocks.CHOCOLATE_BLOCK.getDefaultState(), p, StepEffectBlock::stepOnChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.STAIRS)
        .blockstate((ctx, provider) -> {
            provider.stairsBlock(ctx.get(), AnvilCraft.of("block/chocolate_block"));
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', ModBlocks.CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.CHOCOLATE_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final BlockEntry<StepEffectStairBlock> BLACK_CHOCOLATE_STAIRS = REGISTRATE
        .block("black_chocolate_stairs", p -> new StepEffectStairBlock(
            ModBlocks.BLACK_CHOCOLATE_BLOCK.getDefaultState(), p, StepEffectBlock::stepOnBlackChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.STAIRS)
        .blockstate((ctx, provider) -> {
            provider.stairsBlock(ctx.get(), AnvilCraft.of("block/black_chocolate_block"));
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', ModBlocks.BLACK_CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final BlockEntry<StepEffectStairBlock> WHITE_CHOCOLATE_STAIRS = REGISTRATE
        .block("white_chocolate_stairs", p -> new StepEffectStairBlock(
            ModBlocks.WHITE_CHOCOLATE_BLOCK.getDefaultState(), p, StepEffectBlock::stepOnWhiteChocolateBlock))
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.STAIRS)
        .blockstate((ctx, provider) -> {
            provider.stairsBlock(ctx.get(), AnvilCraft.of("block/white_chocolate_block"));
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', ModBlocks.WHITE_CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
                .save(provider);
            SingleItemRecipeBuilder.stonecutting(
                    Ingredient.of(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    RecipeCategory.BUILDING_BLOCKS,
                    ctx.get())
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
        })
        .register();

    public static final Object2ObjectMap<Color, BlockEntry<ReinforcedConcreteBlock>> REINFORCED_CONCRETES =
        registerReinforcedConcretes();
    public static final Object2ObjectMap<Color, BlockEntry<SlabBlock>> REINFORCED_CONCRETE_SLABS =
        registerReinforcedConcreteSlabs();
    public static final Object2ObjectMap<Color, BlockEntry<StairBlock>> REINFORCED_CONCRETE_STAIRS =
        registerReinforcedConcreteStairs();
    public static final Object2ObjectMap<Color, BlockEntry<WallBlock>> REINFORCED_CONCRETE_WALLS =
        registerReinforcedConcreteWalls();

    public static final BlockEntry<Block> HEATED_NETHERITE = REGISTRATE
        .block("heated_netherite_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, Items.NETHERITE_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<Block> HEATED_TUNGSTEN = REGISTRATE
        .block("heated_tungsten_block", Block::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.TUNGSTEN_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<RedhotMetalBlock> REDHOT_NETHERITE = REGISTRATE
        .block("redhot_netherite_block", RedhotMetalBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 3))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, Items.NETHERITE_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<RedhotMetalBlock> REDHOT_TUNGSTEN = REGISTRATE
        .block("redhot_tungsten_block", RedhotMetalBlock::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 3))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.TUNGSTEN_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<GlowingMetalBlock> GLOWING_NETHERITE = REGISTRATE
        .block("glowing_netherite_block", GlowingMetalBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 7))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.HEATED_NETHERITE))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<GlowingMetalBlock> GLOWING_TUNGSTEN = REGISTRATE
        .block("glowing_tungsten_block", GlowingMetalBlock::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 7))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.HEATED_TUNGSTEN))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<IncandescentMetalBlock> INCANDESCENT_NETHERITE = REGISTRATE
        .block("incandescent_netherite_block", IncandescentMetalBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 15))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.REDHOT_NETHERITE))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<IncandescentMetalBlock> INCANDESCENT_TUNGSTEN = REGISTRATE
        .block("incandescent_tungsten_block", IncandescentMetalBlock::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 15))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.REDHOT_TUNGSTEN))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    // raw blocks
    public static final BlockEntry<Block> RAW_ZINC = REGISTRATE
        .block("raw_zinc_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_ZINC)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_ZINC), AnvilCraftDatagen.has(ModItems.RAW_ZINC))
            .save(provider)
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_RAW_ZINC)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_ZINC,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();
    public static final BlockEntry<Block> RAW_TIN = REGISTRATE
        .block("raw_tin_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TIN), AnvilCraftDatagen.has(ModItems.RAW_TIN))
            .save(provider))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_TIN)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_TIN,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();
    public static final BlockEntry<Block> RAW_TITANIUM = REGISTRATE
        .block("raw_titanium_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TITANIUM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_TITANIUM),
                AnvilCraftDatagen.has(ModItems.RAW_TITANIUM))
            .save(provider))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_TITANIUM)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_TITANIUM,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();
    public static final BlockEntry<Block> RAW_TUNGSTEN = REGISTRATE
        .block("raw_tungsten_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TUNGSTEN)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_TUNGSTEN),
                AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN))
            .save(provider))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_TUNGSTEN,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_TUNGSTEN)
        .register();
    public static final BlockEntry<Block> RAW_LEAD = REGISTRATE
        .block("raw_lead_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_LEAD)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_LEAD), AnvilCraftDatagen.has(ModItems.RAW_LEAD))
            .save(provider))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_LEAD)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_LEAD,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();
    public static final BlockEntry<Block> RAW_SILVER = REGISTRATE
        .block("raw_silver_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_SILVER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_SILVER), AnvilCraftDatagen.has(ModItems.RAW_SILVER))
            .save(provider))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_SILVER)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_SILVER,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();
    public static final BlockEntry<Block> RAW_URANIUM = REGISTRATE
        .block("raw_uranium_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(
                RecipeCategory.BUILDING_BLOCKS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_URANIUM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_URANIUM),
                AnvilCraftDatagen.has(ModItems.RAW_URANIUM))
            .save(provider))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_URANIUM)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_URANIUM,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();
    // ores
    public static final BlockEntry<Block> DEEPSLATE_ZINC_ORE = REGISTRATE
        .block("deepslate_zinc_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.ZINC_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_ZINC.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_ZINC,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_TIN_ORE = REGISTRATE
        .block("deepslate_tin_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.TIN_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TIN.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_TIN,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_TITANIUM_ORE = REGISTRATE
        .block("deepslate_titanium_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.TITANIUM_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TITANIUM.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_TITANIUM,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_TUNGSTEN_ORE = REGISTRATE
        .block("deepslate_tungsten_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.TUNGSTEN_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TUNGSTEN.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_TUNGSTEN,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_LEAD_ORE = REGISTRATE
        .block("deepslate_lead_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.LEAD_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_LEAD.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_LEAD,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_SILVER_ORE = REGISTRATE
        .block("deepslate_silver_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.SILVER_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_SILVER.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_SILVER,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_URANIUM_ORE = REGISTRATE
        .block("deepslate_uranium_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.URANIUM_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_URANIUM.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_URANIUM,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> VOID_STONE = REGISTRATE
        .block("void_stone", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .item()
        .tag(Tags.Items.ORES,
            ModItemTags.VOID_RESISTANT,
            ModItemTags.VOID_MATTER_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.VOID_MATTER.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.ORES_VOID_MATTER,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> EARTH_CORE_SHARD_ORE = REGISTRATE
        .block("earth_core_shard_ore", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(properties -> properties.explosionResistance(1200))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.ORES,
            ModItemTags.EARTH_CORE_SHARD_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.EARTH_CORE_SHARD.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.ORES_EARTH_CORE_SHARD,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> STURDY_DEEPSLATE = REGISTRATE
        .block("sturdy_deepslate", Block::new)
        .initialProperties(() -> Blocks.REINFORCED_DEEPSLATE)
        .properties(properties -> properties.noLootTable().pushReaction(PushReaction.BLOCK))
        .simpleItem()
        .loot((tables, block) -> {
        })
        .tag(BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE)
        .register();

    public static final BlockEntry<VoidMatterBlock> VOID_MATTER_BLOCK = REGISTRATE
        .block("void_matter_block", VoidMatterBlock::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/void_matter_block").get()))
        .recipe((ctx, provider) ->
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.VOID_MATTER)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER),
                    AnvilCraftDatagen.has(ModItems.VOID_MATTER))
                .save(provider))
        .item()
        .tag(ModItemTags.VOID_RESISTANT,
            ModItemTags.STORAGE_BLOCKS_VOID_MATTER,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.STORAGE_BLOCKS_VOID_MATTER,
            Tags.Blocks.STORAGE_BLOCKS)
        .register();

    public static final BlockEntry<Block> EARTH_CORE_SHARD_BLOCK = REGISTRATE
        .block("earth_core_shard_block", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(properties -> properties.explosionResistance(1200))
        .recipe((ctx, provider) ->
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.EARTH_CORE_SHARD)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD),
                    AnvilCraftDatagen.has(ModItems.EARTH_CORE_SHARD))
                .save(provider))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.STORAGE_BLOCKS_EARTH_CORE_SHARD,
            Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.STORAGE_BLOCKS_EARTH_CORE_SHARD,
            Tags.Blocks.STORAGE_BLOCKS)
        .register();

    public static final BlockEntry<NegativeMatterBlock> NEGATIVE_MATTER_BLOCK = REGISTRATE
        .block("negative_matter_block", NegativeMatterBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            Tags.Blocks.STORAGE_BLOCKS)
        .properties(properties -> properties.lightLevel(state -> 7)
            .noOcclusion()
            .emissiveRendering(ModBlocks::always))
        .blockstate((context, provider) -> provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/negative_matter_block").get()))
        .item()
        .initialProperties(Item.Properties::new)
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.NEGATIVE_MATTER)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.NEGATIVE_MATTER),
                    RegistrateRecipeProvider.has(ModItems.NEGATIVE_MATTER))
                .save(provider);
        })
        .defaultLoot()
        .register();

    public static final BlockEntry<LavaCauldronBlock> LAVA_CAULDRON = REGISTRATE
        .block("lava_cauldron", LavaCauldronBlock::new)
        .initialProperties(() -> Blocks.LAVA_CAULDRON)
        .properties(properties ->
            properties.lightLevel(blockState -> blockState.getValue(LavaCauldronBlock.LEVEL) * 4))
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<MeltGemCauldron> MELT_GEM_CAULDRON = REGISTRATE
        .block("melt_gem_cauldron", MeltGemCauldron::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .properties(p -> p.lightLevel(s -> 15))
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<HoneyCauldronBlock> HONEY_CAULDRON = REGISTRATE
        .block("honey_cauldron", HoneyCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<ObsidianCauldron> OBSIDIAN_CAULDRON = REGISTRATE
        .block("obsidian_cauldron", ObsidianCauldron::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(it -> it.pushReaction(PushReaction.BLOCK))
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<OilCauldronBlock> OIL_CAULDRON = REGISTRATE
        .block("oil_cauldron", OilCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<FireCauldronBlock> FIRE_CAULDRON = REGISTRATE
        .block("fire_cauldron", FireCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .properties(properties -> properties.lightLevel(state -> 15))
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();


    public static final Object2ObjectMap<Color, BlockEntry<CementCauldronBlock>> CEMENT_CAULDRONS = registerAllCementCauldrons();

    private static Object2ObjectMap<Color, BlockEntry<ReinforcedConcreteBlock>> registerReinforcedConcretes() {
        Object2ObjectMap<Color, BlockEntry<ReinforcedConcreteBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteBlock(color);
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<ReinforcedConcreteBlock> registerReinforcedConcreteBlock(Color color) {
        return REGISTRATE
            .block("reinforced_concrete_" + color, ReinforcedConcreteBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0f).explosionResistance(15.0f))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE,
                Tags.Items.DYED,
                ModItemTags.DYED_COLORS.get(color))
            .build()
            .blockstate((ctx, provider) -> {
                provider.models()
                    .getBuilder("reinforced_concrete_" + color)
                    .parent(DangerUtil.genUncheckedModelFile("minecraft", "block/cube_all")
                        .get())
                    .texture("all", "block/reinforced_concrete_" + color);
                provider.models()
                    .getBuilder("reinforced_concrete_top_" + color)
                    .parent(DangerUtil.genUncheckedModelFile("minecraft", "block/cube_column")
                        .get())
                    .texture("end", "block/reinforced_concrete_" + color)
                    .texture("side", "block/reinforced_concrete_" + color + "_top");
                provider.models()
                    .getBuilder("reinforced_concrete_bottom_" + color)
                    .parent(DangerUtil.genUncheckedModelFile("minecraft", "block/cube_column")
                        .get())
                    .texture("end", "block/reinforced_concrete_" + color)
                    .texture("side", "block/reinforced_concrete_" + color + "_bottom");
                provider.getVariantBuilder(ctx.get()).forAllStates(blockState -> switch (blockState.getValue(
                    ReinforcedConcreteBlock.HALF)) {
                    case TOP -> DangerUtil.genConfiguredModel("block/reinforced_concrete_top_" + color)
                        .get();
                    case SINGLE -> DangerUtil.genConfiguredModel("block/reinforced_concrete_" + color)
                        .get();
                    case BOTTOM -> DangerUtil.genConfiguredModel("block/reinforced_concrete_bottom_" + color)
                        .get();
                });
            })
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                Tags.Blocks.DYED,
                ModBlockTags.DYED_COLORS.get(color))
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<SlabBlock>> registerReinforcedConcreteSlabs() {
        Object2ObjectMap<Color, BlockEntry<SlabBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteSlabBlock(color, REINFORCED_CONCRETES.get(color));
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<SlabBlock> registerReinforcedConcreteSlabBlock(
        Color color, BlockEntry<ReinforcedConcreteBlock> parent) {
        return REGISTRATE
            .block("reinforced_concrete_" + color + "_slab", SlabBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0f).explosionResistance(15.0f))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE,
                ItemTags.SLABS,
                Tags.Items.DYED,
                ModItemTags.DYED_COLORS.get(color))
            .build()
            .blockstate((ctx, provider) -> provider.slabBlock(
                ctx.get(),
                AnvilCraft.of("block/reinforced_concrete_" + color),
                AnvilCraft.of("block/reinforced_concrete_" + color)))
            .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                BlockTags.SLABS,
                Tags.Blocks.DYED,
                ModBlockTags.DYED_COLORS.get(color))
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 6)
                    .pattern("AAA")
                    .define('A', parent)
                    .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()), AnvilCraftDatagen.has(parent))
                    .save(provider);
                SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(parent),
                        RecipeCategory.BUILDING_BLOCKS,
                        ctx.get(),
                        2)
                    .unlockedBy("hasitem", AnvilCraftDatagen.has(parent))
                    .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            })
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<StairBlock>> registerReinforcedConcreteStairs() {
        Object2ObjectMap<Color, BlockEntry<StairBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteStairBlock(color, REINFORCED_CONCRETES.get(color));
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<StairBlock> registerReinforcedConcreteStairBlock(
        Color color, BlockEntry<ReinforcedConcreteBlock> parent) {
        return REGISTRATE
            .block(
                "reinforced_concrete_" + color + "_stair",
                (properties) -> new StairBlock(parent.getDefaultState(), properties))
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0f).explosionResistance(15.0f))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE,
                ItemTags.STAIRS,
                Tags.Items.DYED,
                ModItemTags.DYED_COLORS.get(color))
            .build()
            .blockstate((ctx, provider) ->
                provider.stairsBlock(ctx.get(), AnvilCraft.of("block/reinforced_concrete_" + color)))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                BlockTags.STAIRS,
                Tags.Blocks.DYED,
                ModBlockTags.DYED_COLORS.get(color))
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                    .pattern("A  ")
                    .pattern("AA ")
                    .pattern("AAA")
                    .define('A', parent)
                    .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()), AnvilCraftDatagen.has(parent))
                    .save(provider);
                SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(parent),
                        RecipeCategory.BUILDING_BLOCKS,
                        ctx.get())
                    .unlockedBy("hasitem", AnvilCraftDatagen.has(parent))
                    .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            })
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<WallBlock>> registerReinforcedConcreteWalls() {
        Object2ObjectMap<Color, BlockEntry<WallBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteWallBlock(color, REINFORCED_CONCRETES.get(color));
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<WallBlock> registerReinforcedConcreteWallBlock(
        Color color, BlockEntry<ReinforcedConcreteBlock> parent) {
        return REGISTRATE
            .block("reinforced_concrete_" + color + "_wall", WallBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0f).explosionResistance(15.0f))
            .blockstate((ctx, provider) ->
                provider.wallBlock(ctx.get(), AnvilCraft.of("block/reinforced_concrete_" + color + "_wall")))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                BlockTags.WALLS,
                Tags.Blocks.DYED,
                ModBlockTags.DYED_COLORS.get(color))
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
                    .pattern("AAA")
                    .pattern("AAA")
                    .define('A', parent)
                    .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()), AnvilCraftDatagen.has(parent))
                    .save(provider);
                SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(parent),
                        RecipeCategory.BUILDING_BLOCKS,
                        ctx.get())
                    .unlockedBy("hasitem", AnvilCraftDatagen.has(parent))
                    .save(provider, AnvilCraft.of("stonecutting/" + ctx.getName()));
            })
            .item()
            .model((ctx, provide) -> provide.wallInventory(
                "reinforced_concrete_" + color + "_wall",
                AnvilCraft.of("block/reinforced_concrete_" + color + "_wall")))
            .tag(ModItemTags.REINFORCED_CONCRETE,
                ItemTags.WALLS,
                Tags.Items.DYED,
                ModItemTags.DYED_COLORS.get(color))
            .build()
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<CementCauldronBlock>> registerAllCementCauldrons() {
        Object2ObjectMap<Color, BlockEntry<CementCauldronBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerCementCauldron(color);
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<CementCauldronBlock> registerCementCauldron(Color color) {
        return REGISTRATE
            .block("%s_cement_cauldron".formatted(color), p -> new CementCauldronBlock(p, color))
            .initialProperties(() -> Blocks.CAULDRON)
            .blockstate((ctx, provider) -> {
                provider.simpleBlock(
                    ctx.get(),
                    provider.models()
                        .withExistingParent(ctx.getName(), provider.mcLoc("block/template_cauldron_full"))
                        .texture("bottom", provider.mcLoc("block/cauldron_bottom"))
                        .texture("inside", provider.mcLoc("block/cauldron_inner"))
                        .texture("side", provider.mcLoc("block/cauldron_side"))
                        .texture("top", provider.mcLoc("block/cauldron_top"))
                        .texture("particle", provider.mcLoc("block/cauldron_side"))
                        .texture("content", provider.modLoc("block/%s_cement".formatted(color)))
                );
            })
            .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                BlockTags.CAULDRONS)
            .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
            .register();
    }

    private static BlockEntry<? extends TimeCountedPressurePlateBlock> registerOtherCopperPressurePlate(String prefix, Block block, int tickCount) {
        ResourceLocation location = BuiltInRegistries.BLOCK.getKey(block);
        String id = prefix + "copper" + "_pressure_plate";
        return REGISTRATE
            .block(id, properties -> new TimeCountedPressurePlateBlock(BlockSetType.IRON, properties, tickCount))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                ModBlockTags.HAMMER_REMOVABLE,
                BlockTags.PRESSURE_PLATES)
            .initialProperties(() -> block)
            .properties(properties -> properties
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollission()
                .strength(0.5f)
                .pushReaction(PushReaction.DESTROY))
            .blockstate((ctx, provider) -> provider.pressurePlateBlock(
                ctx.get(),
                ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "block/" + location.getPath())
            ))
            .item()
            .tag(ModItemTags.PLATES,
                ModItemTags.COPPER_PLATES)
            .build()
            .register();
    }

    @SuppressWarnings("SameParameterValue")
    private static BlockEntry<? extends PowerLevelPressurePlateBlock> registerPressurePlate(
        String type, Supplier<? extends Block> block,
        NonNullFunction<BlockBehaviour.Properties, ? extends PowerLevelPressurePlateBlock> plateBlockFactory,
        Item... ingredients
    ) {
        ResourceLocation location;
        if (block instanceof BlockEntry<? extends Block> entry) location = entry.getId();
        else location = BuiltInRegistries.BLOCK.getKey(block.get());
        String id = type + "_pressure_plate";
        return REGISTRATE
            .block(id, plateBlockFactory)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                ModBlockTags.HAMMER_REMOVABLE,
                BlockTags.PRESSURE_PLATES)
            .initialProperties(block::get)
            .properties(properties -> properties
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollission()
                .strength(0.5f)
                .pushReaction(PushReaction.DESTROY))
            .blockstate((ctx, provider) -> DataGenUtil.powerLevelPressurePlate(
                provider, ctx.getId(), ctx.get(),
                ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "block/" + location.getPath())
            ))
            .item()
            .tag(ModItemTags.PLATES, ModItemTags.bindC("plates/" + type))
            .build()
            .recipe((ctx, provider) -> {
                for (Item ingredient : ingredients) {
                    ResourceLocation location1 = BuiltInRegistries.ITEM.getKey(ingredient);
                    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 1)
                        .pattern("AA")
                        .define('A', ingredient)
                        .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(ingredient))
                        .save(
                            provider,
                            AnvilCraft.of(id + "_from_"
                                + location1.getPath().replace('/', '_')));
                }
            })
            .register();
    }

    @SafeVarargs
    private static BlockEntry<? extends PowerLevelPressurePlateBlock> registerPressurePlate(
        String type, Supplier<? extends Block> block,
        NonNullFunction<BlockBehaviour.Properties, ? extends PowerLevelPressurePlateBlock> plateBlockFactory,
        TagKey<Item>... ingredients
    ) {
        ResourceLocation location;
        if (block instanceof BlockEntry<? extends Block> entry) location = entry.getId();
        else location = BuiltInRegistries.BLOCK.getKey(block.get());
        String id = type + "_pressure_plate";
        return REGISTRATE
            .block(id, plateBlockFactory)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE,
                ModBlockTags.HAMMER_REMOVABLE,
                BlockTags.PRESSURE_PLATES)
            .initialProperties(block::get)
            .properties(properties -> properties
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollission()
                .strength(0.5f)
                .pushReaction(PushReaction.DESTROY))
            .blockstate((ctx, provider) -> DataGenUtil.powerLevelPressurePlate(
                provider, ctx.getId(), ctx.get(),
                ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "block/" + location.getPath())
            ))
            .item()
            .tag(ModItemTags.PLATES, ModItemTags.bindC("plates/" + type), ModItemTags.PLATES)
            .initialProperties(
                () -> type.equals("tungsten") ? new Item.Properties().fireResistant() : new Item.Properties())
            .build()
            .recipe((ctx, provider) -> {
                for (TagKey<Item> ingredient : ingredients) {
                    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 1)
                        .pattern("AA")
                        .define('A', ingredient)
                        .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(ingredient))
                        .save(
                            provider,
                            AnvilCraft.of(id + "_from_"
                                + ingredient
                                .location()
                                .getPath()
                                .replace('/', '_')));
                }
            })
            .register();
    }

    public static final BlockEntry<NestingShulkerBoxBlock> NESTING_SHULKER_BOX = REGISTRATE
        .block("nesting_shulker_box", NestingShulkerBoxBlock::new)
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .properties(properties -> properties.stacksTo(16))
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .recipe((ctx, provider) -> {
        })
        .register();
    public static final BlockEntry<OverNestingShulkerBoxBlock> OVER_NESTING_SHULKER_BOX = REGISTRATE
        .block("over_nesting_shulker_box", OverNestingShulkerBoxBlock::new)
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .properties(properties -> properties.stacksTo(16))
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .recipe((ctx, provider) -> {
        })
        .register();
    public static final BlockEntry<SupercriticalNestingShulkerBoxBlock> SUPERCRITICAL_NESTING_SHULKER_BOX = REGISTRATE
        .block("supercritical_nesting_shulker_box", SupercriticalNestingShulkerBoxBlock::new)
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .blockstate((ctx, provider) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .properties(properties -> properties.stacksTo(16))
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .recipe((ctx, provider) -> {
        })
        .register();

    public static final BlockEntry<LiquidBlock> OIL = REGISTRATE
        .block("oil", p -> new LiquidBlock(ModFluids.OIL.get(), p))
        .properties(it -> it
            .mapColor(MapColor.TERRACOTTA_BLACK)
            .replaceable()
            .noCollission()
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
            .strength(100.0F)
        )
        .blockstate(ModelProviderUtil::liquid)
        .register();

    public static final Object2ObjectMap<Color, BlockEntry<LiquidBlock>> CEMENTS = registerAllCementLiquidBlock();

    private static Object2ObjectMap<Color, BlockEntry<LiquidBlock>> registerAllCementLiquidBlock() {
        Object2ObjectMap<Color, BlockEntry<LiquidBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerCementLiquidBlock(color);
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<LiquidBlock> registerCementLiquidBlock(Color color) {
        return REGISTRATE
            .block("%s_cement".formatted(color), p -> new LiquidBlock(ModFluids.SOURCE_CEMENTS.get(color).get(), p))
            .properties(it -> it
                .mapColor(DyeColor.byName(color.getSerializedName(), DyeColor.GRAY))
                .replaceable()
                .noCollission()
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY)
                .strength(100.0F)
            )
            .blockstate(ModelProviderUtil::liquid)
            .register();
    }

    public static BlockEntry<LiquidBlock> MELT_GEM = REGISTRATE
        .block("melt_gem", p -> new LiquidBlock(ModFluids.MELT_GEM.get(), p))
        .properties(it -> it
            .mapColor(MapColor.EMERALD)
            .lightLevel(s -> 15)
            .replaceable()
            .noCollission()
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
            .strength(100.0F)
        )
        .blockstate(ModelProviderUtil::liquid)
        .register();

    static {
        REGISTRATE.defaultCreativeTab(ModItemGroups.ANVILCRAFT_FUNCTION_BLOCK.getKey());
    }

    public static final BlockEntry<AdvancedRepeaterBlock> ADVANCED_REPEATER = REGISTRATE
        .block("advanced_repeater", AdvancedRepeaterBlock::new)
        .properties(properties -> properties
            .strength(3.0F, 3.5F)
            .sound(SoundType.STONE)
            .noOcclusion()
        )
        .blockstate((ctx, provider) -> DataGenUtil.diodeBlock(
            provider, ctx.getId(), ctx.get()
        ))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .recipe((ctx, provider) ->
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get())
                .pattern("BCR")
                .pattern("III")
                .define('B', ModItems.CIRCUIT_BOARD)
                .define('C', Items.CLOCK)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE_TORCH)
        )
        .register();

    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> COPPER_PRESSURE_PLATE = REGISTRATE
        .block("copper_pressure_plate", properties -> new TimeCountedPressurePlateBlock(BlockSetType.IRON, properties, 10))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.HAMMER_REMOVABLE,
            BlockTags.PRESSURE_PLATES
        )
        .initialProperties(() -> Blocks.COPPER_BLOCK)
        .properties(properties -> properties
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollission()
            .strength(0.5f)
            .pushReaction(PushReaction.DESTROY)
        )
        .blockstate((ctx, provider) -> provider.pressurePlateBlock(
            ctx.get(),
            ResourceLocation.withDefaultNamespace("block/copper_block")
        ))
        .item()
        .tag(ModItemTags.PLATES, ModItemTags.COPPER_PLATES)
        .build()
        .recipe((ctx, provider) -> {
            ResourceLocation location1 = BuiltInRegistries.ITEM.getKey(Items.COPPER_INGOT);
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.get(), 1)
                .pattern("AA")
                .define('A', Items.COPPER_INGOT)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_INGOT), AnvilCraftDatagen.has(Items.COPPER_INGOT))
                .save(
                    provider,
                    AnvilCraft.of("copper_pressure_plate_from_"
                        + location1.getPath().replace('/', '_')));
        })
        .register();
    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> EXPOSED_COPPER_PRESSURE_PLATE =
        registerOtherCopperPressurePlate(
            "exposed_", Blocks.EXPOSED_COPPER, 20
        );
    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> WEATHERED_COPPER_PRESSURE_PLATE =
        registerOtherCopperPressurePlate(
            "weathered_", Blocks.WEATHERED_COPPER, 40
        );
    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> OXIDIZED_COPPER_PRESSURE_PLATE =
        registerOtherCopperPressurePlate(
            "oxidized_", Blocks.OXIDIZED_COPPER, 80
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> TUNGSTEN_PRESSURE_PLATE =
        registerPressurePlate(
            "tungsten", TUNGSTEN_BLOCK,
            FireImmunePressurePlateBlock::new,
            ModItemTags.TUNGSTEN_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> TITANIUM_PRESSURE_PLATE =
        registerPressurePlate(
            "titanium", TITANIUM_BLOCK,
            properties -> new ItemDurabilityPressurePlateBlock(properties, false),
            ModItemTags.TITANIUM_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> ZINC_PRESSURE_PLATE =
        registerPressurePlate(
            "zinc", ZINC_BLOCK,
            properties -> new HealthPercentPressurePlateBlock(properties, false),
            ModItemTags.ZINC_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> TIN_PRESSURE_PLATE =
        registerPressurePlate(
            "tin", TIN_BLOCK,
            properties -> new HealthPercentPressurePlateBlock(properties, true),
            ModItemTags.TIN_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> LEAD_PRESSURE_PLATE =
        registerPressurePlate(
            "lead", LEAD_BLOCK,
            EntityTypePressurePlateBlock::new,
            ModItemTags.LEAD_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> SILVER_PRESSURE_PLATE =
        registerPressurePlate(
            "silver", SILVER_BLOCK,
            properties -> new EntityCountPressurePlateBlock(
                properties, entity -> entity.getType().is(TagKey.create(
                Registries.ENTITY_TYPE,
                ResourceLocation.withDefaultNamespace("undead")
            ))),
            ModItemTags.SILVER_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> URANIUM_PRESSURE_PLATE =
        registerPressurePlate(
            "uranium", URANIUM_BLOCK,
            properties -> new ItemDurabilityPressurePlateBlock(properties, true),
            ModItemTags.URANIUM_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> BRONZE_PRESSURE_PLATE =
        registerPressurePlate(
            "bronze", BRONZE_BLOCK,
            PlayerInventoryPressurePlateBlock::new,
            ModItemTags.BRONZE_INGOTS
        );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> BRASS_PRESSURE_PLATE =
        registerPressurePlate(
            "brass", BRASS_BLOCK,
            PlayerHungerPressurePlateBlock::new,
            ModItemTags.BRASS_INGOTS
        );

    public static void register() {
    }

    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos, EntityType<?> entity) {
        return false;
    }

    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }

    public static boolean always(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return true;
    }
}