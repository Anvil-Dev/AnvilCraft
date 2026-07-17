package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CfaInterfaceScanner {

    public record PrioritizedInterfaces<T>(List<T> active, List<T> passive) {
        public List<T> preferred() {
            return active.isEmpty() ? passive : active;
        }

        public int size() {
            return preferred().size();
        }
    }

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
            BlockEntity be = getLoadedBlockEntity(level, checkPos);
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
            BlockEntity be = getLoadedBlockEntity(level, checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logisticsBe) {
                result.add(logisticsBe.getItemHandler());
            }
        });
        return result;
    }

    public static PrioritizedInterfaces<IItemHandler> findPrioritizedLogisticsInterfaces(
        Level level, BlockPos controllerPos
    ) {
        List<IItemHandler> active = new ArrayList<>();
        List<IItemHandler> passive = new ArrayList<>();
        if (level == null) return new PrioritizedInterfaces<>(active, passive);
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = getLoadedBlockEntity(level, checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logisticsBe) {
                (isActive(logisticsBe) ? active : passive).add(logisticsBe.getItemHandler());
            }
        });
        return new PrioritizedInterfaces<>(active, passive);
    }

    public static List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces(
        Level level, BlockPos controllerPos
    ) {
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> result = new ArrayList<>();
        if (level == null) return result;
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = getLoadedBlockEntity(level, checkPos);
            if (be instanceof CelestialForgingAnvilFluidInterfaceBlockEntity fluidBe) {
                result.add(fluidBe);
            }
        });
        return result;
    }

    public static PrioritizedInterfaces<CelestialForgingAnvilFluidInterfaceBlockEntity>
        findPrioritizedFluidInterfaces(Level level, BlockPos controllerPos) {
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> active = new ArrayList<>();
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> passive = new ArrayList<>();
        if (level == null) return new PrioritizedInterfaces<>(active, passive);
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = getLoadedBlockEntity(level, checkPos);
            if (be instanceof CelestialForgingAnvilFluidInterfaceBlockEntity fluidBe) {
                (isActive(fluidBe) ? active : passive).add(fluidBe);
            }
        });
        return new PrioritizedInterfaces<>(active, passive);
    }

    private static boolean isActive(BlockEntity blockEntity) {
        return blockEntity.getBlockState().hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
               && blockEntity.getBlockState().getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    public static <T extends BlockEntity> Map<BlockPos, T> getInterfacesMap(
        Class<T> type, Level level, BlockPos controllerPos
    ) {
        Map<BlockPos, T> result = new HashMap<>();
        if (level == null) return result;
        scanAdjacentBlocks(controllerPos, level, (checkPos) -> {
            BlockEntity be = getLoadedBlockEntity(level, checkPos);
            if (type.isInstance(be)) {
                BlockPos relOffset = new BlockPos(
                    checkPos.getX() - controllerPos.getX(), 0,
                    checkPos.getZ() - controllerPos.getZ());
                result.put(relOffset, type.cast(be));
            }
        });
        return result;
    }

    /**
     * Reads a block entity without synchronously loading a chunk. This is important while a
     * celestial forging anvil is being removed as part of chunk unloading.
     */
    private static BlockEntity getLoadedBlockEntity(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockEntity(pos);
        }
        return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }
}
