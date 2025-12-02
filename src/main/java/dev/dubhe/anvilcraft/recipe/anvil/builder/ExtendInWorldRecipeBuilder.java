package dev.dubhe.anvilcraft.recipe.anvil.builder;

import dev.anvilcraft.lib.recipe.builder.InWorldRecipeBuilder;
import dev.anvilcraft.lib.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.recipe.trigger.IRecipeTrigger;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceHeat;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import lombok.EqualsAndHashCode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 世界内配方构建器
 *
 * <p>用于构建在世界中使用的铁砧配方，支持各种触发器、谓词和结果。可以设置配方的触发条件、前置条件、冲突条件和结果操作等。</p>
 */
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
public class ExtendInWorldRecipeBuilder extends InWorldRecipeBuilder<ExtendInWorldRecipeBuilder> {
    /**
     * 构造一个新的世界内配方构建器
     *
     * @param trigger    配方触发器
     * @param compatible 是否兼容
     */
    private ExtendInWorldRecipeBuilder(IRecipeTrigger trigger, boolean compatible) {
        super(trigger, compatible);
    }

    /**
     * 创建一个兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 兼容的世界内配方构建器
     */
    public static ExtendInWorldRecipeBuilder compatible(IRecipeTrigger trigger) {
        return new ExtendInWorldRecipeBuilder(trigger, true);
    }

    /**
     * 创建一个兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 兼容的世界内配方构建器
     */
    public static ExtendInWorldRecipeBuilder extendCompatible(Supplier<IRecipeTrigger> trigger) {
        return ExtendInWorldRecipeBuilder.compatible(trigger.get());
    }

    /**
     * 创建一个不兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 不兼容的世界内配方构建器
     */
    public static ExtendInWorldRecipeBuilder incompatible(IRecipeTrigger trigger) {
        return new ExtendInWorldRecipeBuilder(trigger, false);
    }

    /**
     * 创建一个不兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 不兼容的世界内配方构建器
     */
    public static ExtendInWorldRecipeBuilder extendIncompatible(Supplier<IRecipeTrigger> trigger) {
        return ExtendInWorldRecipeBuilder.incompatible(trigger.get());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param consumer HasCauldron构建器消费者
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Consumer<HasCauldron.Builder> consumer) {
        HasCauldron.Builder builder = HasCauldron.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加空炼药锅谓词
     *
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron() {
        return this.with(HasCauldron.builder().empty().offset(this.offset).build());
    }

    /**
     * 添加空炼药锅谓词
     *
     * @param offset 偏移向量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Vec3 offset) {
        return this.with(HasCauldron.builder().empty().offset(offset).build());
    }

    /**
     * 添加空炼药锅谓词
     *
     * @param x X轴偏移量
     * @param y Y轴偏移量
     * @param z Z轴偏移量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(double x, double y, double z) {
        return this.with(HasCauldron.builder().empty().offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param fluid 液体ID
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(ResourceLocation fluid) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(this.offset).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param offset 偏移向量
     * @param fluid  液体ID
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Vec3 offset, ResourceLocation fluid) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(offset).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param fluid 液体ID
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(double x, double y, double z, ResourceLocation fluid) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param fluid   液体ID
     * @param consume 消耗量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(ResourceLocation fluid, int consume) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(this.offset).consume(consume).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param offset  偏移向量
     * @param fluid   液体ID
     * @param consume 消耗量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Vec3 offset, ResourceLocation fluid, int consume) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(offset).consume(consume).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param x       X轴偏移量
     * @param y       Y轴偏移量
     * @param z       Z轴偏移量
     * @param fluid   液体ID
     * @param consume 消耗量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(double x, double y, double z, ResourceLocation fluid, int consume) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(new Vec3(x, y, z)).consume(consume).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param cauldron 炼药锅方块
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Block cauldron) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(this.offset).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param offset   偏移向量
     * @param cauldron 炼药锅方块
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Vec3 offset, Block cauldron) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(offset).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param x        X轴偏移量
     * @param y        Y轴偏移量
     * @param z        Z轴偏移量
     * @param cauldron 炼药锅方块
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(double x, double y, double z, Block cauldron) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param cauldron 炼药锅方块
     * @param consume  消耗量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Block cauldron, int consume) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(this.offset).consume(consume).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param offset   偏移向量
     * @param cauldron 炼药锅方块
     * @param consume  消耗量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(Vec3 offset, Block cauldron, int consume) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(offset).consume(consume).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param x        X轴偏移量
     * @param y        Y轴偏移量
     * @param z        Z轴偏移量
     * @param cauldron 炼药锅方块
     * @param consume  消耗量
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder hasCauldron(double x, double y, double z, Block cauldron, int consume) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(new Vec3(x, y, z)).consume(consume).build());
    }

    /**
     * 添加生成物品结果
     *
     * @param consumer SpawnItem构建器消费者
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder spawnItem(Consumer<SpawnItem.Builder> consumer) {
        SpawnItem.Builder builder = SpawnItem.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.out(builder.build());
    }

    /**
     * 添加生成物品结果
     *
     * @param offset 偏移向量
     * @param chance 生成概率
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder spawnItem(Vec3 offset, double chance, ItemStack stack) {
        return this.out(SpawnItem.builder().offset(offset).count((float) chance).item(stack).build());
    }

    /**
     * 添加生成物品结果
     *
     * @param offset 偏移向量
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder spawnItem(Vec3 offset, ItemStack stack) {
        return this.spawnItem(offset, 1, stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param chance 生成概率
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder spawnItem(double x, double y, double z, double chance, ItemStack stack) {
        return this.spawnItem(new Vec3(x, y, z), chance, stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param stack 物品堆
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder spawnItem(double x, double y, double z, ItemStack stack) {
        return this.spawnItem(new Vec3(x, y, z), stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param stack 物品堆
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder spawnItem(ItemStack stack) {
        return this.spawnItem(this.offset, stack);
    }

    /**
     * 添加产生热量结果
     *
     * @param builder ProduceHeat构建器
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder produceHeat(ProduceHeat.@NotNull Builder builder) {
        this.out(builder.build());
        return this;
    }

    /**
     * 添加损坏铁砧结果
     *
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder damageAnvil() {
        this.out(new DamageAnvil());
        return this;
    }

    /**
     * 设置优先级
     *
     * @param priority 优先级
     * @return 当前构建器实例
     */
    public ExtendInWorldRecipeBuilder priority(Integer priority) {
        this.priority = priority;
        return this;
    }
}
