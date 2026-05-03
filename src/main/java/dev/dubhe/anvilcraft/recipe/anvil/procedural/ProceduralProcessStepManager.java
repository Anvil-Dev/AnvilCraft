package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProceduralProcessStepManager {
    /**
     * 这个映射表是 铁砧砸到的方块->所有可能的ProceduralProcess步骤 的映射表
     * 也就是说，这个映射表可以在铁砧砸到某个方块的时候快速查询这次铁砧砸是否触发了某个step
     */
    public static Map<Block, List<ProceduralProcessStep>> PROCEDURAL_PROCESS_STEP_INQUIRY = new HashMap<>();

    /**
     * 这个映射表是 ProceduralProcess步骤->它所在的序列装配配方的id
     * 也就是说，它是在已知当前步骤是什么的时候查询要执行的配方是哪个的
     */
    public static Map<ProceduralProcessStep, ResourceLocation> PROCEDURAL_RECIPE_INQUIRY = new HashMap<>();
    //其实用step作为key不太好，但是加入step id是非常过分的事情，之后可以看看要不要再改

    /**
     * 在recipe manager构建配方之后执行
     * 指路：dev.dubhe.anvilcraft.mixin.RecipeManagerMixin.afterBuildRecipe
     */
    public static void initialize(Map<ResourceLocation, RecipeHolder<?>> byName) {
        for (ResourceLocation rl : byName.keySet()) {
            if (byName.get(rl).value() instanceof ProceduralProcessRecipe recipe) {
                List<ProceduralProcessStep> steps = recipe.getSteps();
                for (int index = 0; index < steps.size(); index++) {
                    ProceduralProcessStep step = steps.get(index);
                    //这里是在【加载】时设置这个step是本配方中第几步
                    step.setStepIndex(index);
                    //第一步是0，第二步是1……
                    ProceduralProcessStepManager.addStep(step, rl);
                }
            }
        }

    }

    /**
     * 在将ProceduralProcessRecipe加载时，需要填充上方的映射表，
     * 这个函数是用来给每个step填到上面的映射表的
     * @param step 打包好的step数据结构
     */
    public static void addStep(ProceduralProcessStep step, ResourceLocation recipe) {
        if (step.getContent() instanceof AbstractProcessRecipe<?> apr) {
            HolderSet<Block> contactBlocks = apr.getFirstInputBlock().getBlocks();
            for (Holder<Block> contactBlock : contactBlocks) {
                Block b = contactBlock.value();
                if (!PROCEDURAL_PROCESS_STEP_INQUIRY.containsKey(b)) {
                    PROCEDURAL_PROCESS_STEP_INQUIRY.put(b, new ArrayList<>());
                }
                PROCEDURAL_PROCESS_STEP_INQUIRY.get(b).add(step);
            }
        }
        else {
            String recipeTypeWarning =  "Each step of ProceduralProcessRecipe is expected to be an AbstractProcessRecipe. Received: ";
            recipeTypeWarning += step.getContent().getType().toString();
            AnvilCraft.LOGGER.warn(recipeTypeWarning);
            // 如果不是AbstractProcessRecipe，跳过那个步骤的加载并警告
            // 当然，这会让那个配方无法被执行完成，因为无法通过铁砧操作进到正确的步骤
            // 这里的每个步骤都应该是AbstractProcessRecipe，而且需要正确处理WIP方块，
            // 但是因为数据包中的recipe并不区分这个，这导致确实会有不符合结构的序列装配配方被加载进来
        }
        //这里的recipe从那边传入
        PROCEDURAL_RECIPE_INQUIRY.put(step, recipe);

    }



    /**
     * 判断这次铁砧落地事件是否有序列装配配方步骤可以执行，
     * 如果有，且步骤正确，则执行它
     * @param event 铁砧落地事件
     * @return 是否执行了配方
     */
    public static boolean checkAnyMatches(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel sl) {
            BlockPos hitPos = event.getPos().below();
            BlockState state = sl.getBlockState(hitPos);
            if (PROCEDURAL_PROCESS_STEP_INQUIRY.containsKey(state.getBlock())) {
                InWorldRecipeContext context = new InWorldRecipeContext(
                    sl,
                    event.getPos().getCenter().subtract(0.0, 0.5, 0.0),
                    event.getEntity()
                );
                List<ProceduralProcessStep> possibleSteps = PROCEDURAL_PROCESS_STEP_INQUIRY.get(state.getBlock());
                for (ProceduralProcessStep step : possibleSteps) {
                    if (step.getContent() instanceof AbstractProcessRecipe<?> apr) {
                        if (apr.matches(context, sl)) {
                            WipBlockEntity wip = ProceduralProcessRecipe.getWipBlockFromContext(context);
                            if (wip != null && step.getStepIndex() == wip.getStepCount()) {
                                ResourceLocation recipeId = wip.getRecipeId();
                                if (recipeId.equals(PROCEDURAL_RECIPE_INQUIRY.get(step))) {
                                    BlockState initialBlock = wip.getInitialBlock();
                                    apr.assemble(context, sl.registryAccess());
                                    context.accept();
                                    //这个时候原本的wip大概率已经消失了，会有一个新的wip方块
                                    WipBlockEntity wip2 = ProceduralProcessRecipe.getWipBlockFromContext(context);
                                    if (wip2 != null) {
                                        wip2.setStepCount(step.stepIndex + 1);
                                        //第一步执行过之后wip方块的值应该是1，第二步之后是2……要注意，在step的数据结构中第一步是0，这里要+1
                                        wip2.setInitialBlock(initialBlock);
                                        wip2.setRecipeId(recipeId);
                                        //将当前执行的配方设置进去
                                        wip2.setChanged();
                                        sl.sendBlockUpdated(wip2.getBlockPos(), wip2.getBlockState(), wip2.getBlockState(), 3);
                                    }
                                    //如果新的wip方块存在，则设置其中数据
                                    //如果不存在，那么我们就假设它是最后一步吧。

                                    return true;
                                }
                            }
                            if (wip == null && step.getStepIndex() == 0) { //如果是第一步，应当没有wip方块
                                ResourceLocation rl = PROCEDURAL_RECIPE_INQUIRY.get(step);
                                Optional<RecipeHolder<?>> recipeHolder = sl.getRecipeManager().byKey(rl);
                                if (recipeHolder.isPresent() && recipeHolder.get().value() instanceof ProceduralProcessRecipe recipe) {
                                    // 接下来获取初始方块
                                    // 要搜索一下世界上（这里还是暂且以下方2个方块为范围）是否有匹配配方中标注的方块，
                                    // 如果有则按照那个写，如果没有搜索到则直接用配方里的
                                    BlockPos pos = BlockPos.containing(context.getPos());
                                    pos = pos.below();
                                    BlockState initialBlock = recipe.getInitialBlock().getBlocks().get(0).value().defaultBlockState();
                                    // 初始值是配方里的
                                    if (recipe.initialBlock.test(sl, sl.getBlockState(pos), sl.getBlockEntity(pos))) {
                                        initialBlock = sl.getBlockState(pos);
                                    }
                                    else {
                                        pos = pos.below();
                                        if (recipe.initialBlock.test(sl, sl.getBlockState(pos), sl.getBlockEntity(pos))) {
                                            initialBlock = sl.getBlockState(pos);
                                        }
                                    }
                                    // 两个方块逐个判断过后，如果都不是则还是初始值，如果有一个是那就是两个方块里的值
                                    apr.assemble(context, sl.registryAccess());
                                    context.accept();
                                    //然后给新作出来的wip方块进行设置
                                    WipBlockEntity wip0 = ProceduralProcessRecipe.getWipBlockFromContext(context);
                                    if (wip0 != null) {
                                        wip0.setStepCount(1);
                                        //第一步执行过之后wip方块的值应该是1
                                        wip0.setInitialBlock(initialBlock);
                                        wip0.setRecipeId(rl);
                                        //将当前执行的配方设置进去
                                        wip0.setChanged();
                                        sl.sendBlockUpdated(wip0.getBlockPos(), wip0.getBlockState(), wip0.getBlockState(), 3);
                                    }
                                    return true;
                                }
                            }
                            //注意，如果步骤不对不会直接返回false，而是会看下一个step
                        }
                    }
                    //如果不是apr的话，是要被忽略的——按理来说不应该填入其他类型的配方，但是还是那句话，数据包作者写什么东西都有可能
                    //加载时就已经报过相关的警告了，没必要在每次执行的时候再报一堆
                }
            }
        }
        return false;
    }

}
