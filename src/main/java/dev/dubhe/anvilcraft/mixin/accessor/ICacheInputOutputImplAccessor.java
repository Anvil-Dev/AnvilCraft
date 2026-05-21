package dev.dubhe.anvilcraft.mixin.accessor;

import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInputOutputImpl;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.InputOutputOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Deque;
import java.util.Set;

@Mixin(ICacheInputOutputImpl.class)
public interface ICacheInputOutputImplAccessor {
    @Accessor
    Set<ICacheElement> getElements();

    @Accessor
    Deque<InputOutputOperation> getShrinkSimulateStack();
}
