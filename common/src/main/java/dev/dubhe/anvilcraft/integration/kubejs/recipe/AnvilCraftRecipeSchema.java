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
import dev.dubhe.anvilcraft.integration.kubejs.recipe.builder.SelectOneBuilder;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.AnvilCraftRecipeComponents;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.rhino.util.HideFromJS;
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
import org.jetbrains.annotations.NotNull;


/**
 * 铁砧工艺配方架构
 */
public interface AnvilCraftRecipeSchema {

    @SuppressWarnings({"UnusedReturnValue", "unused", "stylecheck:off"})
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
         * KubeJS
         */
        public AnvilCraftRecipeJs addOutcome(final RecipeOutcome outcome) {
            if (getValue(OUTCOMES) == null) setValue(OUTCOMES, new RecipeOutcome[0]);
            setValue(OUTCOMES, ArrayUtils.add(getValue(OUTCOMES), outcome));
            save();
            return this;
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs damageAnvil() {
            return damageAnvil(1.0);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs damageAnvil(double chance) {
            DamageAnvil damageAnvil = new DamageAnvil(chance);
            return addOutcome(damageAnvil);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs runCommand(String command) {
            return runCommand(command, 1.0);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs runCommand(String command, double chance) {
            return runCommand(new Vec3(0, -1, 0), chance, command);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs runCommand(
            String command, double offsetX, double offsetY, double offsetZ, double chance) {
            return runCommand(new Vec3(offsetX, offsetY, offsetZ), chance, command);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs runCommand(Vec3 offset, double chance, String command) {
            RunCommand runCommand = new RunCommand(offset, chance, command);
            return addOutcome(runCommand);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs selectOne(RecipeOutcome... outcomes) {
            SelectOne selectOne = new SelectOne();
            for (RecipeOutcome outcome : outcomes) {
                selectOne.add(outcome);
            }
            return addOutcome(selectOne);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs selectOne(SelectOneBuilder builder) {
            return addOutcome(builder.build());
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs setBlock(Block block) {
            return setBlock(block.defaultBlockState());
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs setBlock(BlockState block) {
            return setBlock(1.0, block);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs setBlock(double chance, BlockState state) {
            return setBlock(new Vec3(0, -1, 0), chance, state);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs setBlock(
            double offsetX, double offsetY, double offsetZ, double chance, BlockState state) {
            return setBlock(new Vec3(offsetX, offsetY, offsetZ), chance, state);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs setBlock(Vec3 offset, double chance, BlockState result) {
            SetBlock setBlock = new SetBlock(offset, chance, result);
            return addOutcome(setBlock);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnExperience(int experience) {
            return spawnExperience(1.0, experience);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnExperience(double chance, int experience) {
            return spawnExperience(new Vec3(0, -1, 0), chance, experience);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnExperience(
            double offsetX, double offsetY, double offsetZ, double chance, int experience) {
            return spawnExperience(new Vec3(offsetX, offsetY, offsetZ), chance, experience);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnExperience(Vec3 offset, double chance, int experience) {
            SpawnExperience spawnExperience = new SpawnExperience(offset, chance, experience);
            return addOutcome(spawnExperience);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnItem(OutputItem result) {
            return spawnItem(1.0, result);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnItem(double chance, OutputItem result) {
            return spawnItem(new Vec3(0, -1, 0), chance, result);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnItem(
            double offsetX, double offsetY, double offsetZ, double chance, OutputItem result) {
            return spawnItem(new Vec3(offsetX, offsetY, offsetZ), chance, result);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnItem(Vec3 offset, double chance, OutputItem result) {
            return spawnItem(new Vec3(offset.x, offset.y, offset.z), chance, result.item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs spawnItem(Vec3 offset, double chance, ItemStack result) {
            SpawnItem spawnItem = new SpawnItem(offset, chance, result);
            return addOutcome(spawnItem);
        }

        /////////////////////////////////////
        //*******     PREDICATE     *******//
        /////////////////////////////////////

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs addPredicate(RecipePredicate predicate) {
            if (getValue(PREDICATES) == null) setValue(PREDICATES, new RecipePredicate[0]);
            setValue(PREDICATES, ArrayUtils.add(getValue(PREDICATES), predicate));
            save();
            return this;
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlock(Block... block) {
            return hasBlock(0, -1, 0, block);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlock(
            double offsetX, double offsetY, double offsetZ, Block... block) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(block);
            return hasBlock(offsetX, offsetY, offsetZ, predicate);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlock(BlockState blockState) {
            return hasBlock(0, -1, 0, blockState);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlock(
            double offsetX, double offsetY, double offsetZ, BlockState blockState) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(blockState.getBlock());
            for (var entry : blockState.getValues().entrySet()) {
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return hasBlock(offsetX, offsetY, offsetZ, predicate);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlock(
            double offsetX, double offsetY, double offsetZ, HasBlock.ModBlockPredicate matchBlock) {
            return hasBlock(new Vec3(offsetX, offsetY, offsetZ), matchBlock);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlock(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
            HasBlock hasBlock = new HasBlock(offset, matchBlock);
            return addPredicate(hasBlock);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlockIngredient(Block... block) {
            return hasBlockIngredient(0, -1, 0, block);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, Block... block) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(block);
            return hasBlockIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlockIngredient(BlockState blockState) {
            return hasBlockIngredient(0, -1, 0, blockState);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, BlockState blockState) {
            HasBlock.ModBlockPredicate predicate = new HasBlock.ModBlockPredicate();
            predicate.block(blockState.getBlock());
            for (var entry : blockState.getValues().entrySet()) {
                predicate.property(entry.getKey().getName(), entry.getValue().toString());
            }
            return hasBlockIngredient(offsetX, offsetY, offsetZ, predicate);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlockIngredient(
            double offsetX, double offsetY, double offsetZ, HasBlock.ModBlockPredicate matchBlock) {
            return hasBlockIngredient(new Vec3(offsetX, offsetY, offsetZ), matchBlock);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasBlockIngredient(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
            HasBlock hasBlock = new HasBlockIngredient(offset, matchBlock);
            return addPredicate(hasBlock);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasLavaFluidCauldron(int deplete) {
            return hasWaterFluidCauldron(0, -1, 0, deplete);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasLavaFluidCauldron(
            double offsetX, double offsetY, double offsetZ, int deplete) {
            return hasFluidCauldron(offsetX, offsetY, offsetZ, ModBlocks.LAVA_CAULDRON.get(), deplete);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasWaterFluidCauldron(int deplete) {
            return hasWaterFluidCauldron(0, -1, 0, deplete);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasWaterFluidCauldron(
            double offsetX, double offsetY, double offsetZ, int deplete) {
            return hasFluidCauldron(offsetX, offsetY, offsetZ, Blocks.WATER_CAULDRON, deplete);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasFluidCauldron(Block matchBlock, int deplete) {
            return hasFluidCauldron(0, -1, 0, matchBlock, deplete);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasFluidCauldron(
            double offsetX, double offsetY, double offsetZ, Block matchBlock, int deplete) {
            return hasFluidCauldron(new Vec3(offsetX, offsetY, offsetZ), matchBlock, deplete);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasFluidCauldron(Vec3 offset, Block matchBlock, int deplete) {
            HasFluidCauldron hasFluidCauldron = new HasFluidCauldron(offset, matchBlock, deplete);
            return addPredicate(hasFluidCauldron);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(int count, ItemLike... items) {
            return hasItem(0, -1, 0, count, items);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(
            double offsetX, double offsetY, double offsetZ, int count, ItemLike... items) {
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate.of(items);
            return hasItem(offsetX, offsetY, offsetZ, item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(int count, TagKey<Item> items) {
            return hasItem(0, -1, 0, count, items);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(
            double offsetX, double offsetY, double offsetZ, int count, TagKey<Item> items) {
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate.of(items);
            return hasItem(offsetX, offsetY, offsetZ, item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(ItemStack itemStack) {
            return hasItem(0, -1, 0, itemStack);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(double offsetX, double offsetY, double offsetZ, ItemStack itemStack) {
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate
                .of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) item.withNbt(itemStack.getOrCreateTag());
            return hasItem(offsetX, offsetY, offsetZ, item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(
            double offsetX, double offsetY, double offsetZ,
            HasItem.ModItemPredicate matchItem
        ) {
            return hasItem(new Vec3(offsetX, offsetY, offsetZ), matchItem);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItem(Vec3 offset, HasItem.ModItemPredicate matchItem) {
            HasItem hasItem = new HasItem(offset, matchItem);
            return addPredicate(hasItem);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(int count, ItemLike... items) {
            return hasItemIngredient(0, -1, 0, count, items);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, int count, ItemLike... items) {
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate.of(items);
            return hasItemIngredient(offsetX, offsetY, offsetZ, item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(int count, TagKey<Item> items) {
            return hasItemIngredient(0, -1, 0, count, items);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, int count, TagKey<Item> items) {
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate.of(items);
            return hasItemIngredient(offsetX, offsetY, offsetZ, item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(ItemStack itemStack) {
            return hasItemIngredient(0, -1, 0, itemStack);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, @NotNull ItemStack itemStack) {
            HasItem.ModItemPredicate item = HasItem.ModItemPredicate
                .of(itemStack.getItem())
                .withCount(MinMaxBounds.Ints.atLeast(itemStack.getCount()));
            if (itemStack.hasTag()) item.withNbt(itemStack.getOrCreateTag());
            return hasItemIngredient(offsetX, offsetY, offsetZ, item);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(
            double offsetX, double offsetY, double offsetZ, HasItem.ModItemPredicate matchItem) {
            return hasItemIngredient(new Vec3(offsetX, offsetY, offsetZ), matchItem);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs hasItemIngredient(Vec3 offset, HasItem.ModItemPredicate matchItem) {
            HasItem hasItem = new HasItemIngredient(offset, matchItem);
            return addPredicate(hasItem);
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs icon(OutputItem outputItem) {
            setValue(ICON, outputItem);
            return this;
        }

        /**
         * KubeJS
         */
        public AnvilCraftRecipeJs anvilRecipeType(String string) {
            setValue(TYPE, string);
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

    RecipeKey<String> TYPE =
            StringComponent.ANY.key("anvil_recipe_type");

    RecipeSchema SCHEMA = new RecipeSchema(
        AnvilCraftRecipeJs.class, AnvilCraftRecipeJs::new, OUTCOMES, PREDICATES, ICON
    ).constructor(((recipe, schemaType, keys, from) -> recipe.id(from.getValue(recipe, ID))), ID);
}
