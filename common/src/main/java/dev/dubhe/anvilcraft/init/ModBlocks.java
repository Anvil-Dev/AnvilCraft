package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.block.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModBlocks {
    public static final BlockEntry<? extends Block> STAMPING_PLATFORM = REGISTRATE
            .block("stamping_platform", StampingPlatformBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();

    public static final BlockEntry<? extends Block> ROYAL_ANVIL = REGISTRATE
            .block("royal_anvil", RoyalAnvilBlock::new)
            .initialProperties(() -> Blocks.ANVIL)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> MAGNET_BLOCK = REGISTRATE
            .block("magnet_block", MagnetBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> HOLLOW_MAGNET_BLOCK = REGISTRATE
            .block("hollow_magnet_block", HollowMagnetBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> FERRITE_CORE_MAGNET_BLOCK = REGISTRATE
            .block("ferrite_core_magnet_block", FerriteCoreMagnetBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(BlockBehaviour.Properties::randomTicks)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> CHUTE = REGISTRATE
            .block("chute", ChuteBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> AUTO_CRAFTER = REGISTRATE
            .block("auto_crafter", AutoCrafterBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> ROYAL_GRINDSTONE = REGISTRATE
            .block("royal_grindstone", RoyalGrindstone::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> ROYAL_SMITHING_TABLE = REGISTRATE
            .block("royal_smithing_table", RoyalSmithingTableBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> ROYAL_STEEL_BLOCK = REGISTRATE
            .block("royal_steel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> SMOOTH_ROYAL_STEEL_BLOCK = REGISTRATE
            .block("smooth_royal_steel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_BLOCK = REGISTRATE
            .block("cut_royal_steel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_SLAB = REGISTRATE
            .block("cut_royal_steel_slab", SlabBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_STAIRS = REGISTRATE
            .block("cut_royal_steel_stairs", (properties) -> new StairBlock(ModBlocks.CUT_ROYAL_STEEL_BLOCK.getDefaultState(), properties))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> LAVA_CAULDRON = REGISTRATE
            .block("lava_cauldron", LavaCauldronBlock::new)
            .initialProperties(() -> Blocks.LAVA_CAULDRON)
            .properties(properties -> properties.lightLevel(blockState -> blockState.getValue(LayeredCauldronBlock.LEVEL) * 5))
            .simpleItem()
            .register();
    public static final BlockEntry<? extends Block> CURSED_GOLD_BLOCK = REGISTRATE
            .block("cursed_gold_block", Block::new)
            .initialProperties(() -> Blocks.GOLD_BLOCK)
            .simpleItem()
            .register();

    public static void register() {
    }
}
