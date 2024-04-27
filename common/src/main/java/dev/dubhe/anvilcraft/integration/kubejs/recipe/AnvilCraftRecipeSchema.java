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

public interface AnvilCraftRecipeSchema {

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    class AnvilCraftRecipeJS extends RecipeJS {
        @HideFromJS
        @Override
        public RecipeJS id(ResourceLocation _id) {
            this.id = new ResourceLocation(
                _id.getNamespace().equals("minecraft") ? this.type.id.getNamespace() : _id.getNamespace(),
                "%s/%s".formatted(this.type.id.getPath(), _id.getPath())
            );
            return this;
        }

        ///////////////////////////////////
        //*******     OUTCOME     *******//
        ///////////////////////////////////
        public AnvilCraftRecipeJS addOutcome(final RecipeOutcome outcome) {
            if (getValue(OUTCOMES) == null) setValue(OUTCOMES, new RecipeOutcome[0]);
            setValue(OUTCOMES, ArrayUtils.add(getValue(OUTCOMES), outcome));
            save();
            return this;
        }

        public AnvilCraftRecipeJS damageAnvil() {
            return damageAnvil(1.0);
        }

        public AnvilCraftRecipeJS damageAnvil(double chance) {
            DamageAnvil damageAnvil = new DamageAnvil(chance);
            return addOutcome(damageAnvil);
        }

        public AnvilCraftRecipeJS runCommand(String command) {
            return runCommand(command, 1.0);
        }

        public AnvilCraftRecipeJS runCommand(String command, double chance) {
            return runCommand(new Vec3(0, -1, 0), chance, command);
        }

        public AnvilCraftRecipeJS runCommand(
            String command, double offsetX, double offsetY, double offsetZ, double chance
        ) {
            return runCommand(new Vec3(offsetX, offsetY, offsetZ), chance, command);
        }

        public AnvilCraftRecipeJS runCommand(
            Vec3 offset, double chance, String command
        ) {
            RunCommand runCommand = new RunCommand(offset, chance, command);
            return addOutcome(runCommand);
        }

        public AnvilCraftRecipeJS selectOne(RecipeOutcome... outcomes) {
            SelectOne selectOne = new SelectOne();
            for (RecipeOutcome outcome : outcomes) {
                selectOne.add(outcome);
            }
            return addOutcome(selectOne);
        }

        public AnvilCraftRecipeJS setBlock(Block block) {
            return setBlock(block.defaultBlockState());
        }

        public AnvilCraftRecipeJS setBlock(BlockState block) {
            return setBlock(1.0, block);
        }

        public AnvilCraftRecipeJS setBlock(double chance, BlockState state) {
            return setBlock(new Vec3(0, -1, 0), chance, state);
        }

        public AnvilCraftRecipeJS setBlock(
            double offsetX, double offsetY, double offsetZ, double chance, BlockState state
        ) {
            return setBlock(new Vec3(offsetX, offsetY, offsetZ), chance, state);
        }

        public AnvilCraftRecipeJS setBlock(Vec3 offset, double chance, BlockState result) {
            SetBlock setBlock = new SetBlock(offset, chance, result);
            return addOutcome(setBlock);
        }

        public AnvilCraftRecipeJS spawnExperience(int experience) {
            return spawnExperience(1.0, experience);
        }

        public AnvilCraftRecipeJS spawnExperience(double chance, int experience) {
            return spawnExperience(new Vec3(0, -1, 0), chance, experience);
        }

        public AnvilCraftRecipeJS spawnExperience(
            double offsetX, double offsetY, double offsetZ, double chance, int experience
        ) {
            return spawnExperience(new Vec3(offsetX, offsetY, offsetZ), chance, experience);
        }

        public AnvilCraftRecipeJS spawnExperience(Vec3 offset, double chance, int experience) {
            SpawnExperience spawnExperience = new SpawnExperience(offset, chance, experience);
            return addOutcome(spawnExperience);
        }

        public AnvilCraftRecipeJS spawnItem(OutputItem result) {
            return spawnItem(1.0, result);
        }

        public AnvilCraftRecipeJS spawnItem(double chance, OutputItem result) {
            return spawnItem(new Vec3(0, -1, 0), chance, result);
        }

        public AnvilCraftRecipeJS spawnItem(
            double offsetX, double offsetY, double offsetZ, double chance, OutputItem result
        ) {
            return spawnItem(new Vec3(offsetX, offsetY, offsetZ), chance, result);
        }

        public AnvilCraftRecipeJS spawnItem(Vec3 offset, double chance, OutputItem result) {
            return spawnItem(new Vec3(offset.x, offset.y, offset.z), chance, result.item);
        }

        public AnvilCraftRecipeJS spawnItem(Vec3 offset, double chance, ItemStack result) {
            SpawnItem spawnItem = new SpawnItem(offset, chance, result);
            return addOutcome(spawnItem);
        }

        /////////////////////////////////////
        //*******     PREDICATE     *******//
        /////////////////////////////////////

        public AnvilCraftRecipeJS addPredicate(RecipePredicate predicate) {
            if (getValue(PREDICATES) == null) setValue(PREDICATES, new RecipePredicate[0]);
            setValue(PREDICATES, ArrayUtils.add(getValue(PREDICATES), predicate));
            save();
            return this;
        }

        public AnvilCraftRecipeJS hasBlock(
            Block... block
        ) {
            return hasBlock(0, -1, 0, block);
        }

        public AnvilCraftRecipeJS hasBlock(
            double offsetX, double offsetY, double offsetZ, Block... block
        ) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(block);
            return hasBlock(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasBlock(BlockState blockState) {
            return hasBlock(0, -1, 0, blockState);
        }

        public AnvilCraftRecipeJS hasBlock(
            double offsetX, double offsetY, double offsetZ, BlockState blockState
        ) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(blockState.getBlock());
            for (var entry : blockState.getValues().entrySet()) {
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return hasBlock(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasBlock(
            double offsetX, double offsetY, double offsetZ, HasBlock.ModBlockPredicate matchBlock
        ) {
            return hasBlock(new Vec3(offsetX, offsetY, offsetZ), matchBlock);
        }

        public AnvilCraftRecipeJS hasBlock(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
            HasBlock hasBlock = new HasBlock(offset, matchBlock);
            return addPredicate(hasBlock);
        }

        public AnvilCraftRecipeJS hasBlockIngredient(
            Block... block
        ) {
            return hasBlockIngredient(0, -1, 0, block);
        }

        public AnvilCraftRecipeJS hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, Block... block
        ) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(block);
            return hasBlockIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasBlockIngredient(BlockState blockState) {
            return hasBlockIngredient(0, -1, 0, blockState);
        }

        public AnvilCraftRecipeJS hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, BlockState blockState
        ) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(blockState.getBlock());
            for (var entry : blockState.getValues().entrySet()) {
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return hasBlockIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, HasBlock.ModBlockPredicate matchBlock
        ) {
            return hasBlockIngredient(new Vec3(offsetX, offsetY, offsetZ), matchBlock);
        }

        public AnvilCraftRecipeJS hasBlockIngredient(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
            HasBlock hasBlock = new HasBlockIngredient(offset, matchBlock);
            return addPredicate(hasBlock);
        }

        public AnvilCraftRecipeJS hasLavaFluidCauldron(int deplete) {
            return hasWaterFluidCauldron(0, -1, 0, deplete);
        }

        public AnvilCraftRecipeJS hasLavaFluidCauldron(
            double offsetX, double offsetY, double offsetZ, int deplete
        ) {
            return hasFluidCauldron(offsetX, offsetY, offsetZ, ModBlocks.LAVA_CAULDRON.get(), deplete);
        }

        public AnvilCraftRecipeJS hasWaterFluidCauldron(int deplete) {
            return hasWaterFluidCauldron(0, -1, 0, deplete);
        }

        public AnvilCraftRecipeJS hasWaterFluidCauldron(
            double offsetX, double offsetY, double offsetZ, int deplete
        ) {
            return hasFluidCauldron(offsetX, offsetY, offsetZ, Blocks.WATER_CAULDRON, deplete);
        }

        public AnvilCraftRecipeJS hasFluidCauldron(Block matchBlock, int deplete) {
            return hasFluidCauldron(0, -1, 0, matchBlock, deplete);
        }

        public AnvilCraftRecipeJS hasFluidCauldron(
            double offsetX, double offsetY, double offsetZ, Block matchBlock, int deplete
        ) {
            return hasFluidCauldron(new Vec3(offsetX, offsetY, offsetZ), matchBlock, deplete);
        }

        public AnvilCraftRecipeJS hasFluidCauldron(Vec3 offset, Block matchBlock, int deplete) {
            HasFluidCauldron hasFluidCauldron = new HasFluidCauldron(offset, matchBlock, deplete);
            return addPredicate(hasFluidCauldron);
        }

        public AnvilCraftRecipeJS hasItem(int count, ItemLike... items) {
            return hasItem(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJS hasItem(
            double offsetX, double offsetY, double offsetZ, int count, ItemLike... items
        ) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItem(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasItem(int count, TagKey<Item> items) {
            return hasItem(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJS hasItem(
            double offsetX, double offsetY, double offsetZ, int count, TagKey<Item> items
        ) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItem(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasItem(ItemStack itemStack) {
            return hasItem(0, -1, 0, itemStack);
        }

        public AnvilCraftRecipeJS hasItem(
            double offsetX, double offsetY, double offsetZ, ItemStack itemStack
        ) {
            ItemPredicate.Builder builder = ItemPredicate.Builder.item().of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) builder.hasNbt(itemStack.getOrCreateTag());
            return hasItem(offsetX, offsetY, offsetZ, builder.build());
        }

        public AnvilCraftRecipeJS hasItem(
            double offsetX, double offsetY, double offsetZ, ItemPredicate matchItem
        ) {
            return hasItem(new Vec3(offsetX, offsetY, offsetZ), matchItem);
        }

        public AnvilCraftRecipeJS hasItem(Vec3 offset, ItemPredicate matchItem) {
            HasItem hasItem = new HasItem(offset, matchItem);
            return addPredicate(hasItem);
        }

        public AnvilCraftRecipeJS hasItemIngredient(int count, ItemLike... items) {
            return hasItemIngredient(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJS hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, int count, ItemLike... items
        ) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItemIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasItemIngredient(int count, TagKey<Item> items) {
            return hasItemIngredient(0, -1, 0, count, items);
        }

        public AnvilCraftRecipeJS hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, int count, TagKey<Item> items
        ) {
            ItemPredicate predicate = ItemPredicate.Builder.item().of(items).build();
            return hasItemIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        public AnvilCraftRecipeJS hasItemIngredient(ItemStack itemStack) {
            return hasItemIngredient(0, -1, 0, itemStack);
        }

        public AnvilCraftRecipeJS hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, ItemStack itemStack
        ) {
            ItemPredicate.Builder builder = ItemPredicate.Builder.item().of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) builder.hasNbt(itemStack.getOrCreateTag());
            return hasItemIngredient(offsetX, offsetY, offsetZ, builder.build());
        }

        public AnvilCraftRecipeJS hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, ItemPredicate matchItem
        ) {
            return hasItemIngredient(new Vec3(offsetX, offsetY, offsetZ), matchItem);
        }

        public AnvilCraftRecipeJS hasItemIngredient(Vec3 offset, ItemPredicate matchItem) {
            HasItem hasItem = new HasItemIngredient(offset, matchItem);
            return addPredicate(hasItem);
        }

        public AnvilCraftRecipeJS icon(OutputItem outputItem) {
            setValue(ICON, outputItem);
            return this;
        }
    }

    RecipeKey<ResourceLocation> ID = AnvilCraftRecipeComponents.RESOURCE_LOCATION.key("id");
    RecipeKey<RecipeOutcome[]> OUTCOMES =
        AnvilCraftRecipeComponents.RECIPE_OUTCOME.asArray().key("outcomes").defaultOptional();

    RecipeKey<RecipePredicate[]> PREDICATES =
        AnvilCraftRecipeComponents.RECIPE_PREDICATE.asArray().key("predicates").defaultOptional();

    RecipeKey<OutputItem> ICON = ItemComponents.OUTPUT.key("icon").optional(OutputItem.of(ModItems.ROYAL_STEEL_NUGGET));

    RecipeSchema SCHEMA = new RecipeSchema(AnvilCraftRecipeJS.class, AnvilCraftRecipeJS::new,
        OUTCOMES, PREDICATES, ICON)
        .constructor(((recipe, schemaType, keys, from) -> recipe.id(from.getValue(recipe, ID))), ID);
}
