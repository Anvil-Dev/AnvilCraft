package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CraftingMachineBlock;
import dev.dubhe.anvilcraft.block.FerriteCoreMagnetBlock;
import dev.dubhe.anvilcraft.block.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.InteractMachineBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ModBlocks {
    private static final Map<String, Block> BLOCK_MAP = new HashMap<>();

    public static final Block MAGNET_BLOCK = registerBlock("magnet_block", Block::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block HOLLOW_MAGNET_BLOCK = registerBlock("hollow_magnet_block", HollowMagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block FERRITE_CORE_MAGNET_BLOCK = registerBlock("ferrite_core_magnet_block", FerriteCoreMagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).randomTicks());
    public static final Block INTERACT_MACHINE = registerBlock("interact_machine", InteractMachineBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block CRAFTING_MACHINE = registerBlock("crafting_machine", CraftingMachineBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));

    public static void register(){
        for (Map.Entry<String, Block> entry : ModBlocks.BLOCK_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.BLOCK, AnvilCraft.of(entry.getKey()), entry.getValue());
        }
    }

    private static Block registerBlock(String id, @NotNull Function<BlockBehaviour.Properties, Block> blockCreator, BlockBehaviour.Properties properties) {
        Block block = blockCreator.apply(properties);
        BLOCK_MAP.put(id, block);
        return block;
    }
}
