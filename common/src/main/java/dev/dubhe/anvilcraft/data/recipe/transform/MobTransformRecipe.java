package dev.dubhe.anvilcraft.data.recipe.transform;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import lombok.Getter;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MobTransformRecipe implements Recipe<MobTransformContainer> {

    public static final Codec<MobTransformRecipe> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(o -> o.id),
            ItemStack.CODEC.fieldOf("iconSrc").forGetter(o -> o.iconSrc),
            ResourceLocation.CODEC
                    .fieldOf("inputEntityType")
                    .forGetter(o -> BuiltInRegistries.ENTITY_TYPE.getKey(o.inputEntityType)),
            TransformResult.CODEC.listOf().fieldOf("results").forGetter(o -> o.results)
    ).apply(ins, MobTransformRecipe::new));

    private final ResourceLocation id;
    @Getter
    private EntityType<?> inputEntityType;
    @Getter
    private List<TransformResult> results;
    private final ItemStack iconSrc;

    public MobTransformRecipe(
            ResourceLocation id,
            ItemStack iconSrc,
            ResourceLocation inputEntityType,
            List<TransformResult> results
    ) {
        this.id = id;
        this.iconSrc = iconSrc;
        this.results = results;
        this.inputEntityType = BuiltInRegistries.ENTITY_TYPE.get(inputEntityType);
    }

    public MobTransformRecipe(ResourceLocation id, ItemStack iconSrc) {
        this.id = id;
        this.iconSrc = iconSrc;
    }

    @Override
    public boolean matches(@NotNull MobTransformContainer container, @NotNull Level level) {
        return container.getEntity().getType() == inputEntityType;
    }

    @Override
    public @NotNull ItemStack assemble(
            @NotNull MobTransformContainer container,
            @NotNull RegistryAccess registryAccess
    ) {
        return ItemStack.EMPTY;
    }

    public EntityType<?> result(RandomSource randomSource) {
        if (results.size() == 1) return results.get(0).resultEntityType();
        List<TransformResult> sorted = new ArrayList<>(results.stream()
                .sorted(Comparator.comparingDouble(TransformResult::probability))
                .toList());
        List<Double> pList = new ArrayList<>(List.of(0d));
        for (TransformResult transformResult : sorted) {
            pList.add(pList.get(pList.size() - 1) + transformResult.probability());
        }
        double p = randomSource.nextDouble();
        for (int i = 1; i < pList.size(); i++) {
            double end = pList.get(i);
            double start = pList.get(i - 1);
            if (p >= start && p < end) {
                return sorted.get(i - 1).resultEntityType();
            }
        }
        return sorted.get(sorted.size() - 1).resultEntityType();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return iconSrc;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static Builder builder(String id) {
        return new Builder(new ResourceLocation("anvilcraft", id));
    }

    public enum Serializer implements RecipeSerializer<MobTransformRecipe> {
        INSTANCE;

        @Override
        public @NotNull MobTransformRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject serializedRecipe) {
            return MobTransformRecipe.CODEC
                    .parse(JsonOps.INSTANCE, serializedRecipe.getAsJsonObject("recipe"))
                    .getOrThrow(false, ignored -> {
                    });
        }

        @Override
        public @NotNull MobTransformRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            MobTransformRecipe recipe = new MobTransformRecipe(recipeId, buffer.readItem());
            ResourceLocation inputMobTypeIdentifier = buffer.readResourceLocation();
            recipe.results = buffer.readList(TransformResult::fromByteBuf);
            recipe.inputEntityType = BuiltInRegistries.ENTITY_TYPE.get(inputMobTypeIdentifier);
            return recipe;
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, MobTransformRecipe recipe) {
            buffer.writeItem(recipe.iconSrc);
            ResourceLocation inputMobTypeIdentifier =
                    BuiltInRegistries.ENTITY_TYPE.getKey(recipe.inputEntityType);
            buffer.writeResourceLocation(inputMobTypeIdentifier);
            buffer.writeCollection(recipe.results, TransformResult::intoByteBuf);
        }
    }


    public enum Type implements RecipeType<MobTransformRecipe> {
        INSTANCE
    }

    public static class Builder {
        private final ResourceLocation id;
        private EntityType<?> inputEntityType;
        private final List<TransformResult> results = new ArrayList<>();
        private ItemStack iconSrc = ItemStack.EMPTY;
        private Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
        private RecipeCategory category = RecipeCategory.MISC;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder category(RecipeCategory category) {
            this.category = category;
            return this;
        }

        public Builder advancement(Advancement.Builder advancement) {
            this.advancement = advancement;
            return this;
        }

        public Builder input(EntityType<?> inputEntityType) {
            this.inputEntityType = inputEntityType;
            return this;
        }

        public Builder result(EntityType<?> resultEntityType, double d) {
            this.results.add(new TransformResult(resultEntityType, d));
            return this;
        }

        public Builder icon(ItemStack iconSrc) {
            this.iconSrc = iconSrc;
            return this;
        }

        public MobTransformRecipe build() {
            MobTransformRecipe r = new MobTransformRecipe(id, iconSrc);
            r.inputEntityType = inputEntityType;
            r.results = results;
            return r;
        }

        public FinishedMobTransformRecipe finish() {
            return new FinishedMobTransformRecipe(
                    build(),
                    advancement,
                    id.withPrefix("recipes/" + this.category.getFolderName() + "/")
            );
        }

        public void accept(RegistrateRecipeProvider provider) {
            provider.accept(finish());
        }
    }
}
