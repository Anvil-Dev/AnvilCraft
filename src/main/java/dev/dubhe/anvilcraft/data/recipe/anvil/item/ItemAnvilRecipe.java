package dev.dubhe.anvilcraft.data.recipe.anvil.item;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.CompoundTagPredicate;
import dev.dubhe.anvilcraft.data.recipe.RecipeSerializerBase;
import dev.dubhe.anvilcraft.inventory.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.util.INonNullListInjector;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemAnvilRecipe implements Recipe<AnvilCraftingContainer> {
    @Getter
    private final ResourceLocation id;
    @Getter
    private final NonNullList<Ingredient> recipeItems;
    @Getter
    final NonNullList<CompoundTagPredicate> compoundTagPredicates;
    @Getter
    private final Location location;
    @Getter
    private final NonNullList<Component> components;
    private final ItemStack result;
    @Getter
    private final Location resultLocation;
    @Getter
    private final boolean isAnvilDamage;

    public ItemAnvilRecipe(ResourceLocation id, NonNullList<Ingredient> recipeItems, NonNullList<CompoundTagPredicate> compoundTagPredicates, Location location, NonNullList<Component> components, ItemStack result, Location resultLocation, boolean isAnvilDamage) {
        this.id = id;
        this.recipeItems = recipeItems;
        this.compoundTagPredicates = compoundTagPredicates;
        this.location = location;
        this.components = components;
        this.result = result;
        this.resultLocation = resultLocation;
        this.isAnvilDamage = isAnvilDamage;
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContainer container, Level level) {
        BlockPos pos = new BlockPos(container.pos());
        for (Component component : this.components) {
            pos = pos.below();
            BlockState state = level.getBlockState(pos);
            if (!component.test(state)) return false;
        }
        pos = new BlockPos(container.pos());
        if (this.location == Location.IN) pos = pos.below();
        if (this.location == Location.UNDER) pos = pos.below(2);
        AABB aabb = new AABB(pos);
        List<ItemEntity> itemEntities = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive);
        List<ItemStack> itemStacks = itemEntities.stream().map(ItemEntity::getItem).map(ItemStack::copy).toList();
        NonNullList<Ingredient> recipeItems = INonNullListInjector.copy(this.recipeItems);
        NonNullList<CompoundTagPredicate> compoundTagPredicates = INonNullListInjector.copy(this.compoundTagPredicates);
        Iterator<Ingredient> iterator1 = recipeItems.iterator();
        Iterator<CompoundTagPredicate> iterator2 = compoundTagPredicates.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            Ingredient ingredient = iterator1.next();
            CompoundTagPredicate compoundTagPredicate = iterator2.next();
            for (ItemStack itemStack : itemStacks) {
                if (!ingredient.test(itemStack) || !compoundTagPredicate.test(itemStack.getOrCreateTag())) continue;
                iterator1.remove();
                iterator2.remove();
                itemStack.setCount(itemStack.getCount() - 1);
            }
        }
        return recipeItems.isEmpty() && compoundTagPredicates.isEmpty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean craft(@NotNull AnvilCraftingContainer container, Level level) {
        if (this.matches(container, level)) return false;
        BlockPos pos = new BlockPos(container.pos());
        if (this.location == Location.IN) pos = pos.below();
        if (this.location == Location.UNDER) pos = pos.below(2);
        List<ItemEntity> itemEntities = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), new AABB(pos), Entity::isAlive);
        Map<ItemStack, ItemEntity> itemStackMap = new HashMap<>();
        for (ItemEntity itemEntity : itemEntities) {
            itemStackMap.put(itemEntity.getItem(), itemEntity);
        }
        NonNullList<Ingredient> recipeItems = INonNullListInjector.copy(this.recipeItems);
        NonNullList<CompoundTagPredicate> compoundTagPredicates = INonNullListInjector.copy(this.compoundTagPredicates);
        Iterator<Ingredient> iterator1 = recipeItems.iterator();
        Iterator<CompoundTagPredicate> iterator2 = compoundTagPredicates.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            Ingredient ingredient = iterator1.next();
            CompoundTagPredicate compoundTagPredicate = iterator2.next();
            for (ItemStack itemStack : itemStackMap.keySet()) {
                if (!ingredient.test(itemStack) || !compoundTagPredicate.test(itemStack.getOrCreateTag())) continue;
                iterator1.remove();
                iterator2.remove();
                itemStack.setCount(itemStack.getCount() - 1);
                itemStackMap.get(itemStack).setItem(itemStack);
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(AnvilCraftingContainer container, RegistryAccess registryAccess) {
        return this.getResultItem(registryAccess).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.result;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return this.recipeItems;
    }

    public static class Type implements RecipeType<ItemAnvilRecipe> {
        public static final Type INSTANCE = new Type();

        private Type() {
        }
    }

    public static class Serializer implements RecipeSerializerBase<ItemAnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public @NotNull ItemAnvilRecipe fromJson(ResourceLocation id, JsonObject json) {
            Pair<NonNullList<Ingredient>, NonNullList<CompoundTagPredicate>> input = shapelessFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));
            Location location = !json.has("location") ? Location.UP : Location.byId(json.get("location").getAsString());
            NonNullList<Component> components = componentsFromJson(GsonHelper.getAsJsonArray(json, "components"));
            ItemStack result = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            Location resultLocation = !json.has("result_location") ? Location.UP : Location.byId(json.get("result_location").getAsString());
            boolean isAnvilDamage = json.has("is_anvil_damage") && json.get("is_anvil_damage").getAsBoolean();
            return new ItemAnvilRecipe(id, input.getFirst(), input.getSecond(), location, components, result, resultLocation, isAnvilDamage);
        }

        @Override
        public @NotNull ItemAnvilRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            NonNullList<CompoundTagPredicate> compoundTagPredicates = NonNullList.withSize(buffer.readVarInt(), CompoundTagPredicate.EMPTY);
            compoundTagPredicates.replaceAll(ignored -> CompoundTagPredicate.fromNetwork(buffer));
            NonNullList<Component> components = NonNullList.withSize(buffer.readVarInt(), Component.EMPTY);
            components.replaceAll(ignored -> Component.fromNetwork(buffer));
            Location location = buffer.readEnum(Location.class);
            ItemStack result = buffer.readItem();
            Location resultLocation = buffer.readEnum(Location.class);
            boolean isAnvilDamage = buffer.readBoolean();
            return new ItemAnvilRecipe(id, ingredients, compoundTagPredicates, location, components, result, resultLocation, isAnvilDamage);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ItemAnvilRecipe recipe) {
            buffer.writeVarInt(recipe.getIngredients().size());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.getCompoundTagPredicates().size());
            for (CompoundTagPredicate predicate : recipe.getCompoundTagPredicates()) {
                predicate.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.getComponents().size());
            for (Component component : recipe.getComponents()) {
                component.toNetwork(buffer);
            }
            buffer.writeEnum(recipe.location);
            buffer.writeItem(recipe.result);
            buffer.writeEnum(recipe.resultLocation);
            buffer.writeBoolean(recipe.isAnvilDamage);
        }
    }

    public enum Location {
        UP, UNDER, IN;

        public @NotNull String getId() {
            return this.name().toLowerCase();
        }

        public static Location byId(@NotNull String id) {
            return Location.valueOf(id.toUpperCase());
        }
    }
}
