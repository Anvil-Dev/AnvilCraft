package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;

public class DummyHolder<T> extends Holder.Reference<T> {
    public DummyHolder(ResourceKey<T> key) {
        super(Type.STAND_ALONE, null, key, null);
    }

    @Override
    public boolean canSerializeIn(HolderOwner<T> context) {
        return true;
    }
}
