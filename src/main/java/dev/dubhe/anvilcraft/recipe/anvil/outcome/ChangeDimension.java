package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter(AccessLevel.PRIVATE)
public class ChangeDimension implements IRecipeOutcome<ChangeDimension> {
    private final ResourceKey<Level> dimensionKey;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Vec3i> centerPos;
    private final Vec2 offset;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ChangeDimension(ResourceKey<Level> dimensionKey, Optional<Vec3i> centerPos, Vec2 offset) {
        this.dimensionKey = dimensionKey;
        this.centerPos = centerPos;
        this.offset = offset;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Type getType() {
        return ModRecipeOutcomeTypes.CHANGE_DIMENSION.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        if (!(context.getEntity() instanceof ThrownEnderpearl pearl)) return;
        Entity owner = pearl.getOwner();
        if (owner == null) return;
        ServerLevel originLevel = context.getLevel();
        MinecraftServer server = originLevel.getServer();
        ServerLevel targetLevel = server.getLevel(this.dimensionKey);
        if (targetLevel == null) return;
        if (!owner.canChangeDimensions(originLevel, targetLevel)) return;
        BlockPos targetPos = this.centerPos.map(BlockPos::new).orElseGet(targetLevel::getSharedSpawnPos);
        targetPos = this.withOffset(targetLevel.random, targetPos);
        owner.changeDimension(new DimensionTransition(
            targetLevel,
            targetPos.getBottomCenter(),
            Vec3.ZERO,
            0.0F,
            0.0F,
            DimensionTransition.PLAY_PORTAL_SOUND
        ));
    }

    private BlockPos withOffset(RandomSource random, BlockPos origin) {
        float dx = random.nextFloat() * this.offset.x;
        float dz = random.nextFloat() * this.offset.y;
        return origin.offset(Math.round(dx), 0, Math.round(dz));
    }

    public static class Type implements IRecipeOutcome.Type<ChangeDimension> {
        public static final MapCodec<ChangeDimension> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("destination").forGetter(ChangeDimension::getDimensionKey),
            Vec3i.CODEC.optionalFieldOf("center").forGetter(ChangeDimension::getCenterPos),
            CodecUtil.VEC2_CODEC.fieldOf("offset").forGetter(ChangeDimension::getOffset)
        ).apply(instance, ChangeDimension::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeDimension> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), ChangeDimension::getDimensionKey,
            ByteBufCodecs.optional(CodecUtil.VEC3I_STREAM_CODEC), ChangeDimension::getCenterPos,
            CodecUtil.VEC2_STREAM_CODEC, ChangeDimension::getOffset,
            ChangeDimension::new
        );

        @Override
        public @NotNull MapCodec<ChangeDimension> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ChangeDimension> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Getter
    public static class Builder {
        private ResourceKey<Level> dimensionKey;
        private Vec3i centerPos;
        private Vec2 offset = new Vec2(5, 5);

        private Builder() {
        }

        public Builder dimension(ResourceKey<Level> dimensionKey) {
            this.dimensionKey = dimensionKey;
            return this;
        }

        public Builder restrictNewPos(Vec3i pos) {
            this.centerPos = pos;
            return this;
        }

        public Builder offset(Vec2 offset) {
            this.offset = offset;
            return this;
        }

        public Builder offset(float x, float z) {
            this.offset = new Vec2(x, z);
            return this;
        }

        public ChangeDimension build() {
            return new ChangeDimension(this.dimensionKey, Optional.ofNullable(this.centerPos), this.offset);
        }
    }
}
