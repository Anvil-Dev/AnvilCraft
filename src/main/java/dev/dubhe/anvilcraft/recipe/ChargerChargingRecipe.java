package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChargerChargingRecipe implements Recipe<ChargerChargingRecipe.Input> {
    public final Item ingredient;
    public final Item result;
    public final int power; //units: kW, positive for discharge and negative for charge
    public final int time; //units: tick

    public ChargerChargingRecipe(Item input, Item result, int power, int time) {
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
        return result.getDefaultInstance();
    }

    @Override
    public ItemStack assemble(ChargerChargingRecipe.Input input, HolderLookup.Provider registries) {
        return result.getDefaultInstance().copy();
    }

    @Override
    public boolean matches(ChargerChargingRecipe.Input input, Level level) {
        return input.item.is(ingredient);
    }

    public record Input(ItemStack item) implements RecipeInput {

        @Override
        public ItemStack getItem(int i) {
            return item;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<ChargerChargingRecipe> {

        private static final MapCodec<ChargerChargingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            CodecUtil.ITEM_CODEC.fieldOf("ingredient").forGetter(ChargerChargingRecipe::getIngredient),
            CodecUtil.ITEM_CODEC.fieldOf("result").forGetter(ChargerChargingRecipe::getResult),
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
            CodecUtil.ITEM_STREAM_CODEC.encode(buf, recipe.ingredient);
            CodecUtil.ITEM_STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.power);
            buf.writeVarInt(recipe.time);
        }

        private static ChargerChargingRecipe decode(RegistryFriendlyByteBuf buf) {
            Item ingredient = CodecUtil.ITEM_STREAM_CODEC.decode(buf);
            Item result = CodecUtil.ITEM_STREAM_CODEC.decode(buf);
            int power = buf.readVarInt();
            int time = buf.readVarInt();
            return new ChargerChargingRecipe(ingredient, result, power, time);
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<ChargerChargingRecipe> {
        private Item ingredient = null;
        private Item result = null;
        private int power = 0;
        private int time = 0;

        public ChargerChargingRecipe.Builder requires(ItemLike pItem) {
            ingredient = pItem.asItem();
            return this;
        }

        public ChargerChargingRecipe.Builder result(ItemLike pItem) {
            result = pItem.asItem();
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
            return result;
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(BuiltInRegistries.ITEM.getKey(getResult()).getPath())
                    .withPrefix(getType() + "/")
                    .withSuffix("_from_" + BuiltInRegistries.ITEM.getKey(ingredient).getPath())
            );
        }
    }

}
