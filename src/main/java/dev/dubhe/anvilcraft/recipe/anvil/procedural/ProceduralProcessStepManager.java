package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProceduralProcessStepManager {
    /**
     * 这个映射表是 铁砧砸到的方块->所有可能的ProceduralProcess第一步步骤 的映射表
     * 也就是说，这个映射表可以在铁砧砸到某个方块的时候快速查询这次铁砧砸是否触发了某个配方的第一步
     */
    public static Map<Block, List<ProceduralProcessStep>> PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY = new HashMap<>();

    /**
     * 这个集合是 存在任何ProceduralProcess步骤 的 方块 的集合
     * 也就是说，这个集合可以在铁砧砸到某个方块的时候快速查询这次铁砧砸是否能触发任何配方的任何步骤
     */
    public static Set<Block> PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY = new HashSet<>();

    /**
     * 铁砧落地时，向下方检测WIP方块的范围
     * 默认为2的意思是，默认为铁砧所砸到的那个方块和那个方块下方的方块
     */
    public static final int WIP_BLOCK_DETECTION_DEPTH = 2;

    /**
     * 在recipe manager构建配方之后执行
     * 指路：dev.dubhe.anvilcraft.mixin.RecipeManagerMixin.afterBuildRecipe
     *
     * @param byName Minecraft的RecipeManager的配方注册表
     */
    public static void initialize(Map<ResourceLocation, RecipeHolder<?>> byName) {
        // 由于有数据包热重载的reload存在，需要在每次重新加载数据包的时候清空两个表
        PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY = new HashMap<>();
        PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY = new HashSet<>();
        for (ResourceLocation rl : byName.keySet()) {
            if (byName.get(rl).value() instanceof ProceduralProcessRecipe recipe) {
                List<ProceduralProcessStep> steps = recipe.getSteps();
                // 实际上数据包里的编号没有用，因为这里会重新编号……
                for (int index = 0; index < steps.size(); index++) {
                    ProceduralProcessStep step = steps.get(index);
                    // 这里是在【加载】时设置这个step是本配方中第几步
                    step.setStepIndex(index);
                    // 同样是在【加载】时，记录这个step属于哪个序列装配配方
                    step.setPpRecipeId(rl);
                    // 第一步是0，第二步是1……
                    ProceduralProcessStepManager.addStep(step);
                }
                // 对于多圈配方的第一步，需要额外注册
                if (recipe.getMultiLoopFirstStep().isPresent()) {
                    ProceduralProcessStep step = recipe.getMultiLoopFirstStep().get();
                    step.setStepIndex(0);
                    step.setPpRecipeId(rl);
                    ProceduralProcessStepManager.addStep(step, false);
                }
            }
        }

    }

    /**
     * 在将ProceduralProcessRecipe加载时，需要填充上方的映射表或者检查其是否是apr，
     * 这个函数是用来给每个step填到上面的映射表的
     * fillsFirstStepInquiry，则不会填充PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY，否则会根据数据填充
     *
     * @param step 打包好的step数据结构
     * @param fillsFirstStepInquiry 是否填充PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY，只对于index为0的步骤有效
     */
    public static void addStep(ProceduralProcessStep step, boolean fillsFirstStepInquiry) {
        if (step.getContent() instanceof AbstractProcessRecipe<?> apr) {
            HolderSet<Block> contactBlocks = apr.getFirstInputBlock().getBlocks();
            if (fillsFirstStepInquiry && step.getStepIndex() == 0) {
                // 如果是第一步，加入第一步的查询表
                for (Holder<Block> contactBlock : contactBlocks) {
                    Block b = contactBlock.value();
                    if (!PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY.containsKey(b)) {
                        PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY.put(b, new ArrayList<>());
                    }
                    PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY.get(b).add(step);
                }
            }
            // 不管是否是第一步，都加入“任意步骤”的表格中
            for (Holder<Block> contactBlock : contactBlocks) {
                Block b = contactBlock.value();
                PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY.add(b);
            }
        } else {
            String recipeTypeWarning =  "Each step of ProceduralProcessRecipe is expected to be an AbstractProcessRecipe. Received: ";
            recipeTypeWarning += step.getContent().getType().toString();
            AnvilCraft.LOGGER.warn(recipeTypeWarning);
            // 如果不是AbstractProcessRecipe，跳过那个步骤的加载并警告
            // 当然，这会让那个配方无法被执行完成，因为无法通过铁砧操作进到正确的步骤
            // 这里的每个步骤都应该是AbstractProcessRecipe，而且需要正确处理WIP方块，
            // 但是因为数据包中的recipe并不区分这个，这导致确实会有不符合结构的序列装配配方被加载进来
        }

    }

    /**
     * 在将ProceduralProcessRecipe加载时，需要填充上方的映射表或者检查其是否是apr，
     * 这个函数是用来给每个step填到上面的映射表的
     *
     * @param step 打包好的step数据结构
     */
    public static void addStep(ProceduralProcessStep step) {
        ProceduralProcessStepManager.addStep(step, true);
    }

    /**
     * 判断这次铁砧落地事件是否有序列装配配方步骤可以执行，
     * 如果有，且步骤正确，则执行它
     *
     * @param event 铁砧落地事件
     * @return 是否执行了配方
     */
    public static boolean checkAnyMatches(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel sl) {
            BlockPos hitPos = event.getPos().below();
            BlockState state = sl.getBlockState(hitPos);
            // 如果可能触发任意步骤，才进入处理
            // 检查PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY几乎是O(1)时间，
            // 由于在这个方法之后紧接着执行的是处理铁砧落地配方，需要遍历配方，
            // 所以这里的查表不会是那个性能关键节点
            if (PROCEDURAL_PROCESS_EXIST_STEP_INQUIRY.contains(state.getBlock())) {
                InWorldRecipeContext context = new InWorldRecipeContext(
                    sl,
                    event.getPos().getCenter().subtract(0.0, 0.5, 0.0),
                    event.getEntity()
                );
                WipBlockEntity wip = ProceduralProcessRecipe.getWipBlockFromContext(context);
                // 如果已有wip方块，检测wip方块中的配方Id（如果存在的话）
                if (wip != null && wip.getRecipeId() != null) {
                    ResourceLocation recipeId = wip.getRecipeId();
                    Optional<RecipeHolder<?>> recipeHolder = sl.getRecipeManager().byKey(recipeId);
                    if (recipeHolder.isPresent() && recipeHolder.get().value() instanceof ProceduralProcessRecipe ppr) {
                        int loopMax = ppr.getLoop();
                        int oneLoopSize = ppr.getSteps().size();
                        int q = wip.getStepCount() / oneLoopSize;
                        // wip方块中储存的步数除以一个循环有多少步的商，也就是wip方块是第几（0，1，2……）个循环的
                        int r = wip.getStepCount() - q * oneLoopSize;
                        // 余数，也就是是那个循环的第几步
                        ProceduralProcessStep step;
                        if (r == 0 && ppr.getMultiLoopFirstStep().isPresent() && q >= 1) {
                            step = ppr.getMultiLoopFirstStep().get();
                            // 如果是多圈的第一步，则需要特殊处理
                        } else {
                            step = ppr.getSteps().get(r);
                            // 获取wip方块存储的recipe中stepIndex编号为r的步骤
                        }
                        // 一共执行一圈的时候loopMax是1，这个时候q应该是0才能继续执行
                        // 然后是如果该步骤有内容且matches，那么我们执行这个步骤
                        if (
                            q < loopMax
                                && step.getContent() instanceof AbstractProcessRecipe<?> apr
                                && apr.matches(context, sl)
                        ) {
                            BlockState initialBlock = wip.getInitialBlock();
                            apr.assemble(context, sl.registryAccess());
                            context.accept();
                            // 这个时候原本的wip大概率已经消失了，会有一个新的wip方块
                            WipBlockEntity wip2 = ProceduralProcessRecipe.getWipBlockFromContext(context);
                            // 对于配方结束时仍然有wip方块的配方（比如不止一圈的配方），需要单独设置结果
                            if (wip2 != null && q == loopMax - 1 && r == oneLoopSize - 1) {
                                BlockPos pos = wip2.getBlockPos();
                                Map.Entry<BlockState, CompoundTag> entry = ppr.getResultBlock().getResult(sl);
                                if (entry != null) {
                                    sl.setBlock(pos, entry.getKey(), Block.UPDATE_ALL);
                                    BlockEntity be = sl.getBlockEntity(pos);
                                    if (entry.getValue() != null && be != null) {
                                        be.loadWithComponents(entry.getValue(), sl.registryAccess());
                                        be.setChanged();
                                        // 手动设置方块实体信息之后要再同步一次
                                        sl.sendBlockUpdated(
                                            pos,
                                            be.getBlockState(),
                                            be.getBlockState(),
                                            Block.UPDATE_ALL
                                        );
                                    }
                                }

                            } else {
                                if (wip2 != null) {
                                    wip2.setStepCount(q * oneLoopSize + step.stepIndex + 1);
                                    // 第一步执行过之后wip方块的值应该是1，第二步之后是2……要注意，在step的数据结构中第一步是0，这里要+1
                                    wip2.setInitialBlock(initialBlock);
                                    wip2.setRecipeId(recipeId);
                                    // 将当前执行的配方设置进去
                                    wip2.setChanged();
                                    // 对于wip方块来说也是一样的，手动设置方块实体信息之后要再同步一次
                                    // 不然的话客户端虽然能看到那个方块，但是无法根据内部的方块实体信息变化渲染
                                    sl.sendBlockUpdated(
                                        wip2.getBlockPos(),
                                        wip2.getBlockState(),
                                        wip2.getBlockState(),
                                        Block.UPDATE_ALL
                                    );
                                }
                                // 如果新的wip方块存在，且不是最后一圈，则设置其中数据
                                // 如果不存在，那么我们就假设它是最后一步吧。
                            }
                            return true;
                        }
                    }
                } else {
                    // 如果是第一步——没有wip方块——则需要遍历所有可能的配方步骤
                    List<ProceduralProcessStep> possibleSteps = PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY.get(state.getBlock());
                    // 如果是第一步且不匹配任何possibleStep，那说明这个方块没有ppr配方，可以直接返回
                    if (possibleSteps == null || possibleSteps.isEmpty()) return false;
                    // 获取所有可能的第一步
                    for (ProceduralProcessStep step : possibleSteps) {
                        // 这个context需要在每次判定之后从level信息重建，因为它【必须】重新读取方块和物品信息，
                        // 不然的话哪怕物品没有消耗，数量也会被临时扣掉导致下一次apr.matches的时候无法匹配
                        InWorldRecipeContext contextOfStep = new InWorldRecipeContext(
                            sl,
                            event.getPos().getCenter().subtract(0.0, 0.5, 0.0),
                            event.getEntity()
                        );
                        if (step.getContent() instanceof AbstractProcessRecipe<?> apr) {
                            // 如果matches了就直接执行这个步骤
                            if (apr.matches(contextOfStep, sl)) {
                                ResourceLocation stepRecipeId = step.getPpRecipeId();
                                Optional<RecipeHolder<?>> recipeHolder = sl.getRecipeManager().byKey(stepRecipeId);
                                if (recipeHolder.isPresent() && recipeHolder.get().value() instanceof ProceduralProcessRecipe ppr) {
                                    // 这里已经判定过了没有存在配方id的wip方块，而且PROCEDURAL_PROCESS_FIRST_STEP_INQUIRY中只存在第一步
                                    // 接下来获取初始方块
                                    // 要搜索一下世界上（这里还是暂且以下方2个方块为范围）是否有匹配配方中标注的方块，
                                    // 如果有则按照那个写，如果没有搜索到则直接用配方里的
                                    BlockPos pos = BlockPos.containing(contextOfStep.getPos());
                                    pos = pos.below();
                                    BlockState initialBlock = ppr.getInitialBlock().getBlocks().get(0).value().defaultBlockState();
                                    // 初始值是配方里的初始方块信息的默认方块状态
                                    BlockPos potentialPos = pos;
                                    for (int i = 0; i < WIP_BLOCK_DETECTION_DEPTH; i++) {
                                        if (ppr.initialBlock.test(sl,
                                            sl.getBlockState(potentialPos),
                                            sl.getBlockEntity(potentialPos)
                                        )) {
                                            initialBlock = sl.getBlockState(potentialPos);
                                            break;
                                        }
                                        potentialPos = potentialPos.below();
                                    }
                                    // 下方的方块（默认为查找两个）逐个判断过后，如果都不是则还是初始值，如果有一个是那就是下方的方块里的方块状态值
                                    apr.assemble(contextOfStep, sl.registryAccess());
                                    contextOfStep.accept();
                                    // 然后给新做出来的wip方块进行设置
                                    WipBlockEntity wip0 = ProceduralProcessRecipe.getWipBlockFromContext(contextOfStep);
                                    if (wip0 != null && ppr.getLoop() == 1 && ppr.getSteps().size() == 1) {
                                        // 如果只有一圈且一圈只有一步，那么第一步就是最后一步，而且有wip方块……
                                        // 实际上不应该把配方写成这样，不过确实可以写，毕竟只有一步不需要序列装配
                                        // 而且可以直接把结果写在步骤里
                                        // 但是还是处理了让它设置一下结果方块，也就是按照ppr中的result来处理
                                        BlockPos pos1 = wip0.getBlockPos();
                                        Map.Entry<BlockState, CompoundTag> entry = ppr.getResultBlock().getResult(sl);
                                        if (entry != null) {
                                            sl.setBlock(pos1, entry.getKey(), Block.UPDATE_ALL);
                                            BlockEntity be = sl.getBlockEntity(pos1);
                                            if (entry.getValue() != null && be != null) {
                                                be.loadWithComponents(entry.getValue(), sl.registryAccess());
                                                be.setChanged();
                                                sl.sendBlockUpdated(
                                                    pos1,
                                                    be.getBlockState(),
                                                    be.getBlockState(),
                                                    Block.UPDATE_ALL
                                                );
                                            }
                                        }
                                    } else if (wip0 != null) {
                                        wip0.setStepCount(1);
                                        // 第一步执行过之后wip方块的值应该是1
                                        wip0.setInitialBlock(initialBlock);
                                        wip0.setRecipeId(step.getPpRecipeId());
                                        // 将当前执行的配方设置进去
                                        wip0.setChanged();
                                        sl.sendBlockUpdated(
                                            wip0.getBlockPos(),
                                            wip0.getBlockState(),
                                            wip0.getBlockState(),
                                            Block.UPDATE_ALL
                                        );
                                    }
                                    return true;
                                }
                            }
                            // 注意，如果步骤不对不会直接返回false，而是会看下一个step
                        }
                        // 如果不是apr的话，是要被忽略的——按理来说不应该填入其他类型的配方，但是还是那句话，数据包作者写什么东西都有可能
                        // 加载时就已经报过相关的警告了，没必要在每次执行的时候再报一堆
                    }
                }

            }
        }
        return false;
    }

}
