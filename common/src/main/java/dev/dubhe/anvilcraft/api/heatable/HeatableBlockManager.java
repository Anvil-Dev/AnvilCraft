package dev.dubhe.anvilcraft.api.heatable;

import dev.dubhe.anvilcraft.block.RedhotMetalBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;

public class HeatableBlockManager {
    private static final HashMap<Block, Block> heatableBlockMap = new HashMap<>();
    
    public static void register(Block block, RedhotMetalBlock hotBlock) {
        heatableBlockMap.put(block, hotBlock);
    }

    public static Block getHotBlock(Block block) {
        if (!heatableBlockMap.containsKey(block)) return null;
        return heatableBlockMap.get(block);
    }

    static {
        register(Blocks.NETHERITE_BLOCK, ModBlocks.REDHOT_NETHERITE.get());
        register(ModBlocks.HEATED_NETHERITE.get(), ModBlocks.REDHOT_NETHERITE.get());
        register(ModBlocks.TUNGSTEN_BLOCK.get(), ModBlocks.REDHOT_TUNGSTEN.get());
        register(ModBlocks.HEATED_TUNGSTEN.get(), ModBlocks.REDHOT_TUNGSTEN.get());
    }
}
