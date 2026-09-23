package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 建造侧流体映射:源液体与分层锅走桶/精确 mB,储罐走原生流体资源事务。
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
        ResourceHandler<FluidResource> handler = fluidHandlerOf(loaded);
        if (handler == null) return Extracted.empty();
        List<TankFluid> tanks = new ArrayList<>();
        try (Transaction transaction = Transaction.openRoot()) {
            for (int tank = 0; tank < handler.size(); tank++) {
                FluidResource fluid = handler.getResource(tank);
                long amount = handler.getAmountAsLong(tank);
                if (fluid.isEmpty() || amount == 0) continue;
                if (amount > Integer.MAX_VALUE || handler.extract(tank, fluid, (int) amount, transaction) != amount) {
                    return new Extracted(List.of(), true);
                }
                tanks.add(new TankFluid(tank, fluid.toStack((int) amount)));
            }
        }
        return new Extracted(List.copyOf(tanks), false);
    }

    public static void insert(BlockEntity blockEntity, List<TankFluid> tanks) {
        ResourceHandler<FluidResource> handler = fluidHandlerOf(blockEntity);
        if (handler == null) return;
        try (Transaction transaction = Transaction.openRoot()) {
            for (TankFluid tank : tanks) {
                FluidStack fluid = tank.fluid();
                if (fluid.isEmpty()) continue;
                if (tank.tank() < 0 || tank.tank() >= handler.size()
                    || handler.insert(tank.tank(), FluidResource.of(fluid), fluid.getAmount(), transaction) != fluid.getAmount()) {
                    throw new IllegalArgumentException("Blueprint tank cannot accept its supplied fluid");
                }
            }
            transaction.commit();
        }
        blockEntity.setChanged();
    }

    public static boolean takeExactFluid(ItemAccess access, FluidStack needed) {
        return transferExactFluid(access, needed, true);
    }

    public static boolean canProvideExactFluid(ItemStack stack, FluidStack needed) {
        if (stack.isEmpty()) return false;
        var items = new ItemStacksResourceHandler(1);
        items.set(0, ItemResource.of(stack), stack.getCount());
        return transferExactFluid(ItemAccess.forHandlerIndexStrict(items, 0), needed, false);
    }

    private static boolean transferExactFluid(ItemAccess access, FluidStack needed, boolean commit) {
        if (needed.isEmpty() || access.getResource().isEmpty()) return false;
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return false;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(FluidResource.of(needed), needed.getAmount(), transaction) != needed.getAmount()) return false;
            if (commit) transaction.commit();
            return true;
        }
    }

    @Nullable
    private static ResourceHandler<FluidResource> fluidHandlerOf(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof IFluidResourceHandlerHolder holder ? holder.getFluidHandler() : null;
    }

    private static int currentAmount(CauldronFluidContent content, BlockState state) {
        int level = content.currentLevel(state);
        if (level <= 0) return 0;
        if (content.levelProperty == null || content.maxLevel <= 0) {
            return content.totalAmount;
        }
        return (int) ((long) content.totalAmount * level / content.maxLevel);
    }
}
