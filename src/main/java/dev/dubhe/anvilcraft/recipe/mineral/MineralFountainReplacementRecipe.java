package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 替换生产：存在主动生产匹配时执行，按优先级依次尝试替换其结果。
 *
 * <p>配方 id 表为空表示可以替换任意主动配方；{@code chance} 为替换概率。</p>
 */
public class MineralFountainReplacementRecipe extends AbstractMineralFountainRecipe {
    /** 可被替换的主动配方 id；为空表示不限制。 */
    private final List<ResourceLocation> replace;
    /** 替换概率。 */
    private final double chance;

    public MineralFountainReplacementRecipe(
        Optional<ResourceLocation> dimension,
        HeightRange height,
        List<ResourceLocation> replace,
        int priority,
        double chance,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat
    ) {
        // 替换生产不使用权重：候选按优先级逐个掷 chance 尝试，故固定为默认权重
        super(dimension, height, priority, DEFAULT_WEIGHT, fromBlock, toBlock, heat);
        this.replace = List.copyOf(replace);
        // 数据包不受 validate() 约束，这里直接夹紧，保证概率语义始终成立
        this.chance = Math.clamp(chance, 0.0, 1.0);
    }

    public List<ResourceLocation> getReplace() {
        return this.replace;
    }

    public double getReplaceChance() {
        return this.chance;
    }

    /**
     * 判断本替换是否适用于给定的主动配方。
     */
    public boolean appliesTo(@Nullable ResourceLocation activeRecipeId) {
        return this.replace.isEmpty() || (activeRecipeId != null && this.replace.contains(activeRecipeId));
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_REPLACEMENT.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_REPLACEMENT_SERIALIZER.get();
    }

    @Override
    public boolean matches(MineralFountainRecipeInput input, Level level) {
        return this.matchesCommon(input, level);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Serializer implements RecipeSerializer<MineralFountainReplacementRecipe> {
        private static final MapCodec<MineralFountainReplacementRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("dimension")
                    .forGetter(AbstractMineralFountainRecipe::getDimension),
                HeightRange.CODEC.optionalFieldOf("height", HeightRange.DEFAULT)
                    .forGetter(AbstractMineralFountainRecipe::getHeight),
                ResourceLocation.CODEC.listOf().optionalFieldOf("replace", List.of())
                    .forGetter(MineralFountainReplacementRecipe::getReplace),
                Codec.INT.optionalFieldOf("priority", 0)
                    .forGetter(AbstractMineralFountainRecipe::getPriority),
                Codec.DOUBLE.optionalFieldOf("chance", 0.0)
                    .forGetter(MineralFountainReplacementRecipe::getReplaceChance),
                BlockStatePredicate.CODEC.optionalFieldOf("from_block", AbstractMineralFountainRecipe.DEFAULT_FROM)
                    .forGetter(AbstractMineralFountainRecipe::getFromBlock),
                ChanceBlockState.CODEC.fieldOf("to_block")
                    .forGetter(AbstractMineralFountainRecipe::getToBlock),
                MineralFountainHeat.CODEC.optionalFieldOf("heat")
                    .forGetter(AbstractMineralFountainRecipe::getHeat)
            ).apply(instance, MineralFountainReplacementRecipe::new));

        // StreamCodec.composite 最多支持 6 个字段，本配方字段更多，故手工编解码
        private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainReplacementRecipe> STREAM_CODEC =
            StreamCodec.of(
                (buf, recipe) -> {
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, recipe.getDimension());
                    HeightRange.STREAM_CODEC.encode(buf, recipe.getHeight());
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getReplace());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getPriority());
                    ByteBufCodecs.DOUBLE.encode(buf, recipe.getReplaceChance());
                    BlockStatePredicate.STREAM_CODEC.encode(buf, recipe.getFromBlock());
                    ChanceBlockState.STREAM_CODEC.encode(buf, recipe.getToBlock());
                    ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).encode(buf, recipe.getHeat());
                },
                buf -> new MineralFountainReplacementRecipe(
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                    HeightRange.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    BlockStatePredicate.STREAM_CODEC.decode(buf),
                    ChanceBlockState.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).decode(buf)
                )
            );

        @Override
        public MapCodec<MineralFountainReplacementRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MineralFountainReplacementRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends AbstractRecipeBuilder<MineralFountainReplacementRecipe> {
        private Optional<ResourceLocation> dimension = Optional.empty();
        private HeightRange height = HeightRange.DEFAULT;
        private List<ResourceLocation> replace = List.of();
        private int priority = 0;
        private double chance = 0.0;
        private BlockStatePredicate fromBlock = AbstractMineralFountainRecipe.DEFAULT_FROM;
        private @Nullable ChanceBlockState toBlock;
        private Optional<MineralFountainHeat> heat = Optional.empty();

        public Builder dimension(ResourceLocation dimension) {
            this.dimension = Optional.of(dimension);
            return this;
        }

        public Builder height(int min, int max) {
            this.height = new HeightRange(min, max);
            return this;
        }

        public Builder replace(List<ResourceLocation> replace) {
            this.replace = List.copyOf(replace);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder chance(double chance) {
            this.chance = chance;
            return this;
        }

        public Builder fromBlock(BlockStatePredicate fromBlock) {
            this.fromBlock = fromBlock;
            return this;
        }

        public Builder toBlock(ChanceBlockState toBlock) {
            this.toBlock = toBlock;
            return this;
        }

        public Builder heat(MineralFountainHeat heat) {
            this.heat = Optional.of(heat);
            return this;
        }

        @Override
        public MineralFountainReplacementRecipe buildRecipe() {
            if (this.toBlock == null) throw new IllegalStateException("to_block is required");
            return new MineralFountainReplacementRecipe(
                this.dimension, this.height, this.replace, this.priority, this.chance,
                this.fromBlock, this.toBlock, this.heat
            );
        }

        @Override
        public void validate(ResourceLocation id) {
            if (this.toBlock == null) {
                throw new IllegalArgumentException("to_block must not be null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain_replacement";
        }

        @Override
        public Item getResult() {
            return this.toBlock == null ? Items.AIR : this.toBlock.state().getBlock().asItem();
        }
    }
}
