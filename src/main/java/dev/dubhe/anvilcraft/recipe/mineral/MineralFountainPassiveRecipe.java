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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 被动生产：没有任何主动生产匹配时执行。
 *
 * <p>默认材料方块为空气，即旧版「上方为空气则生成火山灰」的行为。
 * 同一维度可放多条同优先级配方，按 {@code weight} 随机取一，用数据包表达任意产出比例。</p>
 */
public class MineralFountainPassiveRecipe extends AbstractMineralFountainRecipe {
    public MineralFountainPassiveRecipe(
        Optional<ResourceLocation> dimension,
        HeightRange height,
        int priority,
        int weight,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat,
        Optional<FluidStack> toFluid
    ) {
        super(dimension, height, priority, weight, fromBlock, toBlock, heat, toFluid);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_PASSIVE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_PASSIVE_SERIALIZER.get();
    }

    @Override
    public boolean matches(MineralFountainRecipeInput input, Level level) {
        return this.matchesCommon(input, level);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Serializer implements RecipeSerializer<MineralFountainPassiveRecipe> {
        private static final MapCodec<MineralFountainPassiveRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.optionalFieldOf("dimension")
                    .forGetter(AbstractMineralFountainRecipe::getDimension),
                HeightRange.CODEC.optionalFieldOf("height", HeightRange.DEFAULT)
                    .forGetter(AbstractMineralFountainRecipe::getHeight),
                Codec.INT.optionalFieldOf("priority", 0)
                    .forGetter(AbstractMineralFountainRecipe::getPriority),
                Codec.INT.optionalFieldOf("weight", DEFAULT_WEIGHT)
                    .forGetter(AbstractMineralFountainRecipe::getWeight),
                BlockStatePredicate.CODEC.optionalFieldOf("from_block", DEFAULT_FROM)
                    .forGetter(AbstractMineralFountainRecipe::getFromBlock),
                ChanceBlockState.CODEC.fieldOf("to_block")
                    .forGetter(AbstractMineralFountainRecipe::getToBlock),
                MineralFountainHeat.CODEC.optionalFieldOf("heat")
                    .forGetter(AbstractMineralFountainRecipe::getHeat),
                FluidStack.CODEC.optionalFieldOf("to_fluid")
                    .forGetter(AbstractMineralFountainRecipe::getToFluid)
            ).apply(instance, MineralFountainPassiveRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainPassiveRecipe> STREAM_CODEC =
            StreamCodec.of(
                (buf, recipe) -> {
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, recipe.getDimension());
                    HeightRange.STREAM_CODEC.encode(buf, recipe.getHeight());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getPriority());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getWeight());
                    BlockStatePredicate.STREAM_CODEC.encode(buf, recipe.getFromBlock());
                    ChanceBlockState.STREAM_CODEC.encode(buf, recipe.getToBlock());
                    ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).encode(buf, recipe.getHeat());
                    ByteBufCodecs.optional(FluidStack.STREAM_CODEC).encode(buf, recipe.getToFluid());
                },
                buf -> new MineralFountainPassiveRecipe(
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                    HeightRange.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    BlockStatePredicate.STREAM_CODEC.decode(buf),
                    ChanceBlockState.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.optional(FluidStack.STREAM_CODEC).decode(buf)
                )
            );

        @Override
        public MapCodec<MineralFountainPassiveRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MineralFountainPassiveRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends AbstractRecipeBuilder<MineralFountainPassiveRecipe> {
        private Optional<ResourceLocation> dimension = Optional.empty();
        private HeightRange height = HeightRange.DEFAULT;
        private int priority = 0;
        private int weight = DEFAULT_WEIGHT;
        private BlockStatePredicate fromBlock = DEFAULT_FROM;
        private @Nullable ChanceBlockState toBlock;
        private Optional<MineralFountainHeat> heat = Optional.empty();
        private Optional<FluidStack> toFluid = Optional.empty();

        public Builder dimension(ResourceLocation dimension) {
            this.dimension = Optional.of(dimension);
            return this;
        }

        public Builder height(int min, int max) {
            this.height = new HeightRange(min, max);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
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

        /** 上方为排水口时灌入的流体。 */
        public Builder toFluid(FluidStack toFluid) {
            this.toFluid = Optional.of(toFluid);
            return this;
        }

        @Override
        public MineralFountainPassiveRecipe buildRecipe() {
            if (this.toBlock == null) throw new IllegalStateException("to_block is required");
            return new MineralFountainPassiveRecipe(
                this.dimension, this.height, this.priority, this.weight,
                this.fromBlock, this.toBlock, this.heat, this.toFluid
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
            return "mineral_fountain_passive";
        }

        @Override
        public Item getResult() {
            return this.toBlock == null ? Items.AIR : this.toBlock.state().getBlock().asItem();
        }
    }
}
