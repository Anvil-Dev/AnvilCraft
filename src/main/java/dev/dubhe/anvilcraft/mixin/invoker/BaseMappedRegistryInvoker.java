package dev.dubhe.anvilcraft.mixin.invoker;

import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnstableApiUsage")
@Mixin(BaseMappedRegistry.class)
public interface BaseMappedRegistryInvoker {
    @Invoker
    void invokeSetSync(boolean sync);
}
