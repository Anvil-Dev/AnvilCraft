package dev.dubhe.anvilcraft.api.amulet.def;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public record AmuletDefinition(
    ItemStack amulet,
    List<DamageSourcePredicate> obtains,
    boolean or
) implements IAmuletDefinition {
    public AmuletDefinition(ItemLike amulet, DataComponentPatch components, List<DamageSourcePredicate> obtains, boolean or) {
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), obtains, or);
    }

    public AmuletDefinition(ItemLike amulet, DataComponentPatch components, DamageSourcePredicate obtain, boolean or) {
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), Collections.singletonList(obtain), or);
    }

    public AmuletDefinition(ItemLike amulet, List<DamageSourcePredicate> obtains, boolean or) {
        this(new ItemStack(amulet.asItem()), obtains, or);
    }

    public AmuletDefinition(ItemLike amulet, DamageSourcePredicate obtain, boolean or) {
        this(new ItemStack(amulet.asItem()), Collections.singletonList(obtain), or);
    }

    public AmuletDefinition(ItemLike amulet, DataComponentPatch components, boolean or) {
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), List.of(), or);
    }

    public AmuletDefinition(ItemLike amulet, boolean or) {
        this(new ItemStack(amulet.asItem()), List.of(), or);
    }

    public AmuletDefinition(ItemLike amulet, DataComponentPatch components, List<DamageSourcePredicate> obtains) {
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), obtains, true);
    }

    public AmuletDefinition(ItemLike amulet, DataComponentPatch components, DamageSourcePredicate obtain) {
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), Collections.singletonList(obtain), true);
    }

    public AmuletDefinition(ItemLike amulet, List<DamageSourcePredicate> obtains) {
        this(new ItemStack(amulet.asItem()), obtains, true);
    }

    public AmuletDefinition(ItemLike amulet, DamageSourcePredicate obtain) {
        this(new ItemStack(amulet.asItem()), Collections.singletonList(obtain), true);
    }

    public AmuletDefinition(ItemLike amulet, DataComponentPatch components) {
        this(new ItemStack(amulet.asItem().builtInRegistryHolder(), 1, components), List.of(), true);
    }

    public AmuletDefinition(ItemLike amulet) {
        this(new ItemStack(amulet.asItem()), List.of(), true);
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
        for (DamageSourcePredicate predicate : this.obtains) {
            if (this.or == predicate.matches(victim, source)) {
                return this.or;
            }
        }
        return !this.or;
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
            CodecUtil.zomListMap(DamageSourcePredicate.CODEC, "obtains")
                .forGetter(AmuletDefinition::obtains),
            Codec.BOOL
                .optionalFieldOf("or", true)
                .forGetter(AmuletDefinition::or)
        ).apply(inst, AmuletDefinition::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AmuletDefinition> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            AmuletDefinition::amulet,
            StreamCodecUtil.DAMAGE_SOURCE_PREDICATE.apply(ByteBufCodecs.list()),
            AmuletDefinition::obtains,
            ByteBufCodecs.BOOL,
            AmuletDefinition::or,
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
        private final ImmutableList.Builder<DamageSourcePredicate> obtains = ImmutableList.builder();
        private @Nullable DamageSourcePredicate.Builder obtain = null;
        private boolean or = true;

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

        public Builder obtainEnd() {
            if (this.obtain == null) {
                throw new IllegalStateException("Unexpected end when not started");
            }
            this.obtains.add(this.obtain.build());
            this.obtain = null;
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

        public Builder and() {
            this.or = false;
            return this;
        }

        public AmuletDefinition build() {
            if (this.obtain != null) {
                this.obtains.add(this.obtain.build());
            }
            if (this.components == null) {
                return new AmuletDefinition(this.amulet, this.obtains.build(), this.or);
            } else {
                return new AmuletDefinition(this.amulet, this.components.build(), this.obtains.build(), this.or);
            }
        }
    }
}
