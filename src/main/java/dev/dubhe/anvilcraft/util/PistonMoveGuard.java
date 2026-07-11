package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PistonMoveGuard {
    private static final ThreadLocal<Deque<Reservation>> ACTIVE_RESERVATIONS = new ThreadLocal<>();

    private PistonMoveGuard() {
    }

    public static Scope begin(Level level) {
        Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
        if (reservations == null) {
            reservations = new ArrayDeque<>();
            ACTIVE_RESERVATIONS.set(reservations);
        }
        Reservation reservation = new Reservation(level);
        reservations.push(reservation);
        return new Scope(reservation);
    }

    public static void reserve(Level level, List<BlockPos> toPush, List<BlockPos> toDestroy) {
        Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
        if (reservations == null || reservations.isEmpty()) return;
        Reservation reservation = reservations.peek();
        if (reservation.level != level) return;
        toPush.forEach(pos -> reservation.positions.add(pos.immutable()));
        toDestroy.forEach(pos -> reservation.positions.add(pos.immutable()));
    }

    public static boolean isReserved(Level level, BlockPos pos) {
        Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
        if (reservations == null) return false;
        for (Reservation reservation : reservations) {
            if (reservation.level == level && reservation.positions.contains(pos)) return true;
        }
        return false;
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
            if (closed) return;
            closed = true;
            Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
            if (reservations == null) return;
            reservations.removeFirstOccurrence(reservation);
            if (reservations.isEmpty()) ACTIVE_RESERVATIONS.remove();
        }
    }
}
