package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

public record ImmuneDamageAmulet(List<TagPredicate<DamageType>> immune) implements IAmulet {
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        for (TagPredicate<DamageType> immune : this.immune) {
            if (immune.matches(source.typeHolder())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.IMMUNE_DAMAGE.get();
    }

    public static class Type implements IAmulet.Type<ImmuneDamageAmulet> {
        public static final MapCodec<ImmuneDamageAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TagPredicate.codec(Registries.DAMAGE_TYPE)
                .listOf()
                .optionalFieldOf("immune", List.of())
                .forGetter(ImmuneDamageAmulet::immune)
        ).apply(inst, ImmuneDamageAmulet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImmuneDamageAmulet> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.tagPredicate(Registries.DAMAGE_TYPE).apply(ByteBufCodecs.list()),
            ImmuneDamageAmulet::immune,
            ImmuneDamageAmulet::new
        );

        @Override
        public MapCodec<ImmuneDamageAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneDamageAmulet> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
    
    public static class Builder {
        private final ImmutableList.Builder<TagPredicate<DamageType>> immune = ImmutableList.builder();

        public Builder immune(TagPredicate<DamageType> types) {
            this.immune.add(types);
            return this;
        }

        public Builder immune(TagKey<DamageType> types) {
            return this.immune(TagPredicate.is(types));
        }

        public Builder immuneNot(TagKey<DamageType> types) {
            return this.immune(TagPredicate.isNot(types));
        }

        // CHECKSTYLE.SUPPRESS: OverloadMethodsDeclarationOrder
        public Builder immune(TagKey<DamageType> types, boolean expected) {
            return this.immune(new TagPredicate<>(types, expected));
        }

        public ImmuneDamageAmulet build() {
            return new ImmuneDamageAmulet(this.immune.build());
        }
    }
}
