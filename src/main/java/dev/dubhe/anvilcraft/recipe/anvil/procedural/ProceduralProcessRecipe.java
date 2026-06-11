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

    /**
     * 配方的初始方块
     * 不会参与配方本身执行的判定，但是会决定WIP方块的外观显示以及中途掉落物
     */
    public final BlockStatePredicate initialBlock;
    // 如果有需要的话可以通过给配方（甚至步骤）和WIP增加新的“显示方块状态”数据，
    // 从而把初始方块和显示方块进行解耦，但是目前没有写这方面的东西，可以等有需求的
    // 至于掉落物，则要看到时候跟哪边绑定了。
    /**
     * 配方的步骤的列表，有顺序
     * 步骤的编号从0开始，从0开始，到steps.size()-1结束
     */
    public final List<ProceduralProcessStep> steps;
    /**
     * 配方的结果方块
     * 如果配方结束时有WIP方块，会作为结果方块替换结束后的WIP方块
     * 这个东西主要是给多圈loop的配方用的，因为如果是单圈的话可以直接写在最后一步里
     */
    public final ChanceBlockState resultBlock;
    /**
     * 配方的图标
     */
    public final ItemStack icon;
    /**
     * 配方的循环次数
     * 1为只执行一次（单圈），不可以填0或者负数
     */
    public final int loop;
    /**
     * 需要执行多个循环的配方中，后续循环（即不是第一圈）中每个循环的初始步骤
     * 对于单圈的配方来说不需要有
     */
    public final Optional<ProceduralProcessStep> multiLoopFirstStep;

    // TODO: 由于JEI支持会需要比较复杂的显示方式和UI，而目前还没有说要写成什么样子的，所以暂时没有JEI支持

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

    /**
     * 从配方上下文中获取WIP方块实体
     * 该方法会检查铁砧下方两个位置的方块实体，寻找WIP方块。
     * 首先检测被铁砧砸的方块位置，然后检测其下方的方块位置。
     *
     * @param ctx 配方上下文，包含等级信息和位置信息
     * @return 找到的WIP方块实体，如果未找到则返回null
     */
    public static WipBlockEntity getWipBlockFromContext(InWorldRecipeContext ctx) {
        Level l = ctx.getLevel();
        if (l instanceof ServerLevel sl) {
            // 检测下方一定深度的方块是否是WIP方块，默认为检查两个
            BlockPos potentialPos = BlockPos.containing(ctx.getPos());
            for  (int i = 0; i < ProceduralProcessStepManager.WIP_BLOCK_DETECTION_DEPTH; i++) {
                potentialPos = potentialPos.below();
                if (sl.getBlockEntity(potentialPos) instanceof WipBlockEntity wip) {
                    return wip;
                }
            }
        }
        return null;
    }

    @Override
    public boolean matches(@NotNull InWorldRecipeContext ctx, @NotNull Level level) {
        return false;
        // 这个方法实际上不应该被调用（也没有情况会调用），
        // 实际上ProceduralProcessRecipe也确实需要把每一步依次执行完才能完成，
        // 因此它在静态的上下文中是不会“匹配”的，能够“匹配”的只能是它的每个步骤比如第一步或者最后一步
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull InWorldRecipeContext ctx, HolderLookup.@NotNull Provider provider) {
        // 因为在铁砧砸的时候已经assemble过了它的每个步骤，所以也没啥事情做
        // 它实际上也没有被调用的场合
        return this.icon.copy();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        BlockState state = resultBlock.state();
        if (state.isEmpty() || state.isAir()) return Items.AIR.getDefaultInstance();
        Item item = state.getBlock().asItem();
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
