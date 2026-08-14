package dev.dubhe.anvilcraft.event.giantanvil;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.GiantAnvilEvent;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
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

        BlockState centerState = level.getBlockState(landPos);
        if (centerState.is(ModBlocks.SPACETIME_SUPERCOMPUTER)) {
            GiantAnvilLandingEventListener.handle4DCrafting(level, landPos);
            return;
        }
        int size = GiantAnvilLandingEventListener.findCraftingTableSize(landPos, level);
        if (size < MIN_MULTIBLOCK_SIZE || size > MAX_MULTIBLOCK_SIZE) {
            return;
        }

        BlockPos inputCorner = landPos.offset(-size / 2, -size, -size / 2);
        MultiblockInput input = GiantAnvilLandingEventListener.buildInput(level, inputCorner, size);

        if (centerState.is(ModBlocks.SPACE_OVERCOMPRESSOR)) {
            level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.MULTIBLOCK_TYPE.get(), input, level)
                .ifPresent(holder -> holder.value().assemble(level, landPos, inputCorner, input));
        } else if (centerState.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
            level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.MULTIBLOCK_CONVERSION_TYPE.get(), input, level)
                .ifPresent(holder -> holder.value().assemble(level, landPos, inputCorner, input));
        }
    }

    private static void handle4DCrafting(Level level, BlockPos landPos) {
        AnvilCraft.LOGGER.info("[4D] handle4DCrafting at {}", landPos);
        int size = GiantAnvilLandingEventListener.findCraftingTableSize(landPos, level);
        AnvilCraft.LOGGER.info("[4D] crafting table size: {}", size);
        if (size < MIN_MULTIBLOCK_SIZE || size > MAX_MULTIBLOCK_SIZE) {
            return;
        }
        BlockPos inputCorner = landPos.offset(-size / 2, -size, -size / 2);
        if (!(level.getBlockEntity(landPos) instanceof SpacetimeSupercomputerBlockEntity supercomputer)) {
            AnvilCraft.LOGGER.info("[4D] no supercomputer BE at {}", landPos);
            return;
        }
        MultiblockInput input = GiantAnvilLandingEventListener.buildInput(level, inputCorner, size);

        RecipeHolder<Multiblock4DRecipe> processing = supercomputer.getProcessingRecipe();
        if (processing == null) {
            level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.MULTIBLOCK_4D_TYPE.get(), input, level)
                .ifPresentOrElse(holder -> {
                    Multiblock4DRecipe recipe = holder.value();
                    AnvilCraft.LOGGER.info("[4D] found recipe {}, {} defs", holder.id(), recipe.getDefinitions().size());
                    MultiblockUtil.match(recipe.getDefinitions().getFirst(), input, level)
                        .ifPresent(rotation -> {
                            List<ItemStack> consumed = MultiblockUtil.consume(
                                level, recipe.getDefinitions().getFirst(), input, inputCorner, rotation);
                            if (recipe.getDefinitions().size() == 1) {
                                AnvilUtil.dropItems(
                                    List.of(recipe.getResult().copy()), level, landPos.below().getCenter());
                                return;
                            }
                            supercomputer.addPendingDrops(consumed);
                            supercomputer.setProcessingRecipe(holder);
                            supercomputer.setProcessingStep(1);
                            supercomputer.setProcessingSize(size);
                        });
                }, () -> AnvilCraft.LOGGER.info("[4D] no 4D recipe matched"));
            return;
        }
        Multiblock4DRecipe recipe = processing.value();
        List<MultiblockDefinition> defs = recipe.getDefinitions();
        int step = supercomputer.getProcessingStep();
        AnvilCraft.LOGGER.info("[4D] processing step {} of {} defs", step, defs.size());
        if (step < 0 || step >= defs.size()) {
            return;
        }
        MultiblockUtil.match(defs.get(step), input, level).ifPresent(rotation -> {
            AnvilCraft.LOGGER.info("[4D] step {} matched, rotation {}", step, rotation);
            List<ItemStack> consumed = MultiblockUtil.consume(level, defs.get(step), input, inputCorner, rotation);
            int next = step + 1;
            supercomputer.setProcessingStep(next);
            if (next >= defs.size()) {
                AnvilUtil.dropItems(List.of(recipe.getResult().copy()), level, landPos.below().getCenter());
                supercomputer.clearPendingDrops();
                supercomputer.setProcessingRecipe(null);
                supercomputer.setProcessingStep(-1);
                supercomputer.setProcessingSize(-1);
            } else {
                supercomputer.addPendingDrops(consumed);
            }
        });
    }

    private static MultiblockInput buildInput(Level level, BlockPos inputCorner, int size) {
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
        return new MultiblockInput(blocks, size);
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
