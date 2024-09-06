package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@link Minecraft}访问器
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("pausePartialTick")
    float getPausePartialTick();
}
