package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 免疫指定类型伤害的护符效果
public record ImmuneTypedDamageAmuletEffect(List<TagPredicate<DamageType>> immune) implements IImmuneDamageAmuletEffect {
    @Override
    public boolean shouldImmune(Player player, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        for (TagPredicate<DamageType> immune : this.immune) {
            if (immune.matches(source.typeHolder())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_TYPED_DAMAGE.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneTypedDamageAmuletEffect> {
        public static final MapCodec<ImmuneTypedDamageAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TagPredicate.codec(Registries.DAMAGE_TYPE)
                .listOf()
                .optionalFieldOf("immune", List.of())
                .forGetter(ImmuneTypedDamageAmuletEffect::immune)
        ).apply(inst, ImmuneTypedDamageAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImmuneTypedDamageAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.tagPredicate(Registries.DAMAGE_TYPE).apply(ByteBufCodecs.list()),
            ImmuneTypedDamageAmuletEffect::immune,
            ImmuneTypedDamageAmuletEffect::new
        );

        @Override
        public MapCodec<ImmuneTypedDamageAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneTypedDamageAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
