package dev.dubhe.anvilcraft.api.heat;

import com.google.common.collect.Comparators;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class HeatTierLine {
    private final NavigableMap<Integer, Point> points;

    public HeatTierLine(NavigableMap<Integer, Point> points) {
        this.points = points;
    }

    public static HeatTierLine always(HeatTier tier, int duration) {
        NavigableMap<Integer, Point> map = new TreeMap<>();
        map.put(Integer.MAX_VALUE, new Point(tier, duration));
        return new HeatTierLine(map);
    }

    public static LineBuilder builder() {
        return new LineBuilder();
    }

    public static class LineBuilder {
        private final NavigableMap<Integer, Point> points = new TreeMap<>();

        public LineBuilder() {
        }

        public LineBuilder addPoint(int toNext, HeatTier tier, int duration) {
            return this.addPointInTick(toNext, tier, duration * 20);
        }

        public LineBuilder addPoint(int toNext, HeatTier tier) {
            this.points.put(toNext, new Point(tier, 0));
            return this;
        }

        public LineBuilder addPoint(HeatTier tier, int duration) {
            return this.addPointInTick(tier, duration * 20);
        }

        public LineBuilder addPoint(HeatTier tier) {
            this.points.put(Integer.MAX_VALUE, new Point(tier, 0));
            return this;
        }

        public LineBuilder addPointInTick(int toNext, HeatTier tier, int duration) {
            this.points.put(toNext, new Point(tier, duration));
            return this;
        }

        public LineBuilder addPointInTick(HeatTier tier, int duration) {
            this.points.put(Integer.MAX_VALUE, new Point(tier, duration));
            return this;
        }

        public HeatTierLine build() {
            return new HeatTierLine(this.points);
        }
    }

    public Optional<Point> getPoint(int count) {
        for (Integer toNext : this.points.keySet()) {
            if (count < toNext) return Optional.ofNullable(this.points.get(toNext));
        }
        return Optional.empty();
    }

    public record Point(HeatTier tier, int duration) {
        public Point merge(Point other) {
            return new Point(Comparators.max(this.tier, other.tier), this.duration + other.duration);
        }

        public Point merge(Point... others) {
            HeatTier tier = this.tier;
            int duration = this.duration;
            for (Point other : others) {
                tier = Comparators.max(tier, other.tier);
                duration += other.duration;
            }
            return new Point(tier, duration);
        }
    }
}
