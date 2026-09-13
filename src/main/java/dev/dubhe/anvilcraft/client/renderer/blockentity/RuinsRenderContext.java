package dev.dubhe.anvilcraft.client.renderer.blockentity;

import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** 仅在当前线程绘制遗迹时，让原渲染器读取伪装视图，不替换真实世界的方块。 */
public final class RuinsRenderContext implements AutoCloseable {
    private static final ThreadLocal<Level> ACTIVE = new ThreadLocal<>();
    @Nullable
    private final Level previous;

    private RuinsRenderContext(Level level) {
        this.previous = ACTIVE.get();
        ACTIVE.set(level);
    }

    public static RuinsRenderContext enter(Level level) {
        return new RuinsRenderContext(level);
    }

    public static boolean isActive(Level level) {
        return ACTIVE.get() == level;
    }

    @Override
    public void close() {
        if (this.previous == null) ACTIVE.remove();
        else ACTIVE.set(this.previous);
    }
}
