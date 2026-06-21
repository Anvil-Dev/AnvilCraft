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
