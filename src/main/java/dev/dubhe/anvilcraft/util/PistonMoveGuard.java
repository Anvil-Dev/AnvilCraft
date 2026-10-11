package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 记录"即将被移动"的方块位置，供吞噬器与 TNT 判定跳过。
 *
 * <p>原版活塞在一次 {@code moveBlocks} 调用内完成移动，单次作用域足够。滑动方块与滑轨站把
 * 方块取出世界后要跨 tick 才放回，吞噬器与邻居更新可能在放回前后动手，因此这条路径使用跨
 * tick 保留，由拥有它的滑动方块实体释放。</p>
 */
public final class PistonMoveGuard {
    private static final ThreadLocal<Deque<Reservation>> ACTIVE_RESERVATIONS = new ThreadLocal<>();
    /**
     * 滑动路径的跨 tick 保留。键为弱引用，值只存坐标不存世界，因此世界卸载后条目会被回收，
     * 不会因为漏掉一次释放就永久钉住整个世界。
     */
    private static final Map<Level, Set<BlockPos>> SLIDING_RESERVATIONS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private PistonMoveGuard() {
    }

    /** 打开一个只在本次调用内有效的保留作用域，配合 {@link #reserve} 使用。 */
    public static Scope begin(Level level) {
        Reservation reservation = new Reservation(level);
        Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
        if (reservations == null) {
            reservations = new ArrayDeque<>();
            ACTIVE_RESERVATIONS.set(reservations);
        }
        reservations.push(reservation);
        return new Scope(reservation);
    }

    /**
     * 打开一个跨 tick 的滑动保留，立即覆盖给定位置。
     *
     * @param level     世界
     * @param toPush    将要被推动的方块位置
     * @param toDestroy 将要被破坏的方块位置
     */
    public static SlidingScope beginSliding(Level level, Collection<BlockPos> toPush, Collection<BlockPos> toDestroy) {
        SlidingScope scope = new SlidingScope(level);
        scope.replace(toPush, toDestroy);
        return scope;
    }

    /** 把位置写入当前打开的 {@link #begin(Level)} 作用域；没有打开作用域时不生效。 */
    public static void reserve(Level level, List<BlockPos> toPush, List<BlockPos> toDestroy) {
        Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
        if (reservations == null || reservations.isEmpty()) return;
        Reservation reservation = reservations.peek();
        if (reservation.level != level) return;
        toPush.forEach(pos -> reservation.positions.add(pos.immutable()));
        toDestroy.forEach(pos -> reservation.positions.add(pos.immutable()));
    }

    /** 是否被任何保留覆盖（活塞调用作用域或滑动跨 tick 保留）。 */
    public static boolean isReserved(Level level, BlockPos pos) {
        Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
        if (reservations != null) {
            for (Reservation reservation : reservations) {
                if (reservation.level == level && reservation.positions.contains(pos)) return true;
            }
        }
        return isSlidingReserved(level, pos);
    }

    /** 是否被滑动路径的跨 tick 保留覆盖。 */
    public static boolean isSlidingReserved(Level level, BlockPos pos) {
        Set<BlockPos> positions = SLIDING_RESERVATIONS.get(level);
        return positions != null && positions.contains(pos);
    }

    private static final class Reservation {
        private final Level level;
        private final Set<BlockPos> positions = new HashSet<>();

        private Reservation(Level level) {
            this.level = level;
        }
    }

    /** 只在一次调用内有效的保留作用域。 */
    public static final class Scope implements AutoCloseable {
        private final Reservation reservation;
        private boolean closed;

        private Scope(Reservation reservation) {
            this.reservation = reservation;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
            if (reservations == null) return;
            reservations.removeFirstOccurrence(reservation);
            if (reservations.isEmpty()) ACTIVE_RESERVATIONS.remove();
        }
    }

    /** 跨 tick 的滑动保留；滑动期间用载荷当前坐标刷新覆盖范围，结束时关闭。 */
    public static final class SlidingScope implements AutoCloseable {
        private final Level level;
        private final Set<BlockPos> owned = new HashSet<>();
        private boolean closed;

        private SlidingScope(Level level) {
            this.level = level;
        }

        /** 用新的覆盖范围替换旧范围；滑动过程中载荷位置随实体移动。 */
        public void replace(Collection<BlockPos> toPush, Collection<BlockPos> toDestroy) {
            if (closed) return;
            Set<BlockPos> positions = SLIDING_RESERVATIONS.computeIfAbsent(this.level, key -> new HashSet<>());
            positions.removeAll(this.owned);
            this.owned.clear();
            for (BlockPos pos : toPush) {
                BlockPos immutable = pos.immutable();
                this.owned.add(immutable);
                positions.add(immutable);
            }
            for (BlockPos pos : toDestroy) {
                BlockPos immutable = pos.immutable();
                this.owned.add(immutable);
                positions.add(immutable);
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            SLIDING_RESERVATIONS.computeIfPresent(this.level, (key, positions) -> {
                positions.removeAll(this.owned);
                return positions.isEmpty() ? null : positions;
            });
            this.owned.clear();
        }
    }
}
