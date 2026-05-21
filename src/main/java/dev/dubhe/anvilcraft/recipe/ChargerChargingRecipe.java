package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 充放电器配方
 *
 * @param power units: kW, positive for discharge and negative for charge
 * @param time  units: tick
 */
public record ChargerChargingRecipe(Ingredient ingredient, ItemStackTemplate result, int power, int time) implements
    Recipe<SingleRecipeInput> {
    private static final MapCodec<ChargerChargingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Ingredient.CODEC
            .fieldOf("ingredient")
            .forGetter(ChargerChargingRecipe::ingredient),
        ItemStackTemplate.CODEC
            .fieldOf("result")
            .forGetter(ChargerChargingRecipe::result),
        Codec.INT
            .fieldOf("power")
            .forGetter(ChargerChargingRecipe::power),
        Codec.INT
            .fieldOf("time")
            .forGetter(ChargerChargingRecipe::time)
    ).apply(ins, ChargerChargingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargerChargingRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC,
        ChargerChargingRecipe::ingredient,
        ItemStackTemplate.STREAM_CODEC,
        ChargerChargingRecipe::result,
        ByteBufCodecs.VAR_INT,
        ChargerChargingRecipe::power,
        ByteBufCodecs.VAR_INT,
        ChargerChargingRecipe::time,
        ChargerChargingRecipe::new
    );
    public static final RecipeSerializer<ChargerChargingRecipe> SERIALIZER = new RecipeSerializer<>(
        ChargerChargingRecipe.CODEC,
        ChargerChargingRecipe.STREAM_CODEC
    );

    public static Builder builder(HolderGetter<Item> items) {
        return new Builder(items);
    }

    @Override
    public RecipeType<ChargerChargingRecipe> getType() {
        return ModRecipeTypes.CHARGER_CHARGING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<ChargerChargingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.getItem(0));
    }

    public Block getProcessingBlock() {
        return (this.power < 0 ? ModBlocks.CHARGER : ModBlocks.DISCHARGER).get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "charger_charging";
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<ChargerChargingRecipe> {
        private final HolderGetter<Item> items;
        private Ingredient ingredient = null;
        private ItemStackTemplate result = null;
        private int power = 0;
        private int time = 0;

        public Builder(HolderGetter<Item> items) {
            this.items = items;
        }

        public Builder requires(ItemLike item) {
            this.ingredient = Ingredient.of(item);
            return this;
        }

        public Builder requires(TagKey<Item> tag) {
            this.ingredient = Ingredient.of(this.items.getOrThrow(tag));
            return this;
        }

        public Builder result(ItemLike item) {
            this.result = new ItemStackTemplate(item.asItem());
            return this;
        }

        public Builder power(int power) {
            this.power = power;
            return this;
        }

        public Builder time(int time) {
            this.time = time;
            return this;
        }

        @Override
        public ChargerChargingRecipe buildRecipe() {
            return new ChargerChargingRecipe(this.ingredient, this.result, this.power, this.time);
        }

        @Override
        public void validate(Identifier id) {
            if (this.ingredient == null) throw new IllegalArgumentException("Recipe has no ingredient, RecipeId: " + id);
            if (this.result == null) throw new IllegalArgumentException("Recipe has no result, RecipeId: " + id);
            if (this.power == 0) {
                throw new IllegalArgumentException(
                    "The power release of charging/discharging recipe must be positive or negative, RecipeId: " + id
                );
            }
            if (this.time <= 0) {
                throw new IllegalArgumentException(
                    "Charging time must be a positive number, RecipeId: " + id
                );
            }
        }

        @Override
        public String getType() {
            return "charger_charging";
        }

        @Override
        public ItemStackTemplate getResult() {
            return this.result;
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(BuiltInRegistries.ITEM.getKey(this.result.item().value()).getPath())
                    .withPrefix(this.getType() + "/")
            );
        }
    }

}
