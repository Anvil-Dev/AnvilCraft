package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.client.renderer.Line;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class RenderThunderSupport {
    public static Set<Line> getThunder(Vec3 start, Vec3 end) {
        Vec3 vector = end.subtract(start);
        Vec3 direction = vector.normalize();
        double length = vector.length();
        int paragraphs = getParagraphs(length);
        double average = length / paragraphs;
        Set<Line> lines = new HashSet<>();
        Vec3 current = start;
        for (int i = 0; i < paragraphs; i++) {
            int level = paragraphs - i;
            Vec3 next = current.add(direction.scale(average));
            double offset = level == 1 ? 0 : Math.random() / level;
            next = next.add(direction.yRot((float) (Math.random() * 2 * Math.PI)).scale(offset));
            if (Math.random() < 0.2) {
                lines.addAll(RenderThunderSupport.getThunder(
                    next,
                    next.add(direction.yRot((float) (Math.random() * 2 * Math.PI)).scale(offset * 2))
                ));
            }
            lines.add(new Line(current, next, level));
            current = next;
        }
        return lines;
    }

    private static int getParagraphs(double length) {
        return (int) Math.max(2.0, Math.floor(length));
    }
}
