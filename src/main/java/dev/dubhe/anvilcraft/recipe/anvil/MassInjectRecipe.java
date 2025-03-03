package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MassInjectRecipe extends SingleItemRecipe {

    private final int mass;

    public MassInjectRecipe(Ingredient ingredient, int mass) {
        super(ModRecipeTypes.MASS_INJECT_TYPE.get(),
            ModRecipeTypes.MASS_INJECT_SERIALIZER.get(),
            "mass_inject",
            ingredient,
            ItemStack.EMPTY);
        this.mass = mass;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    public static Component displayMassValue(long mass) {
        if (mass <= 0) return Component.literal("0");
        if (mass % 100 == 0) return Component.literal(String.valueOf(mass / 100));
        if (mass % 10 == 0) return Component.literal(String.valueOf(mass / 100) + '.' + (mass % 100) / 10);
        long rem = mass % 100;
        return Component.literal((mass / 100) + (rem < 10 ? ".0" : ".") + (mass % 100));
    }

    public Component displayMassValue() {
        return displayMassValue(this.mass);
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MASS_INJECT_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MASS_INJECT_SERIALIZER.get();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    public static class Serializer implements RecipeSerializer<MassInjectRecipe> {
        public static final MapCodec<MassInjectRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                inst -> inst.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient")
                            .forGetter(m -> m.ingredient),
                        Codec.INT.fieldOf("mass").forGetter(MassInjectRecipe::getMass)
                    )
                    .apply(inst, MassInjectRecipe::new)
            );
        public static final StreamCodec<RegistryFriendlyByteBuf, MassInjectRecipe> STREAM_CODEC =
            StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                m -> m.ingredient,
                ByteBufCodecs.VAR_INT,
                MassInjectRecipe::getMass,
                MassInjectRecipe::new
            );

        @Override
        public MapCodec<MassInjectRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MassInjectRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @MethodsReturnNonnullByDefault
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MassInjectRecipe> {
        private Ingredient ingredient = null;
        private int mass = 1;
        private String defaultId = null;
        private TagKey<Item> tagCondition = null;

        public Builder requires(Ingredient ingredient) {
            this.ingredient = ingredient;
            return this;
        }

        public Builder requires(ItemLike item) {
            this.defaultId = BuiltInRegistries.ITEM.getKey(item.asItem()).toString().replace(':', '_');
            return requires(Ingredient.of(item));
        }

        public Builder requires(TagKey<Item> tag) {
            this.defaultId = tag.location().toString().replace(':', '_');
            this.tagCondition = tag;
            return requires(Ingredient.of(tag));
        }

        public Builder mass(int mass) {
            this.mass = mass;
            return this;
        }

        @Override
        public MassInjectRecipe buildRecipe() {
            return new MassInjectRecipe(this.ingredient, this.mass);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (this.ingredient == null) {
                throw new IllegalArgumentException("Recipe ingredient must not be null, RecipeId: " + pId);
            }
            if (this.mass <= 0) {
                throw new IllegalArgumentException("Mass value must be non-negative, RecipeId: " + pId
                    + "value: " + this.mass);
            }
        }

        @Override
        public String getType() {
            return "mass_inject";
        }

        @Override
        public Item getResult() {
            return Items.AIR;
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            if (this.defaultId == null) this.defaultId = Integer.toHexString(this.hashCode());
            this.save(recipeOutput, AnvilCraft.of("mass_inject/" + this.defaultId));
        }

        @Override
        public void save(RecipeOutput recipeOutput, ResourceLocation id) {
            if (this.tagCondition != null) {
                recipeOutput = recipeOutput.withConditions(new NotCondition(new TagEmptyCondition(this.tagCondition)));
            }
            super.save(recipeOutput, id);
        }
    }
}
