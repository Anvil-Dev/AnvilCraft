package dev.dubhe.anvilcraft.recipe.anvil.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

@Getter
public class HasEnderPearl implements IRecipePredicate<HasEnderPearl> {
    private final ResourceKey<Level> dimensionKey;
    private final double speed;
    private final double height;

    public HasEnderPearl(ResourceKey<Level> dimensionKey, double speed, double height) {
        this.dimensionKey = dimensionKey;
        this.speed = speed;
        this.height = height;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_ENDER_PEARL.get();
    }

    @Override
    public boolean test(InWorldRecipeContext context) {
        if (!(context.getEntity() instanceof ThrownEnderpearl pearl)) return false;
        if (!context.getLevel().dimension().location().equals(this.dimensionKey.location())) return false;
        if (Math.abs(pearl.getDeltaMovement().y) < this.speed) return false;
        return pearl.position().y >= this.height;
    }

    public static class Type implements IRecipePredicate.Type<HasEnderPearl> {
        public static final MapCodec<HasEnderPearl> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("origin").forGetter(HasEnderPearl::getDimensionKey),
            Codec.DOUBLE.fieldOf("speed").forGetter(HasEnderPearl::getSpeed),
            Codec.DOUBLE.fieldOf("height").forGetter(HasEnderPearl::getHeight)
        ).apply(instance, HasEnderPearl::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, HasEnderPearl> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), HasEnderPearl::getDimensionKey,
            ByteBufCodecs.DOUBLE, HasEnderPearl::getSpeed,
            ByteBufCodecs.DOUBLE, HasEnderPearl::getHeight,
            HasEnderPearl::new
        );

        @Override
        public @NotNull MapCodec<HasEnderPearl> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, HasEnderPearl> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Getter
    public static class Builder {
        private ResourceKey<Level> dimensionKey;
        private double speed;
        private double height;

        private Builder() {
        }

        public Builder dimension(ResourceKey<Level> dimensionKey) {
            this.dimensionKey = dimensionKey;
            return this;
        }

        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder height(double height) {
            this.height = height;
            return this;
        }

        public HasEnderPearl build() {
            return new HasEnderPearl(this.dimensionKey, this.speed, this.height);
        }
    }
}
