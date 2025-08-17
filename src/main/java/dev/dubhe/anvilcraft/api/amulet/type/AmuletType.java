package dev.dubhe.anvilcraft.api.amulet.type;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.fromto.Effect;
import dev.dubhe.anvilcraft.api.amulet.fromto.ImmuneDamage;
import dev.dubhe.anvilcraft.api.amulet.fromto.InventoryTick;
import dev.dubhe.anvilcraft.api.amulet.fromto.Obtain;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.util.Lazy;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
@Accessors(fluent = true, chain = false)
@MethodsReturnNonnullByDefault
public class AmuletType {
    public static final Codec<AmuletType> CODEC = Codec.lazyInitialized(ModRegistries.AMULET_TYPE_REGISTRY::byNameCodec);
    public static final StreamCodec<RegistryFriendlyByteBuf, AmuletType> STREAM_CODEC = ByteBufCodecs.registry(
        ModRegistries.AMULET_TYPE_KEY);
    private final Obtain obtain;
    private final Effect effect;
    private final Supplier<ItemStack> amulet;

    protected AmuletType(Obtain obtain, Effect effect, Supplier<ItemStack> amulet) {
        this.obtain = obtain;
        this.effect = effect;
        this.amulet = Lazy.of(amulet);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean canObtain(ServerPlayer player, DamageSource source) {
        return this.obtain.canObtain(player, source);
    }

    public boolean matches(ItemLike item) {
        return this.amulet.get().is(item.asItem());
    }

    public void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
        this.effect.inventoryTick(player, amulet, isEnabled);
    }

    public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
        return this.effect.shouldImmuneDamage(player, source);
    }

    @ParametersAreNonnullByDefault
    public static class Builder {
        private Obtain obtain = Obtain.NEVER;
        private InventoryTick inventoryTick = InventoryTick.NOP;
        private ImmuneDamage immuneDamage = ImmuneDamage.NEVER;
        private Supplier<ItemStack> amulet;

        Builder() {
        }

        @SafeVarargs
        public final Builder obtainByDamage(ResourceKey<DamageType>... damages) {
            return this.obtain((player, source) -> {
                for (ResourceKey<DamageType> type : damages) {
                    if (source.typeHolder().is(type)) return true;
                }
                return false;
            });
        }

        public Builder obtainByDamage(TagKey<DamageType> tag) {
            return this.obtain((player, source) -> source.typeHolder().is(tag));
        }

        public Builder obtainByDamage(ResourceLocation type) {
            return this.obtainByDamage(TagKey.create(Registries.DAMAGE_TYPE, type));
        }

        public Builder obtainByDamage(String type) {
            return this.obtainByDamage(TagKey.create(Registries.DAMAGE_TYPE, AnvilCraft.of("amulet_valid/" + type)));
        }

        public Builder obtainByMurder(EntityType<?>... entities) {
            return this.obtain((player, source) -> Optional.ofNullable(source.getEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> {
                    for (EntityType<?> type : entities) {
                        if (e.getType().equals(type)) return true;
                    }
                    return false;
                })
                .orElse(false));
        }

        public Builder obtainByMurder(TagKey<EntityType<?>> tag) {
            return this.obtain((player, source) -> Optional.ofNullable(source.getEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> e.getType().is(tag))
                .orElse(false));
        }

        public Builder obtainByMurder(ResourceLocation type) {
            return this.obtainByMurder(TagKey.create(Registries.ENTITY_TYPE, type));
        }

        public Builder obtainByMurder(String type) {
            return this.obtainByMurder(TagKey.create(Registries.ENTITY_TYPE, AnvilCraft.of("amulet_valid/" + type)));
        }

        public Builder obtainByDirectMurder(EntityType<?>... entities) {
            return this.obtain((player, source) -> Optional.ofNullable(source.getDirectEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> {
                    for (EntityType<?> type : entities) {
                        if (e.getType().equals(type)) return true;
                    }
                    return false;
                })
                .orElse(false));
        }

        public Builder obtainByDirectMurder(TagKey<EntityType<?>> tag) {
            return this.obtain((player, source) -> Optional.ofNullable(source.getDirectEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> e.getType().is(tag))
                .orElse(false));
        }

        public Builder obtainByDirectMurder(ResourceLocation type) {
            return this.obtainByDirectMurder(TagKey.create(Registries.ENTITY_TYPE, type));
        }

        public Builder obtainByDirectMurder(String type) {
            return this.obtainByDirectMurder(TagKey.create(Registries.ENTITY_TYPE, AnvilCraft.of("amulet_valid/" + type)));
        }

        public Builder obtain(Obtain obtain) {
            if (this.obtain == Obtain.NEVER) {
                this.obtain = obtain;
                return this;
            }
            this.obtain = this.obtain.and(obtain);
            return this;
        }

        @SafeVarargs
        public final Builder obtainByDamageOr(ResourceKey<DamageType>... damages) {
            return this.obtainOr((player, source) -> {
                for (ResourceKey<DamageType> type : damages) {
                    if (source.typeHolder().is(type)) return true;
                }
                return false;
            });
        }

        public Builder obtainByDamageOr(TagKey<DamageType> tag) {
            return this.obtainOr((player, source) -> source.typeHolder().is(tag));
        }

        public Builder obtainByDamageOr(ResourceLocation type) {
            return this.obtainByDamageOr(TagKey.create(Registries.DAMAGE_TYPE, type));
        }

        public Builder obtainByDamageOr(String type) {
            return this.obtainByDamageOr(TagKey.create(Registries.DAMAGE_TYPE, AnvilCraft.of("amulet_valid/" + type)));
        }

        public Builder obtainByMurderOr(EntityType<?>... entities) {
            return this.obtainOr((player, source) -> Optional.ofNullable(source.getEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> {
                    for (EntityType<?> type : entities) {
                        if (e.getType().equals(type)) return true;
                    }
                    return false;
                })
                .orElse(false));
        }

        public Builder obtainByMurderOr(TagKey<EntityType<?>> tag) {
            return this.obtainOr((player, source) -> Optional.ofNullable(source.getEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> e.getType().is(tag))
                .orElse(false));
        }

        public Builder obtainByMurderOr(ResourceLocation type) {
            return this.obtainByMurderOr(TagKey.create(Registries.ENTITY_TYPE, type));
        }

        public Builder obtainByMurderOr(String type) {
            return this.obtainByMurder(TagKey.create(Registries.ENTITY_TYPE, AnvilCraft.of("amulet_valid/" + type)));
        }

        public Builder obtainByDirectMurderOr(EntityType<?>... entities) {
            return this.obtainOr((player, source) -> Optional.ofNullable(source.getDirectEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> {
                    for (EntityType<?> type : entities) {
                        if (e.getType().equals(type)) return true;
                    }
                    return false;
                })
                .orElse(false));
        }

        public Builder obtainByDirectMurderOr(TagKey<EntityType<?>> tag) {
            return this.obtainOr((player, source) -> Optional.ofNullable(source.getDirectEntity())
                .flatMap(e -> Util.castSafely(e, LivingEntity.class))
                .map(e -> e.getType().is(tag))
                .orElse(false));
        }

        public Builder obtainByDirectMurderOr(ResourceLocation type) {
            return this.obtainByMurderOr(TagKey.create(Registries.ENTITY_TYPE, type));
        }

        public Builder obtainByDirectMurderOr(String type) {
            return this.obtainByMurder(TagKey.create(Registries.ENTITY_TYPE, AnvilCraft.of("amulet_valid/" + type)));
        }

        public Builder obtainOr(Obtain obtain) {
            if (this.obtain == Obtain.NEVER) {
                this.obtain = obtain;
                return this;
            }
            this.obtain = this.obtain.or(obtain);
            return this;
        }

        public Builder inventoryTick(InventoryTick inventoryTick) {
            if (this.inventoryTick == InventoryTick.NOP) {
                this.inventoryTick = inventoryTick;
                return this;
            }
            this.inventoryTick = this.inventoryTick.andThen(inventoryTick);
            return this;
        }

        public Builder immuneDamage(ImmuneDamage immuneDamage) {
            if (this.immuneDamage == ImmuneDamage.NEVER) {
                this.immuneDamage = immuneDamage;
                return this;
            }
            this.immuneDamage = this.immuneDamage.and(immuneDamage);
            return this;
        }

        public Builder immuneDamageOr(ImmuneDamage immuneDamage) {
            if (this.immuneDamage == ImmuneDamage.NEVER) {
                this.immuneDamage = immuneDamage;
                return this;
            }
            this.immuneDamage = this.immuneDamage.or(immuneDamage);
            return this;
        }

        public Builder immuneDamageFromObtain() {
            this.immuneDamage = this.obtain::canObtain;
            return this;
        }

        public Builder amulet(Supplier<ItemStack> amuletGetter) {
            this.amulet = amuletGetter;
            return this;
        }

        public Builder amulet(ItemStack amulet) {
            return this.amulet(() -> amulet);
        }

        public Builder amulet(ItemLike amulet) {
            return this.amulet(() -> amulet.asItem().getDefaultInstance());
        }

        public AmuletType build() {
            if (this.amulet == null) {
                throw new IllegalArgumentException("The amulet of the amulet type cannot be null!");
            }
            Obtain obtain = this.obtain;
            InventoryTick inventoryTick = this.inventoryTick;
            ImmuneDamage immuneDamage = this.immuneDamage;
            return new AmuletType(obtain, new Effect(inventoryTick, immuneDamage), this.amulet);
        }
    }
}
