package dev.dubhe.anvilcraft.integration.kubejs.recipe.builder;

import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.RunCommand;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnExperience;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SelectOneBuilder {
    private final List<RecipeOutcome> outcomes = new ArrayList<>();

    public SelectOneBuilder addOutcome(final RecipeOutcome outcome) {
        outcomes.add(outcome);
        return this;
    }


    public SelectOneBuilder damageAnvil() {
        return damageAnvil(1.0);
    }


    /**
     * 添加铁砧伤害 outcome
     *
     * @param chance 概率
     * @return Self
     */
    public SelectOneBuilder damageAnvil(double chance) {
        DamageAnvil damageAnvil = new DamageAnvil(chance);
        return addOutcome(damageAnvil);
    }


    public SelectOneBuilder runCommand(String command) {
        return runCommand(command, 1.0);
    }


    public SelectOneBuilder runCommand(String command, double chance) {
        return runCommand(0, -1, 0, chance, command);
    }


    public SelectOneBuilder runCommand(
        double offsetX, double offsetY, double offsetZ, double chance, String command) {
        return runCommand(new Vec3(offsetX, offsetY, offsetZ), chance, command);
    }


    /**
     * 添加运行命令 outcome
     *
     * @param offset  偏移
     * @param chance  几率
     * @param command 命令
     * @return Self
     */
    public SelectOneBuilder runCommand(Vec3 offset, double chance, String command) {
        RunCommand runCommand = new RunCommand(offset, chance, command);
        return addOutcome(runCommand);
    }


    public SelectOneBuilder setBlock(Block block) {
        return setBlock(block.defaultBlockState());
    }

    public SelectOneBuilder setBlock(BlockState block) {
        return setBlock(1.0, block);
    }


    public SelectOneBuilder setBlock(double chance, BlockState state) {
        return setBlock(new Vec3(0, -1, 0), chance, state);
    }


    public SelectOneBuilder setBlock(
        double offsetX, double offsetY, double offsetZ, double chance, BlockState state) {
        return setBlock(new Vec3(offsetX, offsetY, offsetZ), chance, state);
    }

    /**
     * 放置方块 outcome
     *
     * @param offset 偏移
     * @param chance 几率
     * @param result 放置的方块
     * @return Self
     */
    public SelectOneBuilder setBlock(Vec3 offset, double chance, BlockState result) {
        SetBlock setBlock = new SetBlock(offset, chance, result);
        return addOutcome(setBlock);
    }


    public SelectOneBuilder spawnExperience(int experience) {
        return spawnExperience(1.0, experience);
    }


    public SelectOneBuilder spawnExperience(double chance, int experience) {
        return spawnExperience(0, -1, 0, chance, experience);
    }


    public SelectOneBuilder spawnExperience(
        double offsetX, double offsetY, double offsetZ, double chance, int experience) {
        return spawnExperience(new Vec3(offsetX, offsetY, offsetZ), chance, experience);
    }

    /**
     * 生成经验 outcome
     *
     * @param offset     偏移
     * @param chance     几率
     * @param experience 经验数量
     * @return Self
     */
    public SelectOneBuilder spawnExperience(Vec3 offset, double chance, int experience) {
        SpawnExperience spawnExperience = new SpawnExperience(offset, chance, experience);
        return addOutcome(spawnExperience);
    }


    public SelectOneBuilder spawnItem(ItemStack result) {
        return spawnItem(1.0, result);
    }


    public SelectOneBuilder spawnItem(double chance, ItemStack result) {
        return spawnItem(new Vec3(0, -1, 0), chance, result);
    }


    public SelectOneBuilder spawnItem(
        double offsetX, double offsetY, double offsetZ, double chance, ItemStack result) {
        return spawnItem(new Vec3(offsetX, offsetY, offsetZ), chance, result);
    }

    /**
     * 生成物品 outcome
     *
     * @param offset 偏移
     * @param chance 几率
     * @param result 生成的物品
     * @return Self
     */
    public SelectOneBuilder spawnItem(Vec3 offset, double chance, ItemStack result) {
        SpawnItem spawnItem = new SpawnItem(offset, chance, result);
        return addOutcome(spawnItem);
    }

    public SelectOne build() {
        SelectOne selectOne = new SelectOne();
        outcomes.forEach(selectOne::add);
        return selectOne;
    }
}
