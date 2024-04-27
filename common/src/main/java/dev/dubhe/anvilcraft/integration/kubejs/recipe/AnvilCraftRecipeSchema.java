package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
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
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.AnvilCraftRecipeComponents;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;


/**
 * 铁砧工艺配方架构
 */
public interface AnvilCraftRecipeSchema {

    @SuppressWarnings({"UnusedReturnValue", "unused", "stylecheck:MissingJavadocMethod"})
    class AnvilCraftRecipeJs extends RecipeJS {
        @HideFromJS
        @Override
        public RecipeJS id(ResourceLocation id) {
            this.id = new ResourceLocation(
                id.getNamespace().equals("minecraft") ? this.type.id.getNamespace() : id.getNamespace(),
                "%s/%s".formatted(this.type.id.getPath(), id.getPath())
            );
            return this;
        }

        ///////////////////////////////////
        //*******     OUTCOME     *******//
        ///////////////////////////////////

        /**
         * 为配方添加 outcome
         *
         * @param outcome outcome
         * @return Self
         */
        public AnvilCraftRecipeJs addOutcome(final RecipeOutcome outcome) {
            if (getValue(OUTCOMES) == null) setValue(OUTCOMES, new RecipeOutcome[0]);
            setValue(OUTCOMES, ArrayUtils.add(getValue(OUTCOMES), outcome));
            save();
            return this;
        }

        public AnvilCraftRecipeJs damageAnvil() {
            return damageAnvil(1.0);
        }

        public AnvilCraftRecipeJs damageAnvil(double chance) {
            DamageAnvil damageAnvil = new DamageAnvil(chance);
            return addOutcome(damageAnvil);
        }

        public AnvilCraftRecipeJs runCommand(String command) {
            return runCommand(command, 1.0);
        }

        public AnvilCraftRecipeJs runCommand(String command, double chance) {
            return runCommand(new Vec3(0, -1, 0), chance, command);
        }

        public AnvilCraftRecipeJs runCommand(
            String command, double offsetX, double offsetY, double offsetZ, double chance) {
            return runCommand(new Vec3(offsetX, offsetY, offsetZ), chance, command);
        }

        public AnvilCraftRecipeJs runCommand(Vec3 offset, double chance, String command) {
            RunCommand runCommand = new RunCommand(offset, chance, command);
            return addOutcome(runCommand);
        }

        /**
         * 添加 SelectOne outcome
         *
         * @param outcomes outcomes
         * @return Self
         */
        public AnvilCraftRecipeJs selectOne(RecipeOutcome... outcomes) {
            SelectOne selectOne = new SelectOne();
            for (RecipeOutcome outcome : outcomes) {
                selectOne.add(outcome);
            }
            return addOutcome(selectOne);
        }

        public AnvilCraftRecipeJs setBlock(Block block) {
            return setBlock(block.defaultBlockState());
        }

        public AnvilCraftRecipeJs setBlock(BlockState block) {
            return setBlock(1.0, block);
        }

        public AnvilCraftRecipeJs setBlock(double chance, BlockState state) {
            return setBlock(new Vec3(0, -1, 0), chance, state);
        }

        public AnvilCraftRecipeJs setBlock(
            double offsetX, double offsetY, double offsetZ, double chance, BlockState state) {
            return setBlock(new Vec3(offsetX, offsetY, offsetZ), chance, state);
        }

        public AnvilCraftRecipeJs setBlock(Vec3 offset, double chance, BlockState result) {
            SetBlock setBlock = new SetBlock(offset, chance, result);
            return addOutcome(setBlock);
        }

        public AnvilCraftRecipeJs spawnExperience(int experience) {
            return spawnExperience(1.0, experience);
        }

        public AnvilCraftRecipeJs spawnExperience(double chance, int experience) {
            return spawnExperience(new Vec3(0, -1, 0), chance, experience);
        }

        public AnvilCraftRecipeJs spawnExperience(
            double offsetX, double offsetY, double offsetZ, double chance, int experience) {
            return spawnExperience(new Vec3(offsetX, offsetY, offsetZ), chance, experience);
        }

        public AnvilCraftRecipeJs spawnExperience(Vec3 offset, double chance, int experience) {
            SpawnExperience spawnExperience = new SpawnExperience(offset, chance, experience);
            return addOutcome(spawnExperience);
        }

        public AnvilCraftRecipeJs spawnItem(OutputItem result) {
            return spawnItem(1.0, result);
        }

        public AnvilCraftRecipeJs spawnItem(double chance, OutputItem result) {
            return spawnItem(new Vec3(0, -1, 0), chance, result);
        }

        public AnvilCraftRecipeJs spawnItem(
            double offsetX, double offsetY, double offsetZ, double chance, OutputItem result) {
            return spawnItem(new Vec3(offsetX, offsetY, offsetZ), chance, result);
        }

        public AnvilCraftRecipeJs spawnItem(Vec3 offset, double chance, OutputItem result) {
            return spawnItem(new Vec3(offset.x, offset.y, offset.z), chance, result.item);
        }

        public AnvilCraftRecipeJs spawnItem(Vec3 offset, double chance, ItemStack result) {
            SpawnItem spawnItem = new SpawnItem(offset, chance, result);
            return addOutcome(spawnItem);
        }

        /////////////////////////////////////
        //*******     PREDICATE     *******//
        /////////////////////////////////////

        /**
         * 为配方添加 predicate
         *
         * @param predicate predicate
         * @return Self
         */
        public AnvilCraftRecipeJs addPredicate(RecipePredicate predicate) {
            if (getValue(PREDICATES) == null) setValue(PREDICATES, new RecipePredicate[0]);
            setValue(PREDICATES, ArrayUtils.add(getValue(PREDICATES), predicate));
            save();
            return this;
        }

        public AnvilCraftRecipeJs hasBlock(Block... block) {
            return hasBlock(0, -1, 0, block);
        }

        /**
         * 检查是否有方块
         *
         * @param offsetX 偏移X
         * @param offsetY 偏移Y
         * @param offsetZ 偏移Z
         * @param block   方块列表
         * @return Self
         */
        public AnvilCraftRecipeJs hasBlock(double offsetX, double offsetY, double offsetZ, Block... block) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(block);
            return hasBlock(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasBlock(BlockState blockState) {
            return hasBlock(0, -1, 0, blockState);
        }

        /**
         * 检查是否有方块
         *
         * @param offsetX    偏移X
         * @param offsetY    偏移Y
         * @param offsetZ    偏移Z
         * @param blockState 方块状态
         * @return Self
         */
        public AnvilCraftRecipeJs hasBlock(double offsetX, double offsetY, double offsetZ, BlockState blockState) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(blockState.getBlock());
            for (var entry : blockState.getValues().entrySet()) {
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return hasBlock(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasBlock(
            double offsetX, double offsetY, double offsetZ, HasBlock.ModBlockPredicate matchBlock) {
            return hasBlock(new Vec3(offsetX, offsetY, offsetZ), matchBlock);
        }

        public AnvilCraftRecipeJs hasBlock(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
            HasBlock hasBlock = new HasBlock(offset, matchBlock);
            return addPredicate(hasBlock);
        }

        public AnvilCraftRecipeJs hasBlockIngredient(Block... block) {
            return hasBlockIngredient(0, -1, 0, block);
        }

        public AnvilCraftRecipeJs hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, Block... block) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(block);
            return hasBlockIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasBlockIngredient(BlockState blockState) {
            return hasBlockIngredient(0, -1, 0, blockState);
        }

        public AnvilCraftRecipeJs hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, BlockState blockState) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(blockState.getBlock());
            for (var entry : blockState.getValues().entrySet()) {
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return hasBlockIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, HasBlock.ModBlockPredicate matchBlock) {
            return hasBlockIngredient(new Vec3(offsetX, offsetY, offsetZ), matchBlock);
        }

        public AnvilCraftRecipeJs hasBlockIngredient(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
            HasBlock hasBlock = new HasBlockIngredient(offset, matchBlock);
            return addPredicate(hasBlock);
        }

        public AnvilCraftRecipeJs hasLavaFluidCauldron(int deplete) {
            return hasWaterFluidCauldron(0, -1, 0, deplete);
        }

        public AnvilCraftRecipeJs hasLavaFluidCauldron(double offsetX, double offsetY, double offsetZ, int deplete) {
            return hasFluidCauldron(offsetX, offsetY, offsetZ, ModBlocks.LAVA_CAULDRON.get(), deplete);
        }

        public AnvilCraftRecipeJs hasWaterFluidCauldron(int deplete) {
            return hasWaterFluidCauldron(0, -1, 0, deplete);
        }

        public AnvilCraftRecipeJs hasWaterFluidCauldron(double offsetX, double offsetY, double offsetZ, int deplete) {
            return hasFluidCauldron(offsetX, offsetY, offsetZ, Blocks.WATER_CAULDRON, deplete);
        }

        public AnvilCraftRecipeJs hasFluidCauldron(Block matchBlock, int deplete) {
            return hasFluidCauldron(0, -1, 0, matchBlock, deplete);
        }

        public AnvilCraftRecipeJs hasFluidCauldron(
            double offsetX, double offsetY, double offsetZ, Block matchBlock, int deplete) {
            return hasFluidCauldron(new Vec3(offsetX, offsetY, offsetZ), matchBlock, deplete);
        }

        public AnvilCraftRecipeJs hasFluidCauldron(Vec3 offset, Block matchBlock, int deplete) {
            HasFluidCauldron hasFluidCauldron = new HasFluidCauldron(offset, matchBlock, deplete);
            return addPredicate(hasFluidCauldron);
        }

        public AnvilCraftRecipeJs hasItem(int count, ItemLike... items) {
            return hasItem(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJs hasItem(double offsetX, double offsetY, double offsetZ, int count, ItemLike... items) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItem(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasItem(int count, TagKey<Item> items) {
            return hasItem(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJs hasItem(double offsetX, double offsetY, double offsetZ, int count, TagKey<Item> items) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItem(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasItem(ItemStack itemStack) {
            return hasItem(0, -1, 0, itemStack);
        }

        public AnvilCraftRecipeJs hasItem(double offsetX, double offsetY, double offsetZ, ItemStack itemStack) {
            ItemPredicate.Builder builder = ItemPredicate.Builder.item()
                .of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) builder.hasNbt(itemStack.getOrCreateTag());
            return hasItem(offsetX, offsetY, offsetZ, builder.build());
        }

        public AnvilCraftRecipeJs hasItem(double offsetX, double offsetY, double offsetZ, ItemPredicate matchItem) {
            return hasItem(new Vec3(offsetX, offsetY, offsetZ), matchItem);
        }

        public AnvilCraftRecipeJs hasItem(Vec3 offset, ItemPredicate matchItem) {
            HasItem hasItem = new HasItem(offset, matchItem);
            return addPredicate(hasItem);
        }

        public AnvilCraftRecipeJs hasItemIngredient(int count, ItemLike... items) {
            return hasItemIngredient(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJs hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, int count, ItemLike... items) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItemIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasItemIngredient(int count, TagKey<Item> items) {
            return hasItemIngredient(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJs hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, int count, TagKey<Item> items) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItemIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJs hasItemIngredient(ItemStack itemStack) {
            return hasItemIngredient(0, -1, 0, itemStack);
        }

        public AnvilCraftRecipeJs hasItemIngredient(double offsetX, double offsetY, double offsetZ, ItemStack itemStack) {
            ItemPredicate.Builder builder = ItemPredicate.Builder.item()
                .of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) builder.hasNbt(itemStack.getOrCreateTag());
            return hasItemIngredient(offsetX, offsetY, offsetZ, builder.build());
        }

        public AnvilCraftRecipeJs hasItemIngredient(double offsetX, double offsetY, double offsetZ, ItemPredicate matchItem) {
            return hasItemIngredient(new Vec3(offsetX, offsetY, offsetZ), matchItem);
        }

        public AnvilCraftRecipeJs hasItemIngredient(Vec3 offset, ItemPredicate matchItem) {
            HasItem hasItem = new HasItemIngredient(offset, matchItem);
            return addPredicate(hasItem);
        }

        public AnvilCraftRecipeJs icon(OutputItem outputItem) {
            setValue(ICON, outputItem);
            return this;
        }
    }

    RecipeKey<ResourceLocation> ID =
        AnvilCraftRecipeComponents.RESOURCE_LOCATION.key("id");
    RecipeKey<RecipeOutcome[]> OUTCOMES =
        AnvilCraftRecipeComponents.RECIPE_OUTCOME.asArray().key("outcomes").defaultOptional();
    RecipeKey<RecipePredicate[]> PREDICATES =
        AnvilCraftRecipeComponents.RECIPE_PREDICATE.asArray().key("predicates").defaultOptional();

    RecipeKey<OutputItem> ICON =
        ItemComponents.OUTPUT.key("icon").optional(OutputItem.of(ModItems.ROYAL_STEEL_NUGGET));

    RecipeSchema SCHEMA = new RecipeSchema(AnvilCraftRecipeJs.class, AnvilCraftRecipeJs::new, OUTCOMES, PREDICATES, ICON)
        .constructor(((recipe, schemaType, keys, from) -> recipe.id(from.getValue(recipe, ID))), ID);
}
