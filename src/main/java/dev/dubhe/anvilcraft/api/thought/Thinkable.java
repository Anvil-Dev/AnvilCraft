package dev.dubhe.anvilcraft.api.thought;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface Thinkable {
    @OnlyIn(Dist.CLIENT)
    default void onThought() {
    }
}
