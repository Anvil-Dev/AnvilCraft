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

/// 记录"即将被移动"的方块位置，供吞噬器与 TNT 判定跳过。
/// 活塞在单次 moveBlocks 内完成，作用域足够；滑动方块跨 tick 才放回，另用跨 tick 保留。
public final class PistonMoveGuard {
    private static final ThreadLocal<Deque<Reservation>> ACTIVE_RESERVATIONS = new ThreadLocal<>();
    /// 滑动路径的跨 tick 保留；键为弱引用，世界卸载后条目会被回收。
    private static final Map<Level, Set<BlockPos>> SLIDING_RESERVATIONS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private PistonMoveGuard() {
    }

    /// 打开只在本次调用内有效的保留作用域。
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

    /// 打开跨 tick 的滑动保留，并立即覆盖给定位置。
    public static SlidingScope beginSliding(Level level, Collection<BlockPos> toPush, Collection<BlockPos> toDestroy) {
        SlidingScope scope = new SlidingScope(level);
        scope.replace(toPush, toDestroy);
        return scope;
    }

    /// 把位置写入当前打开的作用域；没有打开作用域时不生效。
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

    /// 跨 tick 的滑动保留，滑动期间随载荷坐标刷新。
    public static final class SlidingScope implements AutoCloseable {
        private final Level level;
        private final Set<BlockPos> owned = new HashSet<>();
        private boolean closed;

        private SlidingScope(Level level) {
            this.level = level;
        }

        /// 用新范围替换旧范围；载荷位置随实体移动。
        public void replace(Collection<BlockPos> toPush, Collection<BlockPos> toDestroy) {
            if (this.closed) return;
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
            if (this.closed) return;
            this.closed = true;
            SLIDING_RESERVATIONS.computeIfPresent(this.level, (key, positions) -> {
                positions.removeAll(this.owned);
                return positions.isEmpty() ? null : positions;
            });
            this.owned.clear();
        }
    }
}
