package dev.dubhe.anvilcraft.data.recipe.anvil.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.RecipeSerializerBase;
import dev.dubhe.anvilcraft.inventory.AnvilCraftingContainer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Getter
@SuppressWarnings("ClassCanBeRecord")
public class BlockAnvilRecipe implements Recipe<AnvilCraftingContainer> {
    private final ResourceLocation id;
    private final NonNullList<Component> components;
    private final NonNullList<BlockState> results;
    private final NonNullList<ItemStack> dropItems;
    private final boolean isAnvilDamage;

    public BlockAnvilRecipe(ResourceLocation id, NonNullList<Component> components, NonNullList<BlockState> results, NonNullList<ItemStack> dropItems, boolean isAnvilDamage) {
        this.id = id;
        this.components = components;
        this.results = results;
        this.dropItems = dropItems;
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
        return true;
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "UnusedReturnValue"})
    public boolean craft(@NotNull AnvilCraftingContainer container, Level level) {
        if (!this.matches(container, level)) return false;
        BlockPos pos = new BlockPos(container.pos());
        for (BlockState result : this.results) {
            pos = pos.below();
            level.setBlockAndUpdate(pos, result);
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
        return this.results.get(this.results.size() - 1).getBlock().asItem().getDefaultInstance();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<BlockAnvilRecipe> {
        public static final Type INSTANCE = new Type();

        private Type() {
        }
    }

    public static class Serializer implements RecipeSerializerBase<BlockAnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public @NotNull BlockAnvilRecipe fromJson(ResourceLocation id, JsonObject json) {
            NonNullList<Component> components = componentsFromJson(GsonHelper.getAsJsonArray(json, "components"));
            NonNullList<BlockState> results = getBlockResults(GsonHelper.getAsJsonArray(json, "results"));
            NonNullList<ItemStack> dropItems = NonNullList.create();
            if (json.has("drops")) {
                JsonArray drops = GsonHelper.getAsJsonArray(json, "drops");
                for (JsonElement drop : drops) {
                    if (!drop.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
                    ItemStack dropStack = itemStackFromJson(GsonHelper.getAsJsonObject(drop.getAsJsonObject(), "drops"));
                    dropItems.add(dropStack);
                }
            }
            boolean isAnvilDamage = json.has("is_anvil_damage") && json.get("is_anvil_damage").getAsBoolean();
            return new BlockAnvilRecipe(id, components, results, dropItems, isAnvilDamage);
        }

        @Override
        public @NotNull BlockAnvilRecipe fromNetwork(ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
            NonNullList<Component> components = NonNullList.withSize(buffer.readVarInt(), Component.EMPTY);
            components.replaceAll(ignored -> Component.fromNetwork(buffer));
            NonNullList<BlockState> results = NonNullList.withSize(buffer.readVarInt(), Blocks.AIR.defaultBlockState());
            results.replaceAll(ignored -> stateFromNetwork(buffer));
            NonNullList<ItemStack> dropItems = NonNullList.withSize(buffer.readVarInt(), ItemStack.EMPTY);
            dropItems.replaceAll(ignored -> buffer.readItem());
            boolean isAnvilDamage = buffer.readBoolean();
            return new BlockAnvilRecipe(id, components, results, dropItems, isAnvilDamage);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull BlockAnvilRecipe recipe) {
            buffer.writeVarInt(recipe.components.size());
            recipe.components.forEach(component -> component.toNetwork(buffer));
            buffer.writeVarInt(recipe.results.size());
            recipe.results.forEach(result -> this.stateToNetwork(buffer, result));
            buffer.writeVarInt(recipe.dropItems.size());
            recipe.dropItems.forEach(buffer::writeItem);
            buffer.writeBoolean(recipe.isAnvilDamage);
        }
    }
}
