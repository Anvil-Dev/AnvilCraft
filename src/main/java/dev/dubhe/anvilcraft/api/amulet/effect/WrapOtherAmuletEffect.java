package dev.dubhe.anvilcraft.api.amulet.effect;

import com.google.common.collect.ImmutableSet;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.Amulet;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/// 包覆其它护符的护符效果。<br>
/// 该效果自身不产生任何作用，仅将其包覆的护符的效果展开后一并生效。
public final class WrapOtherAmuletEffect implements IAmuletEffect {
    private final List<ResourceKey<Amulet>> others;
    @Unmodifiable
    private @Nullable Set<IAmuletEffect> flatten;

    public WrapOtherAmuletEffect(List<ResourceKey<Amulet>> others) {
        this.others = others;
    }

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        // 该效果不产生任何作用，由护符管理器触发展开后的所有效果
    }

    @Override
    public Set<IAmuletEffect> flatten() {
        return this.getFlattenEffects();
    }

    private Set<IAmuletEffect> getFlattenEffects() {
        if (this.flatten == null) {
            this.flatten = this.computeFlattenEffects();
        }
        return this.flatten;
    }

    private Set<IAmuletEffect> computeFlattenEffects() {
        ImmutableSet.Builder<IAmuletEffect> effects = ImmutableSet.builder();
        effects.add(this);
        for (ResourceKey<Amulet> key : this.others) {
            Amulet amulet = ModRegistries.AMULET.get(key);
            if (amulet == null) {
                continue;
            }
            effects.addAll(amulet.getFlattenEffects());
        }
        return effects.build();
    }

    public List<ResourceKey<Amulet>> others() {
        return this.others;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        WrapOtherAmuletEffect that = Util.cast(obj);
        return Objects.equals(this.others, that.others);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.others);
    }

    @Override
    public String toString() {
        return "WrapOtherAmuletEffect[others=" + this.others + ']';
    }
}
