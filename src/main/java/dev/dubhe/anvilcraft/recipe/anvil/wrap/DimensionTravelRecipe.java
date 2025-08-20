package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ChangeDimension;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.entity.HasEnderPearl;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 维度旅行配方类
 * <p>
 * 该类定义了通过末影珍珠进行维度旅行的配方逻辑，包括判断末影珍珠是否满足条件以及执行维度传送操作
 * <p>
 * 配方由两个主要部分组成：
 * 1. {@link HasEnderPearl} - 判断末影珍珠是否满足触发条件（如速度、高度、所在维度等）
 * 2. {@link ChangeDimension} - 执行维度传送的具体操作
 *
 * @see HasEnderPearl 末影珍珠条件判断
 * @see ChangeDimension 维度传送操作
 */
@Getter(AccessLevel.PRIVATE)
public class DimensionTravelRecipe extends InWorldRecipe {
    private final HasEnderPearl hasEnderPearl;
    private final ChangeDimension changeDimension;

    /**
     * 构造一个维度旅行配方
     *
     * @param hasEnderPearl 末影珍珠条件谓词
     * @param changeDimension 维度传送结果
     */
    public DimensionTravelRecipe(HasEnderPearl hasEnderPearl, ChangeDimension changeDimension) {
        super(
            Items.ENDER_PEARL.getDefaultInstance(),
            ModRecipeTriggers.ON_ENDER_PEARL_TICK.get(),
            List.of(),
            List.of(hasEnderPearl),
            List.of(changeDimension),
            DimensionTravelRecipe.getPriority(hasEnderPearl),
            false
        );
        this.hasEnderPearl = hasEnderPearl;
        this.changeDimension = changeDimension;
    }

    /**
     * 创建一个新的配方构建器
     *
     * @return 配方构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 判断该配方是否为特殊配方（不在配方书中显示）
     *
     * @return true 表示为特殊配方
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * 计算配方优先级，基于末影珍珠的高度和速度
     *
     * @param hasEnderPearl 末影珍珠条件谓词
     * @return 计算出的优先级数值
     */
    private static int getPriority(HasEnderPearl hasEnderPearl) {
        return (int) Math.round(hasEnderPearl.getHeight() * 2)
               + (int) Math.floor(hasEnderPearl.getSpeed() / 3);
    }

    /**
     * 获取配方类型
     *
     * @return 维度旅行配方类型
     */
    @Override
    public @NotNull RecipeType<DimensionTravelRecipe> getType() {
        return ModRecipeTypes.DIMENSION_TRAVEL_TYPE.get();
    }

    /**
     * 获取配方序列化器
     *
     * @return 维度旅行配方序列化器
     */
    @Override
    public @NotNull RecipeSerializer<DimensionTravelRecipe> getSerializer() {
        return ModRecipeTypes.DIMENSION_TRAVEL_SERIALIZER.get();
    }

    /**
     * 维度旅行配方序列化器
     * <p>
     * 负责将配方对象序列化为网络传输格式或从网络传输格式反序列化为配方对象
     */
    public static class Serializer implements RecipeSerializer<DimensionTravelRecipe> {
        public static final MapCodec<DimensionTravelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            HasEnderPearl.Type.CODEC.forGetter(DimensionTravelRecipe::getHasEnderPearl),
            ChangeDimension.Type.CODEC.forGetter(DimensionTravelRecipe::getChangeDimension)
        ).apply(instance, DimensionTravelRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DimensionTravelRecipe> STREAM_CODEC = StreamCodec.composite(
            HasEnderPearl.Type.STREAM_CODEC, DimensionTravelRecipe::getHasEnderPearl,
            ChangeDimension.Type.STREAM_CODEC, DimensionTravelRecipe::getChangeDimension,
            DimensionTravelRecipe::new
        );

        /**
         * 获取配方的编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<DimensionTravelRecipe> codec() {
            return Serializer.CODEC;
        }

        /**
         * 获取配方的流编解码器，用于网络传输
         *
         * @return StreamCodec流编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, DimensionTravelRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    /**
     * 维度旅行配方构建器
     * <p>
     * 提供便捷的方法来构建维度旅行配方，包括设置起始维度、目标维度、传送条件等参数
     */
    public static class Builder extends AbstractRecipeBuilder<DimensionTravelRecipe> {
        private final HasEnderPearl.Builder hasEnderPearl = HasEnderPearl.builder();
        private final ChangeDimension.Builder changeDimension = ChangeDimension.builder();

        /**
         * 从已有的末影珍珠条件构建器复制参数
         *
         * @param builder 末影珍珠条件构建器
         * @return 当前构建器实例
         */
        public Builder from(HasEnderPearl.Builder builder) {
            this.hasEnderPearl
                .dimension(builder.getDimensionKey())
                .speed(builder.getSpeed())
                .height(builder.getHeight());
            return this;
        }

        /**
         * 设置起始维度
         *
         * @param dimensionKey 起始维度键
         * @return 当前构建器实例
         */
        public Builder from(ResourceKey<Level> dimensionKey) {
            this.hasEnderPearl.dimension(dimensionKey);
            return this;
        }

        /**
         * 设置触发传送所需的最小速度
         *
         * @param speed 最小速度
         * @return 当前构建器实例
         */
        public Builder speed(double speed) {
            this.hasEnderPearl.speed(speed);
            return this;
        }

        /**
         * 设置触发传送所需的最小高度
         *
         * @param height 最小高度
         * @return 当前构建器实例
         */
        public Builder height(double height) {
            this.hasEnderPearl.height(height);
            return this;
        }

        /**
         * 从已有的维度传送结果构建器复制参数
         *
         * @param builder 维度传送结果构建器
         * @return 当前构建器实例
         */
        public Builder to(ChangeDimension.Builder builder) {
            this.changeDimension
                .dimension(builder.getDimensionKey())
                .restrictNewPos(builder.getCenterPos())
                .offset(builder.getOffset());
            return this;
        }

        /**
         * 设置目标维度
         *
         * @param dimensionKey 目标维度键
         * @return 当前构建器实例
         */
        public Builder to(ResourceKey<Level> dimensionKey) {
            this.changeDimension.dimension(dimensionKey);
            return this;
        }

        /**
         * 设置传送目标位置
         *
         * @param newPos 目标位置
         * @return 当前构建器实例
         */
        public Builder toPos(Vec3i newPos) {
            this.changeDimension.restrictNewPos(newPos);
            return this;
        }

        /**
         * 设置传送目标位置偏移量
         *
         * @param offset 偏移量向量
         * @return 当前构建器实例
         */
        public Builder toPosOffset(Vec2 offset) {
            this.changeDimension.offset(offset);
            return this;
        }

        /**
         * 设置传送目标位置偏移量
         *
         * @param x X轴偏移量
         * @param z Z轴偏移量
         * @return 当前构建器实例
         */
        public Builder toPosOffset(float x, float z) {
            this.changeDimension.offset(x, z);
            return this;
        }

        /**
         * 验证构建器参数是否合法
         *
         * @param pId 配方ID
         * @throws IllegalArgumentException 当参数不合法时抛出异常
         */
        @Override
        public void validate(@NotNull ResourceLocation pId) {
            if (this.hasEnderPearl.getDimensionKey() == null) {
                throw new IllegalArgumentException("The dimension key of the Ender Pearl must not be null!");
            }
            if (this.hasEnderPearl.getSpeed() <= 0) {
                throw new IllegalArgumentException("The dimension key of the Ender Pearl must not be lesser than 0!");
            }
            if (this.changeDimension.getDimensionKey() == null) {
                throw new IllegalArgumentException("The dimension key of the Destination must not be null!");
            }
        }

        /**
         * 构建配方实例
         *
         * @return 维度旅行配方实例
         */
        @Override
        public @NotNull DimensionTravelRecipe buildRecipe() {
            return new DimensionTravelRecipe(this.hasEnderPearl.build(), this.changeDimension.build());
        }

        /**
         * 获取配方结果物品（用于配方书显示）
         *
         * @return 末影珍珠物品实例
         */
        @Override
        public @NotNull Item getResult() {
            return Items.ENDER_PEARL;
        }

        /**
         * 获取配方类型名称
         *
         * @return 配方类型名称 "dimension_travel"
         */
        @Override
        public @NotNull String getType() {
            return "dimension_travel";
        }

        /**
         * 保存配方到输出
         *
         * @param recipeOutput 配方输出
         */
        @Override
        public void save(@NotNull RecipeOutput recipeOutput) {
            this.save(
                recipeOutput,
                AnvilCraft.of(this.changeDimension.getDimensionKey().location().getPath())
                    .withPrefix("dimension_travel/to_"));
        }
    }
}