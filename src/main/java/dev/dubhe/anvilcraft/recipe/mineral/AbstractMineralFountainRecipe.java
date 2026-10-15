package dev.dubhe.anvilcraft.recipe.mineral;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * 矿物涌泉配方的公共部分：高度范围、世界类型、优先级、权重、材料方块与结果方块。
 *
 * <p>三类配方（被动 / 主动 / 替换）共享这些字段，只有额外的匹配条件与执行时机不同。</p>
 */
@Getter
public abstract class AbstractMineralFountainRecipe implements Recipe<MineralFountainRecipeInput> {
    /** 材料方块的默认值：空气，即「上方为空」时也可生效。 */
    public static final BlockStatePredicate DEFAULT_FROM = BlockStatePredicate.builder().of(Blocks.AIR).build();
    /** 权重默认值：同优先级并列时等概率。 */
    public static final int DEFAULT_WEIGHT = 1;

    /** 生效的世界类型；为空表示任意世界。 */
    private final Optional<ResourceLocation> dimension;
    /** 生效的高度范围。 */
    private final HeightRange height;
    /** 优先级，越大越先尝试。 */
    private final int priority;
    /** 同优先级并列时的抽取权重，越大越容易被选中。 */
    private final int weight;
    /** 上方待转化的材料方块；为空谓词表示任意方块（即空气时也可用）。 */
    private final BlockStatePredicate fromBlock;
    /** 结果方块，必填。 */
    private final ChanceBlockState toBlock;
    /** 满足条件时提供的加热能力；为空表示不加热。 */
    private final Optional<MineralFountainHeat> heat;
    /**
     * 上方为流体容器时的流体产出；为空表示只产出方块。
     *
     * <p>与 {@link #heat} 同为「只看上下文与周围方块」的产出，不受材料方块限制：
     * 上方放的是容器而非空气，材料方块条件天然不成立。</p>
     */
    private final Optional<FluidStack> toFluid;

    protected AbstractMineralFountainRecipe(
        Optional<ResourceLocation> dimension,
        HeightRange height,
        int priority,
        int weight,
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        Optional<MineralFountainHeat> heat,
        Optional<FluidStack> toFluid
    ) {
        this.dimension = dimension;
        this.height = height;
        this.priority = priority;
        this.weight = weight;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.heat = heat;
        this.toFluid = toFluid;
    }

    /**
     * 只判定世界类型与高度范围，不含材料方块。
     */
    protected boolean matchesContext(MineralFountainRecipeInput input, Level level) {
        if (this.dimension.isPresent() && !this.dimension.get().equals(level.dimension().location())) {
            return false;
        }
        return this.height.test(level, input.pos());
    }

    /**
     * 三类配方共有的匹配条件：世界类型、高度范围与材料方块。
     */
    protected boolean matchesCommon(MineralFountainRecipeInput input, Level level) {
        return this.matchesContext(input, level) && this.fromBlock.testWithoutEntity(input.from());
    }

    /**
     * 结果方块的抽取概率。
     */
    public double chance(ServerLevel level) {
        return this.toBlock.chance().getFloat(RecipeUtil.emptyLootContext(level));
    }

    /**
     * 在一组同优先级的候选中按权重随机取一个。
     *
     * <p>权重非正的条目视为不参与抽取；若全部非正则退回等概率，
     * 避免抽取失败导致配方整体不产出。</p>
     *
     * @param random 随机源
     * @param picked 候选列表，调用方需保证非空
     * @param weight 权重取值函数
     * @return 被选中的候选
     */
    public static <T> T pickWeighted(
        RandomSource random,
        List<T> picked,
        ToIntFunction<T> weight
    ) {
        if (picked.size() == 1) return picked.getFirst();
        long total = 0;
        for (T candidate : picked) {
            total += Math.max(0, weight.applyAsInt(candidate));
        }
        if (total <= 0) return picked.get(random.nextInt(picked.size()));
        // 权重之和可能超出 int，这里用 double 抽取，避免溢出
        double roll = random.nextDouble() * total;
        for (T candidate : picked) {
            roll -= Math.max(0, weight.applyAsInt(candidate));
            if (roll < 0) return candidate;
        }
        return picked.getLast();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack assemble(MineralFountainRecipeInput input, HolderLookup.Provider registries) {
        return this.getResultItem(registries);
    }

    @Override
    public ItemStack getResultItem(@Nullable HolderLookup.Provider registries) {
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.toBlock.state().getBlock());
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
