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

/**
 * 维度传送配方结果
 * <p>
 * 该类定义了将实体传送到另一个维度的配方结果操作，通常与末影珍珠相关
 * 当配方匹配时，会将末影珍珠及其投掷者传送到指定的维度和位置
 *
 * @see IRecipeOutcome 配方结果接口
 */
@Getter(AccessLevel.PRIVATE)
public class ChangeDimension implements IRecipeOutcome<ChangeDimension> {
    private final ResourceKey<Level> dimensionKey;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Vec3i> centerPos;
    private final Vec2 offset;

    /**
     * 构造一个维度传送配方结果
     *
     * @param dimensionKey 目标维度键
     * @param centerPos 传送中心位置（可选）
     * @param offset 传送位置偏移量
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ChangeDimension(ResourceKey<Level> dimensionKey, Optional<Vec3i> centerPos, Vec2 offset) {
        this.dimensionKey = dimensionKey;
        this.centerPos = centerPos;
        this.offset = offset;
    }

    /**
     * 创建一个新的维度传送结果构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取配方结果类型
     *
     * @return 维度传送结果类型
     */
    @Override
    public Type getType() {
        return ModRecipeOutcomeTypes.CHANGE_DIMENSION.get();
    }

    /**
     * 执行维度传送操作
     * <p>
     * 检查上下文中的实体是否为末影珍珠，并将其投掷者传送到目标维度
     *
     * @param context 配方上下文
     */
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

    /**
     * 根据随机偏移量计算最终传送位置
     *
     * @param random 随机数生成器
     * @param origin 原始位置
     * @return 带偏移量的目标位置
     */
    private BlockPos withOffset(RandomSource random, BlockPos origin) {
        float dx = random.nextFloat() * this.offset.x;
        float dz = random.nextFloat() * this.offset.y;
        return origin.offset(Math.round(dx), 0, Math.round(dz));
    }

    /**
     * 维度传送结果类型
     * <p>
     * 定义了维度传送结果的序列化和反序列化方式
     */
    public static class Type implements IRecipeOutcome.Type<ChangeDimension> {
        public static final MapCodec<ChangeDimension> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION)
                .fieldOf("destination")
                .forGetter(ChangeDimension::getDimensionKey),
            Vec3i.CODEC
                .optionalFieldOf("center")
                .forGetter(ChangeDimension::getCenterPos),
            CodecUtil.VEC2_CODEC
                .fieldOf("offset")
                .forGetter(ChangeDimension::getOffset)
        ).apply(instance, ChangeDimension::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeDimension> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION),
            ChangeDimension::getDimensionKey,
            ByteBufCodecs.optional(CodecUtil.VEC3I_STREAM_CODEC),
            ChangeDimension::getCenterPos,
            CodecUtil.VEC2_STREAM_CODEC,
            ChangeDimension::getOffset,
            ChangeDimension::new
        );

        /**
         * 获取配方结果的编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<ChangeDimension> codec() {
            return CODEC;
        }

        /**
         * 获取配方结果的流编解码器，用于网络传输
         *
         * @return StreamCodec流编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ChangeDimension> streamCodec() {
            return STREAM_CODEC;
        }
    }

    /**
     * 维度传送结果构建器
     * <p>
     * 提供便捷的方法来构建维度传送结果，包括设置目标维度、传送位置等参数
     */
    @Getter
    public static class Builder {
        private ResourceKey<Level> dimensionKey;
        private Vec3i centerPos;
        private Vec2 offset = new Vec2(5, 5);

        /**
         * 创建一个新的构建器实例
         */
        private Builder() {
        }

        /**
         * 设置目标维度
         *
         * @param dimensionKey 目标维度键
         * @return 当前构建器实例
         */
        public Builder dimension(ResourceKey<Level> dimensionKey) {
            this.dimensionKey = dimensionKey;
            return this;
        }

        /**
         * 设置传送中心位置
         *
         * @param pos 传送中心位置
         * @return 当前构建器实例
         */
        public Builder restrictNewPos(Vec3i pos) {
            this.centerPos = pos;
            return this;
        }

        /**
         * 设置传送位置偏移量
         *
         * @param offset 偏移量向量
         * @return 当前构建器实例
         */
        public Builder offset(Vec2 offset) {
            this.offset = offset;
            return this;
        }

        /**
         * 设置传送位置偏移量
         *
         * @param x X轴偏移量
         * @param z Z轴偏移量
         * @return 当前构建器实例
         */
        public Builder offset(float x, float z) {
            this.offset = new Vec2(x, z);
            return this;
        }

        /**
         * 构建维度传送结果实例
         *
         * @return 维度传送结果实例
         */
        public ChangeDimension build() {
            return new ChangeDimension(this.dimensionKey, Optional.ofNullable(this.centerPos), this.offset);
        }
    }
}