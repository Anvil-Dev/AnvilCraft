package dev.dubhe.anvilcraft.integration.kubejs.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.RunCommand;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnExperience;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.AnvilCraftRecipeComponents;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;

public class AnvilCraftRecipeSchema {

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    class AnvilCraftRecipeJS extends RecipeJS {
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
            return runCommand(new Vec3(0, 0, 0), chance, command);
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
            return setBlock(new Vec3(0, 0, 0), chance, state);
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
            return spawnExperience(new Vec3(0, 0, 0), chance, experience);
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
            return spawnItem(new Vec3(0, 0, 0), chance, result);
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


    }

    RecipeKey<RecipeOutcome[]> OUTCOMES =
        AnvilCraftRecipeComponents.RECIPE_OUTCOME.asArray().key("outcomes");

    RecipeKey<RecipePredicate[]> PREDICATES =
        AnvilCraftRecipeComponents.RECIPE_PREDICATE.asArray().key("predicates");

}
