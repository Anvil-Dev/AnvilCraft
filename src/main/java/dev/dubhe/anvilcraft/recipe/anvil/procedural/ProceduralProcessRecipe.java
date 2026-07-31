package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Defines a procedural process recipe.
 *
 * @param displayedModel     配方执行过程中用于展示的半成品模型
 * @param displayedModels    随流程推进依次使用的模型。单圈配方每完成一步推进一次，多圈配方在下一圈的首步完成时推进；
 *                           当前进度没有对应模型时回退到 [#displayedModel() ]
 * @param multiLoopFirstStep 需要执行多个循环的配方中，后续循环（即不是第一圈）中每个循环的初始步骤
 *
 *                           <p>对于单圈的配方来说不需要有</p>
 */
public record ProceduralProcessRecipe(BlockStatePredicate initialBlock, List<ProceduralProcessStep> steps, ChanceBlockState resultBlock,
                                      Optional<ItemStackTemplate> icon, int loop, Optional<Identifier> displayedModel,
                                      List<Identifier> displayedModels, Optional<ProceduralProcessStep> multiLoopFirstStep) implements
    Recipe<InWorldRecipeContext> {

    public ProceduralProcessRecipe {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Procedural process recipe must have at least one step");
        }
        if (loop <= 0) {
            throw new IllegalArgumentException("Procedural process recipe loop count must be at least 1");
        }
        displayedModels = List.copyOf(displayedModels);
    }

    public Optional<Identifier> getDisplayedModelForStep(int stepCount) {
        if (stepCount > 0 && !this.steps.isEmpty()) {
            // 多圈配方按圈推进模型，单圈配方按步推进
            int modelIndex = this.loop > 1 ? (stepCount - 1) / this.steps.size() : stepCount - 1;
            if (modelIndex < this.displayedModels.size()) {
                return Optional.of(this.displayedModels.get(modelIndex));
            }
        }
        return this.displayedModel;
    }

    public static @Nullable WipBlockEntity getWipBlockFromContext(InWorldRecipeContext ctx) {
        Level l = ctx.getLevel();
        if (l instanceof ServerLevel sl) {
            BlockPos potentialPos = BlockPos.containing(ctx.getPos());
            for (int i = 0; i < ProceduralProcessStepManager.WIP_BLOCK_DETECTION_DEPTH; i++) {
                potentialPos = potentialPos.below();
                if (sl.getBlockEntity(potentialPos) instanceof WipBlockEntity wip) {
                    return wip;
                }
            }
        }
        return null;
    }

    @Override
    public boolean matches(InWorldRecipeContext ctx, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(InWorldRecipeContext ctx) {
        return this.icon.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<ProceduralProcessRecipe> getSerializer() {
        return ProceduralProcessSerializer.INSTANCE;
    }

    @Override
    public RecipeType<ProceduralProcessRecipe> getType() {
        return ModRecipeTypes.PROCEDURAL_PROCESS.get();
    }

    @Override
    public String group() {
        return "procedural_process";
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public List<RecipeDisplay> display() {
        return Collections.emptyList();
    }
}
