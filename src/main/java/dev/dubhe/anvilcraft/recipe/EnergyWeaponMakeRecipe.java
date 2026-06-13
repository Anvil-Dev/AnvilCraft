package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.ArrayList;
import java.util.List;

public record EnergyWeaponMakeRecipe(
    List<ICondition> conditions,
    List<ItemIngredientPredicate> ingredients,
    ItemStack result
) implements Recipe<EnergyWeaponMakeRecipe.Input> {
    private EnergyWeaponMakeRecipe(List<ItemIngredientPredicate> ingredients, ItemStack result) {
        this(new ArrayList<>(), ingredients, result);
    }

    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ENERGY_WEAPON_MAKE_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ENERGY_WEAPON_MAKE_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        ItemStack result = this.result.copy();
        ItemEnchantments enchantments = input.items.getFirst().get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) result.set(DataComponents.ENCHANTMENTS, enchantments);
        if (result.has(ModComponents.STORED_ENERGY)) result.set(ModComponents.STORED_ENERGY, 640000000); // 640 M FE
        return result;
    }

    @Override
    public boolean matches(Input input, Level level) {
        for (ItemStack stack : input.items) {
            boolean passed = false;
            for (ItemIngredientPredicate ingredient : this.ingredients) {
                if (!ingredient.test(stack)) continue;
                passed = true;
                break;
            }
            if (!passed) return false;
        }
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(List<ItemStack> items) implements RecipeInput, IItemsInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<EnergyWeaponMakeRecipe> {
        public static final MapCodec<EnergyWeaponMakeRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ICondition.LIST_CODEC
                .optionalFieldOf("neoforge:conditions", new ArrayList<>())
                .forGetter(EnergyWeaponMakeRecipe::conditions),
            ItemIngredientPredicate.CODEC
                .listOf(1, 6)
                .fieldOf("ingredients")
                .forGetter(EnergyWeaponMakeRecipe::ingredients),
            ItemStack.CODEC
                .fieldOf("result")
                .forGetter(EnergyWeaponMakeRecipe::result)
        ).apply(inst, EnergyWeaponMakeRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EnergyWeaponMakeRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            EnergyWeaponMakeRecipe::ingredients,
            ItemStack.STREAM_CODEC,
            EnergyWeaponMakeRecipe::result,
            EnergyWeaponMakeRecipe::new
        );

        @Override
        public MapCodec<EnergyWeaponMakeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EnergyWeaponMakeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<EnergyWeaponMakeRecipe> {
        private final List<ICondition> conditions = new ArrayList<>();
        private final List<ItemIngredientPredicate> ingredients = new ArrayList<>();
        private ItemStack result = ItemStack.EMPTY;
        
        public Builder withCondition(ICondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder requires(ItemIngredientPredicate.Builder ingredient) {
            this.ingredients.add(ingredient.build());
            return this;
        }

        public Builder requires(ItemLike item, int count) {
            return this.requires(ItemIngredientPredicate.of(item).withCount(count));
        }

        public Builder requires(ItemLike item) {
            return this.requires(item, 1);
        }

        public Builder requires(TagKey<Item> tag, int count) {
            return this.requires(ItemIngredientPredicate.of(tag).withCount(count));
        }

        public Builder requires(TagKey<Item> tag) {
            return this.requires(tag, 1);
        }

        @Override
        public EnergyWeaponMakeRecipe buildRecipe() {
            return new EnergyWeaponMakeRecipe(this.conditions, this.ingredients, this.result);
        }

        @Override
        public void validate(ResourceLocation id) {
            if (this.ingredients.isEmpty() || this.ingredients.size() > 6) {
                throw new IllegalArgumentException("Recipe ingredients size must in 1-6, RecipeId: " + id);
            }
            if (this.result.isEmpty()) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "energy_weapon_make";
        }

        @Override
        public Item getResult() {
            return this.result.getItem();
        }
    }
}
