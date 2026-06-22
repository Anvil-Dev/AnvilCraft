package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

/**
 * Celestial body rendering utilities for CFA.
 * Phase 9: vertex-based rendering methods retained for compatibility;
 * full body rendering uses 26.1 model system via standalone models.
 */
@SuppressWarnings("checkstyle:MultipleVariableDeclarations")
public class CelestialBodyRenderer {

    private static final Vector3f LIGHT_DIR = new Vector3f(0.7f, 0.5f, 0.5f).normalize();

    /** Compute lambertian lighting color from a normal and light direction. */
    public static int computeLambertColor(PoseStack.Pose pose, float nx, float ny, float nz, Vector3f lightDir) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float dot = normal.dot(lightDir);
        float brightness = 0.3f + 0.7f / (1.0f + (float) Math.exp(-20.0 * (dot + 0.08)));
        int c = (int) (brightness * 255);
        return (255 << 24) | (c << 16) | (c << 8) | c;
    }

    /** Compute atmosphere transparency from view angle. */
    public static float computeAtmosphereAlpha(PoseStack.Pose pose, float nx, float ny, float nz,
                                                float baseAlpha, float viewX, float viewY, float viewZ) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float viewDot = Math.abs(normal.x * viewX + normal.y * viewY + normal.z * viewZ);
        float rim = 1.0f - viewDot;
        return baseAlpha * (1.0f + 3.0f * rim);
    }

    /** Get atmosphere color for a given temperature. */
    public static float[] getAtmosphereColor(Temperature temperature) {
        return switch (temperature) {
            case FREEZING -> new float[]{0.4f, 0.6f, 0.9f};
            case COLD -> new float[]{0.5f, 0.7f, 0.9f};
            case MILD -> new float[]{0.6f, 0.8f, 1.0f};
            case HOT -> new float[]{0.9f, 0.5f, 0.3f};
            case SCORCHED -> new float[]{1.0f, 0.3f, 0.1f};
        };
    }

    /** Get RGB color for a star body. */
    public static float[] getStarColor(StarData star) {
        return new float[]{star.colorR() / 255f, star.colorG() / 255f, star.colorB() / 255f};
    }
}
