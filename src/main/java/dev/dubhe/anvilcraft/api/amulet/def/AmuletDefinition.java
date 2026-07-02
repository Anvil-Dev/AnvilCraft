package dev.dubhe.anvilcraft.api.amulet.def;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.item.ModAmuletDefinitionTypes;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SlotsPredicate;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public record AmuletDefinition(
    ItemStack amulet,
    Optional<DamageSourcePredicate> obtain
) implements IAmuletDefinition {
    public AmuletDefinition(ItemLike amulet, DataComponentPatch components, DamageSourcePredicate obtain) {
        // noinspection deprecation
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), Optional.of(obtain));
    }

    public AmuletDefinition(ItemLike amulet, DamageSourcePredicate obtain) {
        this(new ItemStack(amulet.asItem()), Optional.of(obtain));
    }

    public AmuletDefinition(ItemLike amulet, DataComponentPatch components) {
        // noinspection deprecation
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), Optional.empty());
    }

    public AmuletDefinition(ItemLike amulet) {
        this(new ItemStack(amulet.asItem()), Optional.empty());
    }

    public static Builder builder(ItemLike amulet) {
        return new Builder(amulet);
    }

    @Override
    public ItemStack create() {
        return this.amulet.copyWithCount(1);
    }

    @Override
    public boolean mayObtain(ServerPlayer victim, DamageSource source) {
        return this.obtain().map(predicate -> predicate.matches(victim, source)).orElse(false);
    }

    @Override
    public Type getType() {
        return ModAmuletDefinitionTypes.NORMAL.get();
    }

    public static class Type implements IAmuletDefinition.Type<AmuletDefinition> {
        public static final MapCodec<AmuletDefinition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ItemStack.CODEC
                .fieldOf("amulet")
                .forGetter(AmuletDefinition::amulet),
            DamageSourcePredicate.CODEC
                .optionalFieldOf("obtain")
                .forGetter(AmuletDefinition::obtain)
        ).apply(inst, AmuletDefinition::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AmuletDefinition> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            AmuletDefinition::amulet,
            ByteBufCodecs.optional(StreamCodecUtil.DAMAGE_SOURCE_PREDICATE),
            AmuletDefinition::obtain,
            AmuletDefinition::new
        );

        @Override
        public MapCodec<AmuletDefinition> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AmuletDefinition> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    public static class Builder {
        private final ItemLike amulet;
        private @Nullable DataComponentPatch.Builder components = null;
        private @Nullable DamageSourcePredicate.Builder obtain = null;

        public Builder(ItemLike amulet) {
            this.amulet = amulet;
        }

        public <T> Builder component(DataComponentType<T> type, T value) {
            if (this.components == null) {
                this.components = DataComponentPatch.builder();
            }
            this.components.set(type, value);
            return this;
        }

        public <T> Builder component(TypedDataComponent<T> component) {
            if (this.components == null) {
                this.components = DataComponentPatch.builder();
            }
            this.components.set(component);
            return this;
        }

        public Builder component(Iterable<TypedDataComponent<?>> components) {
            if (this.components == null) {
                this.components = DataComponentPatch.builder();
            }
            for (TypedDataComponent<?> component : components) {
                this.components.set(component);
            }
            return this;
        }

        public Builder obtain(TagPredicate<DamageType> types) {
            if (this.obtain == null) {
                this.obtain = DamageSourcePredicate.Builder.damageType();
            }
            this.obtain.tag(types);
            return this;
        }

        public Builder obtain(TagKey<DamageType> types) {
            return this.obtain(TagPredicate.is(types));
        }

        public Builder obtainNot(TagKey<DamageType> types) {
            return this.obtain(TagPredicate.isNot(types));
        }

        // CHECKSTYLE.SUPPRESS: OverloadMethodsDeclarationOrder
        public Builder obtain(TagKey<DamageType> types, boolean expected) {
            return this.obtain(new TagPredicate<>(types, expected));
        }

        public Builder obtain(Consumer<EntityPredicate.Builder> source) {
            if (this.obtain == null) {
                this.obtain = DamageSourcePredicate.Builder.damageType();
            }
            EntityPredicate.Builder builder = EntityPredicate.Builder.entity();
            source.accept(builder);
            this.obtain.source(builder);
            return this;
        }

        public Builder obtain(EntityType<?> type) {
            return this.obtain(builder -> builder.entityType(EntityTypePredicate.of(type)));
        }

        public Builder obtainEntity(TagKey<EntityType<?>> types) {
            return this.obtain(builder -> builder.entityType(EntityTypePredicate.of(types)));
        }

        // CHECKSTYLE.SUPPRESS: OverloadMethodsDeclarationOrder
        public Builder obtain(EntitySubPredicate sub) {
            return this.obtain(builder -> builder.subPredicate(sub));
        }

        public Builder obtain(Map<SlotRange, ItemPredicate> slots) {
            return this.obtain(builder -> builder.slots(new SlotsPredicate(slots)));
        }

        public Builder obtain(SlotRange slot, ItemPredicate.Builder item) {
            return this.obtain(builder -> builder.slots(new SlotsPredicate(Map.of(slot, item.build()))));
        }

        public Builder obtainDirect(Consumer<EntityPredicate.Builder> direct) {
            if (this.obtain == null) {
                this.obtain = DamageSourcePredicate.Builder.damageType();
            }
            EntityPredicate.Builder builder = EntityPredicate.Builder.entity();
            direct.accept(builder);
            this.obtain.direct(builder);
            return this;
        }

        public Builder obtainDirect(EntityType<?> type) {
            return this.obtainDirect(builder -> builder.entityType(EntityTypePredicate.of(type)));
        }

        public Builder obtainDirect(TagKey<EntityType<?>> types) {
            return this.obtainDirect(builder -> builder.entityType(EntityTypePredicate.of(types)));
        }

        public Builder obtainDirect(EntitySubPredicate sub) {
            return this.obtainDirect(builder -> builder.subPredicate(sub));
        }

        public Builder obtainDirect(Map<SlotRange, ItemPredicate> slots) {
            return this.obtainDirect(builder -> builder.slots(new SlotsPredicate(slots)));
        }

        public Builder obtainDirect(SlotRange slot, ItemPredicate.Builder item) {
            return this.obtainDirect(builder -> builder.slots(new SlotsPredicate(Map.of(slot, item.build()))));
        }

        public AmuletDefinition build() {
            if (this.components == null) {
                if (this.obtain == null) {
                    return new AmuletDefinition(this.amulet);
                }
                return new AmuletDefinition(this.amulet, this.obtain.build());
            } else {
                if (this.obtain == null) {
                    return new AmuletDefinition(this.amulet, this.components.build());
                }
                return new AmuletDefinition(this.amulet, this.components.build(), this.obtain.build());
            }
        }
    }
}
