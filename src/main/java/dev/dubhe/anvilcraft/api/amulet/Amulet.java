package dev.dubhe.anvilcraft.api.amulet;

import com.google.common.collect.ImmutableSet;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.effect.IAmuletEffect;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public final class Amulet {
    private final @Unmodifiable Set<IAmuletEffect> effects;
    @Unmodifiable
    private @Nullable Set<IAmuletEffect> flatten;

    private Amulet(@Unmodifiable Set<IAmuletEffect> effects) {
        this.effects = effects;
    }

    public static Amulet of(IAmuletEffect effect, IAmuletEffect... effects) {
        return new Amulet(ImmutableSet.<IAmuletEffect>builder().add(effect).add(effects).build());
    }

    /// 获取该护符自身的效果，不包含其包覆的其它护符的效果
    ///
    /// @return 该护符自身的效果
    public @Unmodifiable Set<IAmuletEffect> getEffects() {
        return this.effects;
    }

    /// 获取该护符展开后的效果，包含其包覆的其它护符的效果
    ///
    /// @return 该护符展开后的效果
    public Set<IAmuletEffect> getFlattenEffects() {
        if (this.flatten == null) {
            this.flatten = this.computeFlattenEffects();
        }
        return this.flatten;
    }

    private Set<IAmuletEffect> computeFlattenEffects() {
        ImmutableSet.Builder<IAmuletEffect> effects = ImmutableSet.builder();
        for (IAmuletEffect effect : this.effects) {
            effects.addAll(effect.flatten());
        }
        return effects.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Amulet that = Util.cast(obj);
        return Objects.equals(this.effects, that.effects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.effects);
    }

    @Override
    public String toString() {
        return "Amulet[effects=" + this.effects + ']';
    }
}
