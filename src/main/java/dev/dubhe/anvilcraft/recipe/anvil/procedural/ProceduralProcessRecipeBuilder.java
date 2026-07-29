package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ProceduralProcessRecipeBuilder extends AbstractRecipeBuilder<ProceduralProcessRecipe> {

    private final BlockStatePredicate initialBlock;
    private final List<ProceduralProcessStep> steps = new ArrayList<>();
    private ChanceBlockState resultBlock = null;
    private Optional<ItemStackTemplate> icon = Optional.empty();
    private int loop = 1;
    private Optional<Identifier> displayedModel = Optional.empty();
    private final List<Identifier> displayedModels = new ArrayList<>();
    private Optional<ProceduralProcessStep> mfs = Optional.empty();

    public ProceduralProcessRecipeBuilder(BlockStatePredicate initialBlock) {
        this.initialBlock = initialBlock;
    }

    public static ProceduralProcessRecipeBuilder of(BlockStatePredicate initialBlock) {
        return new ProceduralProcessRecipeBuilder(initialBlock);
    }

    public static ProceduralProcessRecipeBuilder of(Block initialBlock) {
        return new ProceduralProcessRecipeBuilder(
            BlockStatePredicate.builder().of(initialBlock).build()
        );
    }

    public ProceduralProcessRecipeBuilder addStep(ProceduralProcessStep step) {
        this.steps.add(step);
        return this;
    }

    public ProceduralProcessRecipeBuilder addStep(AbstractProcessRecipe<?> stepContent) {
        ProceduralProcessStep step = new ProceduralProcessStep(this.steps.size(), stepContent);
        return this.addStep(step);
    }

    public ProceduralProcessRecipeBuilder result(ChanceBlockState resultBlock) {
        this.resultBlock = resultBlock;
        return this;
    }

    public ProceduralProcessRecipeBuilder result(Block resultBlock) {
        this.resultBlock = new ChanceBlockState(resultBlock.defaultBlockState(), 1.0f);
        return this;
    }

    public ProceduralProcessRecipeBuilder result(Supplier<? extends Block> resultBlock) {
        this.resultBlock = ChanceBlockState.of(resultBlock);
        return this;
    }

    public ProceduralProcessRecipeBuilder icon(ItemStackTemplate icon) {
        this.icon = Optional.of(icon);
        return this;
    }

    public ProceduralProcessRecipeBuilder loop(int loop) {
        this.loop = loop;
        return this;
    }

    public ProceduralProcessRecipeBuilder multipleLoopFirstStep(ProceduralProcessStep step) {
        this.mfs = Optional.of(step);
        return this;
    }

    public ProceduralProcessRecipeBuilder multipleLoopFirstStep(AbstractProcessRecipe<?> stepContent) {
        ProceduralProcessStep step = new ProceduralProcessStep(0, stepContent);
        return this.multipleLoopFirstStep(step);
    }

    public ProceduralProcessRecipeBuilder displayedModel(Identifier modelId) {
        this.displayedModel = Optional.of(modelId);
        return this;
    }

    /// 按流程顺序设置半成品模型：单圈配方按步推进，多圈配方按圈推进
    public ProceduralProcessRecipeBuilder displayedModels(Identifier... modelIds) {
        this.displayedModels.clear();
        this.displayedModels.addAll(List.of(modelIds));
        return this;
    }

    @Override
    public ProceduralProcessRecipe buildRecipe() {
        if (this.resultBlock == null) {
            if (this.steps.getLast().content instanceof AbstractProcessRecipe<?> apr) {
                this.resultBlock = apr.getFirstResultBlock();
            } else {
                this.resultBlock = new ChanceBlockState(Blocks.AIR.defaultBlockState(), 1f);
            }
        }
        return new ProceduralProcessRecipe(
            this.initialBlock,
            this.steps,
            this.resultBlock,
            this.icon,
            this.loop,
            this.displayedModel,
            this.displayedModels,
            this.mfs
        );
    }

    @Override
    public void validate(Identifier id) {
        if (this.loop <= 0) {
            throw new IllegalArgumentException("Loop count should be at least 1 (default is 1), got: " + this.loop);
        }
        if (this.steps.isEmpty()) {
            throw new IllegalArgumentException("Procedural Procession must have at least one step, RecipeId: " + id);
        }
        int displayedModelLimit = this.loop > 1 ? this.loop : this.steps.size();
        if (this.displayedModels.size() > displayedModelLimit) {
            throw new IllegalArgumentException(
                "Displayed model count must not exceed process stage count, RecipeId: " + id
            );
        }
        for (ProceduralProcessStep step : this.steps) {
            if (!(step.content instanceof AbstractProcessRecipe<?>)) {
                throw new IllegalArgumentException("Each step of Procedural Procession must be an Anvil Process Recipe, RecipeId: " + id);
            }
        }
    }

    @Override
    public String getType() {
        return "procedural_process";
    }

    @Override
    public ItemStackTemplate getResult() {
        return WrapUtils.getItem(this.resultBlock);
    }
}
