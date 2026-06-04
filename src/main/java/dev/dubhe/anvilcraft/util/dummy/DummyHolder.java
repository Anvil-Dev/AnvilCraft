package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class DummyHolder<T> extends Holder.Reference<T> {
    private final HolderLookup.@Nullable RegistryLookup<T> owner;

    public DummyHolder(HolderLookup.@Nullable RegistryLookup<T> owner, ResourceKey<T> key) {
        super(Type.STAND_ALONE, owner, key, null);
        this.owner = owner;
    }

    public DummyHolder(ResourceKey<T> key) {
        this(null, key);
    }

    @Override
    public boolean canSerializeIn(HolderOwner<T> context) {
        return true;
    }

    @Override
    public boolean is(TagKey<T> tag) {
        return this.boundTags().contains(tag);
    }

    protected Set<TagKey<T>> boundTags() {
        if (this.tags == null) {
            this.bound();
            if (this.tags == null) {
                throw new IllegalStateException("Tags not bound");
            }
        }
        return this.tags;
    }

    protected void bound() {
        if (this.owner == null) return;
        Reference<T> exist = this.owner.get(this.getKey()).orElseThrow();
        this.tags = exist.tags;
        this.bindComponents(exist.components());
        this.bindValue(exist.value());
    }
}
