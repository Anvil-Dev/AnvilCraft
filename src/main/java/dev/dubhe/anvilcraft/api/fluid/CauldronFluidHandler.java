package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record CauldronFluidHandler(Level level, BlockPos pos) implements IFluidHandler {
    private static final int DEFAULT_TOTAL_AMOUNT = 1000;

    public CauldronFluidHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public static boolean isCauldron(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.CAULDRON) || fluidForContent(state.getBlock()) != null;
    }

    @Nullable
    public static CauldronFluidHandler create(Level level, BlockPos pos) {
        return isCauldron(level, pos) ? new CauldronFluidHandler(level, pos) : null;
    }

    public int layerAmount() {
        Block content = contentBlock();
        if (content == null) {
            return 0;
        }
        return nextDrainAmount(content, currentLevel());
    }

    public int nextDrainAmount() {
        Block content = contentBlock();
        return content == null ? 0 : nextDrainAmount(content, currentLevel());
    }

    private static int nextDrainAmount(Block contentBlock, int currentLevel) {
        if (currentLevel <= 0) {
            return 0;
        }
        return amountBetweenLevels(contentBlock, currentLevel - 1, currentLevel);
    }

    public int layerAmountFor(FluidStack stack) {
        Block content = contentForFluid(stack.getFluid());
        return content == null ? 0 : nextFillAmount(content, currentLevel());
    }

    public int nextFillAmount(FluidStack stack) {
        return layerAmountFor(stack);
    }

    private static int nextFillAmount(Block contentBlock, int currentLevel) {
        int maxLevel = CauldronUtil.maxLevel(contentBlock);
        if (currentLevel >= maxLevel) {
            return 0;
        }
        return amountBetweenLevels(contentBlock, currentLevel, currentLevel + 1);
    }

    public int minLayerAmount() {
        Block content = contentBlock();
        if (content == null) {
            return DEFAULT_TOTAL_AMOUNT;
        }
        int maxLevel = CauldronUtil.maxLevel(content);
        if (maxLevel <= 0) {
            return DEFAULT_TOTAL_AMOUNT;
        }
        return totalAmount(content) / maxLevel;
    }

    public int layerTransferAmountTo(CauldronFluidHandler target) {
        FluidStack stored = getFluidInTank(0);
        if (stored.isEmpty()) {
            return 0;
        }
        int drainAmount = nextDrainAmount();
        int fillAmount = target.nextFillAmount(stored);
        if (drainAmount <= 0 || fillAmount <= 0) {
            return 0;
        }
        return Math.max(drainAmount, fillAmount);
    }

    public boolean canTransferLayerTo(CauldronFluidHandler target) {
        return transferLayerTo(target, FluidAction.SIMULATE) > 0;
    }

    public int fillLayer(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        Block content = contentForFluid(resource.getFluid());
        if (content == null) {
            return 0;
        }
        int filledAmount = nextFillAmount(content, currentLevel());
        if (filledAmount <= 0 || CauldronUtil.fill(level, pos, content, 1, action.simulate()) != 1) {
            return 0;
        }
        return filledAmount;
    }

    public int transferLayerTo(CauldronFluidHandler target, FluidAction action) {
        Block content = contentBlock();
        Fluid fluid = content == null ? null : fluidForContent(content);
        if (fluid == null) {
            return 0;
        }
        Block targetContent = contentForFluid(fluid);
        if (targetContent == null) {
            return 0;
        }
        int drainAmount = nextDrainAmount(content, currentLevel());
        int fillAmount = target.nextFillAmount(new FluidStack(fluid, totalAmount(targetContent)));
        if (drainAmount <= 0 || fillAmount <= 0) {
            return 0;
        }
        if (CauldronUtil.drain(level, pos, content, 1, true) != 1
            || CauldronUtil.fill(target.level, target.pos, targetContent, 1, true) != 1) {
            return 0;
        }
        if (action.execute()) {
            CauldronUtil.drain(level, pos, content, 1, false);
            CauldronUtil.fill(target.level, target.pos, targetContent, 1, false);
        }
        return Math.max(drainAmount, fillAmount);
    }

    public int currentLevel() {
        return CauldronUtil.currentLevel(level.getBlockState(pos));
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0) {
            return FluidStack.EMPTY;
        }
        Block content = contentBlock();
        Fluid fluid = content == null ? null : fluidForContent(content);
        int currentLevel = currentLevel();
        if (fluid == null || currentLevel <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amountAtLevel(content, currentLevel));
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank != 0) {
            return 0;
        }
        Block content = contentBlock();
        if (content == null) {
            return DEFAULT_TOTAL_AMOUNT;
        }
        return totalAmount(content);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && !stack.isEmpty() && contentForFluid(stack.getFluid()) != null;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        Block content = contentForFluid(resource.getFluid());
        if (content == null) {
            return 0;
        }
        int startLevel = currentLevel();
        int maxLevel = CauldronUtil.maxLevel(content);
        int filledLevels = 0;
        int filledAmount = 0;
        while (startLevel + filledLevels < maxLevel) {
            int nextAmount = nextFillAmount(content, startLevel + filledLevels);
            if (resource.getAmount() - filledAmount < nextAmount) {
                break;
            }
            filledLevels++;
            filledAmount += nextAmount;
        }
        if (filledLevels <= 0) {
            return 0;
        }
        int actuallyFilledLevels = CauldronUtil.fill(level, pos, content, filledLevels, action.simulate());
        return amountBetweenLevels(content, startLevel, startLevel + actuallyFilledLevels);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack stored = getFluidInTank(0);
        if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Block content = contentBlock();
        Fluid fluid = content == null ? null : fluidForContent(content);
        if (fluid == null) {
            return FluidStack.EMPTY;
        }
        int startLevel = currentLevel();
        int drainedLevels = 0;
        int drainedAmount = 0;
        while (startLevel - drainedLevels > 0) {
            int nextAmount = nextDrainAmount(content, startLevel - drainedLevels);
            if (maxDrain - drainedAmount < nextAmount) {
                break;
            }
            drainedLevels++;
            drainedAmount += nextAmount;
        }
        if (drainedLevels <= 0) {
            return FluidStack.EMPTY;
        }
        drainedLevels = CauldronUtil.drain(level, pos, content, drainedLevels, action.simulate());
        if (drainedLevels <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amountBetweenLevels(content, startLevel - drainedLevels, startLevel));
    }

    @Nullable
    private Block contentBlock() {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.CAULDRON) || CauldronUtil.currentLevel(state) <= 0) {
            return null;
        }
        return state.getBlock();
    }

    @Nullable
    private static Block contentForFluid(Fluid fluid) {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (CompatUtil.F2C_TRANSFORM.containsKey(key)) {
            return Objects.requireNonNull(CompatUtil.F2C_TRANSFORM.get(key)).get();
        }
        CauldronFluidContent content = CauldronFluidContent.getForFluid(fluid);
        return content == null ? null : content.block;
    }

    @Nullable
    private static Fluid fluidForContent(Block contentBlock) {
        CauldronFluidContent content = CauldronFluidContent.getForBlock(contentBlock);
        if (content != null) {
            return content.fluid;
        }
        ResourceLocation fluidId = CompatUtil.getFluidFromCauldron(contentBlock);
        return fluidId == null ? null : BuiltInRegistries.FLUID.get(fluidId);
    }

    private static int amountBetweenLevels(Block contentBlock, int fromLevel, int toLevel) {
        return amountAtLevel(contentBlock, toLevel) - amountAtLevel(contentBlock, fromLevel);
    }

    private static int amountAtLevel(Block contentBlock, int level) {
        int maxLevel = Math.max(1, CauldronUtil.maxLevel(contentBlock));
        int clamped = Math.clamp(level, 0, maxLevel);
        int totalAmount = totalAmount(contentBlock);
        int base = totalAmount / maxLevel;
        int remainder = totalAmount % maxLevel;
        return base * clamped + Math.max(0, clamped - (maxLevel - remainder));
    }

    private static int totalAmount(Block contentBlock) {
        CauldronFluidContent content = CauldronFluidContent.getForBlock(contentBlock);
        return content == null ? DEFAULT_TOTAL_AMOUNT : content.totalAmount;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CauldronFluidHandler(Level level1, BlockPos pos1) && level1 == this.level && pos1.equals(this.pos);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(level) + pos.hashCode();
    }
}
