package dev.dubhe.anvilcraft.data.recipe.anvil.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.RecipeSerializerBase;
import dev.dubhe.anvilcraft.data.recipe.TagIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.util.NonNullListUtils;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class ItemAnvilRecipe implements Recipe<AnvilCraftingContainer> {
    private final ResourceLocation id;
    private final NonNullList<TagIngredient> recipeItems;
    private final Location location;
    private final NonNullList<Component> components;
    private final List<ItemStack> results;
    private final Location resultLocation;
    private final boolean isAnvilDamage;
    public ItemAnvilRecipe(ResourceLocation id, NonNullList<TagIngredient> recipeItems, Location location, NonNullList<Component> components, List<ItemStack> results, Location resultLocation, boolean isAnvilDamage) {
        this.id = id;
        this.recipeItems = recipeItems;
        this.location = location;
        this.components = components;
        this.results = results;
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
        NonNullList<TagIngredient> recipeItems = NonNullListUtils.copy(this.recipeItems);
        recipeItems.removeIf(it -> {
            for (ItemStack itemStack : itemStacks) {
                if (!it.test(itemStack)) continue;
                itemStack.shrink(1);
                return true;
            }
            return false;
        });
        return recipeItems.isEmpty();
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean craft(@NotNull AnvilCraftingContainer container, Level level) {
        if (!this.matches(container, level)) return false;
        BlockPos pos = new BlockPos(container.pos());
        if (this.location == Location.IN) pos = pos.below();
        if (this.location == Location.UNDER) pos = pos.below(2);
        List<ItemEntity> itemEntities = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), new AABB(pos), Entity::isAlive);
        List<Pair<ItemEntity, ItemStack>> itemsTraces = new ArrayList<>();
        for (ItemEntity itemEntity : itemEntities) {
            itemsTraces.add(Pair.of(itemEntity, itemEntity.getItem().copy()));//itemStack只有前后不相等才会成功设置并同步到客户端，所以这里需要复制
        }
        NonNullList<TagIngredient> recipeItems = NonNullListUtils.copy(this.recipeItems);
        Set<Pair<ItemEntity, ItemStack>> modifieds = new HashSet<>();
        for (TagIngredient ingredient : recipeItems) {
            for (Pair<ItemEntity, ItemStack> itemTrace : itemsTraces) {
                ItemStack itemStack = itemTrace.getRight();
                if (!ingredient.test(itemStack)) continue;
                itemStack.shrink(1);
                modifieds.add(itemTrace);
                break;
            }
        }
        for (Pair<ItemEntity, ItemStack> itemTrace : modifieds) {
            itemTrace.getLeft().setItem(itemTrace.getRight());//因为ItemStack没有重写equals，而上面已经复制了，所以保证前后是不相等（不同）的itemStack对象
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER_CAULDRON)) {
            if (state.getValue(LayeredCauldronBlock.LEVEL) == 1) {
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) - 1), 3);
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
        return this.results.isEmpty() ? ItemStack.EMPTY : this.results.get(0);
    }
    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }
    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }
    public @NotNull NonNullList<TagIngredient> getTagIngredients() {
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
            NonNullList<TagIngredient> input = shapelessFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));
            Location location = !json.has("location") ? Location.UP : Location.byId(json.get("location").getAsString());
            NonNullList<Component> components = componentsFromJson(GsonHelper.getAsJsonArray(json, "components"));
            JsonArray array = GsonHelper.getAsJsonArray(json, "results");
            List<ItemStack> results = new ArrayList<>();
            for (JsonElement element : array) {
                results.add(itemStackFromJson(element));
            }
            Location resultLocation = !json.has("result_location") ? Location.UP : Location.byId(json.get("result_location").getAsString());
            boolean isAnvilDamage = json.has("is_anvil_damage") && json.get("is_anvil_damage").getAsBoolean();
            return new ItemAnvilRecipe(id, input, location, components, results, resultLocation, isAnvilDamage);
        }
        @Override
        public @NotNull ItemAnvilRecipe fromNetwork(ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
            NonNullList<TagIngredient> ingredients = NonNullList.withSize(buffer.readVarInt(), TagIngredient.EMPTY);
            ingredients.replaceAll(ignored -> TagIngredient.fromNetwork(buffer));
            NonNullList<Component> components = NonNullList.withSize(buffer.readVarInt(), Component.EMPTY);
            components.replaceAll(ignored -> Component.fromNetwork(buffer));
            Location location = buffer.readEnum(Location.class);
            List<ItemStack> results = new ArrayList<>();
            int size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(buffer.readItem());
            }
            Location resultLocation = buffer.readEnum(Location.class);
            boolean isAnvilDamage = buffer.readBoolean();
            return new ItemAnvilRecipe(id, ingredients, location, components, results, resultLocation, isAnvilDamage);
        }
        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ItemAnvilRecipe recipe) {
            buffer.writeVarInt(recipe.getTagIngredients().size());
            for (TagIngredient ingredient : recipe.getTagIngredients()) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.getComponents().size());
            for (Component component : recipe.getComponents()) {
                component.toNetwork(buffer);
            }
            buffer.writeEnum(recipe.location);
            buffer.writeVarInt(recipe.results.size());
            for (ItemStack result : recipe.results) {
                buffer.writeItem(result);
            }
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
