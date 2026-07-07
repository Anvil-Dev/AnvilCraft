package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CfaInterfaceScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for CFA megastructure handlers.
 * Provides utility methods for scanning adjacent interfaces and item/fluid manipulation.
 */
public abstract class BaseMegastructureHandler implements IMegastructureHandler {

    // === NBT defaults (most handlers don't persist data) ===

    @Override
    public void saveAdditional(ValueOutput output) {
    }

    @Override
    public void loadAdditional(ValueInput input) {
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

    // === Interface scanning ===

    protected List<CelestialForgingAnvilLaserInterfaceBlockEntity> findLaserInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected List<ResourceHandler<ItemResource>> findLogisticsInterfaces(CelestialForgingAnvilBlockEntity be) {
        return CfaInterfaceScanner.findLogisticsInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findFluidInterfaces(be.getLevel(), be.getBlockPos());
    }

    protected void scanAdjacentBlocks(Consumer<BlockPos> consumer, CelestialForgingAnvilBlockEntity be) {
        CfaInterfaceScanner.scanAdjacentBlocks(be.getBlockPos(), be.getLevel(), consumer);
    }

    // === Item manipulation (26.1 ResourceHandler API) ===

    /**
     * Insert a stack into a {@link ResourceHandler}, spreading across available slots.
     * Uses {@link Transaction} — the entire insert commits at once.
     */
    protected static ItemStack insertIntoHandler(ResourceHandler<ItemResource> handler, ItemStack stack) {
        ItemStack remainder = stack.copy();
        try (Transaction tx = Transaction.openRoot()) {
            for (int slot = 0; slot < handler.size() && !remainder.isEmpty(); slot++) {
                int inserted = handler.insert(slot, ItemResource.of(remainder), remainder.getCount(), tx);
                if (inserted > 0) {
                    remainder.shrink(inserted);
                }
            }
            tx.commit();
        }
        return remainder;
    }

    /**
     * Read an ItemStack from a slot of a {@link ResourceHandler}.
     * {@code getStackFrom} is protected, so we use {@code getResource + getAmountAsInt + toStack}.
     */
    protected static ItemStack getStackFromHandler(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        return resource.toStack(handler.getAmountAsInt(slot));
    }

    /**
     * Extract from a specific slot without simulation — committed immediately.
     */
    protected static ItemStack extractFromHandler(ResourceHandler<ItemResource> handler, int slot, int amount) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, handler.getAmountAsInt(slot));
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, toExtract, tx);
            if (extracted > 0) {
                tx.commit();
                return resource.toStack(extracted);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Insert fluid into a tank slot.
     */
    protected static int insertFluid(ResourceHandler<FluidResource> tank, int slot, FluidStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = tank.insert(slot, FluidResource.of(stack), stack.getAmount(), tx);
            if (inserted > 0) tx.commit();
            return inserted;
        }
    }

    /**
     * Extract fluid from a tank slot.
     */
    protected static void drainFluid(ResourceHandler<FluidResource> tank, int slot, FluidStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            tank.extract(slot, FluidResource.of(stack), stack.getAmount(), tx);
            tx.commit();
        }
    }

    /**
     * Read a FluidStack from a tank slot.
     */
    protected static FluidStack getFluidFromTank(ResourceHandler<FluidResource> tank, int slot) {
        FluidResource resource = tank.getResource(slot);
        if (resource.isEmpty()) return FluidStack.EMPTY;
        return resource.toStack((int) tank.getAmountAsLong(slot));
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

    // === Laser helpers ===

    protected int countValidLasers(CelestialForgingAnvilBlockEntity be, int threshold) {
        List<CelestialForgingAnvilLaserInterfaceBlockEntity> lasers = this.findLaserInterfaces(be);
        int count = 0;
        for (CelestialForgingAnvilLaserInterfaceBlockEntity laser : lasers) {
            if (laser.getReceivedLaserLevel() >= threshold) {
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
