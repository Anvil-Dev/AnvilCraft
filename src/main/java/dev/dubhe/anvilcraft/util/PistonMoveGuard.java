package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
    /// 滑动载荷跨 tick 移动，保留按刻数存活；载荷每刻续期，实体被卸载时靠该期限兜底回收。
    private static final int SLIDING_RESERVATION_TICKS = 40;

    private static final ThreadLocal<Deque<Reservation>> ACTIVE_RESERVATIONS = new ThreadLocal<>();
    /// 滑动路径的跨刻保留，每条各持自己的坐标，互不覆盖；键为弱引用，维度卸载后条目会被回收。
    private static final Map<Level, List<SlidingScope>> SLIDING_RESERVATIONS =
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
        SlidingScope scope = new SlidingScope(PistonMoveGuard.SLIDING_RESERVATION_TICKS);
        scope.replace(level.getGameTime(), toPush, toDestroy);
        PistonMoveGuard.SLIDING_RESERVATIONS
            .computeIfAbsent(level, ignored -> new ArrayList<>())
            .add(scope);
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
        List<SlidingScope> scopes = PistonMoveGuard.SLIDING_RESERVATIONS.get(level);
        if (scopes == null) return false;
        long now = level.getGameTime();
        scopes.removeIf(scope -> scope.isExpired(now));
        if (scopes.isEmpty()) {
            // 过期项清空后一并移除条目，否则载荷被卸载的维度会留下空列表。
            PistonMoveGuard.SLIDING_RESERVATIONS.remove(level);
            return false;
        }
        for (SlidingScope scope : scopes) {
            if (scope.contains(pos)) return true;
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

    /** 只在一次调用内有效的保留作用域。 */
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
            Deque<Reservation> reservations = ACTIVE_RESERVATIONS.get();
            if (reservations == null) return;
            reservations.removeFirstOccurrence(this.reservation);
            if (reservations.isEmpty()) ACTIVE_RESERVATIONS.remove();
        }
    }

    /// 跨 tick 的滑动保留，滑动期间随载荷坐标刷新并续期。
    /// 不持有 {@link Level}：作为 {@link #SLIDING_RESERVATIONS} 的值，持键会让弱引用永不失效。
    public static final class SlidingScope {
        private final int duration;
        private final Set<BlockPos> owned = new HashSet<>();
        private long expiresAt;
        private boolean closed;

        private SlidingScope(int duration) {
            this.duration = duration;
        }

        /// 用新范围替换旧范围并按当前时刻续期；载荷位置随实体移动。
        public void replace(long now, Collection<BlockPos> toPush, Collection<BlockPos> toDestroy) {
            if (this.closed) return;
            this.owned.clear();
            for (BlockPos pos : toPush) this.owned.add(pos.immutable());
            for (BlockPos pos : toDestroy) this.owned.add(pos.immutable());
            this.expiresAt = now + this.duration;
        }

        private boolean isExpired(long now) {
            return this.expiresAt <= now;
        }

        private boolean contains(BlockPos pos) {
            return this.owned.contains(pos);
        }

        /// 释放该条保留；维度用于定位其所属列表。
        public void close(Level level) {
            if (this.closed) return;
            this.closed = true;
            PistonMoveGuard.SLIDING_RESERVATIONS.computeIfPresent(level, (ignored, scopes) -> {
                scopes.remove(this);
                return scopes.isEmpty() ? null : scopes;
            });
            this.owned.clear();
        }
    }
}
