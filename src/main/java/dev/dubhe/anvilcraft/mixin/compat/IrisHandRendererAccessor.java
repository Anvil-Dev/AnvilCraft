package dev.dubhe.anvilcraft.mixin.compat;

import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = HandRenderer.class, remap = false)
public interface IrisHandRendererAccessor {
    @Accessor("submitNodeCollector")
    SubmitNodeStorage anvilcraft$getSubmitNodeCollector();
}
