package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class MineralFountainActiveRecipe extends AbstractMineralFountainRecipe {

    private final List<AroundRequirement> around;

    public MineralFountainActiveRecipe(
        Optional<Identifier> dimension,
        HeightRange height,
        int priority,
        int weight,
        List<AroundRequirement> around,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat,
        Optional<FluidStackTemplate> toFluid
    ) {
        super(dimension, height, priority, weight, fromBlock, toBlock, heat, toFluid);
        this.around = List.copyOf(around);
    }

    @Override
    public RecipeType<MineralFountainActiveRecipe> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get();
    }

    @Override
    public RecipeSerializer<MineralFountainActiveRecipe> getSerializer() {
        return MineralFountainActiveRecipe.SERIALIZER;
    }

    @Override
    public boolean matches(MineralFountainRecipeInput input, Level level) {
        if (!this.matchesCommon(input, level)) return false;
        return this.matchesAround(input);
    }

    public boolean matchesContextAndAround(MineralFountainRecipeInput input, Level level) {
        return this.matchesContext(input, level) && this.matchesAround(input);
    }

    private boolean matchesAround(MineralFountainRecipeInput input) {
        for (AroundRequirement requirement : this.around) {
            if (!requirement.test(input.around())) return false;
        }
        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final MapCodec<MineralFountainActiveRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Identifier.CODEC.optionalFieldOf("dimension")
                .forGetter(AbstractMineralFountainRecipe::getDimension),
            HeightRange.CODEC.optionalFieldOf("height", HeightRange.DEFAULT)
                .forGetter(AbstractMineralFountainRecipe::getHeight),
            Codec.INT.optionalFieldOf("priority", 0)
                .forGetter(AbstractMineralFountainRecipe::getPriority),
            Codec.INT.optionalFieldOf("weight", DEFAULT_WEIGHT)
                .forGetter(AbstractMineralFountainRecipe::getWeight),
            AroundRequirement.CODEC.listOf().optionalFieldOf("around", List.of())
                .forGetter(MineralFountainActiveRecipe::getAround),
            BlockStatePredicate.CODEC.optionalFieldOf("from_block", AbstractMineralFountainRecipe.DEFAULT_FROM)
                .forGetter(AbstractMineralFountainRecipe::getFromBlock),
            ChanceBlockState.CODEC.fieldOf("to_block")
                .forGetter(AbstractMineralFountainRecipe::getToBlock),
            MineralFountainHeat.CODEC.optionalFieldOf("heat")
                .forGetter(AbstractMineralFountainRecipe::getHeat),
            FluidStackTemplate.CODEC.optionalFieldOf("to_fluid")
                .forGetter(AbstractMineralFountainRecipe::getToFluid)
        ).apply(instance, MineralFountainActiveRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainActiveRecipe> STREAM_CODEC =
        StreamCodec.of(
            (buf, recipe) -> {
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, recipe.getDimension());
                HeightRange.STREAM_CODEC.encode(buf, recipe.getHeight());
                ByteBufCodecs.VAR_INT.encode(buf, recipe.getPriority());
                ByteBufCodecs.VAR_INT.encode(buf, recipe.getWeight());
                AroundRequirement.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getAround());
                BlockStatePredicate.STREAM_CODEC.encode(buf, recipe.getFromBlock());
                ChanceBlockState.STREAM_CODEC.encode(buf, recipe.getToBlock());
                ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).encode(buf, recipe.getHeat());
                ByteBufCodecs.optional(FluidStackTemplate.STREAM_CODEC).encode(buf, recipe.getToFluid());
            },
            buf -> new MineralFountainActiveRecipe(
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf),
                HeightRange.STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                AroundRequirement.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                BlockStatePredicate.STREAM_CODEC.decode(buf),
                ChanceBlockState.STREAM_CODEC.decode(buf),
                ByteBufCodecs.optional(MineralFountainHeat.STREAM_CODEC).decode(buf),
                ByteBufCodecs.optional(FluidStackTemplate.STREAM_CODEC).decode(buf)
            )
        );

    public static final RecipeSerializer<MineralFountainActiveRecipe> SERIALIZER = new RecipeSerializer<>(
        MineralFountainActiveRecipe.CODEC, MineralFountainActiveRecipe.STREAM_CODEC
    );

    public static class Builder extends AbstractRecipeBuilder<MineralFountainActiveRecipe> {
        private Optional<Identifier> dimension = Optional.empty();
        private HeightRange height = HeightRange.DEFAULT;
        private int priority = 0;
        private int weight = DEFAULT_WEIGHT;
        private List<AroundRequirement> around = List.of();
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

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder around(List<AroundRequirement> around) {
            this.around = List.copyOf(around);
            return this;
        }

        public Builder require(AroundRequirement requirement) {
            List<AroundRequirement> list = new ArrayList<>(this.around);
            list.add(requirement);
            this.around = List.copyOf(list);
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
        public MineralFountainActiveRecipe buildRecipe() {
            if (this.toBlock == null) throw new IllegalStateException("to_block is required");
            return new MineralFountainActiveRecipe(
                this.dimension, this.height, this.priority, this.weight, this.around,
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
            return "mineral_fountain_active";
        }

        @Override
        public ItemStackTemplate getResult() {
            return new ItemStackTemplate(this.toBlock == null ? Items.AIR : this.toBlock.state().getBlock().asItem());
        }
    }
}
