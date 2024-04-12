package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.FerriteCoreMagnetBlock;
import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.block.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.block.RoyalGrindstone;
import dev.dubhe.anvilcraft.block.RoyalSmithingTableBlock;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import dev.dubhe.anvilcraft.block.StampingPlatformBlock;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.item.CursedBlockItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModBlocks {
    public static final BlockEntry<? extends Block> STAMPING_PLATFORM = REGISTRATE
            .block("stamping_platform", StampingPlatformBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .blockstate((ctx, provider) -> {
            })
            .simpleItem()
            .defaultLoot()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ctx.get())
                    .pattern("BAB")
                    .pattern("B B")
                    .pattern("B B")
                    .define('A', ModItemTags.IRON_PLATES)
                    .define('B', Items.IRON_INGOT)
                    .unlockedBy("has_" + ModItemTags.IRON_PLATES.location().getPath(),
                        AnvilCraftDatagen.has(ModItemTags.IRON_PLATES))
                    .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                    .save(provider))
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
                    .pattern("AAA")
                    .pattern("ABA")
                    .pattern("ACA")
                    .define('A', Items.IRON_INGOT)
                    .define('B', Items.CRAFTING_TABLE)
                    .define('C', Items.DROPPER)
                    .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
                    .unlockedBy(AnvilCraftDatagen.hasItem(Items.CRAFTING_TABLE),
                        AnvilCraftDatagen.has(Items.CRAFTING_TABLE))
                    .unlockedBy(AnvilCraftDatagen.hasItem(Items.DROPPER), AnvilCraftDatagen.has(Items.DROPPER))
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
            .simpleItem()
            .defaultLoot()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
            .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .pattern("AAA")
                    .pattern("AAA")
                    .pattern("AAA")
                    .define('A', ModItems.ROYAL_STEEL_INGOT)
                    .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                    .save(provider)
            )
            .register();
    public static final BlockEntry<? extends Block> SMOOTH_ROYAL_STEEL_BLOCK = REGISTRATE
            .block("smooth_royal_steel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .defaultLoot()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_BLOCK = REGISTRATE
            .block("cut_royal_steel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
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
                    .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT.get()),
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
                    .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.TOPAZ.get()), AnvilCraftDatagen.has(ModItems.TOPAZ))
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
                    .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY.get()), AnvilCraftDatagen.has(ModItems.RUBY))
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
                    .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SAPPHIRE.get()),
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
                    .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RESIN.get()),
                        AnvilCraftDatagen.has(ModItems.RESIN))
                    .save(provider))
            .register();
    public static final BlockEntry<? extends Block> AMBER_BLOCK = REGISTRATE
            .block("amber_block", HalfTransparentBlock::new)
            .initialProperties(() -> Blocks.EMERALD_BLOCK)
            .blockstate((ctx, provider) -> {
                provider.simpleBlock(ctx.get());
                provider.models().cubeAll(ctx.getName(), provider.modLoc("block/" + ctx.getName()))
                    .renderType("translucent");
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
                    .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.AMBER.get()), AnvilCraftDatagen.has(ModItems.AMBER))
                    .save(provider))
            .register();

    public static void register() {
    }
}
