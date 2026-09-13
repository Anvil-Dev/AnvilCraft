package dev.dubhe.anvilcraft.event.giantanvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GiantAnvilLandingEventListener {
    private static final int MIN_MULTIBLOCK_SIZE = 3;
    private static final int MAX_MULTIBLOCK_SIZE = 15;

    @SubscribeEvent
    public static void handleMultiblock(AnvilEvent.GiantOnLand event) {
        Level level = event.getLevel();
        BlockPos landPos = event.getPos().below(2);

        BlockState centerState = level.getBlockState(landPos);
        boolean overCompressorDetected = false;
        if (centerState.is(ModBlocks.SPACE_OVERCOMPRESSOR)) {
            overCompressorDetected = true;
        } else if (!centerState.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
            return;
        }
        int size = GiantAnvilLandingEventListener.findCraftingTableSize(landPos, level);
        if (size < 3 || size > 15) return;

        BlockPos inputCorner = landPos.offset(-size / 2, -size, -size / 2);

        List<List<List<BlockState>>> blocks = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            List<List<BlockState>> blocksY = new ArrayList<>();
            for (int z = 0; z < size; z++) {
                List<BlockState> blocksZ = new ArrayList<>();
                for (int x = 0; x < size; x++) {
                    BlockState state = level.getBlockState(inputCorner.offset(x, y, z));
                    blocksZ.add(state);
                }
                blocksY.add(blocksZ);
            }
            blocks.add(blocksY);
        }
        MultiblockInput input = new MultiblockInput(blocks, size, landPos);
        if (overCompressorDetected) {
            level.getServer().getRecipeManager().getRecipeFor(ModRecipeTypes.MULTIBLOCK.get(), input, level)
                .ifPresent(recipe -> recipe.value().assemble(level, landPos, inputCorner, input));
        } else {
            level.getServer().getRecipeManager().getRecipeFor(ModRecipeTypes.MULTIBLOCK_CONVERSION.get(), input, level)
                .ifPresent(recipe -> recipe.value().assemble(level, landPos, inputCorner, input));
        }
    }

    private static int findCraftingTableSize(BlockPos centerPos, Level level) {
        int maxSize = 0;
        for (
            int size = GiantAnvilLandingEventListener.MIN_MULTIBLOCK_SIZE;
            size <= GiantAnvilLandingEventListener.MAX_MULTIBLOCK_SIZE;
            size += 2
        ) {
            boolean flag = true;
            for (int x = -size / 2; x <= size / 2 && flag; x++) {
                for (int z = -size / 2; z <= size / 2 && flag; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos pos = centerPos.offset(x, 0, z);
                    if (!level.getBlockState(pos).is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
                        flag = false;
                    }
                }
            }
            if (flag) {
                maxSize = size;
            } else {
                break;
            }
        }
        return maxSize;
    }
}
