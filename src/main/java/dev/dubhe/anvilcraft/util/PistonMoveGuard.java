package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class PistonMoveGuard {
    private static final ThreadLocal<@Nullable Deque<Reservation>> ACTIVE_RESERVATIONS = new ThreadLocal<>();
    /// 跨多个游戏刻存活的预留，按维度索引并带过期刻数；滑轨与推进活塞的移动会跨越 tick，
    /// 而方块吞噬器在之后的 tick 才行动，因此调用作用域的预留不足以覆盖。
    private static final Map<Level, List<TimedReservation>> TIMED_RESERVATIONS = new WeakHashMap<>();

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

    /// 登记一个跨越多个游戏刻仍然有效的预留，用于滑轨与推进活塞的移动。
    ///
    /// @param level     预留所属维度
    /// @param positions 计划移动或销毁的方块位置
    /// @param duration  预留有效的游戏刻数
    public static void reserveUntil(Level level, Iterable<BlockPos> positions, int duration) {
        List<TimedReservation> list = PistonMoveGuard.TIMED_RESERVATIONS.computeIfAbsent(level, ignored -> new ArrayList<>());
        long expiresAt = level.getGameTime() + Math.max(1, duration);
        TimedReservation reservation = new TimedReservation(expiresAt);
        for (BlockPos pos : positions) reservation.positions.add(pos.immutable());
        list.add(reservation);
    }

    /// 释放某个维度中所有尚未过期的时域预留，供滑轨停止时调用。
    public static void releaseTimed(Level level) {
        PistonMoveGuard.TIMED_RESERVATIONS.remove(level);
    }

    private static boolean isTimedReserved(Level level, BlockPos pos) {
        List<TimedReservation> list = PistonMoveGuard.TIMED_RESERVATIONS.get(level);
        if (list == null || list.isEmpty()) return false;
        long now = level.getGameTime();
        list.removeIf(reservation -> reservation.expiresAt <= now);
        for (TimedReservation reservation : list) {
            if (reservation.positions.contains(pos)) return true;
        }
        return false;
    }

    private static final class TimedReservation {
        private final long expiresAt;
        private final Set<BlockPos> positions = new HashSet<>();

        private TimedReservation(long expiresAt) {
            this.expiresAt = expiresAt;
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
