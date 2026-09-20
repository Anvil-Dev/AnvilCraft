package dev.dubhe.anvilcraft.api.amulet.effect;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.Amulet;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
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
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
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
    public Type getType() {
        return ModAmuletEffectTypes.WRAP_OTHER.get();
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

    public static class Type implements IAmuletEffect.Type<WrapOtherAmuletEffect> {
        public static final Codec<List<ResourceKey<Amulet>>> OTHERS_CODEC = ResourceKey.codec(ModRegistryKeys.AMULET).listOf();
        public static final StreamCodec<ByteBuf, ResourceKey<Amulet>> KEY_STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(
            location -> ResourceKey.create(ModRegistryKeys.AMULET, location),
            ResourceKey::location
        );
        public static final MapCodec<WrapOtherAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            WrapOtherAmuletEffect.Type.OTHERS_CODEC
                .fieldOf("others")
                .forGetter(WrapOtherAmuletEffect::others)
        ).apply(inst, WrapOtherAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, WrapOtherAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            WrapOtherAmuletEffect.Type.KEY_STREAM_CODEC.apply(ByteBufCodecs.list()),
            WrapOtherAmuletEffect::others,
            WrapOtherAmuletEffect::new
        );

        @Override
        public MapCodec<WrapOtherAmuletEffect> codec() {
            return WrapOtherAmuletEffect.Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WrapOtherAmuletEffect> streamCodec() {
            return WrapOtherAmuletEffect.Type.STREAM_CODEC;
        }
    }
}
