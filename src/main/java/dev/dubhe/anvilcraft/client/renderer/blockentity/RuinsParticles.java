package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public final class RuinsParticles {
    private static final Cache<BlockPos, BlockState> RECENT = CacheBuilder.newBuilder()
        .maximumSize(256).expireAfterWrite(5, TimeUnit.SECONDS).build();
    private static WeakReference<Level> cachedLevel = new WeakReference<>(null);

    private RuinsParticles() {
    }

    public static void remember(RuinsBlockEntity ruins) {
        Level level = ruins.getLevel();
        if (level == null || !level.isClientSide) return;
        checkLevel(level);
        RECENT.put(ruins.getBlockPos(), ruins.getDisplayState());
    }

    public static BlockState displayState(Level level, BlockPos pos, BlockState fallback) {
        if (level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) return ruins.getDisplayState();
        checkLevel(level);
        BlockState recent = RECENT.getIfPresent(pos);
        return recent == null ? fallback : recent;
    }

    private static void checkLevel(Level level) {
        if (cachedLevel.get() == level) return;
        RECENT.invalidateAll();
        cachedLevel = new WeakReference<>(level);
    }
}
