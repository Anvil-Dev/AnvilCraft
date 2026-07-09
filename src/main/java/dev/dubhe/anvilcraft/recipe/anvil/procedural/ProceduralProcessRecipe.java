package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class ProceduralProcessRecipe implements Recipe<InWorldRecipeContext> {

    public final BlockStatePredicate initialBlock;
    public final List<ProceduralProcessStep> steps;
    public final ChanceBlockState resultBlock;
    public final ItemStack icon;
    public final int loop;
    public final Optional<Identifier> displayedModel;
    public final Optional<ProceduralProcessStep> multiLoopFirstStep;

    public ProceduralProcessRecipe(
        BlockStatePredicate initialBlock,
        List<ProceduralProcessStep> steps,
        ChanceBlockState resultBlock,
        ItemStack icon,
        int loop,
        Optional<Identifier> displayedModel,
        Optional<ProceduralProcessStep> multiLoopFirstStep
    ) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Procedural process recipe must have at least one step");
        }
        if (loop <= 0) {
            throw new IllegalArgumentException("Procedural process recipe loop count must be at least 1");
        }
        this.initialBlock = initialBlock;
        this.steps = steps;
        this.resultBlock = resultBlock;
        this.icon = icon;
        this.loop = loop;
        this.displayedModel = displayedModel;
        this.multiLoopFirstStep = multiLoopFirstStep;
    }

    public static WipBlockEntity getWipBlockFromContext(InWorldRecipeContext ctx) {
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
        return this.icon.copy();
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
    public boolean showNotification() {
        return false;
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return Collections.emptyList();
    }
}
