package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 序列装配配方构建器
 * 用于构建铁砧程序化处理流程的配方，支持多步骤处理、循环处理等功能。
 * 通过链式调用方法可以方便地配置配方的各个参数。
 */
public class ProceduralProcessRecipeBuilder extends AbstractRecipeBuilder<ProceduralProcessRecipe> {

    private final BlockStatePredicate initialBlock;
    private final List<ProceduralProcessStep> steps = new ArrayList<>();
    private ChanceBlockState resultBlock = null;
    private ItemStack icon = null;
    private int loop = 1;
    private Optional<ProceduralProcessStep> mfs = Optional.empty();

    /**
     * 构造序列装配配方构建器
     *
     * @param initialBlock 初始方块状态谓词，用于匹配可以被处理的方块
     */
    public ProceduralProcessRecipeBuilder(BlockStatePredicate initialBlock) {
        this.initialBlock = initialBlock;
    }

    /**
     * 创建序列装配配方构建器实例
     *
     * @param initialBlock 初始方块状态谓词，用于匹配可以被处理的方块
     * @return 新的构建器实例
     */
    public static ProceduralProcessRecipeBuilder of(BlockStatePredicate initialBlock) {
        return new ProceduralProcessRecipeBuilder(initialBlock);
    }

    /**
     * 创建序列装配配方构建器实例
     *
     * @param initialBlock 初始方块，用于匹配可以被处理的方块
     * @return 新的构建器实例
     */
    public static ProceduralProcessRecipeBuilder of(Block initialBlock) {
        return new ProceduralProcessRecipeBuilder(
            BlockStatePredicate.builder().of(initialBlock).build()
        );
    }

    /**
     * 添加处理步骤
     *
     * @param step 要添加的处理步骤
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder addStep(ProceduralProcessStep step) {
        this.steps.add(step);
        return this;
    }

    /**
     * 添加处理步骤
     *
     * @param stepContent 步骤内容，必须是AbstractProcessRecipe铁砧处理配方
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder addStep(AbstractProcessRecipe<?> stepContent) {
        ProceduralProcessStep step = new ProceduralProcessStep(steps.size(), stepContent);
        return this.addStep(step);
    }

    /**
     * 设置结果方块
     *
     * @param resultBlock 带有概率的结果方块状态
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder result(ChanceBlockState resultBlock) {
        this.resultBlock = resultBlock;
        return this;
    }

    /**
     * 设置结果方块
     *
     * @param resultBlock 结果方块
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder result(Block resultBlock) {
        this.resultBlock = new ChanceBlockState(resultBlock.defaultBlockState(), 1.0f);
        return this;
    }

    /**
     * 添加结果方块
     *
     * @param resultBlock 结果方块，必须是方块对象
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder result(Supplier<? extends Block> resultBlock) {
        this.resultBlock = ChanceBlockState.of(resultBlock);
        return this;
    }

    /**
     * 设置结果图标
     *
     * @param icon 结果图标
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder icon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    /**
     * 设置循环次数
     *
     * @param loop 循环次数
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder loop(int loop) {
        this.loop = loop;
        return this;
    }

    /**
     * 设置需要执行多个循环的配方中，后续循环（即不是第一圈）中每个循环的初始步骤
     *
     * @param step 要添加的处理步骤
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder multipleLoopFirstStep(ProceduralProcessStep step) {
        this.mfs = Optional.of(step);
        return this;
    }

    /**
     * 添加需要执行多个循环的配方中，后续循环（即不是第一圈）中每个循环的初始步骤
     *
     * @param stepContent 步骤内容，必须是AbstractProcessRecipe铁砧处理配方
     * @return 当前构建器实例，支持链式调用
     */
    public ProceduralProcessRecipeBuilder multipleLoopFirstStep(AbstractProcessRecipe<?> stepContent) {
        ProceduralProcessStep step = new ProceduralProcessStep(0, stepContent);
        return this.multipleLoopFirstStep(step);
    }

    @Override
    public @NotNull ProceduralProcessRecipe buildRecipe() {
        if (this.resultBlock == null) {
            if (steps.getLast().content instanceof AbstractProcessRecipe<?> apr) {
                this.resultBlock = apr.getFirstResultBlock();
            } else {
                this.resultBlock = new ChanceBlockState(Blocks.AIR.defaultBlockState(), 1f);
            }
        }
        if (this.icon == null) {
            this.icon = this.initialBlock.getBlocks().get(0).value().asItem().getDefaultInstance();
        }
        return new ProceduralProcessRecipe(
            this.initialBlock,
            this.steps,
            this.resultBlock,
            this.icon,
            this.loop,
            this.mfs
        );
    }

    @Override
    public void validate(@NotNull ResourceLocation id) {
        if (loop <= 0) {
            throw new IllegalArgumentException("Loop count should be at least 1 (default is 1), got: " + loop);
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Procedural Procession must have at least one step, RecipeId: " + id);
        }
        for (ProceduralProcessStep step : steps) {
            if (!(step.content instanceof AbstractProcessRecipe<?>)) {
                throw new IllegalArgumentException("Each step of Procedural Procession must be an Anvil Process Recipe, RecipeId: " + id);
            }

        }
    }

    @Override
    public @NotNull String getType() {
        return "procedural_process";
    }

    @Override
    public @NotNull Item getResult() {
        return WrapUtils.getItem(resultBlock);
    }
}
