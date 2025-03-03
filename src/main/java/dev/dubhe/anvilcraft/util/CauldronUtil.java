package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.Layered4LevelCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;

/**
 * 一个用于处理各种炼药锅逻辑的工具类，集中了各种各样的特判代码。<s>把特判集中到一个类里面总比在各种类里面到处特判要好吧</s>
 *
 * @apiNote 本类中所有的名为 {@code cauldronContent}的方法参数代表“炼药锅内容物”，传入的是一种非空的炼药锅方块。
 * 它是一个类似于“流体”的抽象概念，但由于细雪炼药锅装的不是流体，我们不能用{@code Fluid}作为参数。<br/>
 * 本类的大部分方法在 {@code cauldronContent} 方法参数不是一种炼药锅时不会抛出异常，而是返回一些默认值。<br/>
 */
public class CauldronUtil {

    public static IntegerProperty LEVEL_3 = LayeredCauldronBlock.LEVEL;
    public static IntegerProperty LEVEL_4 = Layered4LevelCauldronBlock.LEVEL;

    /**
     * 判断一个方块状态当前的炼药锅层数。如果该方块不是炼药锅，返回0。
     *
     * @param state 需判断层数的方块状态
     * @return 炼药锅层数
     */
    public static int currentLevel(BlockState state) {
        if (state.is(Blocks.CAULDRON)) return 0;
        Block block = state.getBlock();
        if (!(block instanceof AbstractCauldronBlock cauldron)) return 0;
        if (state.hasProperty(LEVEL_3)) return state.getValue(LEVEL_3);
        if (state.hasProperty(LEVEL_4)) return state.getValue(LEVEL_4);
        return Optional.of(cauldron)
            .map(CauldronFluidContent::getForBlock)
            .map(content -> content.currentLevel(state))
            .orElse(1);
    }

    /**
     * 判断一种炼药锅内容物在炼药锅中的最大的堆积层数。如果炼药锅内容物不合法，返回0。
     *
     * @param cauldronContent 需判断的炼药锅内容物
     * @return 最大层数
     */
    public static int maxLevel(Block cauldronContent) {
        if (cauldronContent == Blocks.CAULDRON) return 0;
        if (!(cauldronContent instanceof AbstractCauldronBlock cauldron)) return 0;
        BlockState defaultState = cauldron.defaultBlockState();
        if (defaultState.hasProperty(LEVEL_3)) return 3;
        if (defaultState.hasProperty(LEVEL_4)) return 4;
        return Optional.of(cauldron)
            .map(CauldronFluidContent::getForBlock)
            .map(content -> content.maxLevel)
            .orElse(1);
    }

    /**
     * 测试我们是否可以对一个炼药锅方块状态<b>存入或取出</b>一种炼药锅内容物。
     * 在一般情况下，我们测试的是该方块状态是否与炼药锅内容物是同种方快，或为空炼药锅。
     *
     * @param state           需判断的方块状态。
     * @param cauldronContent 需存入或取出判断的炼药锅内容物
     * @return 若可以存入或取出，返回 {@code true}
     */
    public static boolean compatibleFor(BlockState state, Block cauldronContent) {
        if (cauldronContent == Blocks.CAULDRON) return false;
        return state.is(Blocks.CAULDRON) || state.is(cauldronContent);
    }

    /**
     * 对一个方块状态，测试它还能容纳多少层指定的炼药锅内容物。
     * 若不能容纳，返回0。
     *
     * @param state           需判断的方块状态
     * @param cauldronContent 待存入的炼药锅内容物
     * @return 还能容纳的层数
     */
    public static int remainSpaceFor(BlockState state, Block cauldronContent) {
        if (!compatibleFor(state, cauldronContent)) return 0;
        int contentMaxLevel = maxLevel(cauldronContent);
        return Math.max(0, contentMaxLevel - currentLevel(state));
    }

    /**
     * 根据一种炼药锅内容物，获取到它在特定层数下的方块状态。若{@code cauldronContent} 方法参数不是一种炼药锅，
     * 返回其默认方块状态。
     *
     * @param cauldronContent 需获取方块状态的炼药锅内容物
     * @param cauldronLevel   需获取的方块状态的层数
     * @return 炼药锅内容物在对应层数下的方块状态
     * @apiNote 此方法不检测 {@code cauldronLevel} 参数的合法性，而是将其钳制在 {@code 0}与最大层数之间。
     * 此外，此方法暂时不考虑炼药锅有除了 {@code level}以外的方块状态。<s>等出现兼容性问题以后再改</s>
     */
    public static BlockState getStateFromContentAndLevel(Block cauldronContent, int cauldronLevel) {
        if (!(cauldronContent instanceof AbstractCauldronBlock cauldron)) return cauldronContent.defaultBlockState();
        if (cauldronLevel <= 0) return Blocks.CAULDRON.defaultBlockState();
        BlockState state = cauldronContent.defaultBlockState();
        if (state.hasProperty(LEVEL_3)) {
            return state.setValue(LEVEL_3, Math.min(3, cauldronLevel));
        }
        if (state.hasProperty(LEVEL_4)) {
            return state.setValue(LEVEL_4, Math.min(4, cauldronLevel));
        }
        return Optional.of(cauldron)
            .map(CauldronFluidContent::getForBlock)
            .filter(content -> content.levelProperty != null)
            .map(content -> state.setValue(content.levelProperty,
                Math.min(content.maxLevel, cauldronLevel)))
            .orElse(state);
    }

    /**
     * 根据一种炼药锅内容物，获取到它在装满时的方块状态。若{@code cauldronContent}方法参数不是一种炼药锅，
     * 返回其默认方块状态。
     *
     * @param cauldronContent 需获取方块状态的炼药锅内容物
     * @return 炼药锅内容物在装满时的方块状态
     */
    public static BlockState fullState(Block cauldronContent) {
        if (cauldronContent == Blocks.CAULDRON) return Blocks.CAULDRON.defaultBlockState();
        BlockState state = cauldronContent.defaultBlockState();
        if (!(cauldronContent instanceof AbstractCauldronBlock cauldron)) return state;
        if (state.hasProperty(LEVEL_3)) {
            return state.setValue(LEVEL_3, 3);
        }
        if (state.hasProperty(LEVEL_4)) {
            return state.setValue(LEVEL_4, 4);
        }
        return Optional.of(cauldron)
            .map(CauldronFluidContent::getForBlock)
            .filter(content -> content.levelProperty != null)
            .map(content -> state.setValue(content.levelProperty, content.maxLevel))
            .orElse(state);
    }

    /**
     * 在指定的维度中的指定位置，将其方块设定为一种炼药锅内容物在特定层数下的方块状态。
     *
     * @param level           指定的维度
     * @param pos             指定的方块位置
     * @param cauldronContent 需设定的炼药锅内容物
     * @param cauldronLevel   需设定的层数
     * @apiNote 本方法不会对被替换的方块状态进行任何检查。
     */
    public static void setContentLevel(Level level, BlockPos pos, Block cauldronContent, int cauldronLevel) {
        level.setBlockAndUpdate(pos, getStateFromContentAndLevel(cauldronContent, cauldronLevel));
    }

    /**
     * 模仿 {@link net.neoforged.neoforge.fluids.capability.IFluidHandler#fill}方法的设定，此方法用于
     * 向炼药锅中添加内容物。
     *
     * @param level           指定的维度
     * @param pos             指定的方块位置
     * @param cauldronContent 要添加的炼药锅内容物
     * @param fillLevel       要添加的层数
     * @param simulate        本次添加是否为模拟（若为模拟，则不会对维度内的方块状态产生实际影响）
     * @return 成功添加的炼药锅内容物的层数
     * @apiNote 此方法会检查炼药锅内容物的合法性，若无法放入则不会添加。
     * @see net.neoforged.neoforge.fluids.capability.IFluidHandler#fill
     */
    public static int fill(Level level, BlockPos pos, Block cauldronContent, int fillLevel, boolean simulate) {
        if (fillLevel <= 0) return 0;
        BlockState state = level.getBlockState(pos);
        int remainSpace = remainSpaceFor(state, cauldronContent);
        if (remainSpace <= 0) return 0;
        int filled = Math.min(remainSpace, fillLevel);
        if (!simulate) {
            setContentLevel(level, pos, cauldronContent, currentLevel(state) + filled);
        }
        return filled;
    }

    /**
     * 模仿 {@link net.neoforged.neoforge.fluids.capability.IFluidHandler#drain(FluidStack, IFluidHandler.FluidAction)}
     * 方法的设定，此方法用于从炼药锅中提取内容物。
     *
     * @param level           指定的维度
     * @param pos             指定的方块位置
     * @param cauldronContent 要提取的炼药锅内容物
     * @param drainLevel      要提取的层数
     * @param simulate        本次提取是否为模拟（若为模拟，则不会对维度内的方块状态产生实际影响）
     * @return 提取到的炼药锅内容物的层数
     * @apiNote 此方法会检查炼药锅内容物的合法性，若无法提取则不会提取到内容物。
     * @see net.neoforged.neoforge.fluids.capability.IFluidHandler#fill
     */
    public static int drain(Level level, BlockPos pos, Block cauldronContent, int drainLevel, boolean simulate) {
        if (drainLevel <= 0) return 0;
        BlockState state = level.getBlockState(pos);
        if (!compatibleFor(state, cauldronContent)) return 0;
        int currentLevel = currentLevel(state);
        int drained = Math.min(drainLevel, currentLevel);
        if (drained <= 0) return 0;
        if (!simulate) {
            setContentLevel(level, pos, cauldronContent, currentLevel - drained);
        }
        return drained;
    }

    /**
     * 检测是否能从指定的方块状态中提取出指定层数的炼药锅内容物。<br/>
     * 原则上，若 {@code level.getBlockState(pos) == state}，此方法的调用结果应等价于
     * {@code drain(level, pos, cauldronContent, drainLevel, true) == drainLevel}。
     *
     * @param state           被提取的方块状态
     * @param cauldronContent 要提取的炼药锅内容物
     * @param drainLevel      要提取的层数
     * @return 若能提取出指定层数的炼药锅内容物，返回 {@code true}
     */
    public static boolean compatibleForDrain(BlockState state, Block cauldronContent, int drainLevel) {
        if (drainLevel <= 0) return true;
        return compatibleFor(state, cauldronContent) && currentLevel(state) >= drainLevel;
    }

    /**
     * 检测是否能向指定的方块状态完整地、没有剩余地添加指定层数的炼药锅内容物。<br/>
     * 原则上，若 {@code level.getBlockState(pos) == state}，此方法的调用结果应等价于
     * {@code fill(level, pos, cauldronContent, fillLevel, true) == fillLevel}。
     *
     * @param state           被添加的方块状态
     * @param cauldronContent 要添加的炼药锅内容物
     * @param fillLevel       要添加的层数
     * @return 若能完整地添加指定层数的炼药锅内容物，返回 {@code true}
     */
    public static boolean compatibleForFill(BlockState state, Block cauldronContent, int fillLevel) {
        if (fillLevel <= 0) return true;
        return remainSpaceFor(state, cauldronContent) >= fillLevel;
    }
}
