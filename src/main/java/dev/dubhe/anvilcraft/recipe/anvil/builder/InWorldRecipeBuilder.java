package dev.dubhe.anvilcraft.recipe.anvil.builder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceHeat;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlock;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasBlockIngredient;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.HasItem;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.HasItemIngredient;
import lombok.EqualsAndHashCode;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 世界内配方构建器
 * <p>
 * 用于构建在世界中使用的铁砧配方，支持各种触发器、谓词和结果。
 * 可以设置配方的触发条件、前置条件、冲突条件和结果操作等。
 * </p>
 */
@EqualsAndHashCode
@SuppressWarnings("unused")
public class InWorldRecipeBuilder implements RecipeBuilder {
    /**
     * 配方图标
     */
    private final NonNullList<ItemStack> icon = NonNullList.withSize(1, Items.ANVIL.getDefaultInstance());
    /**
     * 偏移量
     */
    private Vec3 offset = Vec3.ZERO;
    /**
     * 配方触发器
     */
    private final @NotNull IRecipeTrigger trigger;
    /**
     * 冲突的配方谓词列表
     */
    private final List<IRecipePredicate<?>> conflicting = new ArrayList<>();
    /**
     * 非冲突的配方谓词列表
     */
    private final List<IRecipePredicate<?>> nonConflicting = new ArrayList<>();
    /**
     * 配方结果列表
     */
    private final List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
    /**
     * 是否兼容
     */
    private final boolean compatible;
    /**
     * 优先级
     */
    private Integer priority = null;
    /**
     * 配方组
     */
    private String group;
    /**
     * 准则映射
     */
    private final Map<String, Criterion<?>> criteria = Maps.newLinkedHashMap();

    /**
     * 构造一个新的世界内配方构建器
     *
     * @param trigger    配方触发器
     * @param compatible 是否兼容
     */
    private InWorldRecipeBuilder(@NotNull IRecipeTrigger trigger, boolean compatible) {
        this.trigger = trigger;
        this.compatible = compatible;
    }

    /**
     * 创建一个兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 兼容的世界内配方构建器
     */
    public static @NotNull InWorldRecipeBuilder compatible(@NotNull IRecipeTrigger trigger) {
        return new InWorldRecipeBuilder(trigger, true);
    }

    /**
     * 创建一个不兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 不兼容的世界内配方构建器
     */
    public static @NotNull InWorldRecipeBuilder incompatible(@NotNull IRecipeTrigger trigger) {
        return new InWorldRecipeBuilder(trigger, false);
    }

    /**
     * 设置配方图标
     *
     * @param icon 配方图标物品堆
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder icon(@NotNull ItemStack icon) {
        this.icon.set(0, icon);
        return this;
    }

    /**
     * 添加配方谓词
     *
     * @param predicate 配方谓词
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder with(@NotNull IRecipePredicate<?> predicate) {
        if (predicate.getType().conflict()) {
            this.conflicting.add(predicate);
        } else {
            this.nonConflicting.add(predicate);
        }
        return this;
    }

    /**
     * 设置偏移量
     *
     * @param offset 偏移向量
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder offset(Vec3 offset) {
        this.offset = offset;
        return this;
    }

    /**
     * 设置偏移量
     *
     * @param x X轴偏移量
     * @param y Y轴偏移量
     * @param z Z轴偏移量
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder offset(double x, double y, double z) {
        return this.offset(new Vec3(x, y, z));
    }

    /**
     * 设置向下偏移量
     *
     * @param below 向下偏移距离
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder below(double below) {
        return this.offset(Vec3.ZERO.subtract(0, below, 0));
    }

    /**
     * 设置向下偏移1格
     *
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder below() {
        return this.below(1);
    }

    /**
     * 设置向上偏移量
     *
     * @param above 向上偏移距离
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder above(double above) {
        return this.offset(Vec3.ZERO.add(0, above, 0));
    }

    /**
     * 设置向上偏移1格
     *
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder above() {
        return this.above(1);
    }

    /**
     * 添加物品谓词
     *
     * @param consumer HasItem构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(@NotNull Consumer<HasItem.Builder> consumer) {
        HasItem.Builder builder = HasItem.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加物品谓词
     *
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(ItemLike... items) {
        return this.with(HasItem.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param offset 偏移向量
     * @param items  物品列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(Vec3 offset, ItemLike... items) {
        return this.with(HasItem.builder().of(items).offset(offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(double x, double y, double z, ItemLike... items) {
        return this.with(HasItem.builder().of(items).offset(x, y, z).build());
    }

    /**
     * 添加物品谓词
     *
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(TagKey<Item> items) {
        return this.with(HasItem.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param offset 偏移向量
     * @param items  物品标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(Vec3 offset, TagKey<Item> items) {
        return this.with(HasItem.builder().offset(offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItem(double x, double y, double z, TagKey<Item> items) {
        return this.with(HasItem.builder().offset(x, y, z).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param consumer HasItemIngredient构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(@NotNull Consumer<HasItemIngredient.Builder> consumer) {
        HasItemIngredient.Builder builder = HasItemIngredient.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(ItemLike... items) {
        return this.with(HasItemIngredient.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param offset 偏移向量
     * @param items  物品列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(Vec3 offset, ItemLike... items) {
        return this.with(HasItemIngredient.builder().of(items).offset(offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(double x, double y, double z, ItemLike... items) {
        return this.with(HasItemIngredient.builder().of(items).offset(x, y, z).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(TagKey<Item> items) {
        return this.with(HasItemIngredient.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param offset 偏移向量
     * @param items  物品标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(Vec3 offset, TagKey<Item> items) {
        return this.with(HasItemIngredient.builder().of(items).offset(offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasItemIngredient(double x, double y, double z, TagKey<Item> items) {
        return this.with(HasItemIngredient.builder().of(items).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param consumer HasBlock构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(@NotNull Consumer<HasBlock.Builder> consumer) {
        HasBlock.Builder builder = HasBlock.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加方块谓词
     *
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(Block... blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(Vec3 offset, Block... blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(double x, double y, double z, Block... blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(Collection<Block> blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(Vec3 offset, Collection<Block> blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(double x, double y, double z, Collection<Block> blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(TagKey<Block> tag) {
        return this.with(HasBlock.builder().of(tag).offset(this.offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param tag    方块标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(Vec3 offset, TagKey<Block> tag) {
        return this.with(HasBlock.builder().of(tag).offset(offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param x   X轴偏移量
     * @param y   Y轴偏移量
     * @param z   Z轴偏移量
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlock(double x, double y, double z, TagKey<Block> tag) {
        return this.with(HasBlock.builder().of(tag).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param state  方块状态
     * @param <T>    属性类型
     * @return 当前构建器实例
     */
    public <T extends Comparable<T>> InWorldRecipeBuilder hasBlock(Vec3 offset, @NotNull BlockState state) {
        HasBlock.Builder builder = HasBlock.builder();
        Block block = state.getBlock();
        builder.of(block);
        builder.offset(offset);
        BlockState defaultState = block.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = state.getValue(property);
            Comparable<?> defaultValue = defaultState.getValue(property);
            if (value.equals(defaultValue)) continue;
            //noinspection unchecked
            builder.with((Property<T>) property, (T) value);
        }
        return this.with(builder.build());
    }

    /**
     * 添加方块谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param state 方块状态
     * @param <T>   属性类型
     * @return 当前构建器实例
     */
    public <T extends Comparable<T>> InWorldRecipeBuilder hasBlock(double x, double y, double z, @NotNull BlockState state) {
        return this.hasBlock(new Vec3(x, y, z), state);
    }

    /**
     * 添加方块谓词
     *
     * @param state 方块状态
     * @param <T>   属性类型
     * @return 当前构建器实例
     */
    public <T extends Comparable<T>> InWorldRecipeBuilder hasBlock(@NotNull BlockState state) {
        return this.hasBlock(this.offset, state);
    }

    /**
     * 添加方块原料谓词
     *
     * @param consumer HasBlockIngredient构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(@NotNull Consumer<HasBlockIngredient.Builder> consumer) {
        HasBlockIngredient.Builder builder = HasBlockIngredient.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(Block... blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(Vec3 offset, Block... blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(double x, double y, double z, Block... blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(Collection<Block> blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(Vec3 offset, Collection<Block> blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(double x, double y, double z, Collection<Block> blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(new Vec3(x, y, z)).build());
    }


    /**
     * 添加方块原料谓词
     *
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(TagKey<Block> tag) {
        return this.with(HasBlockIngredient.builder().of(tag).offset(this.offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param tag    方块标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(Vec3 offset, TagKey<Block> tag) {
        return this.with(HasBlockIngredient.builder().of(tag).offset(offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x   X轴偏移量
     * @param y   Y轴偏移量
     * @param z   Z轴偏移量
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasBlockIngredient(double x, double y, double z, TagKey<Block> tag) {
        return this.with(HasBlockIngredient.builder().of(tag).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param state  方块状态
     * @param <T>    属性类型
     * @return 当前构建器实例
     */
    public <T extends Comparable<T>> InWorldRecipeBuilder hasBlockIngredient(Vec3 offset, @NotNull BlockState state) {
        HasBlockIngredient.Builder builder = HasBlockIngredient.builder();
        Block block = state.getBlock();
        BlockState defaultState = block.defaultBlockState();
        builder.of(block);
        builder.offset(offset);
        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = state.getValue(property);
            if (value.equals(defaultState.getValue(property))) continue;
            //noinspection unchecked
            builder.with((Property<T>) property, (T) value);
        }
        return this.with(builder.build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param state 方块状态
     * @param <T>   属性类型
     * @return 当前构建器实例
     */
    public <T extends Comparable<T>> InWorldRecipeBuilder hasBlockIngredient(double x, double y, double z, @NotNull BlockState state) {
        return this.hasBlockIngredient(new Vec3(x, y, z), state);
    }

    /**
     * 添加方块原料谓词
     *
     * @param state 方块状态
     * @param <T>   属性类型
     * @return 当前构建器实例
     */
    public <T extends Comparable<T>> InWorldRecipeBuilder hasBlockIngredient(@NotNull BlockState state) {
        return this.hasBlockIngredient(this.offset, state);
    }

    /**
     * 添加炼药锅谓词
     *
     * @param consumer HasCauldron构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(@NotNull Consumer<HasCauldron.Builder> consumer) {
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
    public InWorldRecipeBuilder hasCauldron() {
        return this.with(HasCauldron.builder().empty().offset(this.offset).build());
    }

    /**
     * 添加空炼药锅谓词
     *
     * @param offset 偏移向量
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(Vec3 offset) {
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
    public InWorldRecipeBuilder hasCauldron(double x, double y, double z) {
        return this.with(HasCauldron.builder().empty().offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param fluid 液体ID
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(ResourceLocation fluid) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(this.offset).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param offset 偏移向量
     * @param fluid  液体ID
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(Vec3 offset, ResourceLocation fluid) {
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
    public InWorldRecipeBuilder hasCauldron(double x, double y, double z, ResourceLocation fluid) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param fluid   液体ID
     * @param consume 消耗量
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(ResourceLocation fluid, int consume) {
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
    public InWorldRecipeBuilder hasCauldron(Vec3 offset, ResourceLocation fluid, int consume) {
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
    public InWorldRecipeBuilder hasCauldron(double x, double y, double z, ResourceLocation fluid, int consume) {
        return this.with(HasCauldron.builder().fluid(fluid).offset(new Vec3(x, y, z)).consume(consume).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param cauldron 炼药锅方块
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(Block cauldron) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(this.offset).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param offset   偏移向量
     * @param cauldron 炼药锅方块
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(Vec3 offset, Block cauldron) {
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
    public InWorldRecipeBuilder hasCauldron(double x, double y, double z, Block cauldron) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加炼药锅谓词
     *
     * @param cauldron 炼药锅方块
     * @param consume  消耗量
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder hasCauldron(Block cauldron, int consume) {
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
    public InWorldRecipeBuilder hasCauldron(Vec3 offset, Block cauldron, int consume) {
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
    public InWorldRecipeBuilder hasCauldron(double x, double y, double z, Block cauldron, int consume) {
        return this.with(HasCauldron.builder().cauldron(cauldron).offset(new Vec3(x, y, z)).consume(consume).build());
    }

    /**
     * 添加配方结果
     *
     * @param outcome 配方结果
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder out(@NotNull IRecipeOutcome<?> outcome) {
        this.outcomes.add(outcome);
        return this;
    }

    /**
     * 添加生成物品结果
     *
     * @param consumer SpawnItem构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder spawnItem(@NotNull Consumer<SpawnItem.Builder> consumer) {
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
    public InWorldRecipeBuilder spawnItem(Vec3 offset, double chance, ItemStack stack) {
        return this.out(SpawnItem.builder().offset(offset).count((float) chance).item(stack).build());
    }

    /**
     * 添加生成物品结果
     *
     * @param offset 偏移向量
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder spawnItem(Vec3 offset, ItemStack stack) {
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
    public InWorldRecipeBuilder spawnItem(double x, double y, double z, double chance, ItemStack stack) {
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
    public InWorldRecipeBuilder spawnItem(double x, double y, double z, ItemStack stack) {
        return this.spawnItem(new Vec3(x, y, z), stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param stack 物品堆
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder spawnItem(ItemStack stack) {
        return this.spawnItem(this.offset, stack);
    }

    /**
     * 添加设置方块结果
     *
     * @param consumer SetBlock构建器消费者
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder setBlock(@NotNull Consumer<SetBlock.Builder> consumer) {
        SetBlock.Builder builder = SetBlock.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.out(builder.build());
    }

    /**
     * 添加设置方块结果
     *
     * @param offset 偏移向量
     * @param chance 设置概率
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder setBlock(Vec3 offset, double chance, @NotNull BlockState state) {
        return this.out(SetBlock.builder().block(state).offset(offset).chance((float) chance).build());
    }

    /**
     * 添加设置方块结果
     *
     * @param offset 偏移向量
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder setBlock(Vec3 offset, @NotNull BlockState state) {
        return this.setBlock(offset, 1, state);
    }

    /**
     * 添加设置方块结果
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param chance 设置概率
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder setBlock(double x, double y, double z, double chance, @NotNull BlockState state) {
        return this.setBlock(new Vec3(x, y, z), chance, state);
    }

    /**
     * 添加设置方块结果
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder setBlock(double x, double y, double z, @NotNull BlockState state) {
        return this.setBlock(new Vec3(x, y, z), state);
    }

    /**
     * 添加设置方块结果
     *
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder setBlock(@NotNull BlockState state) {
        return this.setBlock(this.offset, state);
    }

    /**
     * 添加产生热量结果
     *
     * @param builder ProduceHeat构建器
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder produceHeat(ProduceHeat.@NotNull Builder builder) {
        this.out(builder.build());
        return this;
    }

    /**
     * 添加损坏铁砧结果
     *
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder damageAnvil() {
        this.out(new DamageAnvil());
        return this;
    }

    /**
     * 设置优先级
     *
     * @param priority 优先级
     * @return 当前构建器实例
     */
    public InWorldRecipeBuilder priority(Integer priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 构建世界内配方
     *
     * @return 世界内配方
     */
    public InWorldRecipe build() {
        return new InWorldRecipe(
            this.icon.getFirst(),
            this.trigger,
            ImmutableList.copyOf(this.conflicting),
            ImmutableList.copyOf(this.nonConflicting),
            ImmutableList.copyOf(this.outcomes),
            Objects.requireNonNullElseGet(
                this.priority,
                () -> InWorldRecipe.calcPriority(this.trigger, this.conflicting, this.nonConflicting, this.outcomes)
            ),
            this.compatible
        );
    }

    @Override
    public @NotNull InWorldRecipeBuilder unlockedBy(@NotNull String name, @NotNull Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public @NotNull InWorldRecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    @Override
    public @NotNull Item getResult() {
        return this.icon.getFirst().getItem();
    }

    @Override
    public void save(@NotNull RecipeOutput recipeOutput, @NotNull ResourceLocation id) {
        Advancement.Builder builder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        Objects.requireNonNull(builder);
        this.criteria.forEach(builder::addCriterion);
        InWorldRecipe recipe = this.build();
        recipeOutput.accept(
            ResourceLocation.fromNamespaceAndPath(id.getNamespace(), this.group + "/" + id.getPath()),
            recipe,
            builder.build(id.withPrefix("recipes/" + this.group + "/"))
        );
    }
}
