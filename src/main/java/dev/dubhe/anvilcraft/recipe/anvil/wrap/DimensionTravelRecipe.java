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

@Getter(AccessLevel.PRIVATE)
public class DimensionTravelRecipe extends InWorldRecipe {
    private final HasEnderPearl hasEnderPearl;
    private final ChangeDimension changeDimension;

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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static int getPriority(HasEnderPearl hasEnderPearl) {
        return (int) Math.round(hasEnderPearl.getHeight() * 2)
               + (int) Math.floor(hasEnderPearl.getSpeed() / 3);
    }

    @Override
    public @NotNull RecipeType<DimensionTravelRecipe> getType() {
        return ModRecipeTypes.DIMENSION_TRAVEL_TYPE.get();
    }

    @Override
    public @NotNull RecipeSerializer<DimensionTravelRecipe> getSerializer() {
        return ModRecipeTypes.DIMENSION_TRAVEL_SERIALIZER.get();
    }

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

        @Override
        public @NotNull MapCodec<DimensionTravelRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, DimensionTravelRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    public static class Builder extends AbstractRecipeBuilder<DimensionTravelRecipe> {
        private final HasEnderPearl.Builder hasEnderPearl = HasEnderPearl.builder();
        private final ChangeDimension.Builder changeDimension = ChangeDimension.builder();

        public Builder from(HasEnderPearl.Builder builder) {
            this.hasEnderPearl
                .dimension(builder.getDimensionKey())
                .speed(builder.getSpeed())
                .height(builder.getHeight());
            return this;
        }

        public Builder from(ResourceKey<Level> dimensionKey) {
            this.hasEnderPearl.dimension(dimensionKey);
            return this;
        }

        public Builder speed(double speed) {
            this.hasEnderPearl.speed(speed);
            return this;
        }

        public Builder height(double height) {
            this.hasEnderPearl.height(height);
            return this;
        }

        public Builder to(ChangeDimension.Builder builder) {
            this.changeDimension
                .dimension(builder.getDimensionKey())
                .restrictNewPos(builder.getCenterPos())
                .offset(builder.getOffset());
            return this;
        }

        public Builder to(ResourceKey<Level> dimensionKey) {
            this.changeDimension.dimension(dimensionKey);
            return this;
        }

        public Builder toPos(Vec3i newPos) {
            this.changeDimension.restrictNewPos(newPos);
            return this;
        }

        public Builder toPosOffset(Vec2 offset) {
            this.changeDimension.offset(offset);
            return this;
        }

        public Builder toPosOffset(float x, float z) {
            this.changeDimension.offset(x, z);
            return this;
        }

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

        @Override
        public @NotNull DimensionTravelRecipe buildRecipe() {
            return new DimensionTravelRecipe(this.hasEnderPearl.build(), this.changeDimension.build());
        }

        @Override
        public @NotNull Item getResult() {
            return Items.ENDER_PEARL;
        }

        @Override
        public @NotNull String getType() {
            return "dimension_travel";
        }

        @Override
        public void save(@NotNull RecipeOutput recipeOutput) {
            this.save(
                recipeOutput,
                AnvilCraft.of(this.changeDimension.getDimensionKey().location().getPath())
                    .withPrefix("dimension_travel/to_"));
        }
    }
}
