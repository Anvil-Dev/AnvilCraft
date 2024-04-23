package dev.dubhe.anvilcraft.data.recipe.anvil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.RunCommand;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnExperience;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlockIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.util.IItemStackUtil;
import lombok.Getter;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class AnvilRecipe implements Recipe<AnvilCraftingContainer> {
    private final ResourceLocation id;
    @Getter
    private final List<RecipePredicate> predicates = new ArrayList<>();
    @Getter
    private final List<RecipeOutcome> outcomes = new ArrayList<>();
    private final ItemStack icon;

    public AnvilRecipe(ResourceLocation id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AnvilRecipe addPredicates(RecipePredicate... predicates) {
        this.predicates.addAll(Arrays.stream(predicates).toList());
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AnvilRecipe addOutcomes(RecipeOutcome... outcomes) {
        this.outcomes.addAll(Arrays.stream(outcomes).toList());
        return this;
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContainer container, @NotNull Level level) {
        for (RecipePredicate predicate : this.predicates) {
            if (!predicate.matches(container)) return false;
        }
        return true;
    }

    /**
     * 合成
     *
     * @param container 容器
     * @return 是否合成成功
     */
    public boolean craft(@NotNull AnvilCraftingContainer container) {
        if (!this.matches(container, container.getLevel())) return false;
        for (RecipePredicate predicate : this.predicates) predicate.process(container);
        for (RecipeOutcome outcome : this.outcomes) outcome.process(container);
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(
        @NotNull AnvilCraftingContainer container, @NotNull RegistryAccess registryAccess
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return this.icon;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public enum Serializer implements RecipeSerializer<AnvilRecipe> {
        INSTANCE;

        @Override
        public @NotNull AnvilRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject serializedRecipe) {
            ItemStack icon = ItemStack.EMPTY;
            if (serializedRecipe.has("icon")) {
                icon = IItemStackUtil.fromJson(serializedRecipe.get("icon"));
            }
            AnvilRecipe recipe = new AnvilRecipe(recipeId, icon);
            JsonArray predicates = GsonHelper.getAsJsonArray(serializedRecipe, "predicates");
            for (JsonElement predicate : predicates) {
                if (!predicate.isJsonObject()) {
                    throw new JsonSyntaxException("The predicate in the Anvil Recipe should be an object.");
                }
                recipe.addPredicates(RecipePredicate.fromJson(predicate.getAsJsonObject()));
            }
            JsonArray outcomes = GsonHelper.getAsJsonArray(serializedRecipe, "outcomes");
            for (JsonElement outcome : outcomes) {
                if (!outcome.isJsonObject()) {
                    throw new JsonSyntaxException("The outcome in the Anvil Recipe should be an object.");
                }
                recipe.addOutcomes(RecipeOutcome.fromJson(outcome.getAsJsonObject()));
            }
            return recipe;
        }

        @Override
        public @NotNull AnvilRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            AnvilRecipe recipe = new AnvilRecipe(recipeId, buffer.readItem());
            int size;
            size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                recipe.addPredicates(RecipePredicate.fromNetwork(buffer));
            }
            size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                recipe.addOutcomes(RecipeOutcome.fromNetwork(buffer));
            }
            return recipe;
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull AnvilRecipe recipe) {
            buffer.writeItem(recipe.icon);
            buffer.writeVarInt(recipe.predicates.size());
            for (RecipePredicate predicate : recipe.predicates) {
                predicate.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.outcomes.size());
            for (RecipeOutcome outcome : recipe.outcomes) {
                outcome.toNetwork(buffer);
            }
        }
    }

    public static class Builder implements RecipeBuilder {
        @Getter
        private final RecipeCategory category;
        private ItemStack icon;
        private final NonNullList<RecipePredicate> predicates = NonNullList.create();
        private final NonNullList<RecipeOutcome> outcomes = NonNullList.create();
        @Getter
        private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
        @Nullable
        private String group = null;

        private Builder(RecipeCategory category, ItemStack icon) {
            this.category = category;
            this.icon = icon;
        }

        public static @NotNull Builder create(RecipeCategory category) {
            return new Builder(category, Items.ANVIL.getDefaultInstance());
        }

        public static @NotNull Builder create(RecipeCategory category, ItemStack icon) {
            return new Builder(category, icon);
        }

        public @NotNull Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public @NotNull Builder icon(@NotNull ItemLike icon) {
            this.icon = icon.asItem().getDefaultInstance();
            return this;
        }

        public @NotNull Builder addPredicates(RecipePredicate... predicates) {
            this.predicates.addAll(Arrays.stream(predicates).toList());
            return this;
        }

        public @NotNull Builder addOutcomes(RecipeOutcome... outcomes) {
            this.outcomes.addAll(Arrays.stream(outcomes).toList());
            return this;
        }

        /**
         * 拥有物品
         *
         * @param offset 偏移
         * @param count  数量
         * @param items  物品
         * @return 构造器
         */
        public @NotNull Builder hasItem(Vec3 offset, int count, ItemLike... items) {
            ItemPredicate.Builder item = ItemPredicate.Builder.item()
                .of(items).withCount(MinMaxBounds.Ints.atLeast(count));
            return this.addPredicates(new HasItem(offset, item.build()));
        }

        public @NotNull Builder hasItem(Vec3 offset, ItemLike... items) {
            return this.hasItem(offset, 1, items);
        }

        /**
         * 拥有物品
         *
         * @param offset 偏移
         * @param count  数量
         * @param items  物品
         * @return 构造器
         */
        public @NotNull Builder hasItem(Vec3 offset, int count, TagKey<Item> items) {
            ItemPredicate.Builder item = ItemPredicate.Builder.item()
                .of(items).withCount(MinMaxBounds.Ints.atLeast(count));
            return this.addPredicates(new HasItem(offset, item.build()));
        }

        public @NotNull Builder hasItem(Vec3 offset, TagKey<Item> items) {
            return this.hasItem(offset, 1, items);
        }

        /**
         * 拥有物品
         *
         * @param offset    偏移
         * @param itemStack 物品
         * @return 构造器
         */
        public @NotNull Builder hasItem(Vec3 offset, @NotNull ItemStack itemStack) {
            ItemPredicate.Builder item = ItemPredicate.Builder.item().of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) item.hasNbt(itemStack.getOrCreateTag());
            return this.addPredicates(new HasItem(offset, item.build()));
        }

        public @NotNull Builder hasItem(ItemLike... items) {
            return this.hasItem(Vec3.ZERO, items);
        }

        public @NotNull Builder hasItem(TagKey<Item> items) {
            return this.hasItem(Vec3.ZERO, items);
        }

        public @NotNull Builder hasItem(int count, ItemLike... items) {
            return this.hasItem(Vec3.ZERO, count, items);
        }

        public @NotNull Builder hasItem(int count, TagKey<Item> items) {
            return this.hasItem(Vec3.ZERO, count, items);
        }

        public @NotNull Builder hasItem(@NotNull ItemStack itemStack) {
            return this.hasItem(Vec3.ZERO, itemStack);
        }

        /**
         * 拥有物品原料
         *
         * @param offset 偏移
         * @param count  数量
         * @param items  物品
         * @return 构造器
         */
        public @NotNull Builder hasItemIngredient(Vec3 offset, int count, ItemLike... items) {
            ItemPredicate.Builder item = ItemPredicate.Builder.item()
                .of(items).withCount(MinMaxBounds.Ints.atLeast(count));
            return this.addPredicates(new HasItemIngredient(offset, item.build()));
        }

        public @NotNull Builder hasItemIngredient(Vec3 offset, ItemLike... items) {
            return this.hasItemIngredient(offset, 1, items);
        }

        /**
         * 拥有物品原料
         *
         * @param offset 偏移
         * @param count  数量
         * @param items  物品
         * @return 构造器
         */
        public @NotNull Builder hasItemIngredient(Vec3 offset, int count, TagKey<Item> items) {
            ItemPredicate.Builder item = ItemPredicate.Builder.item()
                .of(items).withCount(MinMaxBounds.Ints.atLeast(count));
            return this.addPredicates(new HasItemIngredient(offset, item.build()));
        }

        public @NotNull Builder hasItemIngredient(Vec3 offset, TagKey<Item> items) {
            return this.hasItemIngredient(offset, 1, items);
        }

        /**
         * 拥有物品原料
         *
         * @param offset    偏移
         * @param itemStack 物品
         * @return 构造器
         */
        public @NotNull Builder hasItemIngredient(Vec3 offset, @NotNull ItemStack itemStack) {
            ItemPredicate.Builder item = ItemPredicate.Builder.item().of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) item.hasNbt(itemStack.getOrCreateTag());
            return this.addPredicates(new HasItemIngredient(offset, item.build()));
        }

        public @NotNull Builder hasItemIngredient(ItemLike... items) {
            return this.hasItemIngredient(Vec3.ZERO, items);
        }

        public @NotNull Builder hasItemIngredient(TagKey<Item> items) {
            return this.hasItemIngredient(Vec3.ZERO, items);
        }

        public @NotNull Builder hasItemIngredient(int count, ItemLike... items) {
            return this.hasItemIngredient(Vec3.ZERO, count, items);
        }

        public @NotNull Builder hasItemIngredient(int count, TagKey<Item> items) {
            return this.hasItemIngredient(Vec3.ZERO, count, items);
        }

        public @NotNull Builder hasItemIngredient(@NotNull ItemStack itemStack) {
            return this.hasItemIngredient(Vec3.ZERO, itemStack);
        }

        public @NotNull Builder hasBlock(Vec3 offset, Block... blocks) {
            return this.addPredicates(new HasBlock(offset, new HasBlock.ModBlockPredicate().block(blocks)));
        }

        /**
         * 拥有方块
         *
         * @param block  方块
         * @param offset 偏移
         * @param states 状态
         * @return 构造器
         */
        @SafeVarargs
        public final @NotNull Builder hasBlock(
            Block block, Vec3 offset, Map.Entry<Property<?>, Comparable<?>> @NotNull ... states
        ) {
            return this.addPredicates(new HasBlock(
                offset, new HasBlock.ModBlockPredicate().block(block).property(states)
            ));
        }

        /**
         * 拥有方块
         *
         * @param offset 偏移
         * @param blocks 方块
         * @return 构造器
         */
        public @NotNull Builder hasBlock(Vec3 offset, TagKey<Block> blocks) {
            return this.addPredicates(new HasBlock(offset, new HasBlock.ModBlockPredicate().block(blocks)));
        }

        /**
         * 拥有方块
         *
         * @param offset         偏移
         * @param blockPredicate 方块谓词
         * @return 构造器
         */
        public @NotNull Builder hasBlock(Vec3 offset, HasBlock.ModBlockPredicate blockPredicate) {
            return this.addPredicates(new HasBlock(offset, blockPredicate));
        }

        /**
         * 拥有方块
         *
         * @param offset     偏移
         * @param blockState 方块状态
         * @param <T>        可比较的
         * @return 构造器
         */
        @SuppressWarnings("unchecked")
        public <T extends Comparable<T>> @NotNull Builder hasBlock(Vec3 offset, @NotNull BlockState blockState) {
            BlockState defaultBlockState = blockState.getBlock().defaultBlockState();
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate().block(blockState.getBlock());
            for (Map.Entry<Property<?>, Comparable<?>> entry : blockState.getValues().entrySet()) {
                if (((T) defaultBlockState.getValue(entry.getKey())).compareTo((T) entry.getValue()) == 0) continue;
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return this.addPredicates(new HasBlock(offset, predicate));
        }

        /**
         * 拥有方块
         *
         * @param blocks 方块
         * @return 构造器
         */
        public @NotNull Builder hasBlock(Block... blocks) {
            return this.hasBlock(new Vec3(0.0, -1.0, 0.0), blocks);
        }

        public @NotNull Builder hasBlock(TagKey<Block> blocks) {
            return this.hasBlock(new Vec3(0.0, -1.0, 0.0), blocks);
        }

        public @NotNull Builder hasBlock(BlockState blockState) {
            return this.hasBlock(new Vec3(0.0, -1.0, 0.0), blockState);
        }

        public @NotNull Builder hasBlockIngredient(Vec3 offset, Block... blocks) {
            return this.addPredicates(new HasBlockIngredient(offset, new HasBlock.ModBlockPredicate().block(blocks)));
        }

        public @NotNull Builder hasBlockIngredient(Vec3 offset, TagKey<Block> blocks) {
            return this.addPredicates(new HasBlockIngredient(offset, new HasBlock.ModBlockPredicate().block(blocks)));
        }

        /**
         * 物品原料
         */
        @SuppressWarnings("unchecked")
        public @NotNull <T extends Comparable<T>> Builder hasBlockIngredient(
            Vec3 offset, @NotNull BlockState blockState
        ) {
            BlockState defaultBlockState = blockState.getBlock().defaultBlockState();
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate().block(blockState.getBlock());
            for (Map.Entry<Property<?>, Comparable<?>> entry : blockState.getValues().entrySet()) {
                if (((T) defaultBlockState.getValue(entry.getKey())).compareTo((T) entry.getValue()) == 0) continue;
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return this.addPredicates(new HasBlockIngredient(offset, predicate));
        }

        public @NotNull Builder hasBlockIngredient(Block... blocks) {
            return this.hasBlockIngredient(new Vec3(0.0, -1.0, 0.0), blocks);
        }

        public @NotNull Builder hasBlockIngredient(TagKey<Block> blocks) {
            return this.hasBlockIngredient(new Vec3(0.0, -1.0, 0.0), blocks);
        }

        public @NotNull Builder hasBlockIngredient(BlockState blockState) {
            return this.hasBlockIngredient(new Vec3(0.0, -1.0, 0.0), blockState);
        }

        public @NotNull Builder hasFluidCauldron(Vec3 offset, Block block, int deplete) {
            return this.addPredicates(new HasFluidCauldron(offset, block, deplete));
        }

        public @NotNull Builder hasFluidCauldron(Vec3 offset, Block block) {
            return this.hasFluidCauldron(offset, block, 1);
        }

        public @NotNull Builder hasFluidCauldronFull(Vec3 offset, Block block) {
            return this.hasFluidCauldron(offset, block, 3);
        }

        public @NotNull Builder damageAnvil(double chance) {
            return this.addOutcomes(new DamageAnvil(chance));
        }

        public @NotNull Builder damageAnvil() {
            return this.damageAnvil(1.0);
        }

        public @NotNull Builder setBlock(Vec3 offset, double chance, BlockState block) {
            if (this.icon.is(Items.ANVIL)) this.icon = block.getBlock().asItem().getDefaultInstance();
            return this.addOutcomes(new SetBlock(offset, chance, block));
        }

        public @NotNull Builder setBlock(double chance, BlockState block) {
            return this.setBlock(new Vec3(0.0, -1.0, 0.0), chance, block);
        }

        public @NotNull Builder setBlock(Vec3 offset, BlockState block) {
            return this.setBlock(offset, 1.0, block);
        }

        public @NotNull Builder setBlock(BlockState block) {
            return this.setBlock(1.0, block);
        }

        public @NotNull Builder setBlock(Vec3 offset, double chance, @NotNull Block block) {
            return this.setBlock(offset, chance, block.defaultBlockState());
        }

        public @NotNull Builder setBlock(double chance, @NotNull Block block) {
            return this.setBlock(chance, block.defaultBlockState());
        }

        public @NotNull Builder setBlock(Vec3 offset, @NotNull Block block) {
            return this.setBlock(offset, block.defaultBlockState());
        }

        public @NotNull Builder setBlock(@NotNull Block block) {
            return this.setBlock(block.defaultBlockState());
        }

        public @NotNull Builder spawnItem(Vec3 offset, double chance, ItemStack item) {
            if (this.icon.is(Items.ANVIL)) this.icon = item;
            return this.addOutcomes(new SpawnItem(offset, chance, item));
        }

        public @NotNull Builder spawnItem(double chance, ItemStack item) {
            return this.spawnItem(Vec3.ZERO, chance, item);
        }

        public @NotNull Builder spawnItem(Vec3 offset, ItemStack item) {
            return this.spawnItem(offset, 1.0, item);
        }

        public @NotNull Builder spawnItem(ItemStack item) {
            return this.spawnItem(1.0, item);
        }

        /**
         * 生成物品
         *
         * @param offset 偏移
         * @param chance 几率
         * @param item   物品
         * @param count  数量
         * @return 构造器
         */
        public @NotNull Builder spawnItem(Vec3 offset, double chance, @NotNull ItemLike item, int count) {
            ItemStack stack = item.asItem().getDefaultInstance();
            stack.setCount(count);
            return this.spawnItem(offset, chance, stack);
        }

        public @NotNull Builder spawnItem(double chance, @NotNull ItemLike item, int count) {
            return this.spawnItem(Vec3.ZERO, chance, item, count);
        }

        public @NotNull Builder spawnItem(Vec3 offset, @NotNull ItemLike item, int count) {
            return this.spawnItem(offset, 1.0, item, count);
        }

        public @NotNull Builder spawnItem(@NotNull ItemLike item, int count) {
            return this.spawnItem(Vec3.ZERO, 1.0, item, count);
        }

        public @NotNull Builder spawnItem(Vec3 offset, double chance, @NotNull ItemLike item) {
            return this.spawnItem(offset, chance, item, 1);
        }

        public @NotNull Builder spawnItem(double chance, ItemLike item) {
            return this.spawnItem(Vec3.ZERO, chance, item);
        }

        public @NotNull Builder spawnItem(Vec3 offset, ItemLike item) {
            return this.spawnItem(offset, 1.0, item);
        }

        public @NotNull Builder spawnItem(ItemLike item) {
            return this.spawnItem(1.0, item);
        }

        public @NotNull Builder spawnExperience(Vec3 offset, double chance, int experience) {
            return this.addOutcomes(new SpawnExperience(offset, chance, experience));
        }

        public @NotNull Builder spawnExperience(double chance, int experience) {
            return this.spawnExperience(Vec3.ZERO, chance, experience);
        }

        public @NotNull Builder spawnExperience(Vec3 offset, int experience) {
            return this.spawnExperience(offset, 1.0, experience);
        }

        public @NotNull Builder spawnExperience(int experience) {
            return this.spawnExperience(1.0, experience);
        }

        public @NotNull Builder runCommand(Vec3 offset, double chance, String command) {
            return this.addOutcomes(new RunCommand(offset, chance, command));
        }

        public @NotNull Builder runCommand(Vec3 offset, String command) {
            return this.runCommand(offset, 1.0, command);
        }

        public @NotNull Builder runCommand(double chance, String command) {
            return this.runCommand(Vec3.ZERO, chance, command);
        }

        public @NotNull Builder runCommand(String command) {
            return this.runCommand(1.0, command);
        }

        @Override
        public @NotNull Builder unlockedBy(
            @NotNull String criterionName, @NotNull CriterionTriggerInstance criterionTrigger
        ) {
            this.advancement.addCriterion(criterionName, criterionTrigger);
            return this;
        }

        @Override
        public @NotNull Builder group(@Nullable String groupName) {
            this.group = groupName;
            return this;
        }

        @Override
        public @NotNull Item getResult() {
            return this.icon.getItem();
        }

        @Override
        public void save(@NotNull Consumer<FinishedRecipe> finishedRecipeConsumer, @NotNull ResourceLocation recipeId) {
            this.advancement
                .parent(ROOT_RECIPE_ADVANCEMENT)
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(RequirementsStrategy.OR);
            finishedRecipeConsumer.accept(
                new Result(
                    recipeId,
                    this.icon,
                    this.predicates,
                    this.outcomes,
                    null == this.group ? "" : this.group,
                    this.advancement,
                    recipeId.withPrefix("recipes/" + this.category.getFolderName() + "/")
                )
            );
        }

        static class Result implements FinishedRecipe {
            @Getter
            private final ResourceLocation id;
            private final ItemStack icon;
            private final NonNullList<RecipePredicate> predicates;
            private final NonNullList<RecipeOutcome> outcomes;
            private final String group;
            @Getter
            private final Advancement.Builder advancement;
            @Getter
            private final ResourceLocation advancementId;

            Result(
                ResourceLocation id,
                ItemStack icon,
                NonNullList<RecipePredicate> predicates,
                NonNullList<RecipeOutcome> outcomes,
                String group,
                Advancement.Builder advancement,
                ResourceLocation advancementId
            ) {
                this.id = id;
                this.icon = icon;
                this.predicates = predicates;
                this.outcomes = outcomes;
                this.advancement = advancement;
                this.group = group;
                this.advancementId = advancementId;
            }

            @Override
            public void serializeRecipeData(@NotNull JsonObject json) {
                if (!this.group.isEmpty()) {
                    json.addProperty("group", this.group);
                }
                json.add("icon", IItemStackUtil.toJson(this.icon));
                JsonArray predicates = new JsonArray();
                for (RecipePredicate predicate : this.predicates) {
                    predicates.add(predicate.toJson());
                }
                json.add("predicates", predicates);
                JsonArray outcomes = new JsonArray();
                for (RecipeOutcome outcome : this.outcomes) {
                    outcomes.add(outcome.toJson());
                }
                json.add("outcomes", outcomes);
            }

            @Nullable
            @Override
            public JsonObject serializeAdvancement() {
                return this.advancement.serializeToJson();
            }

            @Override
            public @NotNull RecipeSerializer<?> getType() {
                return Serializer.INSTANCE;
            }
        }
    }

    public enum Type implements RecipeType<AnvilRecipe> {
        INSTANCE
    }

    static {
        RecipePredicate.register("has_item", HasItem::new, HasItem::new);
        RecipePredicate.register("has_item_ingredient", HasItemIngredient::new, HasItemIngredient::new);
        RecipePredicate.register("has_block", HasBlock::new, HasBlock::new);
        RecipePredicate.register("has_block_ingredient", HasBlockIngredient::new, HasBlockIngredient::new);
        RecipePredicate.register("has_fluid_cauldron", HasFluidCauldron::new, HasFluidCauldron::new);
        RecipeOutcome.register("damage_anvil", DamageAnvil::new, DamageAnvil::new);
        RecipeOutcome.register("set_block", SetBlock::new, SetBlock::new);
        RecipeOutcome.register("spawn_item", SpawnItem::new, SpawnItem::new);
        RecipeOutcome.register("spawn_experience", SpawnExperience::new, SpawnExperience::new);
        RecipeOutcome.register("run_command", RunCommand::new, RunCommand::new);
        RecipeOutcome.register("select_one", SelectOne::new, SelectOne::new);
    }
}
