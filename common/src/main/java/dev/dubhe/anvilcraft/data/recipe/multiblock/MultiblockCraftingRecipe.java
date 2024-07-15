package dev.dubhe.anvilcraft.data.recipe.multiblock;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.data.recipe.transform.MobTransformContainer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT;

public class MultiblockCraftingRecipe implements Recipe<MobTransformContainer> {

    public static final PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Character> read(final DynamicOps<T> ops, final T input) {
            return ops.getStringValue(input).map(it -> it.charAt(0));
        }

        @Override
        public <T> T write(final DynamicOps<T> ops, final Character value) {
            return ops.createString(value.toString());
        }

        @Override
        public String toString() {
            return "char";
        }
    };

    public static final Codec<MultiblockCraftingRecipe> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(o -> o.id),
            CraftingLayer.CODEC.listOf().fieldOf("layers").forGetter(o -> o.layers),
            Codec.unboundedMap(
                    CHAR,
                    Codec.either(
                            ResourceLocation.CODEC,
                            BlockStatePredicate.CODEC
                    )
            ).fieldOf("key").forGetter(o -> {
                Map<Character, Either<ResourceLocation, BlockStatePredicate>> map = new HashMap<>();
                o.blockDef.forEach((character, blockStatePredicate) -> {
                    if (blockStatePredicate.getStatePredicate() == null
                            || blockStatePredicate.getStatePredicate().isEmpty()
                    ) {
                        map.put(character, Either.left(blockStatePredicate.getIsBlock()));
                    } else {
                        map.put(character, Either.right(blockStatePredicate));
                    }
                });
                return map;
            })
    ).apply(ins, MultiblockCraftingRecipe::new));

    private final List<CraftingLayer> layers;
    private final Map<Character, BlockStatePredicate> blockDef;
    private final ResourceLocation id;

    MultiblockCraftingRecipe(
            ResourceLocation rl,
            List<CraftingLayer> layers,
            Map<Character, Either<ResourceLocation, BlockStatePredicate>> blockDef
    ) {
        if (layers.size() != 3) throw new IllegalArgumentException(
                "MultiblockCraftingRecipe has and can only have 3 elements."
        );
        this.id = rl;
        this.layers = layers;
        this.blockDef = new HashMap<>();
        blockDef.forEach((character, either) -> {
            either.ifRight(it -> this.blockDef.put(character, it))
                    .ifLeft(it -> this.blockDef.put(character, BlockStatePredicate.of(it)));
        });
    }

    @Override
    public boolean matches(@NotNull MobTransformContainer container, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(
            @NotNull MobTransformContainer container,
            @NotNull RegistryAccess registryAccess
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return MultiblockCraftingRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return MultiblockCraftingRecipeType.INSTANCE;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static class Builder {
        private final List<CraftingLayer> layers = new ArrayList<>();
        private final Map<Character, Either<ResourceLocation, BlockStatePredicate>> blockDef = new HashMap<>();
        private final ResourceLocation id;
        private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
        private final RecipeCategory category = RecipeCategory.MISC;

        Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder layer(String... args) {
            layers.add(CraftingLayer.of(args));
            return this;
        }

        public Builder define(char ch, ResourceLocation block) {
            blockDef.put(ch, Either.left(block));
            return this;
        }

        public Builder define(char ch, Block block) {
            blockDef.put(ch, Either.left(BuiltInRegistries.BLOCK.getKey(block)));
            return this;
        }

        @SafeVarargs
        public final Builder define(char ch, Block block, Map.Entry<Property<?>, StringRepresentable>... states) {
            blockDef.put(ch, Either.right(BlockStatePredicate.of(block, states)));
            return this;
        }

        public MultiblockCraftingRecipe build() {
            return new MultiblockCraftingRecipe(id, layers, blockDef);
        }

        /**
         * 完成
         */
        public FinishedMultiblockCraftingRecipe finish() {
            this.advancement
                    .parent(ROOT_RECIPE_ADVANCEMENT)
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                    .rewards(AdvancementRewards.Builder.recipe(id))
                    .requirements(RequirementsStrategy.OR);
            return new FinishedMultiblockCraftingRecipe(
                    build(),
                    advancement,
                    id.withPrefix("recipes/" + this.category.getFolderName() + "/")
            );
        }

        public void thenAccept(Consumer<FinishedRecipe> provider) {
            provider.accept(finish());
        }
    }


}
