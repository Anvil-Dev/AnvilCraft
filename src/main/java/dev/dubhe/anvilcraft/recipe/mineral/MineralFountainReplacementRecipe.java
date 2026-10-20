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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MineralFountainReplacementRecipe extends AbstractMineralFountainRecipe {

    private final List<Identifier> replace;

    private final double chance;

    public MineralFountainReplacementRecipe(
        Optional<Identifier> dimension,
        HeightRange height,
        List<Identifier> replace,
        int priority,
        double chance,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat,
        Optional<FluidStackTemplate> toFluid
    ) {

        super(dimension, height, priority, DEFAULT_WEIGHT, fromBlock, toBlock, heat, toFluid);
        this.replace = List.copyOf(replace);

        this.chance = Math.clamp(chance, 0.0, 1.0);
    }

    public List<Identifier> getReplace() {
        return this.replace;
    }

    public double getReplaceChance() {
        return this.chance;
    }

    public boolean appliesTo(@Nullable Identifier activeRecipeId) {
        return this.replace.isEmpty() || (activeRecipeId != null && this.replace.contains(activeRecipeId));
    }

    @Override
    public RecipeType<MineralFountainReplacementRecipe> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_REPLACEMENT.get();
    }

    @Override
    public RecipeSerializer<MineralFountainReplacementRecipe> getSerializer() {
        return MineralFountainReplacementRecipe.SERIALIZER;
    }

    @Override
    public boolean matches(MineralFountainRecipeInput input, Level level) {
        return this.matchesCommon(input, level);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final MapCodec<MineralFountainReplacementRecipe> CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("dimension")
                .forGetter(AbstractMineralFountainRecipe::getDimension),
            HeightRange.CODEC.optionalFieldOf("height", HeightRange.DEFAULT)
                .forGetter(AbstractMineralFountainRecipe::getHeight),
            Identifier.CODEC.listOf().optionalFieldOf("replace", List.of())
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
                .forGetter(AbstractMineralFountainRecipe::getHeat),
            FluidStackTemplate.CODEC.optionalFieldOf("to_fluid")
                .forGetter(AbstractMineralFountainRecipe::getToFluid)
        ).apply(instance, MineralFountainReplacementRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainReplacementRecipe> STREAM_CODEC =
        StreamCodec.of(
            (buf, recipe) -> {
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, recipe.getDimension());
                HeightRange.STREAM_CODEC.encode(buf, recipe.getHeight());
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getReplace());
                ByteBufCodecs.VAR_INT.encode(buf, recipe.getPriority());
                ByteBufCodecs.DOUBLE.encode(buf, recipe.getReplaceChance());
                BlockStatePredicate.STREAM_CODEC.encode(buf, recipe.getFromBlock());
                ChanceBlockState.STREAM_CODEC.encode(buf, recipe.getToBlock());
                ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).encode(buf, recipe.getHeat());
                ByteBufCodecs.optional(FluidStackTemplate.STREAM_CODEC).encode(buf, recipe.getToFluid());
            },
            buf -> new MineralFountainReplacementRecipe(
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf),
                HeightRange.STREAM_CODEC.decode(buf),
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                BlockStatePredicate.STREAM_CODEC.decode(buf),
                ChanceBlockState.STREAM_CODEC.decode(buf),
                ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).decode(buf),
                ByteBufCodecs.optional(FluidStackTemplate.STREAM_CODEC).decode(buf)
            )
        );

    public static final RecipeSerializer<MineralFountainReplacementRecipe> SERIALIZER = new RecipeSerializer<>(
        MineralFountainReplacementRecipe.CODEC, MineralFountainReplacementRecipe.STREAM_CODEC
    );

    public static class Builder extends AbstractRecipeBuilder<MineralFountainReplacementRecipe> {
        private Optional<Identifier> dimension = Optional.empty();
        private HeightRange height = HeightRange.DEFAULT;
        private List<Identifier> replace = List.of();
        private int priority = 0;
        private double chance = 0.0;
        private BlockStatePredicate fromBlock = AbstractMineralFountainRecipe.DEFAULT_FROM;
        private @Nullable ChanceBlockState toBlock;
        private Optional<MineralFountainHeat> heat = Optional.empty();
        private Optional<FluidStackTemplate> toFluid = Optional.empty();

        public Builder dimension(Identifier dimension) {
            this.dimension = Optional.of(dimension);
            return this;
        }

        public Builder height(int min, int max) {
            this.height = new HeightRange(min, max);
            return this;
        }

        public Builder replace(List<Identifier> replace) {
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

        public Builder toFluid(FluidStackTemplate toFluid) {
            this.toFluid = Optional.of(toFluid);
            return this;
        }

        @Override
        public MineralFountainReplacementRecipe buildRecipe() {
            if (this.toBlock == null) throw new IllegalStateException("to_block is required");
            return new MineralFountainReplacementRecipe(
                this.dimension, this.height, this.replace, this.priority, this.chance,
                this.fromBlock, this.toBlock, this.heat, this.toFluid
            );
        }

        @Override
        public void validate(Identifier id) {
            if (this.toBlock == null) {
                throw new IllegalArgumentException("to_block must not be null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain_replacement";
        }

        @Override
        public ItemStackTemplate getResult() {
            return new ItemStackTemplate(this.toBlock == null ? Items.AIR : this.toBlock.state().getBlock().asItem());
        }
    }
}
