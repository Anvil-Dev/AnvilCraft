package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.util.INonNullListInjector;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NonNullList.class)
public class NonNullListInjector implements INonNullListInjector {

    @Override
    @SuppressWarnings({"AddedMixinMembersNamePattern", "all"})
    public <E> NonNullList<E> copy() {
        NonNullList<E> ths = (NonNullList<E>) (Object) this;
        NonNullList<E> nonNullList = NonNullList.create();
        nonNullList.addAll(ths);
        return nonNullList;
    }
}
