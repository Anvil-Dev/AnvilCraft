package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.joml.Matrix4f;

public final class EnchantedGoldReferenceClock {
    private static long time;

    public static void install(long millis) {
        time = millis;
        try {
            var state = ModRenderTypes.ENCHANTED_GOLD_GLINT.state;
            var field = state.getClass().getDeclaredField("textureTransform");
            field.setAccessible(true);
            field.set(state, new TextureTransform("enchanted_gold_reference", EnchantedGoldReferenceClock::matrix));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot freeze the reference glint phase", error);
        }
    }

    public static Matrix4f matrix() {
        return new Matrix4f().translation(-(time % 110000) / 110000F, (time % 30000) / 30000F, 0)
            .rotateZ((float) (Math.PI / 18)).scale(8);
    }
}
