package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 免疫指定实体造成的伤害的护符效果
public record ImmuneMurdererDamageAmuletEffect(
    List<TagPredicate<EntityType<?>>> source,
    List<TagPredicate<EntityType<?>>> direct
) implements IImmuneDamageAmuletEffect {
    @Override
    public boolean shouldImmune(Player player, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        Entity entity = source.getEntity();
        if (entity != null) {
            boolean passed = this.source.isEmpty();
            for (TagPredicate<EntityType<?>> immune : this.source) {
                if (immune.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()))) {
                    passed = true;
                }
            }
            if (!passed) {
                return false;
            }
        }
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            boolean passed = this.direct.isEmpty();
            for (TagPredicate<EntityType<?>> immune : this.direct) {
                if (immune.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(direct.getType()))) {
                    passed = true;
                }
            }
            return passed;
        }
        return true;
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IMMUNE_MURDERER_DAMAGE.get();
    }

    public static class Type implements IAmuletEffect.Type<ImmuneMurdererDamageAmuletEffect> {
        public static final MapCodec<ImmuneMurdererDamageAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TagPredicate.codec(Registries.ENTITY_TYPE)
                .listOf()
                .optionalFieldOf("source", List.of())
                .forGetter(ImmuneMurdererDamageAmuletEffect::source),
            TagPredicate.codec(Registries.ENTITY_TYPE)
                .listOf()
                .optionalFieldOf("direct", List.of())
                .forGetter(ImmuneMurdererDamageAmuletEffect::direct)
        ).apply(inst, ImmuneMurdererDamageAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImmuneMurdererDamageAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.tagPredicate(Registries.ENTITY_TYPE).apply(ByteBufCodecs.list()),
            ImmuneMurdererDamageAmuletEffect::source,
            StreamCodecUtil.tagPredicate(Registries.ENTITY_TYPE).apply(ByteBufCodecs.list()),
            ImmuneMurdererDamageAmuletEffect::direct,
            ImmuneMurdererDamageAmuletEffect::new
        );

        @Override
        public MapCodec<ImmuneMurdererDamageAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneMurdererDamageAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
