package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CfaInterfaceScanner;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseMegastructureHandler implements IMegastructureHandler {

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
    }

    protected List<CelestialForgingAnvilLaserInterfaceBlockEntity> findLaserInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected List<IItemHandler> findLogisticsInterfaces(CelestialForgingAnvilBlockEntity be) {
        return CfaInterfaceScanner.findLogisticsInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findFluidInterfaces(be.getLevel(), be.getBlockPos());
    }

    /** Consumes every Primordial Matter stack exposed by the connected fluid interfaces. */
    protected int consumePrimordialMatter(CelestialForgingAnvilBlockEntity be) {
        long consumed = 0L;
        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidInterface : findFluidInterfaces(be)) {
            consumed += fluidInterface.drainFluid(ModFluids.PRIMORDIAL_MATTER.get());
        }
        return (int) Math.min(consumed, Integer.MAX_VALUE);
    }

    protected CfaInterfaceScanner.PrioritizedInterfaces<IItemHandler> findOutputLogisticsInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findPrioritizedLogisticsInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected CfaInterfaceScanner.PrioritizedInterfaces<CelestialForgingAnvilFluidInterfaceBlockEntity>
        findOutputFluidInterfaces(CelestialForgingAnvilBlockEntity be) {
        return CfaInterfaceScanner.findPrioritizedFluidInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected void scanAdjacentBlocks(Consumer<BlockPos> consumer, CelestialForgingAnvilBlockEntity be) {
        CfaInterfaceScanner.scanAdjacentBlocks(be.getBlockPos(), be.getLevel(), consumer);
    }

    protected static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    protected record ItemOutputResult(ItemStack remainder, int nextIndex) {
    }

    protected static ItemOutputResult insertOutputItem(
        CfaInterfaceScanner.PrioritizedInterfaces<IItemHandler> interfaces, ItemStack stack, int startIndex
    ) {
        return insertOutputItem(interfaces.preferred(), stack, startIndex);
    }

    private static ItemOutputResult insertOutputItem(List<IItemHandler> handlers, ItemStack stack, int startIndex) {
        ItemStack remainder = stack.copy();
        if (handlers.isEmpty() || remainder.isEmpty()) return new ItemOutputResult(remainder, startIndex);

        int nextIndex = Math.floorMod(startIndex, handlers.size());
        for (int attempt = 0; attempt < handlers.size() && !remainder.isEmpty(); attempt++) {
            int index = (Math.floorMod(startIndex, handlers.size()) + attempt) % handlers.size();
            int before = remainder.getCount();
            remainder = insertIntoHandler(handlers.get(index), remainder);
            if (remainder.getCount() < before) {
                nextIndex = (index + 1) % handlers.size();
            }
        }
        return new ItemOutputResult(remainder, nextIndex);
    }

    protected record FluidOutputResult(int filled, int nextIndex) {
    }

    protected static FluidOutputResult fillOutputFluid(
        CfaInterfaceScanner.PrioritizedInterfaces<CelestialForgingAnvilFluidInterfaceBlockEntity> interfaces,
        FluidStack stack,
        int startIndex
    ) {
        return fillOutputFluid(interfaces.preferred(), stack, startIndex);
    }

    private static FluidOutputResult fillOutputFluid(
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> interfaces, FluidStack stack, int startIndex
    ) {
        if (interfaces.isEmpty() || stack.isEmpty()) return new FluidOutputResult(0, startIndex);

        int remaining = stack.getAmount();
        int nextIndex = Math.floorMod(startIndex, interfaces.size());
        int normalizedStart = Math.floorMod(startIndex, interfaces.size());
        for (int attempt = 0; attempt < interfaces.size() && remaining > 0; attempt++) {
            int index = (normalizedStart + attempt) % interfaces.size();
            int filled = interfaces.get(index).getInternalFluidHandler().fill(
                stack.copyWithAmount(remaining), IFluidHandler.FluidAction.EXECUTE
            );
            if (filled > 0) {
                remaining -= Math.min(filled, remaining);
                nextIndex = (index + 1) % interfaces.size();
            }
        }
        return new FluidOutputResult(stack.getAmount() - remaining, nextIndex);
    }

    protected static void dropItemOnGround(ItemStack stack, Level level, BlockPos pos) {
        if (level == null || stack.isEmpty()) return;
        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
            level,
            pos.getX() + 0.5,
            pos.getY() + 1,
            pos.getZ() + 0.5,
            stack
        );
        level.addFreshEntity(entity);
    }

    protected int countValidLasers(CelestialForgingAnvilBlockEntity be, int threshold) {
        List<CelestialForgingAnvilLaserInterfaceBlockEntity> lasers = findLaserInterfaces(be);
        int count = 0;
        for (CelestialForgingAnvilLaserInterfaceBlockEntity laser : lasers) {
            int level = laser.getReceivedLaserLevel();
            if (level >= threshold) {
                count++;
            }
        }
        return count;
    }

    protected Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> getLaserInterfacesMap(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilLaserInterfaceBlockEntity.class,
            be.getLevel(),
            be.getBlockPos()
        );
    }

    protected Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> getLogisticsInterfacesMap(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.class,
            be.getLevel(),
            be.getBlockPos()
        );
    }

    protected Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> getFluidInterfacesMap(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilFluidInterfaceBlockEntity.class,
            be.getLevel(),
            be.getBlockPos()
        );
    }
}
