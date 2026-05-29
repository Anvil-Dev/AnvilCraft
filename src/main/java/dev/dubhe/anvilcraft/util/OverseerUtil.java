package dev.dubhe.anvilcraft.util;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OverseerUtil {
    private static final Logger logger = LogUtils.getLogger();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> placedOverseers = new ConcurrentHashMap<>();

    public static void onLoadOverseer(Level level, OverseerBlockEntity overseerBlockEntity) {
        if (!shouldTrack(level, overseerBlockEntity)) return;
        placedOverseers
            .computeIfAbsent(level.dimension(), dim -> ConcurrentHashMap.newKeySet())
            .add(overseerBlockEntity.getBlockPos());
    }

    public static void onUnloadOverseer(Level level, OverseerBlockEntity overseerBlockEntity) {
        if (!shouldTrack(level, overseerBlockEntity)) return;
        Set<BlockPos> posSet = placedOverseers.get(level.dimension());
        if (posSet == null) {
            logger.warn("Unknown dimension: {}", level.dimension().location());
            return;
        }
        posSet.remove(overseerBlockEntity.getBlockPos());
    }

    public static Set<BlockPos> getPlacedOverseers(ResourceKey<Level> dimension) {
        return OverseerUtil.placedOverseers.getOrDefault(dimension, Set.of());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean shouldTrack(Level level, OverseerBlockEntity overseerBlockEntity) {
        if (level.isClientSide) return false;
        BlockState state = overseerBlockEntity.getBlockState();
        Block block = state.getBlock();
        if (block instanceof OverseerBlock overseerBlock) {
            return overseerBlock.isMainPart(state);
        } else {
            logger.warn("Invalid block state related to OverseerBlockEntity at {}", overseerBlockEntity.getBlockPos());
            return false;
        }
    }
}
