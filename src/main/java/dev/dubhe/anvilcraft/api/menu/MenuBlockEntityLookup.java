package dev.dubhe.anvilcraft.api.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 菜单客户端构造在 {@link Level#getBlockEntity(BlockPos)} 失败时的查找扩展。
 * 查找器在客户端与服务器线程都可能被调用；不得改变世界状态。
 */
public final class MenuBlockEntityLookup {
    private static final List<Finder> FINDERS = new CopyOnWriteArrayList<>();

    private MenuBlockEntityLookup() {
    }

    public static void register(Finder finder) {
        FINDERS.add(finder);
    }

    public static @Nullable BlockEntity find(Level level, BlockPos pos, Class<? extends BlockEntity> type) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (type.isInstance(existing)) return existing;
        for (Finder finder : FINDERS) {
            BlockEntity found = finder.find(level, pos, type);
            if (type.isInstance(found)) return found;
        }
        return null;
    }

    @FunctionalInterface
    public interface Finder {
        @Nullable
        BlockEntity find(Level level, BlockPos pos, Class<? extends BlockEntity> type);
    }
}
