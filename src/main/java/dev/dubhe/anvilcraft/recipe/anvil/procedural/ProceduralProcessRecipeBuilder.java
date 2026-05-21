package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.v2.recipe.component.ChanceBlockState;
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

public class ProceduralProcessRecipeBuilder extends AbstractRecipeBuilder<ProceduralProcessRecipe> {

    private final BlockStatePredicate initialBlock;
    private final List<ProceduralProcessStep> steps = new ArrayList<>();
    private ChanceBlockState resultBlock = null;
    private ItemStack icon = null;
    private int loop = 1;
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
        ProceduralProcessStep step = new ProceduralProcessStep(steps.size(), stepContent);
        return this.addStep(step);
    }

    public ProceduralProcessRecipeBuilder result(ChanceBlockState resultBlock) {
        this.resultBlock = resultBlock;
        return this;
    }

    public ProceduralProcessRecipeBuilder result(Supplier<? extends Block> resultBlock) {
        this.resultBlock = ChanceBlockState.of(resultBlock);
        return this;
    }

    public ProceduralProcessRecipeBuilder icon(ItemStack icon) {
        this.icon = icon;
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

    @Override
    public @NotNull ProceduralProcessRecipe buildRecipe() {
        if (this.resultBlock == null) {
            if (steps.getLast().content instanceof AbstractProcessRecipe<?> apr) {
                this.resultBlock = apr.getFirstResultBlock();
            }
            else this.resultBlock = new ChanceBlockState(Blocks.AIR.defaultBlockState(), 1f);
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
