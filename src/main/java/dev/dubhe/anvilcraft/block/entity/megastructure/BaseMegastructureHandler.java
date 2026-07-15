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
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 锻星砧巨构处理器基类，提供接口扫描及物品、流体操作工具。
 */
public abstract class BaseMegastructureHandler implements IMegastructureHandler {

    // === 默认持久化实现，多数处理器没有额外数据 ===

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

    // === 接口扫描 ===

    protected List<CelestialForgingAnvilLaserInterfaceBlockEntity> findLaserInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findLaserInterfaces(Objects.requireNonNull(be.getLevel()), be.getBlockPos());
    }

    protected List<ResourceHandler<ItemResource>> findLogisticsInterfaces(CelestialForgingAnvilBlockEntity be) {
        return CfaInterfaceScanner.findLogisticsInterfaces(Objects.requireNonNull(be.getLevel()), be.getBlockPos());
    }

    protected List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findFluidInterfaces(Objects.requireNonNull(be.getLevel()), be.getBlockPos());
    }

    protected CfaInterfaceScanner.PrioritizedInterfaces<ResourceHandler<ItemResource>> findOutputLogisticsInterfaces(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.findPrioritizedLogisticsInterfaces(
            Objects.requireNonNull(be.getLevel()), be.getBlockPos()
        );
    }

    protected CfaInterfaceScanner.PrioritizedInterfaces<CelestialForgingAnvilFluidInterfaceBlockEntity>
        findOutputFluidInterfaces(CelestialForgingAnvilBlockEntity be) {
        return CfaInterfaceScanner.findPrioritizedFluidInterfaces(
            Objects.requireNonNull(be.getLevel()), be.getBlockPos()
        );
    }

    protected void scanAdjacentBlocks(Consumer<BlockPos> consumer, CelestialForgingAnvilBlockEntity be) {
        CfaInterfaceScanner.scanAdjacentBlocks(be.getBlockPos(), Objects.requireNonNull(be.getLevel()), consumer);
    }

    // === 物品操作，使用 26.1 ResourceHandler API ===

    /** 使用同一事务把物品栈分散插入处理器的可用槽位。 */
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

    protected record ItemOutputResult(ItemStack remainder, int nextIndex) {
    }

    protected static ItemOutputResult insertOutputItem(
        CfaInterfaceScanner.PrioritizedInterfaces<ResourceHandler<ItemResource>> interfaces,
        ItemStack stack,
        int startIndex
    ) {
        List<ResourceHandler<ItemResource>> handlers = interfaces.preferred();
        ItemStack remainder = stack.copy();
        if (handlers.isEmpty() || remainder.isEmpty()) return new ItemOutputResult(remainder, startIndex);

        int normalizedStart = Math.floorMod(startIndex, handlers.size());
        int nextIndex = normalizedStart;
        for (int attempt = 0; attempt < handlers.size() && !remainder.isEmpty(); attempt++) {
            int index = (normalizedStart + attempt) % handlers.size();
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
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> handlers = interfaces.preferred();
        if (handlers.isEmpty() || stack.isEmpty()) return new FluidOutputResult(0, startIndex);

        int remaining = stack.getAmount();
        int normalizedStart = Math.floorMod(startIndex, handlers.size());
        int nextIndex = normalizedStart;
        for (int attempt = 0; attempt < handlers.size() && remaining > 0; attempt++) {
            int index = (normalizedStart + attempt) % handlers.size();
            ResourceHandler<FluidResource> handler = handlers.get(index).getInternalFluidHandler();
            FluidStack output = stack.copyWithAmount(remaining);
            int filled;
            try (Transaction transaction = Transaction.openRoot()) {
                filled = handler.insert(FluidResource.of(output), remaining, transaction);
                if (filled > 0) transaction.commit();
            }
            if (filled > 0) {
                remaining -= Math.min(filled, remaining);
                nextIndex = (index + 1) % handlers.size();
            }
        }
        return new FluidOutputResult(stack.getAmount() - remaining, nextIndex);
    }

    /** 从 ResourceHandler 指定槽位读取物品栈。 */
    protected static ItemStack getStackFromHandler(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        return resource.toStack(handler.getAmountAsInt(slot));
    }

    /** 从指定槽位立即提取物品，不进行模拟。 */
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

    /** 向指定储罐槽位插入流体。 */
    protected static int insertFluid(ResourceHandler<FluidResource> tank, int slot, FluidStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = tank.insert(slot, FluidResource.of(stack), stack.getAmount(), tx);
            if (inserted > 0) tx.commit();
            return inserted;
        }
    }

    /** 从指定储罐槽位提取流体。 */
    protected static void drainFluid(ResourceHandler<FluidResource> tank, int slot, FluidStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            tank.extract(slot, FluidResource.of(stack), stack.getAmount(), tx);
            tx.commit();
        }
    }

    /** 从指定储罐槽位读取流体栈。 */
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

    // === 激光辅助方法 ===

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
            Objects.requireNonNull(be.getLevel()),
            be.getBlockPos()
        );
    }

    protected Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> getLogisticsInterfacesMap(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.class,
            Objects.requireNonNull(be.getLevel()),
            be.getBlockPos()
        );
    }

    protected Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> getFluidInterfacesMap(
        CelestialForgingAnvilBlockEntity be
    ) {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilFluidInterfaceBlockEntity.class,
            Objects.requireNonNull(be.getLevel()),
            be.getBlockPos()
        );
    }
}
