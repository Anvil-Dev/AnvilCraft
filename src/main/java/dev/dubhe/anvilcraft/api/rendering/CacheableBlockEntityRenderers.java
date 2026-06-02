package dev.dubhe.anvilcraft.api.rendering;

import dev.dubhe.anvilcraft.client.renderer.laser.LaserRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashMap;
import java.util.Map;

public class CacheableBlockEntityRenderers {
    private static final Map<BlockEntityType<?>, CacheableBlockEntityRenderer<?>> RENDERERS = new HashMap<>();

    public static <T extends BlockEntity> void register(
        BlockEntityType<? extends T> type,
        CacheableBlockEntityRenderer<T> renderProvider
    ) {
        RENDERERS.put(type, renderProvider);
    }

    public static CacheableBlockEntityRenderer<?> get(BlockEntityType<?> type) {
        return RENDERERS.get(type);
    }

    static {
        LaserRenderer laserRenderer = new LaserRenderer();
        RENDERERS.put(ModBlockEntities.RUBY_LASER.get(), laserRenderer);
        RENDERERS.put(ModBlockEntities.RUBY_PRISM.get(), laserRenderer);
        RENDERERS.put(ModBlockEntities.LARGE_LASER.get(), laserRenderer);
    }
}
