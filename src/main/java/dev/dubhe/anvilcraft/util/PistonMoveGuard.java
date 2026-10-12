package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class PistonMoveGuard {
    /// 推进活塞走原版 PistonMovingBlockEntity，TICKS_TO_EXTEND 为 2；留一点余量覆盖同刻的落位自通知。
    public static final int PISTON_MOVE_RESERVATION_TICKS = 5;

    private static final ThreadLocal<@Nullable Deque<Reservation>> ACTIVE_RESERVATIONS = new ThreadLocal<>();
    /// 跨刻预留，按维度弱引用索引。句柄不持有 {@link Level}，弱键才真正可回收，
    /// 否则维度卸载后会被该维度常驻。客户端与主世界是不同的 Level 键，同一维度的列表
    /// 只由该维度的线程访问，因此这里只需在 map 层加同步。
    private static final Map<Level, List<TimedReservation>> TIMED_RESERVATIONS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private PistonMoveGuard() {
    }

    public static Scope begin(Level level) {
        Deque<Reservation> reservations = PistonMoveGuard.ACTIVE_RESERVATIONS.get();
        if (reservations == null) {
            reservations = new ArrayDeque<>();
            PistonMoveGuard.ACTIVE_RESERVATIONS.set(reservations);
        }
        Reservation reservation = new Reservation(level);
        reservations.push(reservation);
        return new Scope(reservation);
    }

    public static void reserve(Level level, List<BlockPos> toPush, List<BlockPos> toDestroy) {
        Deque<Reservation> reservations = PistonMoveGuard.ACTIVE_RESERVATIONS.get();
        if (reservations == null || reservations.isEmpty()) return;
        Reservation reservation = reservations.peek();
        if (reservation.level != level) return;
        toPush.forEach(pos -> reservation.positions.add(pos.immutable()));
        toDestroy.forEach(pos -> reservation.positions.add(pos.immutable()));
    }

    public static boolean isReserved(Level level, BlockPos pos) {
        Deque<Reservation> reservations = PistonMoveGuard.ACTIVE_RESERVATIONS.get();
        if (reservations != null) {
            for (Reservation reservation : reservations) {
                if (reservation.level == level && reservation.positions.contains(pos)) return true;
            }
        }
        return PistonMoveGuard.isTimedReserved(level, pos);
    }

    /// 登记一个跨刻有效的预留，用于滑轨与推进活塞的移动，返回可刷新/释放的句柄。
    public static TimedReservation reserveUntil(Level level, Iterable<BlockPos> positions, int duration) {
        TimedReservation reservation = new TimedReservation(level.getGameTime(), Math.max(1, duration));
        reservation.replace(level.getGameTime(), positions);
        PistonMoveGuard.TIMED_RESERVATIONS
            .computeIfAbsent(level, ignored -> new ArrayList<>())
            .add(reservation);
        return reservation;
    }

    /// 释放单个预留；同维度其他预留不受影响。
    public static void releaseTimed(Level level, TimedReservation reservation) {
        List<TimedReservation> list = PistonMoveGuard.TIMED_RESERVATIONS.get(level);
        if (list == null) return;
        list.remove(reservation);
        if (list.isEmpty()) PistonMoveGuard.TIMED_RESERVATIONS.remove(level);
    }

    private static boolean isTimedReserved(Level level, BlockPos pos) {
        List<TimedReservation> list = PistonMoveGuard.TIMED_RESERVATIONS.get(level);
        if (list == null || list.isEmpty()) return false;
        long now = level.getGameTime();
        list.removeIf(reservation -> reservation.isExpired(now));
        for (TimedReservation reservation : list) {
            if (reservation.contains(pos)) return true;
        }
        return false;
    }

    /// 跨刻预留的句柄。载荷移动时刷新坐标并续期，停止时释放。
    ///
    /// 不持有 {@link Level}：作为 {@link #TIMED_RESERVATIONS} 的值，持键会让弱引用永不失效。
    public static final class TimedReservation {
        private final int duration;
        private final Set<BlockPos> positions = new HashSet<>();
        private long expiresAt;

        private TimedReservation(long now, int duration) {
            this.duration = duration;
            this.expiresAt = now + duration;
        }

        private boolean isExpired(long now) {
            return this.expiresAt <= now;
        }

        private boolean contains(BlockPos pos) {
            return this.positions.contains(pos);
        }

        /// 替换预留范围并按当前时刻续期；载荷移动时每刻调用，故有效期覆盖整个搬运过程。
        public void replace(long now, Iterable<BlockPos> newPositions) {
            this.positions.clear();
            for (BlockPos pos : newPositions) this.positions.add(pos.immutable());
            this.expiresAt = now + this.duration;
        }
    }

    private static final class Reservation {
        private final Level level;
        private final Set<BlockPos> positions = new HashSet<>();

        private Reservation(Level level) {
            this.level = level;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Reservation reservation;
        private boolean closed;

        private Scope(Reservation reservation) {
            this.reservation = reservation;
        }

        @Override
        public void close() {
            if (this.closed) return;
            this.closed = true;
            Deque<Reservation> reservations = PistonMoveGuard.ACTIVE_RESERVATIONS.get();
            if (reservations == null) return;
            reservations.removeFirstOccurrence(this.reservation);
            if (reservations.isEmpty()) PistonMoveGuard.ACTIVE_RESERVATIONS.remove();
        }
    }
}
