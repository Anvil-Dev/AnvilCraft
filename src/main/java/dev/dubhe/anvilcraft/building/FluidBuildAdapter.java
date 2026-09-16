package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * 建造侧流体映射:源液体与分层锅走桶/精确 mB,储罐走 IFluidHandler。
 * 不改 OrdinaryBlockAdapter,含水固体仍是 PLACE 的状态属性。
 */
public final class FluidBuildAdapter {
    public static final int BUCKET_AMOUNT = 1000;

    public record TankFluid(int tank, FluidStack fluid) {
    }

    public record Extracted(List<TankFluid> tanks, boolean unmapped) {
        public static Extracted empty() {
            return new Extracted(List.of(), false);
        }
    }

    private FluidBuildAdapter() {
    }

    public static boolean isLiquidBlock(BlockState state) {
        return state.getBlock() instanceof LiquidBlock;
    }

    public static boolean isFilledCauldron(BlockState state) {
        CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
        return content != null && currentAmount(content, state) > 0;
    }

    public static FluidStack liquidOf(BlockState state) {
        if (!isLiquidBlock(state)) {
            return FluidStack.EMPTY;
        }
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isSource()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluidState.getType(), BUCKET_AMOUNT);
    }

    public static FluidStack cauldronFluidOf(BlockState state) {
        CauldronFluidContent content = CauldronFluidContent.getForBlock(state.getBlock());
        if (content == null) {
            return FluidStack.EMPTY;
        }
        int amount = currentAmount(content, state);
        if (amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(content.fluid, amount);
    }

    public static ItemStack bucketOf(FluidStack fluid) {
        if (fluid.isEmpty() || fluid.getAmount() != BUCKET_AMOUNT) {
            return ItemStack.EMPTY;
        }
        Item bucket = fluid.getFluid().getBucket();
        if (bucket == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(bucket);
    }

    public static ItemStack emptyBucket() {
        return new ItemStack(Items.BUCKET);
    }

    public static ItemStack cauldronItem(BlockState state) {
        Item asItem = state.getBlock().asItem();
        if (asItem == Items.AIR
            || asItem == Items.WATER_BUCKET
            || asItem == Items.LAVA_BUCKET
            || asItem == Items.POWDER_SNOW_BUCKET) {
            return new ItemStack(Items.CAULDRON);
        }
        return new ItemStack(asItem);
    }

    public static Extracted extractTanks(BlockState state, @Nullable CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt == null || nbt.isEmpty()) {
            return Extracted.empty();
        }
        BlockEntity loaded = BlockEntity.loadStatic(BlockPos.ZERO, state, nbt, registries);
        IFluidHandler handler = fluidHandlerOf(loaded);
        if (handler == null) {
            return Extracted.empty();
        }
        List<TankFluid> tanks = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            FluidStack drained = handler.drain(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                return new Extracted(List.of(), true);
            }
            if (drained.getAmount() != fluid.getAmount() || !FluidStack.isSameFluidSameComponents(drained, fluid)) {
                return new Extracted(List.of(), true);
            }
            tanks.add(new TankFluid(tank, drained.copy()));
        }
        return new Extracted(List.copyOf(tanks), false);
    }

    public static void insert(BlockEntity blockEntity, List<TankFluid> tanks) {
        IFluidHandler handler = fluidHandlerOf(blockEntity);
        if (handler == null) {
            return;
        }
        for (TankFluid tank : tanks) {
            if (tank.fluid().isEmpty()) {
                continue;
            }
            handler.fill(tank.fluid().copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        blockEntity.setChanged();
    }

    public static boolean takeExactFluid(ItemStack stack, FluidStack needed) {
        if (needed.isEmpty() || stack.isEmpty()) {
            return false;
        }
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return false;
        }
        FluidStack simulated = handler.drain(needed.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (simulated.getAmount() != needed.getAmount()
            || !FluidStack.isSameFluidSameComponents(simulated, needed)) {
            return false;
        }
        handler.drain(needed.copy(), IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    public static boolean canProvideExactFluid(ItemStack stack, FluidStack needed) {
        if (needed.isEmpty() || stack.isEmpty()) {
            return false;
        }
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return false;
        }
        FluidStack simulated = handler.drain(needed.copy(), IFluidHandler.FluidAction.SIMULATE);
        return simulated.getAmount() == needed.getAmount()
            && FluidStack.isSameFluidSameComponents(simulated, needed);
    }

    @Nullable
    private static IFluidHandler fluidHandlerOf(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        if (blockEntity instanceof IFluidHandlerHolder holder) {
            return holder.getFluidHandler();
        }
        return blockEntity instanceof IFluidHandler handler ? handler : null;
    }

    private static int currentAmount(CauldronFluidContent content, BlockState state) {
        if (content.levelProperty == null || content.maxLevel <= 0) {
            return content.totalAmount;
        }
        int level = content.currentLevel(state);
        return (int) ((long) content.totalAmount * level / content.maxLevel);
    }
}
