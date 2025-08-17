package dev.dubhe.anvilcraft.recipe.anvil.wrap.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 表示一个带概率的物品堆栈
 * <p>
 * 该类用于定义在配方中可能出现的物品结果，包含物品堆栈和数量/概率信息
 * </p>
 */
@Getter
public class ChanceItemStack {
    /**
     * 物品堆栈
     */
    private final ItemStack stack;

    /**
     * 数量提供器（可以是固定值或概率分布）
     */
    private final NumberProvider count;

    /**
     * 构造一个带概率的物品堆栈
     *
     * @param stack 物品堆栈
     * @param count 数量提供器
     */
    private ChanceItemStack(ItemStack stack, NumberProvider count) {
        this.stack = stack;
        this.count = count;
    }

    /**
     * 构造一个带概率的物品堆栈
     *
     * @param item       物品持有者
     * @param components 数据组件补丁
     * @param count      数量提供器
     */
    private ChanceItemStack(Holder<Item> item, DataComponentPatch components, NumberProvider count) {
        this(new ItemStack(item, 1, components), count);
    }

    /**
     * 创建一个带数量提供器的ChanceItemStack
     *
     * @param item   物品
     * @param amount 数量提供器
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(ItemLike item, NumberProvider amount) {
        return new ChanceItemStack(new ItemStack(item, 1), amount);
    }

    /**
     * 创建一个带固定数量的ChanceItemStack
     *
     * @param item  物品
     * @param count 数量
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(ItemLike item, int count) {
        return new ChanceItemStack(new ItemStack(item, 1), ConstantValue.exactly(count));
    }

    /**
     * 创建一个带数量提供器的ChanceItemStack
     *
     * @param stack  物品堆栈
     * @param amount 数量提供器
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(@NotNull ItemStack stack, NumberProvider amount) {
        return new ChanceItemStack(stack.copyWithCount(1), amount);
    }

    /**
     * 创建一个带固定数量的ChanceItemStack
     *
     * @param stack 物品堆栈
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(@NotNull ItemStack stack) {
        return new ChanceItemStack(stack.copyWithCount(1), ConstantValue.exactly(stack.getCount()));
    }

    /**
     * 创建一个带固定数量的ChanceItemStack
     *
     * @param stack 物品堆栈
     * @param count 数量
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(@NotNull ItemStack stack, int count) {
        return new ChanceItemStack(stack.copyWithCount(1), ConstantValue.exactly(count));
    }

    /**
     * 创建一个带二项分布概率的ChanceItemStack
     *
     * @param stack  物品堆栈
     * @param count  数量
     * @param chance 概率
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(@NotNull ItemStack stack, int count, float chance) {
        return new ChanceItemStack(stack.copyWithCount(1), BinomialDistributionGenerator.binomial(count, chance));
    }

    /**
     * 创建一个带二项分布概率的ChanceItemStack
     *
     * @param stack  物品堆栈
     * @param chance 概率
     * @return ChanceItemStack实例
     */
    public static @NotNull ChanceItemStack of(@NotNull ItemStack stack, float chance) {
        return new ChanceItemStack(stack.copyWithCount(1), BinomialDistributionGenerator.binomial(stack.getCount(), chance));
    }

    /**
     * ChanceItemStack的编解码器
     */
    public static final Codec<ChanceItemStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.ITEM_NON_AIR_CODEC
            .fieldOf("id")
            .forGetter(ChanceItemStack::getItemHolder),
        DataComponentPatch.CODEC
            .optionalFieldOf("components", DataComponentPatch.EMPTY)
            .forGetter(ChanceItemStack::getComponentsPatch),
        CodecUtil.NUMBER_PROVIDER_CODEC
            .optionalFieldOf("count", ConstantValue.exactly(1.0f))
            .forGetter(ChanceItemStack::getCount)
    ).apply(instance, ChanceItemStack::new));

    /**
     * ChanceItemStack的网络流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceItemStack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull ChanceItemStack value) {
            ItemStack.STREAM_CODEC.encode(buffer, value.stack);
            RecipeUtil.toNetwork(buffer, value.count);
        }

        @Override
        public @NotNull ChanceItemStack decode(@NotNull RegistryFriendlyByteBuf buffer) {
            ItemStack decode = ItemStack.STREAM_CODEC.decode(buffer);
            NumberProvider count = RecipeUtil.fromNetwork(buffer);
            return new ChanceItemStack(decode, count);
        }
    };

    /**
     * 获取物品
     *
     * @return 物品
     */
    public @NotNull Item getItem() {
        return this.stack.getItem();
    }

    /**
     * 获取物品持有者
     *
     * @return 物品持有者
     */
    public Holder<Item> getItemHolder() {
        return this.stack.getItemHolder();
    }

    /**
     * 获取最大数量
     *
     * @return 最大数量
     */
    public int getMaxCount() {
        return (int) Math.round(RecipeUtil.getExpectedValue(this.count));
    }

    /**
     * 获取数据组件补丁
     *
     * @return 数据组件补丁
     */
    public DataComponentPatch getComponentsPatch() {
        DataComponentMap components = this.stack.getComponents();
        if (components instanceof PatchedDataComponentMap patched) return patched.asPatch();
        else return DataComponentPatch.EMPTY;
    }

    /**
     * 将此ChanceItemStack转换为SpawnItem结果
     *
     * @param offset 偏移量
     * @return SpawnItem结果
     */
    public SpawnItem toSpawnItem(Vec3 offset) {
        return SpawnItem.builder().item(this.stack).count(this.count).offset(offset).build();
    }
}