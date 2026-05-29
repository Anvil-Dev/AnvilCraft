package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Getter
public class ProceduralProcessRecipe implements Recipe<InWorldRecipeContext> {

    public final BlockStatePredicate initialBlock;
    // 这个不会参与判定，但是会决定它显示成什么
    public final List<ProceduralProcessStep> steps;
    public final ChanceBlockState resultBlock;
    // 这个东西是给多圈loop的配方用的，如果是单圈的话可以直接写在最后一步里
    public final ItemStack icon;
    public final int loop;
    public final Optional<ProceduralProcessStep> multiLoopFirstStep;

    //TODO：考虑重构方块->步骤的检索过程
    //  用ppr配方rl和步数能够O(1)检索，但是有必要吗？
    //  不过，wip方块对应的步骤种类数可能会稍微多一些

    //TODO：为一些具有API性质的东西写javadoc

    //TODO: JEI（查看配方）和Jade（查看方块实体内容：哪个配方第几步）支持

    public ProceduralProcessRecipe(
        BlockStatePredicate initialBlock,
        List<ProceduralProcessStep> steps,
        ChanceBlockState resultBlock,
        ItemStack icon,
        int loop,
        Optional<ProceduralProcessStep> multiLoopFirstStep
    ) {
        this.initialBlock = initialBlock;
        this.steps = steps;
        this.resultBlock = resultBlock;
        this.icon = icon;
        this.loop = loop;
        this.multiLoopFirstStep = multiLoopFirstStep;
    }

    public static WipBlockEntity getWipBlockFromContext(InWorldRecipeContext ctx) {
        Level l = ctx.getLevel();
        if (l instanceof ServerLevel sl) {
            BlockPos pos = BlockPos.containing(ctx.getPos());
            //暂时写成只检测下面两个方块（被铁砧砸的方块和其下方的方块）是否是WIP方块，如果不够再加
            pos = pos.below();
            if (sl.getBlockEntity(pos) instanceof WipBlockEntity wip) {
                return wip;
            }
            pos = pos.below();
            if (sl.getBlockEntity(pos) instanceof WipBlockEntity wip) {
                return wip;
            }
        }
        return null;
    }

    @Override
    public boolean matches(@NotNull InWorldRecipeContext ctx, @NotNull Level level) {
        WipBlockEntity wip = getWipBlockFromContext(ctx);
        if (wip == null) return false;
        return wip.getStepCount() >= steps.size();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull InWorldRecipeContext ctx, HolderLookup.@NotNull Provider provider) {
        //因为在铁砧砸的时候已经assemble过了它的每个步骤，所以也没啥事情做
        return this.icon.copy();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        BlockState state = resultBlock.state();
        if (state.isEmpty() || state.isAir()) return Items.ANVIL.getDefaultInstance();
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) item = Items.ANVIL;
        return item.getDefaultInstance();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.PROCEDURAL_PROCESS_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipeTypes.PROCEDURAL_PROCESS.get();
    }
}
