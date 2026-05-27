package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OverseerUtil {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> placedOverseers = new ConcurrentHashMap<>();

    public static void onLoadOverseer(Level level, OverseerBlockEntity overseerBlockEntity) {
        if (!shouldTrack(level, overseerBlockEntity)) return;
        placedOverseers
            .computeIfAbsent(level.dimension(), dim -> new HashSet<>())
            .add(overseerBlockEntity.getBlockPos());
    }

    public static void onUnloadOverseer(Level level, OverseerBlockEntity overseerBlockEntity) {
        if (!shouldTrack(level, overseerBlockEntity)) return;
        placedOverseers
            .getOrDefault(level.dimension(), Set.of())
            .remove(overseerBlockEntity.getBlockPos());
    }

    public static Set<BlockPos> getPlacedOverseers(ResourceKey<Level> dimension) {
        return OverseerUtil.placedOverseers.getOrDefault(dimension, Set.of());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean shouldTrack(Level level, OverseerBlockEntity overseerBlockEntity) {
        if (level.isClientSide) return false;
        BlockState state = overseerBlockEntity.getBlockState();
        return ((OverseerBlock) state.getBlock()).isMainPart(state);
    }
}
