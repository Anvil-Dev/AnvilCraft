package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChargerChargingRecipe implements Recipe<SingleRecipeInput> {
    public final Ingredient ingredient;
    public final ItemStack result;
    public final int power; //units: kW, positive for discharge and negative for charge
    public final int time; //units: tick

    public ChargerChargingRecipe(Ingredient input, ItemStack result, int power, int time) {
        this.ingredient = input;
        this.result = result;
        this.power = power;
        this.time = time;
    }

    @Contract(" -> new")
    public static ChargerChargingRecipe.Builder builder() {
        return new ChargerChargingRecipe.Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CHARGER_CHARGING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CHARGER_CHARGING_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.getItem(0));
    }

    public Block getProcessingBlock() {
        return (this.power < 0 ? ModBlocks.CHARGER : ModBlocks.DISCHARGER).get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ChargerChargingRecipe> {

        private static final MapCodec<ChargerChargingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ChargerChargingRecipe::getIngredient),
            ItemStack.CODEC.fieldOf("result").forGetter(ChargerChargingRecipe::getResult),
            Codec.INT.fieldOf("power").forGetter(ChargerChargingRecipe::getPower),
            Codec.INT.fieldOf("time").forGetter(ChargerChargingRecipe::getTime)
        ).apply(ins, ChargerChargingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChargerChargingRecipe> STREAM_CODEC = StreamCodec.of(
            ChargerChargingRecipe.Serializer::encode, ChargerChargingRecipe.Serializer::decode
        );

        @Override
        public MapCodec<ChargerChargingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChargerChargingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void encode(RegistryFriendlyByteBuf buf, ChargerChargingRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.power);
            buf.writeVarInt(recipe.time);
        }

        private static ChargerChargingRecipe decode(RegistryFriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int power = buf.readVarInt();
            int time = buf.readVarInt();
            return new ChargerChargingRecipe(ingredient, result, power, time);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<ChargerChargingRecipe> {
        private Ingredient ingredient = null;
        private ItemStack result = null;
        private int power = 0;
        private int time = 0;

        public ChargerChargingRecipe.Builder requires(ItemLike pItem) {
            ingredient = Ingredient.of(pItem);
            return this;
        }

        public ChargerChargingRecipe.Builder requires(TagKey<Item> tag) {
            ingredient = Ingredient.of(tag);
            return this;
        }

        public ChargerChargingRecipe.Builder result(ItemLike pItem) {
            result = pItem.asItem().getDefaultInstance();
            return this;
        }

        public ChargerChargingRecipe.Builder power(int power) {
            this.power = power;
            return this;
        }

        public ChargerChargingRecipe.Builder time(int time) {
            this.time = time;
            return this;
        }

        @Override
        public ChargerChargingRecipe buildRecipe() {
            return new ChargerChargingRecipe(ingredient, result, power, time);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredient == null)
                throw new IllegalArgumentException("Recipe has no ingredient, RecipeId: " + pId);
            if (result == null)
                throw new IllegalArgumentException("Recipe has no result, RecipeId: " + pId);
            if (power == 0)
                throw new IllegalArgumentException("The power release of charging/discharging recipe must be positive or negative, RecipeId: " + pId);
            if (time <= 0)
                throw new IllegalArgumentException("Charging time must be a positive number, RecipeId: " + pId);
        }

        @Override
        public String getType() {
            return "charger_charging";
        }

        @Override
        public Item getResult() {
            return result.getItem();
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(BuiltInRegistries.ITEM.getKey(getResult()).getPath())
                    .withPrefix(getType() + "/")
            );
        }
    }

}
