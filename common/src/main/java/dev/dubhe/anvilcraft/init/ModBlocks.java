package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.CreativeGeneratorBlock;
import dev.dubhe.anvilcraft.block.FerriteCoreMagnetBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.JewelCraftingTable;
import dev.dubhe.anvilcraft.block.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.block.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.block.RoyalGrindstone;
import dev.dubhe.anvilcraft.block.RoyalSmithingTableBlock;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import dev.dubhe.anvilcraft.block.StampingPlatformBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Half;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.item.CursedBlockItem;
import dev.dubhe.anvilcraft.item.PlaceInWaterBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.GravelBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;
import static dev.dubhe.anvilcraft.api.power.IPowerComponent.SWITCH;

public class ModBlocks {
    public static final BlockEntry<? extends Block> STAMPING_PLATFORM = REGISTRATE
        .block("stamping_platform", StampingPlatformBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
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
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .save(provider);
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                .pattern("BAB")
                .pattern("B B")
                .pattern("B B")
                .define('A', ModItemTags.IRON_PLATES_FORGE)
                .define('B', Items.IRON_INGOT)
                .unlockedBy("has_" + ModItemTags.IRON_PLATES_FORGE.location().getPath(),
                    AnvilCraftDatagen.has(ModItemTags.IRON_PLATES_FORGE))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                .save(provider, BuiltInRegistries.ITEM.getKey(ctx.get().asItem()) + "_forge");
        })
        .register();
    public static final BlockEntry<? extends Block> CORRUPTED_BEACON = REGISTRATE
        .block("corrupted_beacon", CorruptedBeaconBlock::new)
        .initialProperties(() -> Blocks.BEACON)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_ANVIL = REGISTRATE
        .block("royal_anvil", RoyalAnvilBlock::new)
        .initialProperties(() -> Blocks.ANVIL)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.ANVIL, ModBlockTags.CANT_BROKEN_ANVIL, BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> MAGNET_BLOCK = REGISTRATE
        .block("magnet_block", MagnetBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
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
        .defaultLoot()
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
        .defaultLoot()
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
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModBlocks.CHUTE)
            .pattern("A A")
            .pattern("ABA")
            .pattern(" A ")
            .define('A', Items.IRON_INGOT)
            .define('B', Items.DROPPER)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
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
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
            .pattern("ABA")
            .pattern("CDA")
            .pattern("AEA")
            .define('A', Items.IRON_INGOT)
            .define('B', Items.CRAFTING_TABLE)
            .define('C', Items.DROPPER)
            .define('D', ModItems.MAGNETOELECTRIC_CORE)
            .define('E', ModItems.CIRCUIT_BOARD)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(Items.CRAFTING_TABLE),
                AnvilCraftDatagen.has(Items.CRAFTING_TABLE)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
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
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_SMITHING_TABLE = REGISTRATE
        .block("royal_smithing_table", RoyalSmithingTableBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> ROYAL_STEEL_BLOCK = REGISTRATE
        .block("royal_steel_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(provider);
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(ModItems.ROYAL_STEEL_INGOT)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.IRON_BLOCK)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.DIAMOND_BLOCK)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.AMETHYST_BLOCK)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.GEM_BLOCKS)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ctx.get().asItem(), 1)
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
                .unlockedBy(AnvilCraftDatagen.hasItem(Items.DIAMOND_BLOCK), AnvilCraftDatagen.has(Items.DIAMOND_BLOCK))
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.AMETHYST_BLOCK), AnvilCraftDatagen.has(Items.AMETHYST_BLOCK)
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
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_BLOCK = REGISTRATE
        .block("cut_royal_steel_block", Block::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .simpleItem()
        .defaultLoot()
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
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.explosionResistance(15.0F))
        .blockstate((ctx, provider) -> provider.stairsBlock(ctx.get(),
            AnvilCraft.of("block/cut_royal_steel_block")))
        .simpleItem()
        .defaultLoot()
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
        .defaultLoot()
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
    public static final BlockEntry<? extends Block> TOPAZ_BLOCK = REGISTRATE
        .block("topaz_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.TOPAZ)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TOPAZ), AnvilCraftDatagen.has(ModItems.TOPAZ))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> RUBY_BLOCK = REGISTRATE
        .block("ruby_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.RUBY)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY), AnvilCraftDatagen.has(ModItems.RUBY))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> SAPPHIRE_BLOCK = REGISTRATE
        .block("sapphire_block", Block::new)
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .simpleItem()
        .defaultLoot()
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
        .block("resin_block", SlimeBlock::new)
        .initialProperties(() -> Blocks.SLIME_BLOCK)
        .blockstate((ctx, provider) -> {
        })
        .properties(properties -> properties.sound(SoundType.HONEY_BLOCK))
        .simpleItem()
        .defaultLoot()
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
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ModItems.AMBER)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.AMBER), AnvilCraftDatagen.has(ModItems.AMBER))
            .save(provider))
        .register();
    public static final BlockEntry<? extends Block> CREATIVE_GENERATOR = REGISTRATE
        .block("creative_generator", CreativeGeneratorBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> HEATER = REGISTRATE
        .block("heater", HeaterBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.noOcclusion().lightLevel(state -> state.getValue(OVERLOAD) ? 0 : 15))
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABA")
            .pattern("BCB")
            .pattern("BBB")
            .define('A', Items.TERRACOTTA)
            .define('B', Items.IRON_INGOT)
            .define('C', ModItems.MAGNETOELECTRIC_CORE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.TERRACOTTA), AnvilCraftDatagen.has(Items.TERRACOTTA))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
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
                    if (state.getValue(TransmissionPoleBlock.HALF) != Half.TOP) return 0;
                    if (state.getValue(SWITCH) == IPowerComponent.Switch.OFF) return 0;
                    if (state.getValue(OVERLOAD)) return 6;
                    return 15;
                }
            )
        )
        .blockstate((ctx, provider) -> {
        })
        .item()
        .model((ctx, provider) -> {
        })
        .build()
        .defaultLoot()
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
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.LIGHTNING_ROD), AnvilCraftDatagen.has(Items.LIGHTNING_ROD))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
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
        .defaultLoot()
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
            .defaultLoot()
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .register();
    public static final BlockEntry<? extends Block> QUARTZ_SAND = REGISTRATE
            .block("quartz_sand", GravelBlock::new)
            .initialProperties(() -> Blocks.SAND)
            .simpleItem()
            .defaultLoot()
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .register();
    public static final BlockEntry<? extends Block> TEMPERING_GLASS = REGISTRATE
            .block("tempering_glass", GlassBlock::new)
            .initialProperties(() -> Blocks.GLASS)
            .properties(properties -> properties.explosionResistance(15.0F))
            .blockstate((ctx, provider) -> {
                provider.simpleBlock(ctx.get());
                provider.models().cubeAll(ctx.getName(), provider.modLoc("block/" + ctx.getName()))
                        .renderType("translucent");
            })
            .simpleItem()
            .defaultLoot()
            .register();

    public static final BlockEntry<JewelCraftingTable> JEWEL_CRAFTING_TABLE = REGISTRATE
        .block("jewelcrafting_table", JewelCraftingTable::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate((ctx, provider) -> {
        })
        .simpleItem()
        .defaultLoot()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .lang("Jewel Crafting Table")
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("ABC")
            .pattern("DED")
            .pattern("F F")
            .define('A', Items.DIAMOND)
            .define('B', Blocks.GLASS)
            .define('C', Blocks.GRINDSTONE)
            .define('D', Blocks.SMOOTH_STONE)
            .define('E', Blocks.STONECUTTER)
            .define('F', ItemTags.PLANKS)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.GRINDSTONE), AnvilCraftDatagen.has(Blocks.GRINDSTONE))
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.STONECUTTER), AnvilCraftDatagen.has(Blocks.STONECUTTER))
            .save(provider))
        .register();

    public static void register() {
    }
}
