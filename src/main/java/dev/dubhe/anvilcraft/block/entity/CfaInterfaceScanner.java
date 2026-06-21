package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CfaInterfaceScanner {

    private CfaInterfaceScanner() {
    }

    public static void scanAdjacentBlocks(BlockPos controllerPos, Level level, Consumer<BlockPos> consumer) {
        if (level == null) return;
        int y = controllerPos.getY();
        int cx = controllerPos.getX();
        int cz = controllerPos.getZ();
        for (int dx = -1; dx <= 1; dx++) {
            consumer.accept(new BlockPos(cx + dx, y, cz - 2));
        }
        for (int dx = -1; dx <= 1; dx++) {
            consumer.accept(new BlockPos(cx + dx, y, cz + 2));
        }
        for (int dz = -1; dz <= 1; dz++) {
            consumer.accept(new BlockPos(cx - 2, y, cz + dz));
        }
        for (int dz = -1; dz <= 1; dz++) {
            consumer.accept(new BlockPos(cx + 2, y, cz + dz));
        }
    }

    public static List<CelestialForgingAnvilLaserInterfaceBlockEntity> findLaserInterfaces(
        Level level, BlockPos controllerPos
    ) {
        List<CelestialForgingAnvilLaserInterfaceBlockEntity> result = new ArrayList<>();
        if (level == null) return result;
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserBe) {
                result.add(laserBe);
            }
        });
        return result;
    }

    public static List<IItemHandler> findLogisticsInterfaces(Level level, BlockPos controllerPos) {
        List<IItemHandler> result = new ArrayList<>();
        if (level == null) return result;
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logisticsBe) {
                result.add(logisticsBe.getItemHandler());
            }
        });
        return result;
    }

    public static List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces(
        Level level, BlockPos controllerPos
    ) {
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> result = new ArrayList<>();
        if (level == null) return result;
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilFluidInterfaceBlockEntity fluidBe) {
                result.add(fluidBe);
            }
        });
        return result;
    }

    public static <T extends BlockEntity> Map<BlockPos, T> getInterfacesMap(
        Class<T> type, Level level, BlockPos controllerPos
    ) {
        Map<BlockPos, T> result = new HashMap<>();
        if (level == null) return result;
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (type.isInstance(be)) {
                BlockPos relOffset = new BlockPos(
                    checkPos.getX() - controllerPos.getX(), 0,
                    checkPos.getZ() - controllerPos.getZ());
                result.put(relOffset, type.cast(be));
            }
        });
        return result;
    }
}
