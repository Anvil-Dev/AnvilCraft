package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProceduralProcessStepManager {
    public static Map<Block, List<ProceduralProcessStep>> PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY = new HashMap<>();
    public static Set<Block> PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY = new HashSet<>();
    public static final int WIP_BLOCK_DETECTION_DEPTH = 2;
    private static RecipeMap initializedRecipes;

    public static void initialize(RecipeMap recipes) {
        ProceduralProcessStepManager.initialize(List.copyOf(recipes.values()));
        ProceduralProcessStepManager.initializedRecipes = recipes;
    }

    public static void initialize(List<RecipeHolder<?>> recipeHolders) {
        ProceduralProcessStepManager.PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY = new HashMap<>();
        ProceduralProcessStepManager.PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY = new HashSet<>();
        for (RecipeHolder<?> holder : recipeHolders) {
            if (holder.value() instanceof ProceduralProcessRecipe recipe) {
                Identifier rl = holder.id().identifier();
                List<ProceduralProcessStep> steps = recipe.steps();
                for (int index = 0; index < steps.size(); index++) {
                    ProceduralProcessStep step = steps.get(index);
                    step.setStepIndex(index);
                    step.setPpRecipeId(rl);
                    ProceduralProcessStepManager.addStep(step);
                }
                if (recipe.multiLoopFirstStep().isPresent()) {
                    ProceduralProcessStep step = recipe.multiLoopFirstStep().get();
                    step.setStepIndex(0);
                    step.setPpRecipeId(rl);
                    ProceduralProcessStepManager.addStep(step, false);
                }
            }
        }
    }

    public static void addStep(ProceduralProcessStep step, boolean fillsFirstStepInquiry) {
        if (step.getContent() instanceof AbstractProcessRecipe<?> apr) {
            HolderSet<Block> contactBlocks = apr.getFirstInputBlock().getBlocks();
            if (fillsFirstStepInquiry && step.getStepIndex() == 0) {
                for (Holder<Block> contactBlock : contactBlocks) {
                    Block b = contactBlock.value();
                    ProceduralProcessStepManager.PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY.computeIfAbsent(b, _ -> new ArrayList<>()).add(step);
                }
            }
            for (Holder<Block> contactBlock : contactBlocks) {
                Block b = contactBlock.value();
                ProceduralProcessStepManager.PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY.add(b);
            }
        } else {
            String recipeTypeWarning = "Each step of ProceduralProcessRecipe is expected to be an AbstractProcessRecipe. Received: ";
            recipeTypeWarning += step.getContent().getType().toString();
            AnvilCraft.LOGGER.warn(recipeTypeWarning);
        }
    }

    public static void addStep(ProceduralProcessStep step) {
        ProceduralProcessStepManager.addStep(step, true);
    }

    public static boolean checkAnyMatches(AnvilEvent.OnLand event) {
        ServerLevel sl = event.getLevel();
        RecipeMap recipes = sl.getServer().getRecipeManager().recipeMap();
        if (ProceduralProcessStepManager.initializedRecipes != recipes) {
            ProceduralProcessStepManager.initialize(recipes);
        }
        BlockPos hitPos = event.getPos().below();
        BlockState state = sl.getBlockState(hitPos);
        if (ProceduralProcessStepManager.PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY.contains(state.getBlock())) {
            InWorldRecipeContext context = new InWorldRecipeContext(
                sl,
                event.getPos().getCenter().subtract(0.0, 0.5, 0.0),
                event.getEntity()
            );
            WipBlockEntity wip = ProceduralProcessRecipe.getWipBlockFromContext(context);
            if (wip != null && wip.getRecipeId() != null) {
                Identifier recipeId = wip.getRecipeId();
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
                RecipeHolder<?> recipeHolder = RecipesRecord.getRecipes(sl).byKey(key);
                if (recipeHolder != null && recipeHolder.value() instanceof ProceduralProcessRecipe ppr) {
                    int loopMax = ppr.loop();
                    int oneLoopSize = ppr.steps().size();
                    int q = wip.getStepCount() / oneLoopSize;
                    int r = wip.getStepCount() - q * oneLoopSize;
                    ProceduralProcessStep step;
                    if (r == 0 && ppr.multiLoopFirstStep().isPresent() && q >= 1) {
                        step = ppr.multiLoopFirstStep().get();
                    } else {
                        step = ppr.steps().get(r);
                    }
                    if (
                        q < loopMax
                        && step.getContent() instanceof AbstractProcessRecipe<?> apr
                        && apr.matches(context, sl)
                    ) {
                        BlockState initialBlock = wip.getInitialBlock();
                        apr.assemble(context);
                        context.accept();
                        WipBlockEntity wip2 = ProceduralProcessRecipe.getWipBlockFromContext(context);
                        if (wip2 != null && q == loopMax - 1 && r == oneLoopSize - 1) {
                            BlockPos pos = wip2.getBlockPos();
                            Map.Entry<BlockState, CompoundTag> entry = ppr.resultBlock().getResult(sl);
                            if (entry != null) {
                                sl.setBlock(pos, entry.getKey(), Block.UPDATE_ALL);
                                BlockEntity be = sl.getBlockEntity(pos);
                                if (entry.getValue() != null && be != null) {
                                    be.loadCustomOnly(TagValueInput.create(
                                        ProblemReporter.DISCARDING,
                                        sl.registryAccess(),
                                        entry.getValue()
                                    ));
                                    be.setChanged();
                                    sl.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
                                }
                            }
                        } else {
                            if (wip2 != null) {
                                wip2.setStepCount(q * oneLoopSize + step.stepIndex + 1);
                                wip2.setInitialBlock(initialBlock);
                                wip2.setRecipeId(recipeId);
                                wip2.setChanged();
                                sl.sendBlockUpdated(
                                    wip2.getBlockPos(), wip2.getBlockState(), wip2.getBlockState(), Block.UPDATE_ALL
                                );
                            }
                        }
                        return true;
                    }
                }
            } else {
                List<ProceduralProcessStep> possibleSteps = ProceduralProcessStepManager.PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY.get(state.getBlock());
                if (possibleSteps == null || possibleSteps.isEmpty()) return false;
                for (ProceduralProcessStep step : possibleSteps) {
                    InWorldRecipeContext contextOfStep = new InWorldRecipeContext(
                        sl,
                        event.getPos().getCenter().subtract(0.0, 0.5, 0.0),
                        event.getEntity()
                    );
                    if (step.getContent() instanceof AbstractProcessRecipe<?> apr) {
                        if (apr.matches(contextOfStep, sl)) {
                            Identifier stepRecipeId = step.getPpRecipeId();
                            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, stepRecipeId);
                            RecipeHolder<?> holder = RecipesRecord.getRecipes(sl).byKey(key);
                            if (holder != null && holder.value() instanceof ProceduralProcessRecipe ppr) {
                                BlockPos pos = BlockPos.containing(contextOfStep.getPos());
                                pos = pos.below();
                                BlockState initialBlockState = ppr.initialBlock().getBlocks().get(0).value().defaultBlockState();
                                BlockPos potentialPos = pos;
                                for (int i = 0; i < ProceduralProcessStepManager.WIP_BLOCK_DETECTION_DEPTH; i++) {
                                    if (ppr.initialBlock().test(sl, sl.getBlockState(potentialPos), sl.getBlockEntity(potentialPos))) {
                                        initialBlockState = sl.getBlockState(potentialPos);
                                        break;
                                    }
                                    potentialPos = potentialPos.below();
                                }
                                apr.assemble(contextOfStep);
                                contextOfStep.accept();
                                WipBlockEntity wip0 = ProceduralProcessRecipe.getWipBlockFromContext(contextOfStep);
                                if (wip0 != null && ppr.loop() == 1 && ppr.steps().size() == 1) {
                                    BlockPos pos1 = wip0.getBlockPos();
                                    Map.Entry<BlockState, CompoundTag> entry = ppr.resultBlock().getResult(sl);
                                    if (entry != null) {
                                        sl.setBlock(pos1, entry.getKey(), Block.UPDATE_ALL);
                                        BlockEntity be = sl.getBlockEntity(pos1);
                                        if (entry.getValue() != null && be != null) {
                                            be.loadCustomOnly(TagValueInput.create(
                                                ProblemReporter.DISCARDING,
                                                sl.registryAccess(),
                                                entry.getValue()
                                            ));
                                            be.setChanged();
                                            sl.sendBlockUpdated(pos1, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
                                        }
                                    }
                                } else if (wip0 != null) {
                                    wip0.setStepCount(1);
                                    wip0.setInitialBlock(initialBlockState);
                                    wip0.setRecipeId(step.getPpRecipeId());
                                    wip0.setChanged();
                                    sl.sendBlockUpdated(
                                        wip0.getBlockPos(), wip0.getBlockState(), wip0.getBlockState(), Block.UPDATE_ALL
                                    );
                                }
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
