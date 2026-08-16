package dev.dubhe.anvilcraft.event.giantanvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.GiantAnvilEvent;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.multiblock.IMultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GiantAnvilLandingEventListener {
    private static final int MIN_MULTIBLOCK_SIZE = 3;
    private static final int MAX_MULTIBLOCK_SIZE = 15;

    @SubscribeEvent
    public static void handleMultiblock(AnvilEvent.GiantOnLand event) {
        if (NeoForge.EVENT_BUS.post(new GiantAnvilEvent.Multiblock(event)).isCanceled()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos landPos = event.getPos().below(2);

        int size = GiantAnvilLandingEventListener.findCraftingTableSize(landPos, level);
        if (size < MIN_MULTIBLOCK_SIZE || size > MAX_MULTIBLOCK_SIZE) {
            return;
        }

        BlockPos inputCorner = landPos.offset(-size / 2, -size, -size / 2);
        MultiblockInput input = GiantAnvilLandingEventListener.buildInput(level, inputCorner, size, landPos);
        BlockState centerState = level.getBlockState(landPos);

        GiantAnvilLandingEventListener.craft(
            level,
            landPos,
            inputCorner,
            input,
            centerState,
            ModRecipeTypes.MULTIBLOCK_4D_TYPE.get()
        );
        GiantAnvilLandingEventListener.craft(
            level,
            landPos,
            inputCorner,
            input,
            centerState,
            ModRecipeTypes.MULTIBLOCK_TYPE.get()
        );
        GiantAnvilLandingEventListener.craft(
            level,
            landPos,
            inputCorner,
            input,
            centerState,
            ModRecipeTypes.MULTIBLOCK_CONVERSION_TYPE.get()
        );
    }

    private static void craft(
        Level level,
        BlockPos landPos,
        BlockPos inputCorner,
        MultiblockInput input,
        BlockState centerState,
        RecipeType<? extends IMultiblockRecipe> type
    ) {
        if (!GiantAnvilLandingEventListener.isValidCenter(level, landPos, centerState, type)) {
            return;
        }
        level.getRecipeManager()
            .getRecipeFor(type, input, level)
            .ifPresent(holder -> holder.value().assemble(level, landPos, inputCorner, input));
    }

    private static boolean isValidCenter(
        Level level,
        BlockPos pos,
        BlockState state,
        RecipeType<? extends IMultiblockRecipe> type
    ) {
        return level.getRecipeManager().getAllRecipesFor(type).stream()
            .anyMatch(holder -> holder.value().isValidCenterBlock(level, pos, state));
    }

    private static MultiblockInput buildInput(Level level, BlockPos inputCorner, int size, BlockPos centerPos) {
        List<List<List<BlockState>>> blocks = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            List<List<BlockState>> blocksY = new ArrayList<>();
            for (int z = 0; z < size; z++) {
                List<BlockState> blocksZ = new ArrayList<>();
                for (int x = 0; x < size; x++) {
                    blocksZ.add(level.getBlockState(inputCorner.offset(x, y, z)));
                }
                blocksY.add(blocksZ);
            }
            blocks.add(blocksY);
        }
        return new MultiblockInput(blocks, size, centerPos);
    }

    private static int findCraftingTableSize(BlockPos centerPos, Level level) {
        int maxSize = 0;
        for (int size = MIN_MULTIBLOCK_SIZE; size <= MAX_MULTIBLOCK_SIZE; size += 2) {
            boolean flag = true;
            for (int x = -size / 2; x <= size / 2 && flag; x++) {
                for (int z = -size / 2; z <= size / 2 && flag; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
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
