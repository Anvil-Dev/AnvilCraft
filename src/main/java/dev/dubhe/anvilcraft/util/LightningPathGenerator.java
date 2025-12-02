package dev.dubhe.anvilcraft.util;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BiConsumer;

public class LightningPathGenerator {
    public static void generatePath(Vec3 start, Vec3 end, int branches, double deviationPercentage, @Nullable PathConsumer consumer) {
        if (consumer == null) {
            consumer = (s, e) -> {
            };
        }
        Vec3 current = start;
        Vec3 vector = end.subtract(start);
        double length = vector.length();
        double deviation = length / branches * deviationPercentage;
        Random random = new Random();
        for (int i = 0; i < branches; i++) {
            Vec3 next = current.add(vector.scale(1.0 / branches))
                .add(
                    random.nextDouble(-deviation, deviation),
                    random.nextDouble(-deviation, deviation),
                    random.nextDouble(-deviation, deviation)
                );
            consumer.accept(current, next);
            current = next;
        }
        consumer.accept(current, end);
    }

    @FunctionalInterface
    public interface PathConsumer extends BiConsumer<Vec3, Vec3> {
        @Override
        void accept(Vec3 start, Vec3 end);
    }
}
