package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.block.Portal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PortalProcessor.class)
public interface PortalProcessorAccessor {
    @Accessor
    Portal getPortal();
}
