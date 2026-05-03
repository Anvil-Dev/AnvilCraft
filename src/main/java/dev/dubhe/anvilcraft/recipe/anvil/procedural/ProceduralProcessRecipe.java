package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import dev.anvilcraft.lib.v2.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.v2.recipe.component.ChanceBlockState;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
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

@Getter
public class ProceduralProcessRecipe implements Recipe<InWorldRecipeContext> {

    public final BlockStatePredicate initialBlock;
    public final List<ProceduralProcessStep> steps;
    public final ChanceBlockState resultBlock;
    // 说实在的，这个resultBlock的作用不大——毕竟按理说应该写到最后一个步骤里。不过，还是暂且让它承担名义输出的作用了。
    public final ItemStack icon;

    //TODO：支持循环（实际上和直接写也没有太大区别，不过显示和写数据包上确实方便一些）
    // （也可以写成builder和jei支持循环嘛）

    //TODO：写一些方块操作配方
    // 目前是没有闪炼、时移、中子辐照对普通方块进行操作的配方的；不过，这些可能可以归并成“反向涂抹”？

    //TODO：为一些具有API性质的东西写javadoc

    //TODO: 把方块涂抹里面的草方块配方迁移到正确的地方去

    //TODO：考虑是否需要修改PROCEDURAL_RECIPE_INQUIRY的索引方式

    public ProceduralProcessRecipe(BlockStatePredicate initialBlock, List<ProceduralProcessStep> steps, ChanceBlockState resultBlock, ItemStack icon) {
        this.initialBlock = initialBlock;
        this.steps = steps;
        this.resultBlock = resultBlock;
        this.icon = icon;
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
