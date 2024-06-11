package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent.Switch;
import dev.dubhe.anvilcraft.block.AbstractMultiplePartBlock;
import dev.dubhe.anvilcraft.block.ActiveSilencerBlock;
import dev.dubhe.anvilcraft.block.ArrowBlock;
import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.block.BlockDevourerBlock;
import dev.dubhe.anvilcraft.block.BlockPlacerBlock;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.ChargeCollectorBlock;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.CreativeGeneratorBlock;
import dev.dubhe.anvilcraft.block.DischargerBlock;
import dev.dubhe.anvilcraft.block.FerriteCoreMagnetBlock;
import dev.dubhe.anvilcraft.block.GlowingMetalBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.block.HeavyIronPlateBlock;
import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.HoneyCauldronBlock;
import dev.dubhe.anvilcraft.block.ImpactPileBlock;
import dev.dubhe.anvilcraft.block.IncandescentMetalBlock;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.block.JewelCraftingTable;
import dev.dubhe.anvilcraft.block.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.LoadMonitorBlock;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.block.MeltGemCauldron;
import dev.dubhe.anvilcraft.block.MineralFountainBlock;
import dev.dubhe.anvilcraft.block.MobAmberBlock;
import dev.dubhe.anvilcraft.block.ObsidianCauldron;
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
import dev.dubhe.anvilcraft.block.StampingPlatformBlock;
import dev.dubhe.anvilcraft.block.ThermoelectricConverterBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.generator.recipe.BlockSmashRecipesLoader;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.item.CursedBlockItem;
import dev.dubhe.anvilcraft.item.EndDustBlockItem;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.OverseerBlockItem;
import dev.dubhe.anvilcraft.item.PlaceInWaterBlockItem;
import dev.dubhe.anvilcraft.item.RemoteTransmissionPoleBlockItem;
import dev.dubhe.anvilcraft.item.ResinBlockItem;
import dev.dubhe.anvilcraft.item.TransmissionPoleBlockItem;
import dev.dubhe.anvilcraft.util.DangerUtil;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.GravelBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.SWITCH;

@SuppressWarnings("unused")
public class ModBlocks {
    public static final BlockEntry<? extends Block> STAMPING_PLATFORM = REGISTRATE
        .block("stamping_platform", StampingPlatformBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                .pattern("BAB")
                .pattern("B B")
                .pattern("B B")
                .define('A', ModItemTags.IRON_PLATES)
                .define('B', Items.IRON_INGOT)
                .unlockedBy("has_" + ModItemTags.IRON_PLATES.location().getPath(),
                    AnvilCraftDatagen.has(ModItemTags.IRON_PLATES))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                    AnvilCraftDatagen.has(Items.IRON_INGOT))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                .pattern("BAB")
                .pattern("B B")
                .pattern("B B")
                .define('A', ModItemTags.IRON_PLATES_FORGE)
                .define('B', Items.IRON_INGOT)
                .unlockedBy("has_" + ModItemTags.IRON_PLATES_FORGE.location().getPath(),
                    AnvilCraftDatagen.has(ModItemTags.IRON_PLATES_FORGE))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                    AnvilCraftDatagen.has(Items.IRON_INGOT))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_forge");
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
    public static final BlockEntry<? extends Block> ROYAL_ANVIL = REGISTRATE
        .block("royal_anvil", RoyalAnvilBlock::new)
        .initialProperties(() -> Blocks.ANVIL)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.ANVIL, ModBlockTags.CANT_BROKEN_ANVIL, BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> MAGNET_BLOCK = REGISTRATE
        .block("magnet_block", MagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(ModBlockTags.MAGNET, BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.MAGNET_INGOT)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider)
        )
        .register();
    public static final BlockEntry<? extends Block> HOLLOW_MAGNET_BLOCK = REGISTRATE
        .block("hollow_magnet_block", HollowMagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(ModBlockTags.MAGNET, BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("AAA")
            .pattern("A A")
            .pattern("AAA")
            .define('A', ModItems.MAGNET_INGOT)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> FERRITE_CORE_MAGNET_BLOCK = REGISTRATE
        .block("ferrite_core_magnet_block", FerriteCoreMagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::randomTicks)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(ModBlockTags.MAGNET, BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModItems.MAGNET_INGOT)
            .define('B', Items.IRON_INGOT)
            .unlockedBy("has_magnet_ingot", RegistrateRecipeProvider.has(ModItems.MAGNET_INGOT))
            .unlockedBy("has_iron_ingot", RegistrateRecipeProvider.has(Items.IRON_INGOT))
            .save(provider)
        )
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
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModBlocks.CHUTE)
            .pattern("A A")
            .pattern("ABA")
            .pattern(" A ")
            .define('A', Items.IRON_INGOT)
            .define('B', Items.DROPPER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER),
                AnvilCraftDatagen.has(Items.DROPPER))
            .save(provider)
        )
        .register();
    public static final BlockEntry<SimpleChuteBlock> SIMPLE_CHUTE = REGISTRATE
        .block("simple_chute", SimpleChuteBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.CHUTE))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
        .register();
    public static final BlockEntry<? extends Block> AUTO_CRAFTER = REGISTRATE
        .block("auto_crafter", AutoCrafterBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ABA")
            .pattern("ADC")
            .pattern("AEA")
            .define('A', Items.IRON_INGOT)
            .define('B', Items.CRAFTING_TABLE)
            .define('C', Items.DROPPER)
            .define('D', ModItems.MAGNETOELECTRIC_CORE)
            .define('E', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.CRAFTING_TABLE),
                AnvilCraftDatagen.has(Items.CRAFTING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER),
                AnvilCraftDatagen.has(Items.DROPPER))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD),
                AnvilCraftDatagen.has(ModItems.CIRCUIT_BOARD)
            )
            .save(provider)
        )
        .register();
    public static final BlockEntry<? extends Block> ROYAL_GRINDSTONE = REGISTRATE
        .block("royal_grindstone", RoyalGrindstone::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_SMITHING_TABLE = REGISTRATE
        .block("royal_smithing_table", RoyalSmithingTableBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_STEEL_BLOCK = REGISTRATE
        .block("royal_steel_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, ModBlockTags.OVERSEER_BASE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider);
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(ctx.get())
                .type(AnvilRecipeType.SUPER_HEATING)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0),
                    Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.IRON_BLOCK)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.DIAMOND_BLOCK)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.AMETHYST_BLOCK)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.GEM_BLOCKS)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get().asItem(), 1)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK),
                    AnvilCraftDatagen.has(Items.IRON_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.DIAMOND_BLOCK),
                    AnvilCraftDatagen.has(Items.DIAMOND_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK),
                    AnvilCraftDatagen.has(Items.AMETHYST_BLOCK)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItemTags.GEM_BLOCKS),
                    AnvilCraftDatagen.has(ModItemTags.GEM_BLOCKS)
                )
                .save(provider, AnvilCraft.of("heating/"
                    + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath()));
        })
        .register();
    public static final BlockEntry<? extends Block> SMOOTH_ROYAL_STEEL_BLOCK = REGISTRATE
        .block("smooth_royal_steel_block", Block::new)
        .tag(ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_BLOCK = REGISTRATE
        .block("cut_royal_steel_block", Block::new)
        .tag(ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("AA")
            .pattern("AA")
            .define('A', ModBlocks.ROYAL_STEEL_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("craft/cut_royal_steel_block"))
        )
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_SLAB = REGISTRATE
        .block("cut_royal_steel_slab", SlabBlock::new)
        .tag(ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .blockstate((ctx, provider) -> provider.slabBlock(ctx.get(),
            AnvilCraft.of("block/cut_royal_steel_block"),
            AnvilCraft.of("block/cut_royal_steel_block")))
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables::createSlabItemTable))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 6)
            .pattern("AAA")
            .define('A', ModBlocks.CUT_ROYAL_STEEL_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.CUT_ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("craft/cut_royal_steel_slab")))
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_STAIRS = REGISTRATE
        .block("cut_royal_steel_stairs", (properties) ->
            new StairBlock(ModBlocks.CUT_ROYAL_STEEL_BLOCK.getDefaultState(), properties))
        .tag(ModBlockTags.OVERSEER_BASE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .blockstate((ctx, provider) -> provider.stairsBlock(ctx.get(),
            AnvilCraft.of("block/cut_royal_steel_block")))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.CUT_ROYAL_STEEL_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asItem()),
                AnvilCraftDatagen.has(ModBlocks.CUT_ROYAL_STEEL_BLOCK))
            .save(provider, AnvilCraft.of("craft/cut_royal_steel_stairs")))
        .register();
    public static final BlockEntry<? extends Block> HEAVY_IRON_BLOCK = REGISTRATE
        .block("heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_BLOCK = REGISTRATE
        .block("polished_heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_SLAB = REGISTRATE
        .block("polished_heavy_iron_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> provider.slabBlock(ctx.get(),
            AnvilCraft.of("block/polished_heavy_iron_block"),
            AnvilCraft.of("block/polished_heavy_iron_block")))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_STAIRS = REGISTRATE
        .block("polished_heavy_iron_stairs", (properties) ->
            new StairBlock(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.getDefaultState(), properties))
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> provider.stairsBlock(ctx.get(),
            AnvilCraft.of("block/polished_heavy_iron_block")))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_BLOCK = REGISTRATE
        .block("cut_heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_SLAB = REGISTRATE
        .block("cut_heavy_iron_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> provider.slabBlock(ctx.get(),
            AnvilCraft.of("block/cut_heavy_iron_block"),
            AnvilCraft.of("block/cut_heavy_iron_block")))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_STAIRS = REGISTRATE
        .block("cut_heavy_iron_stairs", (properties) ->
            new StairBlock(ModBlocks.CUT_HEAVY_IRON_BLOCK.getDefaultState(), properties))
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> provider.stairsBlock(ctx.get(),
            AnvilCraft.of("block/cut_heavy_iron_block")))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> HEAVY_IRON_PLATE = REGISTRATE
        .block("heavy_iron_plate", HeavyIronPlateBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> HEAVY_IRON_COLUMN = REGISTRATE
        .block("heavy_iron_column", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> HEAVY_IRON_BEAM = REGISTRATE
        .block("heavy_iron_beam", HeavyIronBeamBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .item()
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> LAVA_CAULDRON = REGISTRATE
        .block("lava_cauldron", LavaCauldronBlock::new)
        .initialProperties(() -> Blocks.LAVA_CAULDRON)
        .properties(properties -> properties.lightLevel(blockState ->
            blockState.getValue(LayeredCauldronBlock.LEVEL) * 5))
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> CURSED_GOLD_BLOCK = REGISTRATE
        .block("cursed_gold_block", Block::new)
        .initialProperties(() -> Blocks.GOLD_BLOCK)
        .item(CursedBlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.CURSED_GOLD_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT),
                AnvilCraftDatagen.has(ModItems.CURSED_GOLD_INGOT))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> ZINC_BLOCK = REGISTRATE
        .block("zinc_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.ZINC_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.ZINC_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.ZINC_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.ZINC_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.ZINC_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> TIN_BLOCK = REGISTRATE
        .block("tin_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TIN_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TIN_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TIN_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TIN_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.TIN_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> TITANIUM_BLOCK = REGISTRATE
        .block("titanium_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TITANIUM_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TITANIUM_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TITANIUM_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TITANIUM_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.TITANIUM_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> TUNGSTEN_BLOCK = REGISTRATE
        .block("tungsten_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TUNGSTEN_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.TUNGSTEN_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.TUNGSTEN_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.TUNGSTEN_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.TUNGSTEN_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> LEAD_BLOCK = REGISTRATE
        .block("lead_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.LEAD_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.LEAD_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.LEAD_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.LEAD_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.LEAD_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> SILVER_BLOCK = REGISTRATE
        .block("silver_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.SILVER_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.SILVER_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.SILVER_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.SILVER_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.SILVER_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> URANIUM_BLOCK = REGISTRATE
        .block("uranium_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.URANIUM_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.URANIUM_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.URANIUM_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.URANIUM_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.URANIUM_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> BRONZE_BLOCK = REGISTRATE
        .block("bronze_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRONZE_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.BRONZE_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRONZE_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRONZE_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.BRONZE_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> BRASS_BLOCK = REGISTRATE
        .block("brass_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRASS_INGOTS)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS),
                    AnvilCraftDatagen.has(ModItemTags.BRASS_INGOTS))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItemTags.BRASS_INGOTS_FORGE)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS_FORGE),
                    AnvilCraftDatagen.has(ModItemTags.BRASS_INGOTS_FORGE))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath() + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> TOPAZ_BLOCK = REGISTRATE
        .block("topaz_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.TOPAZ)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TOPAZ),
                AnvilCraftDatagen.has(ModItems.TOPAZ))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> RUBY_BLOCK = REGISTRATE
        .block("ruby_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RUBY)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY),
                AnvilCraftDatagen.has(ModItems.RUBY))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> SAPPHIRE_BLOCK = REGISTRATE
        .block("sapphire_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.SAPPHIRE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SAPPHIRE),
                AnvilCraftDatagen.has(ModItems.SAPPHIRE))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> RESIN_BLOCK = REGISTRATE
        .block("resin_block", ResinBlock::new)
        .initialProperties(() -> Blocks.SLIME_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .properties(properties -> properties.sound(SoundType.HONEY_BLOCK))
        .item(ResinBlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RESIN),
                AnvilCraftDatagen.has(ModItems.RESIN))
            .save(provider))
        .register();

    public static final BlockEntry<? extends Block> AMBER_BLOCK = REGISTRATE
        .block("amber_block", HalfTransparentBlock::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .properties(BlockBehaviour.Properties::noOcclusion)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.AMBER)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.AMBER),
                    AnvilCraftDatagen.has(ModItems.AMBER))
                .save(provider);
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate
                .of(ModBlocks.RESIN_BLOCK)
                .withCount(MinMaxBounds.Ints.atLeast(1));
            HasItem hasItem = new HasItemIngredient(new Vec3(0.0, -1.0, 0.0), item).notHasTag("BlockEntityTag.entity");
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(ctx.get())
                .type(AnvilRecipeType.TIMEWARP)
                .hasBlock(
                    ModBlocks.CORRUPTED_BEACON.get(),
                    new Vec3(0.0, -2.0, 0.0),
                    Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasBlock(Blocks.CAULDRON)
                .addPredicates(hasItem)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get(), 1)
                .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()),
                    AnvilCraftDatagen.has(ModBlocks.RESIN_BLOCK))
                .save(
                    provider,
                    AnvilCraft.of(
                        "timewarp/" + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath())
                );
        })
        .register();

    public static final BlockEntry<MobAmberBlock> MOB_AMBER_BLOCK = REGISTRATE
        .block("mob_amber_block", MobAmberBlock::new)
        .blockstate((ctx, provider) -> {
        })
        .item(HasMobBlockItem::new)
        .recipe((ctx, provider) -> {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("is_monster", false);
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate
                .of(ModBlocks.RESIN_BLOCK)
                .withCount(MinMaxBounds.Ints.atLeast(1))
                .withNbt(tag);
            RecipePredicate hasItem = new HasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .hasTag("BlockEntityTag.entity")
                .saveItemData("resin");
            RecipeOutcome spawnItem0 = new SpawnItem(
                new Vec3(0.0, -1.0, 0.0),
                1.0,
                ctx.get().getDefaultInstance()
            ).loadItemData("resin");
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(ctx.get())
                .type(AnvilRecipeType.TIMEWARP)
                .hasBlock(
                    ModBlocks.CORRUPTED_BEACON.get(),
                    new Vec3(0.0, -2.0, 0.0),
                    Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasBlock(Blocks.CAULDRON)
                .addPredicates(hasItem)
                .addOutcomes(spawnItem0)
                .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()),
                    AnvilCraftDatagen.has(ModBlocks.RESIN_BLOCK))
                .save(
                    provider,
                    AnvilCraft.of(
                        "timewarp/" + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath())
                );

            tag = new CompoundTag();
            tag.putBoolean("is_monster", true);
            HasItem.ModItemPredicate monster = HasItem.ModItemPredicate
                .of(ModBlocks.RESIN_BLOCK)
                .withCount(MinMaxBounds.Ints.atLeast(1))
                .withNbt(tag);
            RecipePredicate hasItemMonster = new HasItemIngredient(new Vec3(0.0, -1.0, 0.0), monster)
                .hasTag("BlockEntityTag.entity")
                .saveItemData("resin");
            RecipeOutcome spawnItem1 = new SpawnItem(
                new Vec3(0.0, -1.0, 0.0),
                0.95,
                ctx.get().getDefaultInstance()
            ).loadItemData("resin");
            RecipeOutcome spawnItem2 = new SpawnItem(
                new Vec3(0.0, -1.0, 0.0),
                0.05,
                ModBlocks.RESENTFUL_AMBER_BLOCK.asItem().getDefaultInstance()
            ).loadItemData("resin");
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(ctx.get())
                .type(AnvilRecipeType.TIMEWARP)
                .hasBlock(
                    ModBlocks.CORRUPTED_BEACON.get(),
                    new Vec3(0.0, -2.0, 0.0),
                    Map.entry(CorruptedBeaconBlock.LIT, true)
                )
                .hasBlock(Blocks.CAULDRON)
                .addPredicates(hasItemMonster)
                .addOutcomes(new SelectOne().add(spawnItem1).add(spawnItem2))
                .unlockedBy(AnvilCraftDatagen.hasItem(ctx.get()),
                    AnvilCraftDatagen.has(ModBlocks.RESIN_BLOCK))
                .save(
                    provider,
                    AnvilCraft.of(
                        "timewarp/"
                            + BuiltInRegistries.ITEM.getKey(ctx.get().asItem()).getPath()
                            + "_resentful"
                    )
                );
        })
        .build()
        .initialProperties(ModBlocks.AMBER_BLOCK)
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(ModBlocks.MOB_AMBER_BLOCK))
                        .apply(
                            CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                .copy("entity", "BlockEntityTag.entity")
                        )
                );
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
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(ModBlocks.RESENTFUL_AMBER_BLOCK))
                        .apply(
                            CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                .copy("entity", "BlockEntityTag.entity")
                        )
                );
            ctx.add(prov, builder);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<? extends Block> CREATIVE_GENERATOR = REGISTRATE
        .block("creative_generator", CreativeGeneratorBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> HEATER = REGISTRATE
        .block("heater", HeaterBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.noOcclusion()
            .lightLevel(state -> state.getValue(OVERLOAD) ? 0 : 15))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("BCB")
            .pattern("BBB")
            .define('A', Items.TERRACOTTA)
            .define('B', Items.IRON_INGOT)
            .define('C', ModItems.MAGNETOELECTRIC_CORE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.TERRACOTTA),
                AnvilCraftDatagen.has(Items.TERRACOTTA))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            )
            .save(provider))
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
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 8)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', Items.IRON_INGOT)
            .define('B', ModItems.MAGNETOELECTRIC_CORE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            )
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> TRANSMISSION_POLE = REGISTRATE
        .block("transmission_pole", TransmissionPoleBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.noOcclusion()
            .lightLevel(
                state -> {
                    if (state.getValue(TransmissionPoleBlock.HALF) != Vertical3PartHalf.TOP)
                        return 0;
                    if (state.getValue(SWITCH) == IPowerComponent.Switch.OFF)
                        return 0;
                    if (state.getValue(OVERLOAD))
                        return 6;
                    return 15;
                }
            )
        )
        .blockstate((ctx, provider) -> {
        })
        .item(TransmissionPoleBlockItem::new)
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', ModItems.MAGNETOELECTRIC_CORE)
            .define('B', Items.LIGHTNING_ROD)
            .define('C', Items.IRON_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD),
                AnvilCraftDatagen.has(Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK),
                AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .save(provider))
        .loot(AbstractMultiplePartBlock::loot)
        .register();
    public static final BlockEntry<? extends Block> REMOTE_TRANSMISSION_POLE = REGISTRATE
        .block("remote_transmission_pole", RemoteTransmissionPoleBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .loot(AbstractMultiplePartBlock::loot)
        .properties(properties -> properties.noOcclusion()
            .lightLevel(
                state -> {
                    if (state.getValue(RemoteTransmissionPoleBlock.HALF) != Vertical4PartHalf.TOP) return 0;
                    if (state.getValue(SWITCH) == IPowerComponent.Switch.OFF) return 0;
                    if (state.getValue(OVERLOAD)) return 6;
                    return 15;
                }
            )
        )
        .blockstate((ctx, provider) -> {
        })
        .item(RemoteTransmissionPoleBlockItem::new)
        .model((ctx, provider) -> {
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', ModItems.MAGNETOELECTRIC_CORE)
            .define('B', ModBlocks.TRANSMISSION_POLE)
            .define('C', Items.ANVIL)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.TRANSMISSION_POLE),
                AnvilCraftDatagen.has(ModBlocks.TRANSMISSION_POLE))
            .save(provider))
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
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("B B")
            .pattern("ABA")
            .define('A', Items.STICK)
            .define('B', Items.STRING)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(Items.STRING))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> CINERITE = REGISTRATE
        .block("cinerite", GravelBlock::new)
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<? extends Block> QUARTZ_SAND = REGISTRATE
        .block("quartz_sand", GravelBlock::new)
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<? extends Block> TEMPERING_GLASS = REGISTRATE
        .block("tempering_glass", GlassBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(properties -> properties
            .explosionResistance(15.0F)
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never)
        )
        .blockstate((ctx, provider) -> {
            provider.simpleBlock(ctx.get());
            provider.models().cubeAll(ctx.getName(), provider.modLoc("block/" + ctx.getName()))
                .renderType("translucent");
        })
        .simpleItem()
        .register();
    public static final BlockEntry<JewelCraftingTable> JEWEL_CRAFTING_TABLE = REGISTRATE
        .block("jewelcrafting_table", JewelCraftingTable::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .lang("Jewel Crafting Table")
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABC")
            .pattern("DDD")
            .pattern("F F")
            .define('A', Blocks.GRINDSTONE)
            .define('B', Blocks.GLASS)
            .define('C', Blocks.GRINDSTONE)
            .define('D', Blocks.SMOOTH_STONE)
            .define('F', ItemTags.PLANKS)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.GRINDSTONE),
                AnvilCraftDatagen.has(Blocks.GRINDSTONE))
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.STONECUTTER),
                AnvilCraftDatagen.has(Blocks.STONECUTTER))
            .save(provider))
        .register();
    public static final BlockEntry<FallingBlock> NETHER_DUST = REGISTRATE
        .block("nether_dust", FallingBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.BLACK_CONCRETE_POWDER)
        .recipe((ctx, provider) -> BlockSmashRecipesLoader.smash(Blocks.NETHERRACK, ctx.get(),
            provider))
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<Block> END_DUST = REGISTRATE
        .block("end_dust", Block::new)
        .item(EndDustBlockItem::new)
        .build()
        .initialProperties(() -> Blocks.BLACK_CONCRETE_POWDER)
        .recipe(
            (ctx, provider) -> BlockSmashRecipesLoader.smash(Blocks.END_STONE, ctx.get(), provider))
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<FallingBlock> DEEPSLATE_CHIPS = REGISTRATE
        .block("deepslate_chips", FallingBlock::new)
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<FallingBlock> BLACK_SAND = REGISTRATE
        .block("black_sand", FallingBlock::new)
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();
    public static final BlockEntry<PiezoelectricCrystalBlock> PIEZOELECTRIC_CRYSTAL =
        REGISTRATE
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
                    .define('B', ModItemTags.QUARTZ_BLOCKS)
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                        AnvilCraftDatagen.has(Items.COPPER_INGOT)
                    )
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(ModItemTags.QUARTZ_BLOCKS),
                        AnvilCraftDatagen.has(ModItemTags.QUARTZ_BLOCKS)
                    )
                    .save(provider);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .pattern("ABA")
                    .pattern(" B ")
                    .pattern("ABA")
                    .define('A', Items.COPPER_INGOT)
                    .define('B', ModItemTags.AMETHYST_BLOCKS)
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                        AnvilCraftDatagen.has(Items.COPPER_INGOT)
                    )
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(ModItemTags.AMETHYST_BLOCKS),
                        AnvilCraftDatagen.has(ModItemTags.AMETHYST_BLOCKS)
                    )
                    .save(
                        provider,
                        BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_amethyst"
                    );
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .pattern("ABA")
                    .pattern(" B ")
                    .pattern("ABA")
                    .define('A', Items.COPPER_INGOT)
                    .define('B', ModItemTags.QUARTZ_BLOCKS_FORGE)
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                        AnvilCraftDatagen.has(Items.COPPER_INGOT)
                    )
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(ModItemTags.QUARTZ_BLOCKS_FORGE),
                        AnvilCraftDatagen.has(ModItemTags.QUARTZ_BLOCKS_FORGE)
                    )
                    .save(
                        provider,
                        BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_forge"
                    );
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .pattern("ABA")
                    .pattern(" B ")
                    .pattern("ABA")
                    .define('A', Items.COPPER_INGOT)
                    .define('B', ModItemTags.AMETHYST_BLOCKS_FORGE)
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                        AnvilCraftDatagen.has(Items.COPPER_INGOT)
                    )
                    .unlockedBy(
                        AnvilCraftDatagen.hasItem(ModItemTags.AMETHYST_BLOCKS_FORGE),
                        AnvilCraftDatagen.has(ModItemTags.AMETHYST_BLOCKS_FORGE)
                    )
                    .save(
                        provider,
                        BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_amethyst_forge"
                    );
            })
            .register();
    public static final BlockEntry<ChargeCollectorBlock> CHARGE_COLLECTOR =
        REGISTRATE
            .block("charge_collector", ChargeCollectorBlock::new)
            .simpleItem()
            .properties(BlockBehaviour.Properties::noOcclusion)
            .blockstate((ctx, provider) -> {
            })
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern(" A ")
                .pattern("B B")
                .pattern("CCC")
                .define('A', ModItems.MAGNETOELECTRIC_CORE)
                .define('B', Items.COPPER_INGOT)
                .define('C', Items.IRON_INGOT)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                    AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                    AnvilCraftDatagen.has(Items.COPPER_INGOT)
                )
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                    AnvilCraftDatagen.has(Items.IRON_INGOT)
                )
                .save(provider))
            .register();

    public static final BlockEntry<MeltGemCauldron> MELT_GEM_CAULDRON = REGISTRATE
        .block("melt_gem_cauldron", MeltGemCauldron::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ItemCollectorBlock> ITEM_COLLECTOR = REGISTRATE
        .block("item_collector", ItemCollectorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .blockstate((c, p) -> {
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
            .pattern("ABA")
            .pattern("CDC")
            .pattern("ACA")
            .define('A', Items.IRON_INGOT)
            .define('B', ModItems.MAGNET)
            .define('C', Items.HOPPER)
            .define('D', ModItems.MAGNETOELECTRIC_CORE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                AnvilCraftDatagen.has(Items.IRON_INGOT)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNET),
                AnvilCraftDatagen.has(ModItems.MAGNET)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(Items.HOPPER),
                AnvilCraftDatagen.has(Items.HOPPER)
            ).save(p))
        .register();

    public static final BlockEntry<HoneyCauldronBlock> HONEY_CAULDRON = REGISTRATE
        .block("honey_cauldron", HoneyCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<PowerConverterSmallBlock> POWER_CONVERTER_SMALL = REGISTRATE
        .block("power_converter_small", PowerConverterSmallBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(state -> {
            if (state.getValue(OVERLOAD))
                return 6;
            else
                return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            VanillaRecipeProvider.stonecutterResultFromBase(
                provider, RecipeCategory.MISC,
                ctx.get().asItem(), ModBlocks.POWER_CONVERTER_MIDDLE, 3
            );
            VanillaRecipeProvider.stonecutterResultFromBase(
                provider, RecipeCategory.MISC,
                ctx.get().asItem(), ModBlocks.POWER_CONVERTER_BIG, 9
            );
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
            if (state.getValue(OVERLOAD))
                return 6;
            else
                return 15;
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
                    AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_SMALL)
                )
                .save(
                    provider,
                    BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_from_small"
                );
            VanillaRecipeProvider.stonecutterResultFromBase(
                provider, RecipeCategory.MISC,
                ctx.get().asItem(), ModBlocks.POWER_CONVERTER_BIG, 3
            );
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
            if (state.getValue(OVERLOAD))
                return 6;
            else
                return 15;
        }))
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("B")
                .define('A', ModItems.MAGNETOELECTRIC_CORE)
                .define('B', Items.COPPER_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                    AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
                )
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("A")
                .pattern("A")
                .pattern("A")
                .define('A', ModBlocks.POWER_CONVERTER_MIDDLE)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_MIDDLE),
                    AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE)
                )
                .save(
                    provider,
                    BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_from_middle"
                );
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModBlocks.POWER_CONVERTER_SMALL)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.POWER_CONVERTER_MIDDLE),
                    AnvilCraftDatagen.has(ModBlocks.POWER_CONVERTER_MIDDLE)
                )
                .save(
                    provider,
                    BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_from_small"
                );
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<LoadMonitorBlock> LOAD_MONITOR = REGISTRATE
        .block("load_monitor", LoadMonitorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(state -> {
            if (state.getValue(OVERLOAD)) return 6;
            else return 15;
        }).noOcclusion())
        .blockstate((ctx, provider) -> {
        })
        .item()
        .model((ctx, provider) -> provider.blockItem(ctx, "_0"))
        .build()
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
            .pattern("A")
            .pattern("B")
            .define('A', Items.COMPASS)
            .define('B', ModItems.MAGNETOELECTRIC_CORE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.COMPASS),
                AnvilCraftDatagen.has(Items.COMPASS)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            )
            .save(provider)
        )
        .register();
    public static final BlockEntry<BlockPlacerBlock> BLOCK_PLACER = REGISTRATE
        .block("block_placer", BlockPlacerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("DCB")
            .pattern("AAA")
            .define('A', Items.COBBLESTONE)
            .define('B', ModItems.CRAB_CLAW)
            .define('C', Items.REDSTONE)
            .define('D', Items.HOPPER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CRAB_CLAW),
                AnvilCraftDatagen.has(ModItems.CRAB_CLAW)
            )
            .save(provider))
        .register();

    public static final BlockEntry<ActiveSilencerBlock> ACTIVE_SILENCER = REGISTRATE
        .block("active_silencer", ActiveSilencerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ACA")
            .define('A', Items.AMETHYST_BLOCK)
            .define('B', Items.JUKEBOX)
            .define('C', Items.SCULK_SENSOR)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK),
                AnvilCraftDatagen.has(Items.AMETHYST_BLOCK)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(Items.JUKEBOX),
                AnvilCraftDatagen.has(Items.JUKEBOX)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(Items.SCULK_SENSOR),
                AnvilCraftDatagen.has(Items.SCULK_SENSOR)
            )
            .save(provider))
        .register();

    public static final BlockEntry<ObsidianCauldron> OBSIDIDAN_CAULDRON = REGISTRATE
        .block("obsidian_cauldron", ObsidianCauldron::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(it -> it.pushReaction(PushReaction.BLOCK))
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ThermoelectricConverterBlock> THERMOELECTRIC_CONVERTER = REGISTRATE
        .block("thermoelectric_converter", ThermoelectricConverterBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("CDC")
            .pattern("ABA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', Items.COPPER_INGOT)
            .define('C', ModBlocks.SAPPHIRE_BLOCK)
            .define('D', Items.BLUE_ICE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                AnvilCraftDatagen.has(Items.COPPER_INGOT)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.SAPPHIRE_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.SAPPHIRE_BLOCK)
            ).unlockedBy(
                AnvilCraftDatagen.hasItem(Items.BLUE_ICE),
                AnvilCraftDatagen.has(Items.BLUE_ICE)
            )
            .save(provider))
        .register();

    public static final BlockEntry<BlockDevourerBlock> BLOCK_DEVOURER = REGISTRATE
        .block("block_devourer", BlockDevourerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
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
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.PISTON),
                AnvilCraftDatagen.has(Items.PISTON))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.REDSTONE),
                AnvilCraftDatagen.has(Items.REDSTONE))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.COBBLESTONE),
                AnvilCraftDatagen.has(Items.COBBLESTONE))
            .save(provider))
        .register();

    public static final BlockEntry<OverseerBlock> OVERSEER_BLOCK = REGISTRATE
        .block("overseer", OverseerBlock::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .loot(AbstractMultiplePartBlock::loot)
        .item(OverseerBlockItem::new)
        .model((ctx, provider) -> {
        })
        .build()
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("ABA")
            .pattern("CBC")
            .define('A', Items.OBSIDIAN)
            .define('B', Items.ENDER_EYE)
            .define('C', ModBlocks.ROYAL_STEEL_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.ROYAL_STEEL_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.ROYAL_STEEL_BLOCK)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.ENDER_EYE),
                AnvilCraftDatagen.has(Items.ENDER_EYE)
            )
            .save(provider)
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ArrowBlock> ARROW = REGISTRATE
        .block("arrow", ArrowBlock::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .register();

    public static final BlockEntry<CementCauldronBlock> CEMENT_CAULDRON = REGISTRATE
        .block("cement_cauldron", CementCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate((ctx, provider) -> {
        })
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ChargerBlock> CHARGER = REGISTRATE
        .block("charger", ChargerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, prov) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A A")
            .pattern("ABA")
            .pattern("CCC")
            .define('A', Items.COPPER_INGOT)
            .define('B', ModItems.MAGNETOELECTRIC_CORE)
            .define('C', Items.IRON_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.COPPER_INGOT),
                AnvilCraftDatagen.has(Items.COPPER_INGOT)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.MAGNETOELECTRIC_CORE),
                AnvilCraftDatagen.has(ModItems.MAGNETOELECTRIC_CORE)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.IRON_INGOT),
                AnvilCraftDatagen.has(Items.IRON_INGOT)
            )
            .save(provider)
        )
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
        .register();

    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_BLACK =
        registerReinforcedConcreteBlock(Color.BLACK);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_BLUE =
        registerReinforcedConcreteBlock(Color.BLUE);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_BROWN =
        registerReinforcedConcreteBlock(Color.BROWN);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_CYAN =
        registerReinforcedConcreteBlock(Color.CYAN);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_GRAY =
        registerReinforcedConcreteBlock(Color.GRAY);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_GREEN =
        registerReinforcedConcreteBlock(Color.GREEN);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_LIGHT_BLUE =
        registerReinforcedConcreteBlock(Color.LIGHT_BLUE);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_LIGHT_GRAY =
        registerReinforcedConcreteBlock(Color.LIGHT_GRAY);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_LIME =
        registerReinforcedConcreteBlock(Color.LIME);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_MAGENTA =
        registerReinforcedConcreteBlock(Color.MAGENTA);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_ORANGE =
        registerReinforcedConcreteBlock(Color.ORANGE);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_PINK =
        registerReinforcedConcreteBlock(Color.PINK);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_PURPLE =
        registerReinforcedConcreteBlock(Color.PURPLE);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_RED =
        registerReinforcedConcreteBlock(Color.RED);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_WHITE =
        registerReinforcedConcreteBlock(Color.WHITE);
    public static final BlockEntry<ReinforcedConcreteBlock> REINFORCED_CONCRETE_YELLOW =
        registerReinforcedConcreteBlock(Color.YELLOW);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_BLACK_SLAB =
        registerReinforcedConcreteSlabBlock(Color.BLACK, REINFORCED_CONCRETE_BLACK);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_BLUE_SLAB =
        registerReinforcedConcreteSlabBlock(Color.BLUE, REINFORCED_CONCRETE_BLUE);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_BROWN_SLAB =
        registerReinforcedConcreteSlabBlock(Color.BROWN, REINFORCED_CONCRETE_BROWN);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_CYAN_SLAB =
        registerReinforcedConcreteSlabBlock(Color.CYAN, REINFORCED_CONCRETE_CYAN);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_GRAY_SLAB =
        registerReinforcedConcreteSlabBlock(Color.GRAY, REINFORCED_CONCRETE_GRAY);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_GREEN_SLAB =
        registerReinforcedConcreteSlabBlock(Color.GREEN, REINFORCED_CONCRETE_GREEN);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_LIGHT_BLUE_SLAB =
        registerReinforcedConcreteSlabBlock(Color.LIGHT_BLUE, REINFORCED_CONCRETE_LIGHT_BLUE);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_LIGHT_GRAY_SLAB =
        registerReinforcedConcreteSlabBlock(Color.LIGHT_GRAY, REINFORCED_CONCRETE_LIGHT_GRAY);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_LIME_SLAB =
        registerReinforcedConcreteSlabBlock(Color.LIME, REINFORCED_CONCRETE_LIME);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_MAGENTA_SLAB =
        registerReinforcedConcreteSlabBlock(Color.MAGENTA, REINFORCED_CONCRETE_MAGENTA);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_ORANGE_SLAB =
        registerReinforcedConcreteSlabBlock(Color.ORANGE, REINFORCED_CONCRETE_ORANGE);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_PINK_SLAB =
        registerReinforcedConcreteSlabBlock(Color.PINK, REINFORCED_CONCRETE_PINK);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_PURPLE_SLAB =
        registerReinforcedConcreteSlabBlock(Color.PURPLE, REINFORCED_CONCRETE_PURPLE);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_RED_SLAB =
        registerReinforcedConcreteSlabBlock(Color.RED, REINFORCED_CONCRETE_RED);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_WHITE_SLAB =
        registerReinforcedConcreteSlabBlock(Color.WHITE, REINFORCED_CONCRETE_WHITE);
    public static final BlockEntry<SlabBlock> REINFORCED_CONCRETE_YELLOW_SLAB =
        registerReinforcedConcreteSlabBlock(Color.YELLOW, REINFORCED_CONCRETE_YELLOW);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_BLACK_STAIR =
        registerReinforcedConcreteStairBlock(Color.BLACK, REINFORCED_CONCRETE_BLACK);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_BLUE_STAIR =
        registerReinforcedConcreteStairBlock(Color.BLUE, REINFORCED_CONCRETE_BLUE);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_BROWN_STAIR =
        registerReinforcedConcreteStairBlock(Color.BROWN, REINFORCED_CONCRETE_BROWN);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_CYAN_STAIR =
        registerReinforcedConcreteStairBlock(Color.CYAN, REINFORCED_CONCRETE_CYAN);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_GRAY_STAIR =
        registerReinforcedConcreteStairBlock(Color.GRAY, REINFORCED_CONCRETE_GRAY);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_GREEN_STAIR =
        registerReinforcedConcreteStairBlock(Color.GREEN, REINFORCED_CONCRETE_GREEN);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_LIGHT_BLUE_STAIR =
        registerReinforcedConcreteStairBlock(Color.LIGHT_BLUE, REINFORCED_CONCRETE_LIGHT_BLUE);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_LIGHT_GRAY_STAIR =
        registerReinforcedConcreteStairBlock(Color.LIGHT_GRAY, REINFORCED_CONCRETE_LIGHT_GRAY);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_LIME_STAIR =
        registerReinforcedConcreteStairBlock(Color.LIME, REINFORCED_CONCRETE_LIME);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_MAGENTA_STAIR =
        registerReinforcedConcreteStairBlock(Color.MAGENTA, REINFORCED_CONCRETE_MAGENTA);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_ORANGE_STAIR =
        registerReinforcedConcreteStairBlock(Color.ORANGE, REINFORCED_CONCRETE_ORANGE);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_PINK_STAIR =
        registerReinforcedConcreteStairBlock(Color.PINK, REINFORCED_CONCRETE_PINK);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_PURPLE_STAIR =
        registerReinforcedConcreteStairBlock(Color.PURPLE, REINFORCED_CONCRETE_PURPLE);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_RED_STAIR =
        registerReinforcedConcreteStairBlock(Color.RED, REINFORCED_CONCRETE_RED);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_WHITE_STAIR =
        registerReinforcedConcreteStairBlock(Color.WHITE, REINFORCED_CONCRETE_WHITE);
    public static final BlockEntry<StairBlock> REINFORCED_CONCRETE_YELLOW_STAIR =
        registerReinforcedConcreteStairBlock(Color.YELLOW, REINFORCED_CONCRETE_YELLOW);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_BLACK_WALL =
        registerReinforcedConcreteWallBlock(Color.BLACK, REINFORCED_CONCRETE_BLACK);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_BLUE_WALL =
        registerReinforcedConcreteWallBlock(Color.BLUE, REINFORCED_CONCRETE_BLUE);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_BROWN_WALL =
        registerReinforcedConcreteWallBlock(Color.BROWN, REINFORCED_CONCRETE_BROWN);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_CYAN_WALL =
        registerReinforcedConcreteWallBlock(Color.CYAN, REINFORCED_CONCRETE_CYAN);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_GRAY_WALL =
        registerReinforcedConcreteWallBlock(Color.GRAY, REINFORCED_CONCRETE_GRAY);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_GREEN_WALL =
        registerReinforcedConcreteWallBlock(Color.GREEN, REINFORCED_CONCRETE_GREEN);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_LIGHT_BLUE_WALL =
        registerReinforcedConcreteWallBlock(Color.LIGHT_BLUE, REINFORCED_CONCRETE_LIGHT_BLUE);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_LIGHT_GRAY_WALL =
        registerReinforcedConcreteWallBlock(Color.LIGHT_GRAY, REINFORCED_CONCRETE_LIGHT_GRAY);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_LIME_WALL =
        registerReinforcedConcreteWallBlock(Color.LIME, REINFORCED_CONCRETE_LIME);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_MAGENTA_WALL =
        registerReinforcedConcreteWallBlock(Color.MAGENTA, REINFORCED_CONCRETE_MAGENTA);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_ORANGE_WALL =
        registerReinforcedConcreteWallBlock(Color.ORANGE, REINFORCED_CONCRETE_ORANGE);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_PINK_WALL =
        registerReinforcedConcreteWallBlock(Color.PINK, REINFORCED_CONCRETE_PINK);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_PURPLE_WALL =
        registerReinforcedConcreteWallBlock(Color.PURPLE, REINFORCED_CONCRETE_PURPLE);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_RED_WALL =
        registerReinforcedConcreteWallBlock(Color.RED, REINFORCED_CONCRETE_RED);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_WHITE_WALL =
        registerReinforcedConcreteWallBlock(Color.WHITE, REINFORCED_CONCRETE_WHITE);
    public static final BlockEntry<WallBlock> REINFORCED_CONCRETE_YELLOW_WALL =
        registerReinforcedConcreteWallBlock(Color.YELLOW, REINFORCED_CONCRETE_YELLOW);

    public static final BlockEntry<Block> HEATED_NETHERITE = REGISTRATE
        .block("heated_netherite_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<Block> HEATED_TUNGSTEN = REGISTRATE
        .block("heated_tungsten_block", Block::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<RedhotMetalBlock> REDHOT_NETHERITE = REGISTRATE
        .block("redhot_netherite_block", RedhotMetalBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 3))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<RedhotMetalBlock> REDHOT_TUNGSTEN = REGISTRATE
        .block("redhot_tungsten_block", RedhotMetalBlock::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 3))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<GlowingMetalBlock> GLOWING_NETHERITE = REGISTRATE
        .block("glowing_netherite_block", GlowingMetalBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 7))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<GlowingMetalBlock> GLOWING_TUNGSTEN = REGISTRATE
        .block("glowing_tungsten_block", GlowingMetalBlock::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 7))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<IncandescentMetalBlock> INCANDESCENT_NETHERITE = REGISTRATE
        .block("incandescent_netherite_block", IncandescentMetalBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 15))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<IncandescentMetalBlock> INCANDESCENT_TUNGSTEN = REGISTRATE
        .block("incandescent_tungsten_block", IncandescentMetalBlock::new)
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 15))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<MineralFountainBlock> MINERAL_FOUNTAIN = REGISTRATE
        .block("mineral_fountain", MineralFountainBlock::new)
        .initialProperties(() -> Blocks.BEDROCK)
        .simpleItem()
        .blockstate((context, provider) -> provider.simpleBlock(context.get(),
            DangerUtil.genConfiguredModel("block/mineral_fountain").get()))
        .loot((tables, block) -> tables.dropOther(block, Items.AIR))
        .register();

    public static final BlockEntry<ImpactPileBlock> IMPACT_PILE = REGISTRATE
        .block("impact_pile", ImpactPileBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((context, provider) -> provider.simpleBlock(context.get(),
            DangerUtil.genConfiguredModel("block/impact_pile").get()))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern(" A ")
            .pattern(" B ")
            .pattern(" B ")
            .define('A', Blocks.OBSIDIAN)
            .define('B', Items.NETHERITE_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.OBSIDIAN),
                AnvilCraftDatagen.has(Blocks.OBSIDIAN))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.NETHERITE_INGOT),
                AnvilCraftDatagen.has(Items.NETHERITE_INGOT))
            .save(provider))
        .simpleItem()
        .register();

    public static final BlockEntry<RubyPrismBlock> RUBY_PRISM = REGISTRATE
        .block("ruby_prism", RubyPrismBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ACA")
            .pattern("CBC")
            .pattern("AAA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', ModBlocks.RUBY_BLOCK)
            .define('C', ModItems.RUBY)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RUBY),
                AnvilCraftDatagen.has(ModItems.RUBY)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<RubyLaserBlock> RUBY_LASER = REGISTRATE
        .block("ruby_laser", RubyLaserBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(it -> {
            if (it.getValue(RubyLaserBlock.SWITCH) == Switch.ON)
                return 15;
            else return 0;
        }))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ACA")
            .pattern("BDB")
            .pattern("ACA")
            .define('A', ModItems.ROYAL_STEEL_INGOT)
            .define('B', ModBlocks.INDUCTION_LIGHT)
            .define('C', ModItems.SILVER_INGOT)
            .define('D', ModBlocks.RUBY_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ROYAL_STEEL_INGOT),
                AnvilCraftDatagen.has(ModItems.ROYAL_STEEL_INGOT)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.INDUCTION_LIGHT),
                AnvilCraftDatagen.has(ModBlocks.INDUCTION_LIGHT)
            )
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.SILVER_INGOT),
                AnvilCraftDatagen.has(ModItems.SILVER_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.RUBY_BLOCK),
                AnvilCraftDatagen.has(ModBlocks.RUBY_BLOCK)
            )
            .save(provider)
        )
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();


    public static final BlockEntry<Block> RAW_ZINC = REGISTRATE
        .block("raw_zinc_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_ZINC)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_ZINC),
                AnvilCraftDatagen.has(ModItems.RAW_ZINC)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> RAW_TIN = REGISTRATE
        .block("raw_tin_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TIN)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_TIN),
                AnvilCraftDatagen.has(ModItems.RAW_TIN)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> RAW_TITANIUM = REGISTRATE
        .block("raw_titanium_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TITANIUM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_TITANIUM),
                AnvilCraftDatagen.has(ModItems.RAW_TITANIUM)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> RAW_TUNGSTEN = REGISTRATE
        .block("raw_tungsten_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_TUNGSTEN)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_TUNGSTEN),
                AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> RAW_LEAD = REGISTRATE
        .block("raw_lead_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_LEAD)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_LEAD),
                AnvilCraftDatagen.has(ModItems.RAW_LEAD)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> RAW_SILVER = REGISTRATE
        .block("raw_silver_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_SILVER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_SILVER),
                AnvilCraftDatagen.has(ModItems.RAW_SILVER)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> RAW_URANIUM = REGISTRATE
        .block("raw_uranium_block", Block::new)
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RAW_URANIUM)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.RAW_URANIUM),
                AnvilCraftDatagen.has(ModItems.RAW_URANIUM)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_ZINC_ORE = REGISTRATE
        .block("deepslate_zinc_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_ZINC.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_TIN_ORE = REGISTRATE
        .block("deepslate_tin_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TIN.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_TITANIUM_ORE = REGISTRATE
        .block("deepslate_titanium_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TITANIUM.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_TUNGSTEN_ORE = REGISTRATE
        .block("deepslate_tungsten_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TUNGSTEN.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_LEAD_ORE = REGISTRATE
        .block("deepslate_lead_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_LEAD.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_SILVER_ORE = REGISTRATE
        .block("deepslate_silver_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_SILVER.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<Block> DEEPSLATE_URANIUM_ORE = REGISTRATE
        .block("deepslate_uranium_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_URANIUM.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<Block> VOID_STONE = REGISTRATE
        .block("void_stone", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .simpleItem()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.VOID_MATTER.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .tag(BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<Block> VOID_MATTER_BLOCK = REGISTRATE
        .block("void_matter_block", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((context, provider) -> provider.simpleBlock(context.get(),
            DangerUtil.genConfiguredModel("block/void_matter_block").get()))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.VOID_MATTER)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.VOID_MATTER),
                AnvilCraftDatagen.has(ModItems.VOID_MATTER)
            )
            .save(provider)
        )
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .tag(BlockTags.NEEDS_DIAMOND_TOOL)
        .register();
    public static final BlockEntry<Block> EARTH_CORE_SHARD_ORE = REGISTRATE
        .block("earth_core_shard_ore", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(properties -> properties.explosionResistance(1200))
        .simpleItem()
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.EARTH_CORE_SHARD.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .tag(BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<Block> EARTH_CORE_SHARD_BLOCK = REGISTRATE
        .block("earth_core_shard_block", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(properties -> properties.explosionResistance(1200))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.EARTH_CORE_SHARD)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.EARTH_CORE_SHARD),
                AnvilCraftDatagen.has(ModItems.EARTH_CORE_SHARD)
            )
            .save(provider)
        )
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .tag(BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    private static @NotNull BlockEntry<ReinforcedConcreteBlock> registerReinforcedConcreteBlock(@NotNull Color color) {
        return REGISTRATE
            .block("reinforced_concrete_" + color, ReinforcedConcreteBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.explosionResistance(15.0f))
            .simpleItem()
            .blockstate((ctx, provider) -> {
                provider.models().getBuilder("reinforced_concrete_" + color)
                    .parent(DangerUtil.genUncheckedModelFile("minecraft", "block/cube_all").get())
                    .texture("all", "block/reinforced_concrete_" + color);
                provider.models().getBuilder("reinforced_concrete_top_" + color)
                    .parent(DangerUtil.genUncheckedModelFile("minecraft", "block/cube_column").get())
                    .texture("end", "block/reinforced_concrete_" + color)
                    .texture("side", "block/reinforced_concrete_" + color + "_top");
                provider.models().getBuilder("reinforced_concrete_bottom_" + color)
                    .parent(DangerUtil.genUncheckedModelFile("minecraft", "block/cube_column").get())
                    .texture("end", "block/reinforced_concrete_" + color)
                    .texture("side", "block/reinforced_concrete_" + color + "_bottom");
                provider.getVariantBuilder(ctx.get()).forAllStates(
                    blockState -> switch (blockState.getValue(ReinforcedConcreteBlock.HALF)) {
                        case TOP -> DangerUtil.genConfiguredModel("block/reinforced_concrete_top_" + color).get();
                        case SINGLE -> DangerUtil.genConfiguredModel("block/reinforced_concrete_" + color).get();
                        case BOTTOM -> DangerUtil.genConfiguredModel("block/reinforced_concrete_bottom_" + color).get();
                    }
                );
            })
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .register();
    }


    private static @NotNull BlockEntry<SlabBlock> registerReinforcedConcreteSlabBlock(
        @NotNull Color color,
        BlockEntry<ReinforcedConcreteBlock> parent
    ) {
        return REGISTRATE
            .block("reinforced_concrete_" + color + "_slab", SlabBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.explosionResistance(15.0f))
            .simpleItem()
            .blockstate((ctx, provider) -> provider.slabBlock(ctx.get(),
                AnvilCraft.of("block/reinforced_concrete_" + color),
                AnvilCraft.of("block/reinforced_concrete_" + color)))
            .loot((tables, block) -> tables.add(block, tables::createSlabItemTable))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 6)
                    .pattern("AAA")
                    .define('A', parent)
                    .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()),
                        AnvilCraftDatagen.has(parent))
                    .save(provider, AnvilCraft.of("craft/reinforced_concrete_" + color + "_slab"));
                VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(), parent);
            })
            .register();
    }

    private static @NotNull BlockEntry<StairBlock> registerReinforcedConcreteStairBlock(
        @NotNull Color color,
        BlockEntry<ReinforcedConcreteBlock> parent
    ) {
        return REGISTRATE
            .block("reinforced_concrete_" + color + "_stair", (properties) ->
                new StairBlock(parent.getDefaultState(), properties))
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.explosionResistance(15.0f))
            .simpleItem()
            .blockstate((ctx, provider) -> provider.stairsBlock(ctx.get(),
                AnvilCraft.of("block/reinforced_concrete_" + color)))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                    .pattern("A  ")
                    .pattern("AA ")
                    .pattern("AAA")
                    .define('A', parent)
                    .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()),
                        AnvilCraftDatagen.has(parent))
                    .save(provider, AnvilCraft.of("craft/reinforced_concrete_" + color + "_stair"));
                VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(), parent);
            })
            .register();
    }

    private static @NotNull BlockEntry<WallBlock> registerReinforcedConcreteWallBlock(
        @NotNull Color color,
        BlockEntry<ReinforcedConcreteBlock> parent
    ) {
        return REGISTRATE
            .block("reinforced_concrete_" + color + "_wall", WallBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.explosionResistance(15.0f))
            .blockstate((ctx, provider) -> provider.wallBlock(ctx.get(),
                AnvilCraft.of("block/reinforced_concrete_" + color + "_wall")))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.WALLS)
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                    .pattern("AAA")
                    .pattern("AAA")
                    .define('A', parent)
                    .unlockedBy(AnvilCraftDatagen.hasItem(parent.asItem()),
                        AnvilCraftDatagen.has(parent))
                    .save(provider, AnvilCraft.of("craft/reinforced_concrete_" + color + "_wall"));
                VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
                    ctx.get(), parent);
            })
            .item()
            .model((ctx, provide) -> provide.wallInventory(
                "reinforced_concrete_" + color + "_wall",
                AnvilCraft.of("block/reinforced_concrete_" + color + "_wall")
            ))
            .build()
            .register();
    }

    public static void register() {
    }

    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos, EntityType<?> entity) {
        return false;
    }

    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }
}
