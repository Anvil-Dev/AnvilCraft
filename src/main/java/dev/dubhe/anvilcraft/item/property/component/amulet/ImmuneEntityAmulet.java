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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public record ImmuneEntityAmulet(List<TagPredicate<EntityType<?>>> source, List<TagPredicate<EntityType<?>>> direct) implements IAmulet {
    @Override
    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        Entity entity = source.getEntity();
        if (entity != null) {
            boolean passed = this.source.isEmpty();
            for (TagPredicate<EntityType<?>> immune : this.source) {
                if (immune.matches(entity.typeHolder())) {
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
                if (immune.matches(direct.typeHolder())) {
                    passed = true;
                }
            }
            if (!passed) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.IMMUNE_ENTITY.get();
    }

    public static class Type implements IAmulet.Type<ImmuneEntityAmulet> {
        public static final MapCodec<ImmuneEntityAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TagPredicate.codec(Registries.ENTITY_TYPE)
                .listOf()
                .optionalFieldOf("source", List.of())
                .forGetter(ImmuneEntityAmulet::source),
            TagPredicate.codec(Registries.ENTITY_TYPE)
                .listOf()
                .optionalFieldOf("direct", List.of())
                .forGetter(ImmuneEntityAmulet::direct)
        ).apply(inst, ImmuneEntityAmulet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImmuneEntityAmulet> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.tagPredicate(Registries.ENTITY_TYPE).apply(ByteBufCodecs.list()),
            ImmuneEntityAmulet::source,
            StreamCodecUtil.tagPredicate(Registries.ENTITY_TYPE).apply(ByteBufCodecs.list()),
            ImmuneEntityAmulet::direct,
            ImmuneEntityAmulet::new
        );

        @Override
        public MapCodec<ImmuneEntityAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneEntityAmulet> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<TagPredicate<EntityType<?>>> source = ImmutableList.builder();
        private final ImmutableList.Builder<TagPredicate<EntityType<?>>> direct = ImmutableList.builder();

        public Builder immune(TagPredicate<EntityType<?>> types) {
            this.source.add(types);
            return this;
        }

        public Builder immune(TagKey<EntityType<?>> types, boolean expected) {
            return this.immune(new TagPredicate<>(types, expected));
        }

        public Builder immune(TagKey<EntityType<?>> types) {
            return this.immune(TagPredicate.is(types));
        }

        public Builder immuneNot(TagKey<EntityType<?>> types) {
            return this.immune(TagPredicate.isNot(types));
        }

        public Builder immuneDirect(TagPredicate<EntityType<?>> types) {
            this.direct.add(types);
            return this;
        }

        public Builder immuneDirect(TagKey<EntityType<?>> types, boolean expected) {
            return this.immuneDirect(new TagPredicate<>(types, expected));
        }

        public Builder immuneDirect(TagKey<EntityType<?>> types) {
            return this.immuneDirect(TagPredicate.is(types));
        }

        public Builder immuneDirectNot(TagKey<EntityType<?>> types) {
            return this.immuneDirect(TagPredicate.isNot(types));
        }

        public ImmuneEntityAmulet build() {
            return new ImmuneEntityAmulet(this.source.build(), this.direct.build());
        }
    }
}
