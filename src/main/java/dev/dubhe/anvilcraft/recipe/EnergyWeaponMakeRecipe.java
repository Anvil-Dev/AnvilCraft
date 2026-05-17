package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.IItemsInput;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
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
    ItemStackTemplate result
) implements Recipe<EnergyWeaponMakeRecipe.Input> {
    public static final RecipeSerializer<EnergyWeaponMakeRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            ICondition.LIST_CODEC
                .optionalFieldOf("neoforge:conditions", new ArrayList<>())
                .forGetter(EnergyWeaponMakeRecipe::conditions),
            ItemIngredientPredicate.CODEC
                .listOf(1, 6)
                .fieldOf("ingredients")
                .forGetter(EnergyWeaponMakeRecipe::ingredients),
            ItemStackTemplate.CODEC
                .fieldOf("result")
                .forGetter(EnergyWeaponMakeRecipe::result)
        ).apply(inst, EnergyWeaponMakeRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            EnergyWeaponMakeRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC,
            EnergyWeaponMakeRecipe::result,
            EnergyWeaponMakeRecipe::new
        )
    );

    private EnergyWeaponMakeRecipe(List<ItemIngredientPredicate> ingredients, ItemStackTemplate result) {
        this(new ArrayList<>(), ingredients, result);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<EnergyWeaponMakeRecipe> getType() {
        return ModRecipeTypes.ENERGY_WEAPON_MAKE.get();
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
    public RecipeSerializer<EnergyWeaponMakeRecipe> getSerializer() {
        return ModRecipeSerializers.ENERGY_WEAPON_MAKE.get();
    }

    @Override
    public ItemStack assemble(Input input) {
        ItemStack result = this.result.create();
        ItemEnchantments enchantments = input.items.getFirst().get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) result.set(DataComponents.ENCHANTMENTS, enchantments);
        if (result.has(ModComponents.STORED_ENERGY)) result.set(ModComponents.STORED_ENERGY, new StoredEnergy(320000)); // 320MJ
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

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "energy_weapon_make";
    }

    public record Input(List<ItemStack> items) implements RecipeInput, IItemsInput {
        @Override
        public ItemStack getItem(int index) {
            return this.items.get(index);
        }

        @Override
        public int size() {
            return this.items.size();
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<EnergyWeaponMakeRecipe> {
        private final List<ICondition> conditions = new ArrayList<>();
        private final List<ItemIngredientPredicate> ingredients = new ArrayList<>();
        private ItemStackTemplate result;

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
        public void validate(Identifier id) {
            if (this.ingredients.isEmpty() || this.ingredients.size() > 6) {
                throw new IllegalArgumentException("Recipe ingredients size must in 1-6, RecipeId: " + id);
            }
            if (this.result == null) {
                throw new IllegalArgumentException("Recipe result must not be empty, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "energy_weapon_make";
        }

        @Override
        public ItemStackTemplate getResult() {
            return this.result;
        }
    }
}
