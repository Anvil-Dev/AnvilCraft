package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(WorldBorder.class)
public interface WorldBorderAccessor {
    @Invoker("getListeners")
    List<BorderChangeListener> invokeGetListeners();
}
