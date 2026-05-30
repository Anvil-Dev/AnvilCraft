package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.SlotsPredicate;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.SlotRange;

import java.util.Map;
import java.util.function.Consumer;

public record ImmuneAmulet(DamageSourcePredicate immune) implements IAmulet {
    public ImmuneAmulet(DamageSourcePredicate.Builder immune) {
        this(immune.build());
    }

    public static ImmuneAmulet of(Consumer<DamageSourcePredicate.Builder> immune) {
        DamageSourcePredicate.Builder builder = DamageSourcePredicate.Builder.damageType();
        immune.accept(builder);
        return new ImmuneAmulet(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        return this.immune.matches(player, source);
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.IMMUNE.get();
    }

    public static class Type implements IAmulet.Type<ImmuneAmulet> {
        public static final MapCodec<ImmuneAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            DamageSourcePredicate.CODEC
                .fieldOf("immune")
                .forGetter(ImmuneAmulet::immune)
        ).apply(inst, ImmuneAmulet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImmuneAmulet> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.DAMAGE_SOURCE_PREDICATE,
            ImmuneAmulet::immune,
            ImmuneAmulet::new
        );

        @Override
        public MapCodec<ImmuneAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ImmuneAmulet> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
    
    public static class Builder {
        private final DamageSourcePredicate.Builder immune = DamageSourcePredicate.Builder.damageType();

        public Builder immune(TagPredicate<DamageType> types) {
            this.immune.tag(types);
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

        public Builder immune(Consumer<EntityPredicate.Builder> source) {
            EntityPredicate.Builder builder = EntityPredicate.Builder.entity();
            source.accept(builder);
            this.immune.source(builder);
            return this;
        }

        public Builder immune(HolderGetter<EntityType<?>> lookup, EntityType<?> type) {
            return this.immune(builder -> builder.entityType(EntityTypePredicate.of(lookup, type)));
        }

        public Builder immune(HolderGetter<EntityType<?>> lookup, TagKey<EntityType<?>> types) {
            return this.immune(builder -> builder.entityType(EntityTypePredicate.of(lookup, types)));
        }

        public Builder immune(EntitySubPredicate sub) {
            return this.immune(builder -> builder.subPredicate(sub));
        }

        public Builder immune(Map<SlotRange, ItemPredicate> slots) {
            return this.immune(builder -> builder.slots(new SlotsPredicate(slots)));
        }

        public Builder immune(SlotRange slot, ItemPredicate.Builder item) {
            return this.immune(builder -> builder.slots(new SlotsPredicate(Map.of(slot, item.build()))));
        }

        public Builder immuneDirect(Consumer<EntityPredicate.Builder> direct) {
            EntityPredicate.Builder builder = EntityPredicate.Builder.entity();
            direct.accept(builder);
            this.immune.direct(builder);
            return this;
        }

        public Builder immuneDirect(HolderGetter<EntityType<?>> lookup, EntityType<?> type) {
            return this.immuneDirect(builder -> builder.entityType(EntityTypePredicate.of(lookup, type)));
        }

        public Builder immuneDirect(HolderGetter<EntityType<?>> lookup, TagKey<EntityType<?>> types) {
            return this.immuneDirect(builder -> builder.entityType(EntityTypePredicate.of(lookup, types)));
        }

        public Builder immuneDirect(EntitySubPredicate sub) {
            return this.immuneDirect(builder -> builder.subPredicate(sub));
        }

        public Builder immuneDirect(Map<SlotRange, ItemPredicate> slots) {
            return this.immuneDirect(builder -> builder.slots(new SlotsPredicate(slots)));
        }

        public Builder immuneDirect(SlotRange slot, ItemPredicate.Builder item) {
            return this.immuneDirect(builder -> builder.slots(new SlotsPredicate(Map.of(slot, item.build()))));
        }

        public Builder isDirect(boolean isDirect) {
            this.immune.isDirect(isDirect);
            return this;
        }

        public ImmuneAmulet build() {
            return new ImmuneAmulet(this.immune.build());
        }
    }
}
